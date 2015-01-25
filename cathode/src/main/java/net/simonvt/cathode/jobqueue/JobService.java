/*
 * Copyright (C) 2015 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.simonvt.cathode.jobqueue;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.format.DateUtils;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import timber.log.Timber;

public class JobService extends Service {

  private static final String WAKELOCK_TAG = "net.simonvt.cathode.sync.TraktTaskService";

  static final String RETRY_DELAY = "net.simonvt.cathode.sync.TraktTaskService.retryDelay";

  private static final int MAX_RETRY_DELAY = 60;

  private static volatile PowerManager.WakeLock sWakeLock = null;

  private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

  @Inject JobManager jobManager;

  @Inject Bus bus;

  private int retryDelay = -1;

  private volatile JobThread jobThread;

  private class JobThread extends Thread {

    @Override public void run() {
      while (jobManager.hasJobs()) {
        Job job = jobManager.nextJob();

        if (job.requiresWakelock()) {
          acquireLock(JobService.this);
        } else {
          releaseLock(JobService.this);
        }

        try {
          Timber.d("Executing job: " + job.getClass().getSimpleName());
          job.perform();
          jobFinished(job);
        } catch (Throwable t) {
          Timber.e(t, "Failed to execute job");
          jobFailed(job);
          break;
        }
      }

      MAIN_HANDLER.post(new Runnable() {
        @Override public void run() {
          jobThread = null;
          if (jobManager.hasJobs()) {
            Timber.d("Re-starting JobThread");
            startJobThread();
          } else {
            Timber.d("Stopping service");
            stopSelf();
          }
        }
      });
    }
  }

  private Runnable stopSelf = new Runnable() {
    @Override public void run() {
      stopSelf();
    }
  };

  private Runnable scheduleAlarm = new Runnable() {
    @Override public void run() {
      Intent intent = new Intent(JobService.this, JobService.class);
      final int retryDelay = Math.max(1, JobService.this.retryDelay);
      final int nextDelay = Math.min(retryDelay * 2, MAX_RETRY_DELAY);
      intent.putExtra(RETRY_DELAY, nextDelay);

      PendingIntent pi =
          PendingIntent.getBroadcast(JobService.this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

      AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
      final long runAt = SystemClock.elapsedRealtime() + retryDelay * DateUtils.MINUTE_IN_MILLIS;
      am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, runAt, pi);

      Timber.d("Scheduling alarm in " + retryDelay + " minutes");
    }
  };

  private static PowerManager.WakeLock getLock(Context context) {
    if (sWakeLock == null) {
      PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
      sWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
    }

    return sWakeLock;
  }

  static boolean hasLock(Context context) {
    PowerManager.WakeLock lock = getLock(context);
    return lock.isHeld();
  }

  static void acquireLock(Context context) {
    PowerManager.WakeLock lock = getLock(context);
    if (!lock.isHeld()) {
      Timber.d("Acquiring wakelock");
      lock.acquire();
    }
  }

  static void releaseLock(Context context) {
    PowerManager.WakeLock lock = getLock(context);
    if (lock.isHeld()) {
      Timber.d("Releasing wakelock");
      lock.release();
    }
  }

  @Override public void onCreate() {
    super.onCreate();
    Timber.d("JobService started");
    CathodeApp.inject(this);

    acquireLock(this);

    cancelAlarm();

    startJobThread();

    bus.register(this);
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    if (retryDelay == -1) {
      if (intent != null) {
        int delay = intent.getIntExtra(RETRY_DELAY, -1);
        if (delay != -1) {
          retryDelay = delay;
        }
      }
    }

    MAIN_HANDLER.removeCallbacks(stopSelf);

    if (jobThread == null) {
      startJobThread();
    }

    return START_STICKY;
  }

  private void startJobThread() {
    jobThread = new JobThread();
    jobThread.start();
  }

  private void cancelAlarm() {
    Intent intent = new Intent(this, JobService.class);
    PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);

    AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    am.cancel(pi);
  }

  void jobFinished(Job job) {
    jobManager.jobDone(job);
  }

  void jobFailed(Job job) {
    jobManager.jobFailed(job);
    MAIN_HANDLER.post(scheduleAlarm);
    MAIN_HANDLER.post(stopSelf);
  }

  @Produce public SyncEvent provideRunningEvent() {
    return new SyncEvent(true);
  }

  @Override public void onDestroy() {
    bus.unregister(this);
    bus.post(new SyncEvent(false));

    releaseLock(this);

    Timber.d("JobService stopped");
    super.onDestroy();
  }

  @Override public IBinder onBind(Intent intent) {
    return null;
  }
}