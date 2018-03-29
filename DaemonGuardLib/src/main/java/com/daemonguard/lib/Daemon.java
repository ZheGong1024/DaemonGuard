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

  public final Map<Class<? extends Service>, IBinder> BINDER_MAP = new HashMap<>();

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
  public void start(@NonNull Application application,
      @NonNull Class<? extends Service> workService) {
    this.mApplication = application;
    mWorkService = workService;
    isInitialized = true;
    startServiceMayBind(workService);
  }

  public void startServiceMayBind(@NonNull final Class<? extends Service> serviceClass) {
    if (!isInitialized) return;
    final Intent i = new Intent(mApplication, serviceClass);
    startServiceSafely(i);
    ServiceConnection bound = BIND_STATE_MAP.get(serviceClass);
    if (bound == null) {
      mApplication.bindService(i, new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) {
          BIND_STATE_MAP.put(serviceClass, this);
          BINDER_MAP.put(serviceClass, service);
        }

        @Override public void onServiceDisconnected(ComponentName name) {
          BIND_STATE_MAP.remove(serviceClass);
          BINDER_MAP.remove(serviceClass);
          startServiceSafely(i);
          if (!isInitialized) return;
          Log.d(TAG, "onServiceDisconnected bindService again: service=" + serviceClass);
          mApplication.bindService(i, this, Context.BIND_AUTO_CREATE);
        }

        @Override public void onBindingDied(ComponentName name) {
          onServiceDisconnected(name);
        }
      }, Context.BIND_AUTO_CREATE);
    }
  }

  public void startServiceSafely(Intent i) {
    if (!isInitialized) return;
    try {
      mApplication.startService(i);
    } catch (Exception ignored) {
    }
  }

  /**
   * Stop daemon
   */
  public void stopDaemon() {
    Log.d(TAG, "stopDaemon");
    isDaemonOpen = false;
    cancelJobAlarmSub();
  }

  public boolean isDaemonOpen() {
    return isDaemonOpen;
  }

  /**
   * 用于在不需要服务运行的时候取消 Job / Alarm / Subscription.
   */
  private void cancelJobAlarmSub() {
    if (!isInitialized) return;
    mApplication.sendBroadcast(new Intent(WakeUpReceiver.ACTION_CANCEL_JOB_ALARM_SUB));
  }
}
