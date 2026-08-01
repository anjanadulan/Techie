package com.example.techfix;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
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
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FirebaseSyncWorker extends Worker {
  private static final String TAG = "TechFixFirebaseSync";
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
      ensureRemoteIdentifiers();
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
        syncServicePartRequirements(firestore);
      }
      long localUserId =
          new SessionManager(getApplicationContext()).getUserId();
      syncAppointments(firestore, firebaseUser, manager ? null : localUserId);
      syncPayments(firestore, firebaseUser, manager ? null : localUserId);
      syncHistory(firestore, firebaseUser, manager ? null : localUserId);
      return Result.success();
    } catch (Exception exception) {
      Log.e(TAG, "Firebase synchronization failed", exception);
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
             TechFixDatabaseHelper.TABLE_BRANCHES, null,
             TechFixDatabaseHelper.SYNC_DIRTY + "=1", null, null, null, null)) {
      while (cursor.moveToNext()) {
        long updatedAt = number(cursor, TechFixDatabaseHelper.UPDATED_AT);
        Map<String, Object> values = base(cursor);
        values.put("name", text(cursor, TechFixDatabaseHelper.NAME));
        values.put("address", text(cursor, TechFixDatabaseHelper.ADDRESS));
        values.put("phone", text(cursor, TechFixDatabaseHelper.PHONE));
        values.put("latitude", decimal(cursor, TechFixDatabaseHelper.LATITUDE));
        values.put("longitude",
                   decimal(cursor, TechFixDatabaseHelper.LONGITUDE));
        values.put("active", bool(cursor, TechFixDatabaseHelper.ACTIVE));
        set(firestore, "branches", remoteId(cursor), values);
        markSynced(TechFixDatabaseHelper.TABLE_BRANCHES, remoteId(cursor),
                   updatedAt);
      }
    }
  }

  private void syncCategories(FirebaseFirestore firestore) throws Exception {
    try (Cursor cursor = helper.getReadableDatabase().query(
             TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES, null,
             TechFixDatabaseHelper.SYNC_DIRTY + "=1", null, null, null,
             null)) {
      while (cursor.moveToNext()) {
        long updatedAt = number(cursor, TechFixDatabaseHelper.UPDATED_AT);
        Map<String, Object> values = base(cursor);
        values.put("name", text(cursor, TechFixDatabaseHelper.NAME));
        values.put("description",
                   text(cursor, TechFixDatabaseHelper.DESCRIPTION));
        values.put("active", bool(cursor, TechFixDatabaseHelper.ACTIVE));
        set(firestore, "deviceCategories", remoteId(cursor), values);
        markSynced(TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES,
                   remoteId(cursor),
                   updatedAt);
      }
    }
  }

  private void syncServices(FirebaseFirestore firestore) throws Exception {
    try (Cursor cursor = helper.getReadableDatabase().query(
             TechFixDatabaseHelper.TABLE_REPAIR_SERVICES, null,
             TechFixDatabaseHelper.SYNC_DIRTY + "=1", null, null, null,
             null)) {
      while (cursor.moveToNext()) {
        long updatedAt = number(cursor, TechFixDatabaseHelper.UPDATED_AT);
        Map<String, Object> values = base(cursor);
        values.put("categoryId",
                   number(cursor, TechFixDatabaseHelper.CATEGORY_ID));
        values.put("categoryRemoteId",
                   referenceRemoteId(
                       TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES,
                       number(cursor, TechFixDatabaseHelper.CATEGORY_ID)));
        values.put("name", text(cursor, TechFixDatabaseHelper.NAME));
        values.put("description",
                   text(cursor, TechFixDatabaseHelper.DESCRIPTION));
        values.put("basePriceCents",
                   number(cursor, TechFixDatabaseHelper.BASE_PRICE_CENTS));
        values.put("estimatedMinutes",
                   number(cursor, TechFixDatabaseHelper.ESTIMATED_MINUTES));
        values.put("active", bool(cursor, TechFixDatabaseHelper.ACTIVE));
        set(firestore, "services", remoteId(cursor), values);
        markSynced(TechFixDatabaseHelper.TABLE_REPAIR_SERVICES,
                   remoteId(cursor),
                   updatedAt);
      }
    }
  }

  private void syncTechnicians(FirebaseFirestore firestore) throws Exception {
    try (Cursor cursor = helper.getReadableDatabase().query(
             TechFixDatabaseHelper.TABLE_TECHNICIANS, null,
             TechFixDatabaseHelper.SYNC_DIRTY + "=1", null, null, null,
             null)) {
      while (cursor.moveToNext()) {
        long updatedAt = number(cursor, TechFixDatabaseHelper.UPDATED_AT);
        Map<String, Object> values = base(cursor);
        values.put("branchId", number(cursor, TechFixDatabaseHelper.BRANCH_ID));
        values.put("branchRemoteId",
                   referenceRemoteId(TechFixDatabaseHelper.TABLE_BRANCHES,
                                     number(cursor,
                                            TechFixDatabaseHelper.BRANCH_ID)));
        values.put("fullName", text(cursor, TechFixDatabaseHelper.FULL_NAME));
        values.put("email", text(cursor, TechFixDatabaseHelper.EMAIL));
        values.put("phone", text(cursor, TechFixDatabaseHelper.PHONE));
        values.put("specialty", text(cursor, TechFixDatabaseHelper.SPECIALTY));
        values.put("active", bool(cursor, TechFixDatabaseHelper.ACTIVE));
        set(firestore, "technicians", remoteId(cursor), values);
        markSynced(TechFixDatabaseHelper.TABLE_TECHNICIANS,
                   remoteId(cursor),
                   updatedAt);
      }
    }
  }

  private void syncParts(FirebaseFirestore firestore) throws Exception {
    try (Cursor cursor = helper.getReadableDatabase().query(
             TechFixDatabaseHelper.TABLE_SPARE_PARTS, null,
             TechFixDatabaseHelper.SYNC_DIRTY + "=1", null, null, null,
             null)) {
      while (cursor.moveToNext()) {
        long updatedAt = number(cursor, TechFixDatabaseHelper.UPDATED_AT);
        Map<String, Object> values = base(cursor);
        values.put("branchId", number(cursor, TechFixDatabaseHelper.BRANCH_ID));
        values.put("branchRemoteId",
                   referenceRemoteId(TechFixDatabaseHelper.TABLE_BRANCHES,
                                     number(cursor,
                                            TechFixDatabaseHelper.BRANCH_ID)));
        values.put("categoryId",
                   nullableNumber(cursor, TechFixDatabaseHelper.CATEGORY_ID));
        Long categoryId =
            nullableNumber(cursor, TechFixDatabaseHelper.CATEGORY_ID);
        values.put("categoryRemoteId",
                   categoryId == null
                       ? null
                       : referenceRemoteId(
                             TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES,
                             categoryId));
        values.put("name", text(cursor, TechFixDatabaseHelper.NAME));
        values.put("sku", text(cursor, TechFixDatabaseHelper.SKU));
        values.put("unitPriceCents",
                   number(cursor, TechFixDatabaseHelper.UNIT_PRICE_CENTS));
        values.put("quantityAvailable",
                   number(cursor, TechFixDatabaseHelper.QUANTITY_AVAILABLE));
        values.put("active", bool(cursor, TechFixDatabaseHelper.ACTIVE));
        set(firestore, "spareParts", remoteId(cursor), values);
        markSynced(TechFixDatabaseHelper.TABLE_SPARE_PARTS,
                   remoteId(cursor),
                   updatedAt);
      }
    }
  }

  private void syncServicePartRequirements(FirebaseFirestore firestore)
      throws Exception {
    try (Cursor cursor = helper.getReadableDatabase().query(
             TechFixDatabaseHelper.TABLE_SERVICE_PART_REQUIREMENTS, null,
             TechFixDatabaseHelper.SYNC_DIRTY + "=1", null, null, null,
             null)) {
      while (cursor.moveToNext()) {
        long updatedAt = number(cursor, TechFixDatabaseHelper.UPDATED_AT);
        long serviceId = number(cursor, TechFixDatabaseHelper.SERVICE_ID);
        long partId = number(cursor, TechFixDatabaseHelper.SPARE_PART_ID);
        Map<String, Object> values = base(cursor);
        values.put("serviceId", serviceId);
        values.put("serviceRemoteId",
                   referenceRemoteId(
                       TechFixDatabaseHelper.TABLE_REPAIR_SERVICES,
                       serviceId));
        values.put("sparePartId", partId);
        values.put("sparePartRemoteId",
                   referenceRemoteId(TechFixDatabaseHelper.TABLE_SPARE_PARTS,
                                     partId));
        values.put("requiredQuantity",
                   number(cursor, TechFixDatabaseHelper.REQUIRED_QUANTITY));
        values.put("active", bool(cursor, TechFixDatabaseHelper.ACTIVE));
        set(firestore, "servicePartRequirements", remoteId(cursor), values);
        markSynced(TechFixDatabaseHelper.TABLE_SERVICE_PART_REQUIREMENTS,
                   remoteId(cursor), updatedAt);
      }
    }
  }

  private void syncAppointments(FirebaseFirestore firestore,
                                FirebaseUser firebaseUser, Long localUserId)
      throws Exception {
    SQLiteDatabase database = helper.getReadableDatabase();
    String selection = TechFixDatabaseHelper.SYNC_DIRTY + "=1" +
                       (localUserId == null
                            ? ""
                            : " AND " + TechFixDatabaseHelper.USER_ID + "=?");
    String[] arguments = localUserId == null
                             ? null
                             : new String[] {String.valueOf(localUserId)};
    try (Cursor cursor =
             database.query(TechFixDatabaseHelper.TABLE_APPOINTMENTS, null,
                             selection, arguments, null, null, null)) {
      while (cursor.moveToNext()) {
        long userId = number(cursor, TechFixDatabaseHelper.USER_ID);
        long updatedAt = number(cursor, TechFixDatabaseHelper.UPDATED_AT);
        if (localUserId != null &&
            Tasks.await(firestore.collection("appointments")
                            .document(remoteId(cursor))
                            .get())
                .exists()) {
          markSynced(TechFixDatabaseHelper.TABLE_APPOINTMENTS,
                     remoteId(cursor), updatedAt);
          continue;
        }
        Map<String, Object> values = dynamicBase(cursor);
        String email = userEmail(database, userId);
        String customerUid =
            nullableText(cursor, TechFixDatabaseHelper.CUSTOMER_UID);
        if (customerUid == null &&
            userId == new SessionManager(getApplicationContext()).getUserId())
          customerUid = firebaseUser.getUid();
        if (customerUid == null)
          customerUid = customerUidForEmail(firestore, email);
        if (customerUid != null)
          values.put("customerUid", customerUid);
        values.put("customerEmail", email);
        values.put("branchId",
                   nullableNumber(cursor, TechFixDatabaseHelper.BRANCH_ID));
        values.put("technicianId",
                   nullableNumber(cursor, TechFixDatabaseHelper.TECHNICIAN_ID));
        values.put("serviceId",
                   number(cursor, TechFixDatabaseHelper.SERVICE_ID));
        values.put("serviceRemoteId",
                   referenceRemoteId(
                       TechFixDatabaseHelper.TABLE_REPAIR_SERVICES,
                       number(cursor, TechFixDatabaseHelper.SERVICE_ID)));
        values.put("deviceDetails",
                   text(cursor, TechFixDatabaseHelper.DEVICE_DETAILS));
        values.put("problemDescription",
                   text(cursor, TechFixDatabaseHelper.PROBLEM_DESCRIPTION));
        values.put("status", text(cursor, TechFixDatabaseHelper.STATUS));
        values.put("appointmentAt",
                   number(cursor, TechFixDatabaseHelper.APPOINTMENT_AT));
        values.put("requestLatitude",
                   nullableDecimal(cursor,
                                   TechFixDatabaseHelper.REQUEST_LATITUDE));
        values.put("requestLongitude",
                   nullableDecimal(cursor,
                                   TechFixDatabaseHelper.REQUEST_LONGITUDE));
        values.put("createdAt",
                   number(cursor, TechFixDatabaseHelper.CREATED_AT));
        values.put("remoteId", remoteId(cursor));
        set(firestore, "appointments", remoteId(cursor), values);
        markSynced(TechFixDatabaseHelper.TABLE_APPOINTMENTS, remoteId(cursor),
                   updatedAt);
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
                 " WHERE h." + TechFixDatabaseHelper.SYNC_DIRTY + "=1" +
                 (localUserId == null
                      ? ""
                      : " AND a." + TechFixDatabaseHelper.USER_ID + "=?");
    String[] arguments =
        localUserId == null ? null : new String[] {String.valueOf(localUserId)};
    try (Cursor cursor =
             helper.getReadableDatabase().rawQuery(sql, arguments)) {
      while (cursor.moveToNext()) {
        long updatedAt = number(cursor, TechFixDatabaseHelper.UPDATED_AT);
        boolean remoteExists = localUserId != null &&
                               Tasks
                                   .await(firestore.collection("repairHistory")
                                              .document(remoteId(cursor))
                                              .get())
                                   .exists();
        String syncedImage =
            syncImage(firebaseUser, remoteId(cursor),
                      nullableText(cursor, TechFixDatabaseHelper.IMAGE_PATH));
        if (remoteExists) {
          Map<String, Object> imageUpdate = new HashMap<>();
          imageUpdate.put("customerUid", firebaseUser.getUid());
          imageUpdate.put("imagePath", syncedImage);
          imageUpdate.put("updatedAt", updatedAt);
          set(firestore, "repairHistory", remoteId(cursor), imageUpdate);
          markSynced(TechFixDatabaseHelper.TABLE_REPAIR_HISTORY,
                     remoteId(cursor), updatedAt);
          continue;
        }
        Map<String, Object> values = dynamicBase(cursor);
        long appointmentId =
            number(cursor, TechFixDatabaseHelper.APPOINTMENT_ID);
        values.put("appointmentId", appointmentId);
        String appointmentRemoteId = appointmentRemoteId(appointmentId);
        values.put("appointmentRemoteId", appointmentRemoteId);
        values.put("remoteId", remoteId(cursor));
        String customerUid = appointmentCustomerUid(appointmentId);
        if (customerUid == null && localUserId != null)
          customerUid = firebaseUser.getUid();
        if (customerUid == null)
          customerUid = customerUid(firestore, appointmentRemoteId);
        if (customerUid != null)
          values.put("customerUid", customerUid);
        values.put("status", text(cursor, TechFixDatabaseHelper.STATUS));
        values.put("notes", text(cursor, TechFixDatabaseHelper.NOTES));
        values.put("featured",
                   bool(cursor, TechFixDatabaseHelper.FEATURED));
        values.put("imagePath", syncedImage);
        values.putAll(galleryMetadata(appointmentId));
        values.put("recordedAt",
                   number(cursor, TechFixDatabaseHelper.RECORDED_AT));
        set(firestore, "repairHistory", remoteId(cursor), values);
        markSynced(TechFixDatabaseHelper.TABLE_REPAIR_HISTORY,
                   remoteId(cursor), updatedAt);
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
                 " WHERE p." + TechFixDatabaseHelper.SYNC_DIRTY + "=1" +
                 (localUserId == null
                      ? ""
                      : " AND a." + TechFixDatabaseHelper.USER_ID + "=?");
    String[] arguments =
        localUserId == null ? null : new String[] {String.valueOf(localUserId)};
    try (Cursor cursor =
             helper.getReadableDatabase().rawQuery(sql, arguments)) {
      while (cursor.moveToNext()) {
        long updatedAt = number(cursor, TechFixDatabaseHelper.UPDATED_AT);
        if (localUserId != null) {
          DocumentSnapshot remotePayment = Tasks.await(
              firestore.collection("payments").document(remoteId(cursor)).get());
          if (remotePayment.exists() &&
              !"FAILED".equals(remotePayment.getString("status"))) {
            markSynced(TechFixDatabaseHelper.TABLE_PAYMENTS,
                       remoteId(cursor), updatedAt);
            continue;
          }
        }
        Map<String, Object> values = dynamicBase(cursor);
        long appointmentId =
            number(cursor, TechFixDatabaseHelper.APPOINTMENT_ID);
        values.put("appointmentId", appointmentId);
        String appointmentRemoteId = appointmentRemoteId(appointmentId);
        values.put("appointmentRemoteId", appointmentRemoteId);
        values.put("remoteId", remoteId(cursor));
        String customerUid = appointmentCustomerUid(appointmentId);
        if (customerUid == null && localUserId != null)
          customerUid = firebaseUser.getUid();
        if (customerUid == null)
          customerUid = customerUid(firestore, appointmentRemoteId);
        if (customerUid != null)
          values.put("customerUid", customerUid);
        values.put("method", text(cursor, TechFixDatabaseHelper.METHOD));
        if (localUserId == null) {
          values.put("amountCents",
                     number(cursor, TechFixDatabaseHelper.AMOUNT_CENTS));
          values.put("status", text(cursor, TechFixDatabaseHelper.STATUS));
          values.put("reference",
                     nullableText(cursor, TechFixDatabaseHelper.REFERENCE));
          values.put("paidAt",
                     nullableNumber(cursor, TechFixDatabaseHelper.PAID_AT));
        } else {
          values.put("status", "PENDING");
        }
        values.put("createdAt",
                   number(cursor, TechFixDatabaseHelper.CREATED_AT));
        set(firestore, "payments", remoteId(cursor), values);
        markSynced(TechFixDatabaseHelper.TABLE_PAYMENTS, remoteId(cursor),
                   updatedAt);
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

  private String customerUid(FirebaseFirestore firestore,
                             String appointmentRemoteId) throws Exception {
    return Tasks
        .await(firestore.collection("appointments")
                   .document(appointmentRemoteId)
                   .get())
        .getString("customerUid");
  }

  private String customerUidForEmail(FirebaseFirestore firestore,
                                     String email) throws Exception {
    if (email == null || email.trim().isEmpty())
      return null;
    com.google.firebase.firestore.QuerySnapshot users =
        Tasks.await(firestore.collection("users")
                        .whereEqualTo("email", email.trim().toLowerCase(
                                                   java.util.Locale.ROOT))
                        .limit(1)
                        .get());
    return users.isEmpty() ? null : users.getDocuments().get(0).getId();
  }

  private String appointmentRemoteId(long appointmentId) {
    try (Cursor cursor = helper.getReadableDatabase().query(
             TechFixDatabaseHelper.TABLE_APPOINTMENTS,
             new String[] {TechFixDatabaseHelper.REMOTE_ID},
             TechFixDatabaseHelper.ID + "=?",
             new String[] {String.valueOf(appointmentId)}, null, null, null,
             "1")) {
      if (!cursor.moveToFirst() || cursor.isNull(0))
        throw new IllegalStateException(
            "Appointment remote identifier is unavailable.");
      return cursor.getString(0);
    }
  }

  private void ensureRemoteIdentifiers() {
    SQLiteDatabase database = helper.getWritableDatabase();
    String[] tables = {TechFixDatabaseHelper.TABLE_APPOINTMENTS,
                       TechFixDatabaseHelper.TABLE_PAYMENTS,
                       TechFixDatabaseHelper.TABLE_REPAIR_HISTORY};
    for (String table : tables) {
      try (Cursor cursor =
               database.query(table, new String[] {TechFixDatabaseHelper.ID},
                              TechFixDatabaseHelper.REMOTE_ID + " IS NULL",
                              null, null, null, null)) {
        while (cursor.moveToNext()) {
          ContentValues values = new ContentValues();
          values.put(TechFixDatabaseHelper.REMOTE_ID,
                     UUID.randomUUID().toString());
          database.update(table, values, TechFixDatabaseHelper.ID + "=?",
                          new String[] {String.valueOf(cursor.getLong(0))});
        }
      }
    }
  }

  private Map<String, Object> galleryMetadata(long appointmentId) {
    String sql =
        "SELECT a." + TechFixDatabaseHelper.DEVICE_DETAILS + ",s." +
        TechFixDatabaseHelper.NAME + ",COALESCE(b." +
        TechFixDatabaseHelper.NAME + ",'TechFix') FROM " +
        TechFixDatabaseHelper.TABLE_APPOINTMENTS + " a JOIN " +
        TechFixDatabaseHelper.TABLE_REPAIR_SERVICES + " s ON s." +
        TechFixDatabaseHelper.ID + "=a." + TechFixDatabaseHelper.SERVICE_ID +
        " LEFT JOIN " + TechFixDatabaseHelper.TABLE_BRANCHES + " b ON b." +
        TechFixDatabaseHelper.ID + "=a." + TechFixDatabaseHelper.BRANCH_ID +
        " WHERE a." + TechFixDatabaseHelper.ID + "=?";
    Map<String, Object> metadata = new HashMap<>();
    try (Cursor cursor = helper.getReadableDatabase().rawQuery(
             sql, new String[] {String.valueOf(appointmentId)})) {
      if (cursor.moveToFirst()) {
        metadata.put("device", cursor.getString(0));
        metadata.put("serviceName", cursor.getString(1));
        metadata.put("branchName", cursor.getString(2));
      }
    }
    return metadata;
  }

  private String syncImage(FirebaseUser firebaseUser, String historyId,
                           String imagePath) throws Exception {
    if (imagePath == null || (!imagePath.startsWith("content://") &&
                              !imagePath.startsWith("file://")))
      return imagePath;
    Uri localImage = Uri.parse(imagePath);
    StorageReference image = FirebaseStorage.getInstance()
                                 .getReference()
                                 .child("repair-images")
                                 .child(firebaseUser.getUid())
                                 .child(historyId + ".jpg");
    String downloadUrl;
    try {
      Tasks.await(image.putFile(localImage));
      downloadUrl = Tasks.await(image.getDownloadUrl()).toString();
    } catch (Exception storageUnavailable) {
      throw storageUnavailable;
    }
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.IMAGE_PATH, downloadUrl);
    helper.getWritableDatabase().update(
        TechFixDatabaseHelper.TABLE_REPAIR_HISTORY, values,
        TechFixDatabaseHelper.REMOTE_ID + "=?", new String[] {historyId});
    if ("file".equals(localImage.getScheme()) && localImage.getPath() != null) {
      File localFile = new File(localImage.getPath());
      File imageDirectory =
          new File(getApplicationContext().getFilesDir(), "repair-images");
      try {
        if (localFile.getCanonicalPath().startsWith(
                imageDirectory.getCanonicalPath() + File.separator))
          localFile.delete();
      } catch (java.io.IOException ignored) {
        // The Firestore URL is already saved; cleanup can be retried later.
      }
    }
    return downloadUrl;
  }

  private Map<String, Object> base(Cursor cursor) {
    Map<String, Object> values = new HashMap<>();
    values.put("localId", number(cursor, TechFixDatabaseHelper.ID));
    values.put("remoteId", remoteId(cursor));
    values.put("updatedAt",
               number(cursor, TechFixDatabaseHelper.UPDATED_AT));
    return values;
  }

  private Map<String, Object> dynamicBase(Cursor cursor) {
    Map<String, Object> values = new HashMap<>();
    values.put("updatedAt",
               number(cursor, TechFixDatabaseHelper.UPDATED_AT));
    return values;
  }

  private void markSynced(String table, String documentId, long updatedAt) {
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.SYNC_DIRTY, 0);
    helper.getWritableDatabase().update(
        table, values,
        TechFixDatabaseHelper.REMOTE_ID + "=? AND " +
            TechFixDatabaseHelper.UPDATED_AT + "=?",
        new String[] {documentId, String.valueOf(updatedAt)});
  }

  private String referenceRemoteId(String table, long localId) {
    try (Cursor cursor = helper.getReadableDatabase().query(
             table, new String[] {TechFixDatabaseHelper.REMOTE_ID},
             TechFixDatabaseHelper.ID + "=?",
             new String[] {String.valueOf(localId)}, null, null, null, "1")) {
      if (!cursor.moveToFirst() || cursor.isNull(0))
        throw new IllegalStateException(
            "Reference data is missing a Firebase identifier: " + table +
            " #" + localId);
      return cursor.getString(0);
    }
  }

  private String appointmentCustomerUid(long appointmentId) {
    try (Cursor cursor = helper.getReadableDatabase().query(
             TechFixDatabaseHelper.TABLE_APPOINTMENTS,
             new String[] {TechFixDatabaseHelper.CUSTOMER_UID},
             TechFixDatabaseHelper.ID + "=?",
             new String[] {String.valueOf(appointmentId)}, null, null, null,
             "1")) {
      return cursor.moveToFirst() && !cursor.isNull(0) ? cursor.getString(0)
                                                       : null;
    }
  }

  private String id(Cursor cursor) {
    return String.valueOf(number(cursor, TechFixDatabaseHelper.ID));
  }

  private String remoteId(Cursor cursor) {
    return cursor.getString(
        cursor.getColumnIndexOrThrow(TechFixDatabaseHelper.REMOTE_ID));
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

  private Double nullableDecimal(Cursor cursor, String column) {
    int index = cursor.getColumnIndexOrThrow(column);
    return cursor.isNull(index) ? null : cursor.getDouble(index);
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
