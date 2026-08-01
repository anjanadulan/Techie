package com.example.techfix;

import android.content.ContentValues;
import java.util.UUID;

final class LocalSyncState {
  private LocalSyncState() {}

  static void prepareNew(ContentValues values, long updatedAt) {
    if (!values.containsKey(TechFixDatabaseHelper.REMOTE_ID))
      values.put(TechFixDatabaseHelper.REMOTE_ID,
                 UUID.randomUUID().toString());
    markDirty(values, updatedAt);
  }

  static void markDirty(ContentValues values, long updatedAt) {
    values.put(TechFixDatabaseHelper.UPDATED_AT, updatedAt);
    values.put(TechFixDatabaseHelper.SYNC_DIRTY, 1);
  }

  static void markSynced(ContentValues values, long updatedAt) {
    values.put(TechFixDatabaseHelper.UPDATED_AT, updatedAt);
    values.put(TechFixDatabaseHelper.SYNC_DIRTY, 0);
  }
}
