package com.daemonguard.lib.service;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;

/**
 * Use JobScheduler to guard application process after Android Lolipop.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP) public class DaemonJobService extends JobService {
  @Override public boolean onStartJob(JobParameters params) {
    return false;
  }

  @Override public boolean onStopJob(JobParameters params) {
    return false;
  }
}
