package com.daemonguard.app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

public class MyNormalService extends Service {

  private static final String TAG = "MyNormalService";

  boolean running = false;

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    start();
    return super.onStartCommand(intent, flags, startId);
  }

  private void start() {
    running = true;
    new Thread(new Runnable() {
      @Override public void run() {
        if (running) {
          try {
            Thread.sleep(3000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          Log.d(TAG, "running");
        }
      }
    }).start();
  }

  @Nullable @Override public IBinder onBind(Intent intent) {
    start();
    return null;
  }

  @Override public boolean onUnbind(Intent intent) {
    end();
    return super.onUnbind(intent);
  }

  private void end() {
    running = false;
  }

  @Override public void onDestroy() {
    end();
    super.onDestroy();
  }
}
