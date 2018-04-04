package com.daemonguard.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WakeUpReceiver extends BroadcastReceiver {

  /**
   * 向 WakeUpReceiver 发送带有此 Action 的广播, 即可在不需要服务运行的时候取消 Job / Alarm / Subscription.
   */
  public static final String ACTION_CANCEL_JOB_ALARM_SUB =
      "com.daemonguard.lib.CANCEL_JOB_ALARM_SUB";

  /**
   * 监听 8 种系统广播 :
   * CONNECTIVITY\_CHANGE, USER\_PRESENT, ACTION\_POWER\_CONNECTED, ACTION\_POWER\_DISCONNECTED,
   * BOOT\_COMPLETED, MEDIA\_MOUNTED, PACKAGE\_ADDED, PACKAGE\_REMOVED.
   * 在网络连接改变, 用户屏幕解锁, 电源连接 / 断开, 系统启动完成, 挂载 SD 卡, 安装 / 卸载软件包时拉起 Service.
   * Service 内部做了判断，若 Service 已在运行，不会重复启动.
   * 运行在:watch子进程中.
   */
  @Override public void onReceive(Context context, Intent intent) {
    if (intent != null && ACTION_CANCEL_JOB_ALARM_SUB.equals(intent.getAction())) {
      Daemon.getInstance().cancelJobAlarmSub();
      return;
    }
    if (!Daemon.getInstance().isInitialized) return;
    if (Daemon.getInstance().isDaemonOpen()) {
      Log.d(Daemon.TAG, "WakeUPReceiver onReceive. Daemon is open, start worker service.");
      Daemon.getInstance().startServiceMayBind(Daemon.getInstance().mWorkService);
    }
  }

  public static class WakeUpAutoStartReceiver extends BroadcastReceiver {

    @Override public void onReceive(Context context, Intent intent) {
      if (!Daemon.getInstance().isInitialized) return;
      if (Daemon.getInstance().isDaemonOpen()) {
        Log.d(Daemon.TAG,
            "WakeUpAutoStartReceiver onReceive. Daemon is open, start worker service.");
        Daemon.getInstance().startServiceMayBind(Daemon.getInstance().mWorkService);
      }
    }
  }
}
