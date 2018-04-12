package com.daemonguard.app;

import android.app.Application;
import com.daemonguard.lib.Daemon;

public class DaemonApplication extends Application {
  @Override public void onCreate() {
    super.onCreate();
    Daemon.getInstance().start(this, MyWorkerService.class, true);
  }
}
