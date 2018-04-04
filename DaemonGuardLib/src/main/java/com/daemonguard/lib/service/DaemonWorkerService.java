package com.daemonguard.lib.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import com.daemonguard.lib.Daemon;
import com.daemonguard.lib.DaemonConfig;

public abstract class DaemonWorkerService extends Service {

  protected boolean mFirstStarted = true;

  protected static PendingIntent sPendingIntent;

  @Nullable public abstract IBinder onBind(Intent intent, Void alwaysNull);

  /**
   * 1. API level < 25: Start as foreground service without notifications when API < 25. (Use the flaw in Android framework)
   * 2. API level > 20: Use JobScheduler to wakeup the process.
   * 3. API level < 21: Use AlarmManager to wakeup the process.
   */
  protected int onStart() {
    Log.d(Daemon.TAG, "DaemonWorkerService onStart. API=" + Build.VERSION.SDK_INT);
    if (mFirstStarted) {
      mFirstStarted = false;
      // Start as foreground service without notifications when API < 25. (Use the flaw in Android framework)
      if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
        // For API level < 18, only need to start foreground with a new Notification.
        startForeground(DaemonConfig.WORKER_SERVICE_HASH_CODE, new Notification());
        // After API level 18, start a foreground with the same hashcode and then stop itself to hide the notifications.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
          Daemon.getInstance()
              .startServiceSafely(
                  new Intent(getApplication(), DaemonWorkerNotificationService.class));
        }
      }

      // Check DaemonJobService regularly to guard it.
      // For Android 5.0+, use JobScheduler.
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        JobInfo.Builder builder = new JobInfo.Builder(DaemonConfig.JOB_HASH_CODE,
            new ComponentName(Daemon.getInstance().mApplication, DaemonJobService.class));
        builder.setPeriodic(DaemonConfig.WAKEUP_INTERVAL);
        //Android 7.0+ 增加了一项针对 JobScheduler 的新限制，最小间隔只能是下面设定的数字
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          builder.setPeriodic(JobInfo.getMinPeriodMillis(), JobInfo.getMinFlexMillis());
        }
        builder.setPersisted(true);
        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        if (scheduler != null) scheduler.schedule(builder.build());
      } else {
        // Before Android 5.0, use AlarmManager.
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent i = new Intent(Daemon.getInstance().mApplication, Daemon.getInstance().mWorkService);
        sPendingIntent =
            PendingIntent.getService(Daemon.getInstance().mApplication, DaemonConfig.JOB_HASH_CODE,
                i, PendingIntent.FLAG_UPDATE_CURRENT);
        if (am != null) {
          am.setRepeating(AlarmManager.RTC_WAKEUP,
              System.currentTimeMillis() + DaemonConfig.WAKEUP_INTERVAL,
              DaemonConfig.WAKEUP_INTERVAL, sPendingIntent);
        }
      }
    }

    return START_STICKY;
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    return onStart();
  }

  @Nullable @Override public IBinder onBind(Intent intent) {
    onStart();
    return onBind(intent, null);
  }

  protected void onEnd() {
    if (!Daemon.getInstance().isInitialized) return;
    if (Daemon.getInstance().isDaemonOpen()) {
      Log.d(Daemon.TAG, "DaemonWorkerService onEnd. Daemon is open. Restart services.");
      Daemon.getInstance().startServiceMayBind(Daemon.getInstance().mWorkService);
    }
  }

  /**
   * Swiping close
   */
  @Override public void onTaskRemoved(Intent rootIntent) {
    onEnd();
  }

  @Override public void onDestroy() {
    onEnd();
  }

  /**
   * Cancel JobScheduler and Alarm when we don't need it.
   */
  public static void cancelJobAlarmSub() {
    if (!Daemon.getInstance().isInitialized) return;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      JobScheduler jobScheduler =
          (JobScheduler) Daemon.getInstance().mApplication.getSystemService(JOB_SCHEDULER_SERVICE);
      if (jobScheduler != null) jobScheduler.cancel(DaemonConfig.JOB_HASH_CODE);
    } else {
      AlarmManager alarmManager =
          (AlarmManager) Daemon.getInstance().mApplication.getSystemService(ALARM_SERVICE);
      if (alarmManager != null) if (sPendingIntent != null) alarmManager.cancel(sPendingIntent);
    }
  }

  public static class DaemonWorkerNotificationService extends Service {

    /**
     * Use ForegroundService without notifications with API levels > 18 and < 25.
     */
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
      startForeground(DaemonConfig.WORKER_SERVICE_HASH_CODE, new Notification());
      stopSelf();
      return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) {
      return null;
    }
  }
}
