package com.daemonguard.lib.service;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import com.daemonguard.lib.Daemon;
import com.daemonguard.lib.DaemonConfig;
import com.daemonguard.lib.WakeUpReceiver;

public abstract class DaemonWorkService extends Service {
  protected boolean mFirstStarted = true;
  private boolean shouldStopService = false;

  /**
   * 用于在不需要服务运行的时候取消 Job / Alarm / Subscription.
   */
  public static void cancelJobAlarmSub() {
    if (!Daemon.getInstance().isInitialized) return;
    Daemon.getInstance().mApplication.sendBroadcast(
        new Intent(WakeUpReceiver.ACTION_CANCEL_JOB_ALARM_SUB));
  }

  /**
   * 是否 任务完成, 不再需要服务运行?
   *
   * @return 应当停止服务, true; 应当启动服务, false; 无法判断, 什么也不做, null.
   */
  public boolean shouldStopService() {
    return shouldStopService;
  }

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
    Daemon.getInstance().startServiceMayBind(DaemonService.class);

    //业务逻辑: 实际使用时，根据需求，将这里更改为自定义的条件，判定服务应当启动还是停止 (任务是否需要运行)
    boolean shouldStopService = shouldStopService();
    if (shouldStopService) {
      stopDaemon();
    }

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
      getPackageManager().setComponentEnabledSetting(
          new ComponentName(getPackageName(), DaemonService.class.getName()),
          PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    return START_STICKY;
  }

  /**
   * 停止服务并取消定时唤醒
   *
   * 停止服务使用取消订阅的方式实现，而不是调用 Context.stopDaemon(Intent name)。因为：
   * 1.stopDaemon 会调用 Service.onDestroy()，而 AbsWorkService 做了保活处理，会把 Service 再拉起来；
   * 2.我们希望 AbsWorkService 起到一个类似于控制台的角色，即 AbsWorkService 始终运行 (无论任务是否需要运行)，
   * 而是通过 onStart() 里自定义的条件，来决定服务是否应当启动或停止。
   */
  public void stopDaemon() {
    shouldStopService = true;
    //取消 Job / Alarm / Subscription
    cancelJobAlarmSub();
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    return onStart();
  }

  @Nullable @Override public IBinder onBind(Intent intent) {
    onStart();
    return onBind(intent, null);
  }

  protected void onEnd(Intent rootIntent) {
    if (!Daemon.getInstance().isInitialized) return;
    Daemon.getInstance().startServiceMayBind(Daemon.getInstance().mWorkService);
    Daemon.getInstance().startServiceMayBind(DaemonService.class);
  }

  /**
   * 最近任务列表中划掉卡片时回调
   */
  @Override public void onTaskRemoved(Intent rootIntent) {
    onEnd(rootIntent);
  }

  /**
   * 设置-正在运行中停止服务时回调
   */
  @Override public void onDestroy() {
    onEnd(null);
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
