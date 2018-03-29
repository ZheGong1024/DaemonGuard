package com.daemonguard.lib.service;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import com.daemonguard.lib.Daemon;
import com.daemonguard.lib.DaemonConfig;

public abstract class DaemonWorkerService extends Service {

  protected boolean mFirstStarted = true;

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
      //getPackageManager().setComponentEnabledSetting(
      //    new ComponentName(getPackageName(), DaemonService.class.getName()),
      //    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
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
