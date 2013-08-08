package net.simonvt.trakt.remote;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.format.DateUtils;
import javax.inject.Inject;
import net.simonvt.trakt.BuildConfig;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.util.LogWrapper;

public class TraktTaskService extends Service implements TraktTask.TaskCallback {

  private static final String TAG = "TraktTaskService";
  private static final String WAKELOCK_TAG = "net.simonvt.trakt.sync.TraktTaskService";

  private static volatile PowerManager.WakeLock sWakeLock = null;

  @Inject PriorityTraktTaskQueue priorityQueue;
  @Inject TraktTaskQueue queue;

  private boolean running;

  private boolean executingPriorityTask;

  private static PowerManager.WakeLock getLock(Context context) {
    if (sWakeLock == null) {
      PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
      sWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
    }

    return sWakeLock;
  }

  static void acquireLock(Context context) {
    PowerManager.WakeLock lock = getLock(context);
    if (!lock.isHeld()) {
      lock.acquire();
    }
  }

  static void releaseLock(Context context) {
    PowerManager.WakeLock lock = getLock(context);
    if (lock.isHeld()) {
      lock.release();
    }
  }

  public void onCreate() {
    super.onCreate();
    acquireLock(this);
    TraktApp.inject(this);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    executeNext();
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    releaseLock(this);
    super.onDestroy();
  }

  private void executeNext() {
    if (running) return; // Only one task at a time.

    TraktTask priorityTask = priorityQueue.peek();

    if (priorityTask != null) {
      running = true;
      executingPriorityTask = true;
      priorityTask.execute(this);
    } else {
      TraktTask task = queue.peek();
      if (task != null) {
        LogWrapper.i(TAG, "Execute next");
        running = true;
        task.execute(this);
      } else {
        LogWrapper.i(TAG, "Service stopping!");
        stopSelf(); // No more tasks are present. Stop.
      }
    }
  }

  @Override
  public void onSuccess() {
    running = false;
    if (executingPriorityTask) {
      executingPriorityTask = false;
      priorityQueue.remove();
    } else {
      queue.remove();
    }
    executeNext();
  }

  @Override
  public void onFailure() {
    LogWrapper.i(TAG, "[onFailure] Scheduling restart in 10 minutes");
    running = false;

    Intent intent = new Intent(this, TaskServiceReceiver.class);
    PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);

    AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    final long runAt =
        SystemClock.elapsedRealtime() + (BuildConfig.DEBUG ? 1 : 10) * DateUtils.MINUTE_IN_MILLIS;
    am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, runAt, pi);

    stopSelf();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}