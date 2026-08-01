package com.example.techfix;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;

public final class FirebaseSyncWorker extends Worker {
  private final TechFixDatabaseHelper helper;

  public FirebaseSyncWorker(@NonNull Context context,
                            @NonNull WorkerParameters parameters) {
    super(context, parameters);
    helper = new TechFixDatabaseHelper(context.getApplicationContext());
  }

  @NonNull
  @Override
  public Result doWork() {
    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    if (firebaseUser == null)
      return Result.failure();
    try {
      FirebaseFirestore firestore = FirebaseFirestore.getInstance();
      syncProfile(firestore, firebaseUser);
      DocumentSnapshot profile = Tasks.await(
          firestore.collection("users").document(firebaseUser.getUid()).get());
      boolean manager = "manager".equals(profile.getString("role"));
      if (manager) {
        syncBranches(firestore);
        syncCategories(firestore);
        syncServices(firestore);
        syncTechnicians(firestore);
        syncParts(firestore);
      }
      long localUserId =
          new SessionManager(getApplicationContext()).getUserId();
      syncAppointments(firestore, firebaseUser, manager ? null : localUserId);
      syncHistory(firestore, firebaseUser, manager ? null : localUserId);
      if (manager)
        syncPayments(firestore, firebaseUser, null);
      return Result.success();
    } catch (Exception exception) {
      return getRunAttemptCount() >= 4 ? Result.failure() : Result.retry();
    } finally {
      helper.close();
    }
  }

  private void syncProfile(FirebaseFirestore firestore, FirebaseUser user)
      throws Exception {
    SessionManager session = new SessionManager(getApplicationContext());
    Map<String, Object> values = new HashMap<>();
    values.put("fullName", session.getFullName());
    values.put("email", session.getEmail());
    values.put("updatedAt", System.currentTimeMillis());
    Tasks.await(firestore.collection("users")
                    .document(user.getUid())
                    .set(values, SetOptions.merge()));
  }

  private void syncBranches(FirebaseFirestore firestore) throws Exception {
    try (Cursor cursor = helper.getReadableDatabase().query(
             TechFixDatabaseHelper.TABLE_BRANCHES, null, null, null, null, null,
             null)) {
      while (cursor.moveToNext()) {
        Map<String, Object> values = base(cursor);
        values.put("name", text(cursor, TechFixDatabaseHelper.NAME));
        values.put("address", text(cursor, TechFixDatabaseHelper.ADDRESS));
        values.put("phone", text(cursor, TechFixDatabaseHelper.PHONE));
        values.put("latitude", decimal(cursor, TechFixDatabaseHelper.LATITUDE));
        values.put("longitude",
                   decimal(cursor, TechFixDatabaseHelper.LONGITUDE));
        values.put("active", bool(cursor, TechFixDatabaseHelper.ACTIVE));
        set(firestore, "branches", id(cursor), values);
      }
    }
  }

  private void syncCategories(FirebaseFirestore firestore) throws Exception {
    try (Cursor cursor = helper.getReadableDatabase().query(
             TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES, null, null, null,
             null, null, null)) {
      while (cursor.moveToNext()) {
        Map<String, Object> values = base(cursor);
        values.put("name", text(cursor, TechFixDatabaseHelper.NAME));
        values.put("description",
                   text(cursor, TechFixDatabaseHelper.DESCRIPTION));
        values.put("active", bool(cursor, TechFixDatabaseHelper.ACTIVE));
        set(firestore, "deviceCategories", id(cursor), values);
      }
    }
  }

  private void syncServices(FirebaseFirestore firestore) throws Exception {
    try (Cursor cursor = helper.getReadableDatabase().query(
             TechFixDatabaseHelper.TABLE_REPAIR_SERVICES, null, null, null,
             null, null, null)) {
      while (cursor.moveToNext()) {
        Map<String, Object> values = base(cursor);
        values.put("categoryId",
                   number(cursor, TechFixDatabaseHelper.CATEGORY_ID));
        values.put("name", text(cursor, TechFixDatabaseHelper.NAME));
        values.put("description",
                   text(cursor, TechFixDatabaseHelper.DESCRIPTION));
        values.put("basePriceCents",
                   number(cursor, TechFixDatabaseHelper.BASE_PRICE_CENTS));
        values.put("estimatedMinutes",
                   number(cursor, TechFixDatabaseHelper.ESTIMATED_MINUTES));
        values.put("active", bool(cursor, TechFixDatabaseHelper.ACTIVE));
        set(firestore, "services", id(cursor), values);
      }
    }
  }

  private void syncTechnicians(FirebaseFirestore firestore) throws Exception {
    try (Cursor cursor = helper.getReadableDatabase().query(
             TechFixDatabaseHelper.TABLE_TECHNICIANS, null, null, null, null,
             null, null)) {
      while (cursor.moveToNext()) {
        Map<String, Object> values = base(cursor);
        values.put("branchId", number(cursor, TechFixDatabaseHelper.BRANCH_ID));
        values.put("fullName", text(cursor, TechFixDatabaseHelper.FULL_NAME));
        values.put("email", text(cursor, TechFixDatabaseHelper.EMAIL));
        values.put("phone", text(cursor, TechFixDatabaseHelper.PHONE));
        values.put("specialty", text(cursor, TechFixDatabaseHelper.SPECIALTY));
        values.put("active", bool(cursor, TechFixDatabaseHelper.ACTIVE));
        set(firestore, "technicians", id(cursor), values);
      }
    }
  }

  private void syncParts(FirebaseFirestore firestore) throws Exception {
    try (Cursor cursor = helper.getReadableDatabase().query(
             TechFixDatabaseHelper.TABLE_SPARE_PARTS, null, null, null, null,
             null, null)) {
      while (cursor.moveToNext()) {
        Map<String, Object> values = base(cursor);
        values.put("branchId", number(cursor, TechFixDatabaseHelper.BRANCH_ID));
        values.put("categoryId",
                   nullableNumber(cursor, TechFixDatabaseHelper.CATEGORY_ID));
        values.put("name", text(cursor, TechFixDatabaseHelper.NAME));
        values.put("sku", text(cursor, TechFixDatabaseHelper.SKU));
        values.put("unitPriceCents",
                   number(cursor, TechFixDatabaseHelper.UNIT_PRICE_CENTS));
        values.put("quantityAvailable",
                   number(cursor, TechFixDatabaseHelper.QUANTITY_AVAILABLE));
        values.put("active", bool(cursor, TechFixDatabaseHelper.ACTIVE));
        set(firestore, "spareParts", id(cursor), values);
      }
    }
  }

  private void syncAppointments(FirebaseFirestore firestore,
                                FirebaseUser firebaseUser, Long localUserId)
      throws Exception {
    SQLiteDatabase database = helper.getReadableDatabase();
    String selection =
        localUserId == null ? null : TechFixDatabaseHelper.USER_ID + "=?";
    String[] arguments =
        localUserId == null ? null : new String[] {String.valueOf(localUserId)};
    try (Cursor cursor =
             database.query(TechFixDatabaseHelper.TABLE_APPOINTMENTS, null,
                            selection, arguments, null, null, null)) {
      while (cursor.moveToNext()) {
        if (localUserId != null &&
            Tasks
                .await(firestore.collection("appointments")
                           .document(id(cursor))
                           .get())
                .exists())
          continue;
        long userId = number(cursor, TechFixDatabaseHelper.USER_ID);
        Map<String, Object> values = base(cursor);
        if (userId == new SessionManager(getApplicationContext()).getUserId())
          values.put("customerUid", firebaseUser.getUid());
        values.put("userLocalId", userId);
        values.put("customerEmail", userEmail(database, userId));
        values.put("branchId",
                   nullableNumber(cursor, TechFixDatabaseHelper.BRANCH_ID));
        values.put("technicianId",
                   nullableNumber(cursor, TechFixDatabaseHelper.TECHNICIAN_ID));
        values.put("serviceId",
                   number(cursor, TechFixDatabaseHelper.SERVICE_ID));
        values.put("deviceDetails",
                   text(cursor, TechFixDatabaseHelper.DEVICE_DETAILS));
        values.put("problemDescription",
                   text(cursor, TechFixDatabaseHelper.PROBLEM_DESCRIPTION));
        values.put("status", text(cursor, TechFixDatabaseHelper.STATUS));
        values.put("appointmentAt",
                   number(cursor, TechFixDatabaseHelper.APPOINTMENT_AT));
        values.put("createdAt",
                   number(cursor, TechFixDatabaseHelper.CREATED_AT));
        set(firestore, "appointments", id(cursor), values);
      }
    }
  }

  private void syncHistory(FirebaseFirestore firestore,
                           FirebaseUser firebaseUser, Long localUserId)
      throws Exception {
    String sql = "SELECT h.* FROM " +
                 TechFixDatabaseHelper.TABLE_REPAIR_HISTORY + " h JOIN " +
                 TechFixDatabaseHelper.TABLE_APPOINTMENTS + " a ON a." +
                 TechFixDatabaseHelper.ID + "=h." +
                 TechFixDatabaseHelper.APPOINTMENT_ID +
                 (localUserId == null
                      ? ""
                      : " WHERE a." + TechFixDatabaseHelper.USER_ID + "=?");
    String[] arguments =
        localUserId == null ? null : new String[] {String.valueOf(localUserId)};
    try (Cursor cursor =
             helper.getReadableDatabase().rawQuery(sql, arguments)) {
      while (cursor.moveToNext()) {
        if (localUserId != null &&
            Tasks
                .await(firestore.collection("repairHistory")
                           .document(id(cursor))
                           .get())
                .exists())
          continue;
        Map<String, Object> values = base(cursor);
        values.put("appointmentId",
                   number(cursor, TechFixDatabaseHelper.APPOINTMENT_ID));
        if (localUserId != null)
          values.put("customerUid", firebaseUser.getUid());
        values.put("status", text(cursor, TechFixDatabaseHelper.STATUS));
        values.put("notes", text(cursor, TechFixDatabaseHelper.NOTES));
        values.put(
            "imagePath",
            syncImage(firebaseUser, id(cursor),
                      nullableText(cursor, TechFixDatabaseHelper.IMAGE_PATH)));
        values.put("recordedAt",
                   number(cursor, TechFixDatabaseHelper.RECORDED_AT));
        set(firestore, "repairHistory", id(cursor), values);
      }
    }
  }

  private void syncPayments(FirebaseFirestore firestore,
                            FirebaseUser firebaseUser, Long localUserId)
      throws Exception {
    String sql = "SELECT p.* FROM " + TechFixDatabaseHelper.TABLE_PAYMENTS +
                 " p JOIN " + TechFixDatabaseHelper.TABLE_APPOINTMENTS +
                 " a ON a." + TechFixDatabaseHelper.ID + "=p." +
                 TechFixDatabaseHelper.APPOINTMENT_ID +
                 (localUserId == null
                      ? ""
                      : " WHERE a." + TechFixDatabaseHelper.USER_ID + "=?");
    String[] arguments =
        localUserId == null ? null : new String[] {String.valueOf(localUserId)};
    try (Cursor cursor =
             helper.getReadableDatabase().rawQuery(sql, arguments)) {
      while (cursor.moveToNext()) {
        Map<String, Object> values = base(cursor);
        values.put("appointmentId",
                   number(cursor, TechFixDatabaseHelper.APPOINTMENT_ID));
        if (localUserId != null)
          values.put("customerUid", firebaseUser.getUid());
        values.put("amountCents",
                   number(cursor, TechFixDatabaseHelper.AMOUNT_CENTS));
        values.put("method", text(cursor, TechFixDatabaseHelper.METHOD));
        values.put("status", text(cursor, TechFixDatabaseHelper.STATUS));
        values.put("reference",
                   nullableText(cursor, TechFixDatabaseHelper.REFERENCE));
        values.put("paidAt",
                   nullableNumber(cursor, TechFixDatabaseHelper.PAID_AT));
        values.put("createdAt",
                   number(cursor, TechFixDatabaseHelper.CREATED_AT));
        set(firestore, "payments", id(cursor), values);
      }
    }
  }

  private void set(FirebaseFirestore firestore, String collection,
                   String document, Map<String, Object> values)
      throws Exception {
    Tasks.await(firestore.collection(collection)
                    .document(document)
                    .set(values, SetOptions.merge()));
  }

  private String syncImage(FirebaseUser firebaseUser, String historyId,
                           String imagePath) throws Exception {
    if (imagePath == null || !imagePath.startsWith("content://"))
      return imagePath;
    StorageReference image = FirebaseStorage.getInstance()
                                 .getReference()
                                 .child("repair-images")
                                 .child(firebaseUser.getUid())
                                 .child(historyId + ".jpg");
    Tasks.await(image.putFile(Uri.parse(imagePath)));
    String downloadUrl = Tasks.await(image.getDownloadUrl()).toString();
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.IMAGE_PATH, downloadUrl);
    helper.getWritableDatabase().update(
        TechFixDatabaseHelper.TABLE_REPAIR_HISTORY, values,
        TechFixDatabaseHelper.ID + "=?", new String[] {historyId});
    return downloadUrl;
  }

  private Map<String, Object> base(Cursor cursor) {
    Map<String, Object> values = new HashMap<>();
    values.put("localId", number(cursor, TechFixDatabaseHelper.ID));
    values.put("updatedAt", System.currentTimeMillis());
    return values;
  }

  private String id(Cursor cursor) {
    return String.valueOf(number(cursor, TechFixDatabaseHelper.ID));
  }

  private long number(Cursor cursor, String column) {
    return cursor.getLong(cursor.getColumnIndexOrThrow(column));
  }

  private Long nullableNumber(Cursor cursor, String column) {
    int index = cursor.getColumnIndexOrThrow(column);
    return cursor.isNull(index) ? null : cursor.getLong(index);
  }

  private double decimal(Cursor cursor, String column) {
    return cursor.getDouble(cursor.getColumnIndexOrThrow(column));
  }

  private String text(Cursor cursor, String column) {
    return cursor.getString(cursor.getColumnIndexOrThrow(column));
  }

  private String nullableText(Cursor cursor, String column) {
    int index = cursor.getColumnIndexOrThrow(column);
    return cursor.isNull(index) ? null : cursor.getString(index);
  }

  private boolean bool(Cursor cursor, String column) {
    return number(cursor, column) == 1;
  }

  private String userEmail(SQLiteDatabase database, long userId) {
    try (Cursor cursor = database.query(
             TechFixDatabaseHelper.TABLE_USERS,
             new String[] {TechFixDatabaseHelper.EMAIL},
             TechFixDatabaseHelper.ID + "=?",
             new String[] {String.valueOf(userId)}, null, null, null, "1")) {
      return cursor.moveToFirst() ? cursor.getString(0) : "";
    }
  }
}
