package com.example.techfix;

import android.content.Context;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public final class FirebaseSyncScheduler {
  private static final String IMMEDIATE_WORK = "techfix-firebase-sync-now";
  private static final String PERIODIC_WORK = "techfix-firebase-sync-periodic";

  private FirebaseSyncScheduler() {}

  public static void enqueueNow(Context context) {
    Constraints constraints = new Constraints.Builder()
                                  .setRequiredNetworkType(NetworkType.CONNECTED)
                                  .build();
    OneTimeWorkRequest request =
        new OneTimeWorkRequest.Builder(FirebaseSyncWorker.class)
            .setConstraints(constraints)
            .build();
    WorkManager.getInstance(context.getApplicationContext())
        .enqueueUniqueWork(IMMEDIATE_WORK, ExistingWorkPolicy.REPLACE, request);
  }

  public static void schedulePeriodic(Context context) {
    Constraints constraints = new Constraints.Builder()
                                  .setRequiredNetworkType(NetworkType.CONNECTED)
                                  .setRequiresBatteryNotLow(true)
                                  .build();
    PeriodicWorkRequest request =
        new PeriodicWorkRequest
            .Builder(FirebaseSyncWorker.class, 15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build();
    WorkManager.getInstance(context.getApplicationContext())
        .enqueueUniquePeriodicWork(PERIODIC_WORK,
                                   ExistingPeriodicWorkPolicy.KEEP, request);
  }

  public static void cancel(Context context) {
    WorkManager manager =
        WorkManager.getInstance(context.getApplicationContext());
    manager.cancelUniqueWork(IMMEDIATE_WORK);
    manager.cancelUniqueWork(PERIODIC_WORK);
  }
}
