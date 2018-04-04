package com.daemonguard.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WakeUpReceiver extends BroadcastReceiver {

  /**
   * Listening to the following events to start daemon job service:
   * android.intent.action.USER_PRESENT: User unlock the screen.
   * android.intent.action.ACTION_POWER_CONNECTED: Power connected.
   * android.intent.action.ACTION_POWER_DISCONNECTED: Power disconnected.
   * android.intent.action.BOOT_COMPLETED: System boot.
   * android.net.conn.CONNECTIVITY_CHANGE: Connectivity changes.
   * android.intent.action.PACKAGE_ADDED: App install.
   * android.intent.action.PACKAGE_REMOVED: App uninstall.
   */
  @Override public void onReceive(Context context, Intent intent) {
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
