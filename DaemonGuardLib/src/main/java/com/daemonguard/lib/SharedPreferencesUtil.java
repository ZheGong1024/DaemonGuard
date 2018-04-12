package com.daemonguard.lib;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUtil {

  /**
   * PREFERENCE_FILE_KEY for daemon guard.
   */
  private static String SHARED_PREFERENCES_FILE_KEY = "com.daemonguard.lib.PREFERENCE_FILE_KEY";

  /**
   * SharedPreferencesKey for isDaemonOpen.
   */
  private static String SHARED_PREFERENCES_IS_DAEMON_OPEN = "com.daemonguard.lib.isDaemonOpen";

  public static void startDaemon(Application application) {
    // TODO
    //SharedPreferences sharedPreferences =
    //    application.getSharedPreferences(SHARED_PREFERENCES_FILE_KEY, Context.MODE_PRIVATE);
    //SharedPreferences.Editor editor = sharedPreferences.edit();
    //editor.putBoolean(SHARED_PREFERENCES_IS_DAEMON_OPEN, true);
    //editor.apply();
  }
}
