package com.daemonguard.app;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import com.daemonguard.lib.service.DaemonWorkerService;

public class MyWorkerService extends DaemonWorkerService {

  private final IBinder mBinder = new LocalBinder();

  @Override public IBinder onBind(Intent intent, Void v) {
    return mBinder;
  }

  /**
   * Class used for the client Binder.  Because we know this service always
   * runs in the same process as its clients, we don't need to deal with IPC.
   */
  public class LocalBinder extends Binder {
    MyWorkerService getService() {
      // Return this instance of LocalService so clients can call public methods
      return MyWorkerService.this;
    }
  }

  @Override public IBinder onBind(Intent intent) {
    return mBinder;
  }
}
