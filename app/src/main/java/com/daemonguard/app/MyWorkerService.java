package com.daemonguard.app;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.daemonguard.lib.service.DaemonWorkService;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import java.util.concurrent.TimeUnit;

public class MyWorkerService extends DaemonWorkService {

  private static final String TAG = "MyWorkerService";

  //是否 任务完成, 不再需要服务运行?
  public static boolean sShouldStopService = false;
  public static Disposable sDisposable;

  public static void stopService() {
    //我们现在不再需要服务运行了, 将标志位置为 true
    sShouldStopService = true;
    //取消对任务的订阅
    if (sDisposable != null) sDisposable.dispose();
    //取消 Job / Alarm / Subscription
    cancelJobAlarmSub();
  }

  /**
   * 是否 任务完成, 不再需要服务运行?
   *
   * @return 应当停止服务, true; 应当启动服务, false; 无法判断, 什么也不做, null.
   */
  @Override public Boolean shouldStopService(Intent intent, int flags, int startId) {
    return sShouldStopService;
  }

  @Override public void startWork(Intent intent, int flags, int startId) {
    Log.i(TAG, "startWork...");
    sDisposable = Observable.interval(3, TimeUnit.SECONDS).doOnDispose(new Action() {
      @Override public void run() throws Exception {
        cancelJobAlarmSub();
      }
    }).subscribe(new Consumer<Long>() {
      @Override public void accept(Long aLong) throws Exception {
        Log.d(TAG, "working");
      }
    });
  }

  @Override public void stopWork(Intent intent, int flags, int startId) {
    stopService();
  }

  /**
   * 任务是否正在运行?
   *
   * @return 任务正在运行, true; 任务当前不在运行, false; 无法判断, 什么也不做, null.
   */
  @Override public Boolean isWorkRunning(Intent intent, int flags, int startId) {
    //若还没有取消订阅, 就说明任务仍在运行.
    return sDisposable != null && !sDisposable.isDisposed();
  }

  @Override public IBinder onBind(Intent intent, Void v) {
    return null;
  }

  @Override public void onServiceKilled(Intent rootIntent) {
    Log.d(TAG, "onServiceKilled");
  }
}
