package com.daemonguard.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import com.daemonguard.lib.Daemon;

public class MainActivity extends AppCompatActivity {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }

  public void onStopDaemonClick(View view) {
    Daemon.getInstance().stopDaemon();
  }
}
