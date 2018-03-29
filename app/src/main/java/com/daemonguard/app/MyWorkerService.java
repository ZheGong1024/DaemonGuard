package com.daemonguard.app;

import android.content.Intent;
import android.os.IBinder;
import com.daemonguard.lib.service.DaemonWorkerService;

public class MyWorkerService extends DaemonWorkerService {
  @Override public IBinder onBind(Intent intent, Void v) {
    return null;
  }
}
