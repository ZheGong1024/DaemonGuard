package com.daemonguard.lib;

import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import com.daemonguard.lib.service.DaemonWorkerService;
import java.util.HashMap;
import java.util.Map;

public class Daemon {

  public static final String TAG = "Daemon";

  private static Daemon instance;

  public boolean isInitialized;

  private boolean isDaemonOpen = true;

  public Application mApplication;

  public Class<? extends Service> mWorkService;

  public final Map<Class<? extends Service>, ServiceConnection> BIND_STATE_MAP = new HashMap<>();

  protected Daemon() {
  }

  /**
   * When this singleton is called by :daemon process at first time. :daemon process will copy a new instance.
   */
  public static Daemon getInstance() {
    if (instance == null) {
      instance = new Daemon();
    }
    return instance;
  }

  /**
   * Start the daemon process.
   *
   * @param application Application context.
   */
  public void start(@NonNull Application application, @NonNull Class<? extends Service> workService,
      boolean startDaemon) {
    this.mApplication = application;
    mWorkService = workService;
    isInitialized = true;
    isDaemonOpen = startDaemon;
    startServiceMayBind(workService, true);
  }

  /**
   * Use BIND_STATE_MAP to avoid duplicated start and bind.
   *
   * @param serviceClass Service class
   * @param initialStart Whether this is the initial start for the service.
   */
  private void startServiceMayBind(@NonNull final Class<? extends Service> serviceClass,
      boolean initialStart) {
    if (!isInitialized) return;
    // Daemon is not open and it is not the initial start.
    if (!isDaemonOpen && !initialStart) return;

    Log.d(TAG, "startServiceMayBind serviceClass=" + serviceClass);
    printStackTrace();
    final Intent i = new Intent(mApplication, serviceClass);
    startServiceSafely(i);
    ServiceConnection bound = BIND_STATE_MAP.get(serviceClass);
    if (bound == null) {
      mApplication.bindService(i, new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) {
          BIND_STATE_MAP.put(serviceClass, this);
        }

        @Override public void onServiceDisconnected(ComponentName name) {
          BIND_STATE_MAP.remove(serviceClass);
          if (isDaemonOpen) {
            startServiceSafely(i);
            if (!isInitialized) return;
            Log.d(TAG, "onServiceDisconnected bindService again: service=" + serviceClass);
            mApplication.bindService(i, this, Context.BIND_AUTO_CREATE);
          }
        }

        @Override public void onBindingDied(ComponentName name) {
          onServiceDisconnected(name);
        }
      }, Context.BIND_AUTO_CREATE);
    }
  }

  /**
   * Use BIND_STATE_MAP to avoid duplicated start and bind.
   *
   * @param serviceClass Service class
   */
  public void startServiceMayBind(@NonNull final Class<? extends Service> serviceClass) {
    startServiceMayBind(serviceClass, false);
  }

  /**
   * Print stack trace.
   */
  private void printStackTrace() {
    for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
      Log.d(TAG, element.toString());
    }
  }

  /**
   * Start service when daemon is initialized.
   */
  public void startServiceSafely(Intent i) {
    if (!isInitialized) return;
    try {
      mApplication.startService(i);
    } catch (Exception e) {
      Log.e(TAG, "startServiceSafely", e);
    }
  }

  /**
   * Stop daemon
   */
  public void stopDaemon() {
    isDaemonOpen = false;
    DaemonWorkerService.cancelJobAlarmSub();
  }
}
