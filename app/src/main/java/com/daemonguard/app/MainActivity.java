package com.daemonguard.app;

import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import com.daemonguard.lib.Daemon;

public class MainActivity extends AppCompatActivity {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }

  public void onStopDaemonClick(View view) {
    MyWorkerService.LocalBinder mBinder =
        (MyWorkerService.LocalBinder) Daemon.getInstance().BINDER_MAP.get(MyWorkerService.class);
    if (mBinder != null) {
      mBinder.getService().stopDaemon();
    }
  }
}
