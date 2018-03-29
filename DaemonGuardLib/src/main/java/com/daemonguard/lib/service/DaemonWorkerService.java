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

import static com.daemonguard.lib.DaemonConfig.FOREGROUND_SERVICE_HASH_CODE;

public abstract class DaemonWorkerService extends Service {

  protected boolean mFirstStarted = true;

  protected static PendingIntent sPendingIntent;

  @Nullable public abstract IBinder onBind(Intent intent, Void alwaysNull);

  /**
   * 1.防止重复启动，可以任意调用 DaemonEnv.startServiceMayBind(Class serviceClass);
   * 2.利用漏洞启动前台服务而不显示通知;
   * 3.在子线程中运行定时任务，处理了运行前检查和销毁时保存的问题;
   * 4.启动守护服务;
   * 5.守护 Service 组件的启用状态, 使其不被 MAT 等工具禁用.
   */
  protected int onStart() {
    //启动守护服务，运行在:watch子进程中
    //Daemon.getInstance().startServiceMayBind(DaemonService.class);
    Log.d(Daemon.TAG, "DaemonWorkerService onStart. API=" + Build.VERSION.SDK_INT);
    if (mFirstStarted) {
      mFirstStarted = false;
      //启动前台服务而不显示通知的漏洞已在 API Level 25 修复，大快人心！
      if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
        //利用漏洞在 API Level 17 及以下的 Android 系统中，启动前台服务而不显示通知
        startForeground(DaemonConfig.WORKER_SERVICE_HASH_CODE, new Notification());
        //利用漏洞在 API Level 18 及以上的 Android 系统中，启动前台服务而不显示通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
          Daemon.getInstance()
              .startServiceSafely(
                  new Intent(getApplication(), DaemonWorkerNotificationService.class));
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
        sPendingIntent = PendingIntent.getService(Daemon.getInstance().mApplication,
            FOREGROUND_SERVICE_HASH_CODE, i, PendingIntent.FLAG_UPDATE_CURRENT);
        if (am != null) {
          am.setRepeating(AlarmManager.RTC_WAKEUP,
              System.currentTimeMillis() + DaemonConfig.WAKEUP_INTERVAL,
              DaemonConfig.WAKEUP_INTERVAL, sPendingIntent);
        }
      }
    }

    return START_STICKY;
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    return onStart();
  }

  @Nullable @Override public IBinder onBind(Intent intent) {
    onStart();
    return onBind(intent, null);
  }

  protected void onEnd() {
    if (!Daemon.getInstance().isInitialized) return;
    if (Daemon.getInstance().isDaemonOpen()) {
      Log.d(Daemon.TAG, "DaemonWorkerService onEnd. Daemon is open. Restart services.");
      Daemon.getInstance().startServiceMayBind(Daemon.getInstance().mWorkService);
      //Daemon.getInstance().startServiceMayBind(DaemonService.class);
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
  }

  public static class DaemonWorkerNotificationService extends Service {

    /**
     * 利用漏洞在 API Level 18 及以上的 Android 系统中，启动前台服务而不显示通知
     */
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
      startForeground(DaemonConfig.WORKER_SERVICE_HASH_CODE, new Notification());
      stopSelf();
      return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) {
      return null;
    }
  }
}
