package com.daemonguard.lib;

import com.daemonguard.lib.service.DaemonService;
import com.daemonguard.lib.service.DaemonWorkerService;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * The configurations of daemon library.
 */
public class DaemonConfig {

  public static final int DEFAULT_WAKE_UP_INTERVAL = 6 * 60 * 1000;
  public static final int MINIMAL_WAKE_UP_INTERVAL = 3 * 60 * 1000;

  /**
   * Hashcode for worker service.
   */
  public static int WORKER_SERVICE_HASH_CODE =
      new HashCodeBuilder().append(DaemonWorkerService.class.getSimpleName()).toHashCode();
  /**
   * Hashcode for the foreground service.
   */
  public static int FOREGROUND_SERVICE_HASH_CODE =
      new HashCodeBuilder().append(DaemonService.class.getSimpleName()).toHashCode();

  /**
   * Wakeup interval.
   */
  public static int WAKEUP_INTERVAL = DEFAULT_WAKE_UP_INTERVAL;
}
