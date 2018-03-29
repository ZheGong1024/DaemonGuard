package com.daemonguard.lib;

import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import java.util.HashMap;
import java.util.Map;

public class Daemon {

  private static Daemon instance;

  public boolean isInitialized;

  public Application mApplication;

  public Class<? extends Service> mWorkService;

  public final Map<Class<? extends Service>, ServiceConnection> BIND_STATE_MAP = new HashMap<>();

  protected Daemon() {
  }

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
        }

        @Override public void onServiceDisconnected(ComponentName name) {
          BIND_STATE_MAP.remove(serviceClass);
          startServiceSafely(i);
          if (!isInitialized) return;
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
}
