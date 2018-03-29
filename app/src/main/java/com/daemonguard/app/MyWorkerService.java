package com.daemonguard.app;

import android.content.Intent;
import android.os.IBinder;
import com.daemonguard.lib.service.DaemonWorkService;

public class MyWorkerService extends DaemonWorkService {
  @Override public IBinder onBind(Intent intent, Void v) {
    return null;
  }
}
