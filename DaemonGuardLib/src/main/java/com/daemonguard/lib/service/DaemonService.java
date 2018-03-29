package com.daemonguard.lib.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import com.daemonguard.lib.Daemon;
import com.daemonguard.lib.DaemonConfig;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import java.util.concurrent.TimeUnit;

import static com.daemonguard.lib.DaemonConfig.FOREGROUND_SERVICE_HASH_CODE;

public class DaemonService extends Service {

  protected static Disposable sDisposable;

  protected static PendingIntent sPendingIntent;

  /**
   * Daemon service.
   */
  protected final void onStart() {
    if (!Daemon.getInstance().isInitialized) return;
    Log.d(Daemon.TAG, "DaemonService onStart. API=" + Build.VERSION.SDK_INT);
    if (sDisposable != null && !sDisposable.isDisposed()) return;

    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
      startForeground(FOREGROUND_SERVICE_HASH_CODE, new Notification());
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        Daemon.getInstance()
            .startServiceSafely(
                new Intent(Daemon.getInstance().mApplication, DaemonNotificationService.class));
      }
    }

    //定时检查 AbsWorkService 是否在运行，如果不在运行就把它拉起来
    //Android 5.0+ 使用 JobScheduler，效果比 AlarmManager 好
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      JobInfo.Builder builder = new JobInfo.Builder(FOREGROUND_SERVICE_HASH_CODE,
          new ComponentName(Daemon.getInstance().mApplication, DaemonJobService.class));
      builder.setPeriodic(DaemonConfig.WAKEUP_INTERVAL);
      //Android 7.0+ 增加了一项针对 JobScheduler 的新限制，最小间隔只能是下面设定的数字
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        builder.setPeriodic(JobInfo.getMinPeriodMillis(), JobInfo.getMinFlexMillis());
      }
      builder.setPersisted(true);
      JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
      if (scheduler != null) scheduler.schedule(builder.build());
    } else {
      //Android 4.4- 使用 AlarmManager
      AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
      Intent i = new Intent(Daemon.getInstance().mApplication, Daemon.getInstance().mWorkService);
      sPendingIntent =
          PendingIntent.getService(Daemon.getInstance().mApplication, FOREGROUND_SERVICE_HASH_CODE,
              i, PendingIntent.FLAG_UPDATE_CURRENT);
      if (am != null) {
        am.setRepeating(AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + DaemonConfig.WAKEUP_INTERVAL, DaemonConfig.WAKEUP_INTERVAL,
            sPendingIntent);
      }
    }

    //使用定时 Observable，避免 Android 定制系统 JobScheduler / AlarmManager 唤醒间隔不稳定的情况
    sDisposable = Observable.interval(DaemonConfig.WAKEUP_INTERVAL, TimeUnit.MILLISECONDS)
        .subscribe(new Consumer<Long>() {
          @Override public void accept(Long aLong) throws Exception {
            Daemon.getInstance().startServiceMayBind(Daemon.getInstance().mWorkService);
          }
        }, new Consumer<Throwable>() {
          @Override public void accept(Throwable throwable) throws Exception {
            throwable.printStackTrace();
          }
        });

    //守护 Service 组件的启用状态, 使其不被 MAT 等工具禁用
    getPackageManager().setComponentEnabledSetting(
        new ComponentName(getPackageName(), Daemon.getInstance().mWorkService.getName()),
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
  }

  @Nullable @Override public IBinder onBind(Intent intent) {
    onStart();
    return null;
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    onStart();
    return super.onStartCommand(intent, flags, startId);
  }

  protected void onEnd() {
    if (!Daemon.getInstance().isInitialized) return;
    if (Daemon.getInstance().isDaemonOpen()) {
      Log.d(Daemon.TAG, "DaemonService onEnd. Daemon is open. Restart services.");
      Daemon.getInstance().startServiceMayBind(Daemon.getInstance().mWorkService);
      Daemon.getInstance().startServiceMayBind(DaemonService.class);
    }
  }

  /**
   * 最近任务列表中划掉卡片时回调
   */
  @Override public void onTaskRemoved(Intent rootIntent) {
    onEnd();
  }

  /**
   * 设置-正在运行中停止服务时回调
   */
  @Override public void onDestroy() {
    onEnd();
  }

  /**
   * 用于在不需要服务运行的时候取消 Job / Alarm / Subscription.
   *
   * 因 WatchDogService 运行在 :watch 子进程, 请勿在主进程中直接调用此方法.
   * 而是向 WakeUpReceiver 发送一个 Action 为 WakeUpReceiver.ACTION_CANCEL_JOB_ALARM_SUB 的广播.
   */
  public static void cancelJobAlarmSub() {
    if (!Daemon.getInstance().isInitialized) return;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      JobScheduler jobScheduler =
          (JobScheduler) Daemon.getInstance().mApplication.getSystemService(JOB_SCHEDULER_SERVICE);
      if (jobScheduler != null) jobScheduler.cancel(DaemonConfig.FOREGROUND_SERVICE_HASH_CODE);
    } else {
      AlarmManager alarmManager =
          (AlarmManager) Daemon.getInstance().mApplication.getSystemService(ALARM_SERVICE);
      if (alarmManager != null) if (sPendingIntent != null) alarmManager.cancel(sPendingIntent);
    }
    if (sDisposable != null) sDisposable.dispose();
  }

  public static class DaemonNotificationService extends Service {

    /**
     * 利用漏洞在 API Level 18 及以上的 Android 系统中，启动前台服务而不显示通知
     * 运行在:watch子进程中
     */
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
      startForeground(DaemonConfig.FOREGROUND_SERVICE_HASH_CODE, new Notification());
      stopSelf();
      return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) {
      return null;
    }
  }
}
