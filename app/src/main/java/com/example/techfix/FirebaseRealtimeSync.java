package com.example.techfix;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class FirebaseRealtimeSync {
  private static final String TAG = "TechFixRealtimeSync";
  private static final List<ListenerRegistration> listeners = new ArrayList<>();
  private static final List<DataObserver> observers =
      new CopyOnWriteArrayList<>();
  private static List<DocumentSnapshot> latestBranches = Collections.emptyList();
  private static List<DocumentSnapshot> latestCategories = Collections.emptyList();
  private static List<DocumentSnapshot> latestServices = Collections.emptyList();
  private static List<DocumentSnapshot> latestTechnicians = Collections.emptyList();
  private static List<DocumentSnapshot> latestParts = Collections.emptyList();
  private static List<DocumentSnapshot> latestRequirements =
      Collections.emptyList();
  private static List<DocumentSnapshot> latestAppointments = Collections.emptyList();
  private static List<DocumentSnapshot> latestPayments = Collections.emptyList();
  private static List<DocumentSnapshot> latestHistory = Collections.emptyList();
  private static List<DocumentSnapshot> latestFeaturedHistory = Collections.emptyList();
  private static List<DocumentSnapshot> latestUsers = Collections.emptyList();
  private static String activeUserId;
  private static boolean featuredHistoryLoaded;

  private FirebaseRealtimeSync() {}

  public static synchronized void start(Context context) {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
      stop();
      return;
    }
    if (user.getUid().equals(activeUserId) && !listeners.isEmpty())
      return;
    stop();
    clearSnapshots();
    activeUserId = user.getUid();
    Context appContext = context.getApplicationContext();
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    listen(firestore.collection("branches"),
           documents -> {
             latestBranches = documents;
             reconcile(appContext);
           });
    listen(firestore.collection("deviceCategories"),
           documents -> {
             latestCategories = documents;
             reconcile(appContext);
           });
    listen(firestore.collection("services"),
           documents -> {
             latestServices = documents;
             reconcile(appContext);
           });
    listen(firestore.collection("technicians"),
           documents -> {
             latestTechnicians = documents;
             reconcile(appContext);
           });
    listen(firestore.collection("spareParts"),
           documents -> {
             latestParts = documents;
             reconcile(appContext);
           });
    listen(firestore.collection("servicePartRequirements"),
           documents -> {
             latestRequirements = documents;
             reconcile(appContext);
           });
    listen(firestore.collection("repairHistory").whereEqualTo("featured", true),
           documents -> {
             latestFeaturedHistory = documents;
             featuredHistoryLoaded = true;
             reconcile(appContext);
           });

    boolean manager = new SessionManager(appContext).isManager();
    if (manager) {
      listen(firestore.collection("users").whereEqualTo("role", "customer"),
             documents -> {
               latestUsers = documents;
               reconcile(appContext);
             });
    }
    Query appointments =
        manager ? firestore.collection("appointments")
                : firestore.collection("appointments")
                      .whereEqualTo("customerUid", user.getUid());
    Query payments =
        manager ? firestore.collection("payments")
                : firestore.collection("payments")
                      .whereEqualTo("customerUid", user.getUid());
    Query history = manager
                        ? firestore.collection("repairHistory")
                        : firestore.collection("repairHistory")
                              .whereEqualTo("customerUid", user.getUid());
    listen(appointments, documents -> {
      latestAppointments = documents;
      reconcile(appContext);
    });
    listen(payments, documents -> {
      latestPayments = documents;
      reconcile(appContext);
    });
    listen(history, documents -> {
      latestHistory = documents;
      reconcile(appContext);
    });
  }

  public static synchronized void stop() {
    for (ListenerRegistration listener : listeners)
      listener.remove();
    listeners.clear();
    activeUserId = null;
  }

  private static void clearSnapshots() {
    latestBranches = Collections.emptyList();
    latestCategories = Collections.emptyList();
    latestServices = Collections.emptyList();
    latestTechnicians = Collections.emptyList();
    latestParts = Collections.emptyList();
    latestRequirements = Collections.emptyList();
    latestAppointments = Collections.emptyList();
    latestPayments = Collections.emptyList();
    latestHistory = Collections.emptyList();
    latestFeaturedHistory = Collections.emptyList();
    latestUsers = Collections.emptyList();
    featuredHistoryLoaded = false;
  }

  public static void addObserver(DataObserver observer) {
    if (observer != null && !observers.contains(observer)) {
      observers.add(observer);
      observer.onDataChanged();
    }
  }

  public static void removeObserver(DataObserver observer) {
    observers.remove(observer);
  }

  private static synchronized void listen(Query query,
                                          DocumentConsumer consumer) {
    listeners.add(query.addSnapshotListener((snapshot, error) -> {
      if (error == null && snapshot != null) {
        try {
          consumer.accept(snapshot.getDocuments());
        } catch (RuntimeException exception) {
          Log.e(TAG, "Unable to apply Firebase snapshot", exception);
        }
      } else if (error != null) {
        Log.e(TAG, "Firebase listener failed", error);
      }
    }));
  }

  private static synchronized void reconcile(Context context) {
    try {
      applyBranches(context, latestBranches);
      applyCategories(context, latestCategories);
      applyServices(context, latestServices);
      applyTechnicians(context, latestTechnicians);
      applyParts(context, latestParts);
      applyServicePartRequirements(context, latestRequirements);
      applyUsers(context, latestUsers);
      applyAppointments(context, latestAppointments);
      applyPayments(context, latestPayments);
      applyHistory(context, latestHistory);
      applyHistory(context, latestFeaturedHistory);
      if (featuredHistoryLoaded)
        applyRepairSamples(context, latestFeaturedHistory);
      notifyDataChanged();
    } catch (RuntimeException exception) {
      Log.e(TAG, "Firebase reconciliation failed", exception);
    }
  }

  private static void notifyDataChanged() {
    for (DataObserver observer : observers)
      observer.onDataChanged();
  }

  private static void applyBranches(Context context,
                                    List<DocumentSnapshot> documents) {
    withDatabase(context, database -> {
      for (DocumentSnapshot document : documents) {
        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.NAME, text(document, "name"));
        values.put(TechFixDatabaseHelper.ADDRESS, text(document, "address"));
        values.put(TechFixDatabaseHelper.PHONE, text(document, "phone"));
        values.put(TechFixDatabaseHelper.LATITUDE,
                   decimal(document, "latitude"));
        values.put(TechFixDatabaseHelper.LONGITUDE,
                   decimal(document, "longitude"));
        values.put(TechFixDatabaseHelper.ACTIVE, bool(document, "active"));
        LocalSyncState.markSynced(values, number(document, "updatedAt"));
        upsertReference(database, TechFixDatabaseHelper.TABLE_BRANCHES,
                        document, values);
      }
    });
  }

  private static void applyCategories(Context context,
                                      List<DocumentSnapshot> documents) {
    withDatabase(context, database -> {
      for (DocumentSnapshot document : documents) {
        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.NAME, text(document, "name"));
        values.put(TechFixDatabaseHelper.DESCRIPTION,
                   text(document, "description"));
        values.put(TechFixDatabaseHelper.ACTIVE, bool(document, "active"));
        LocalSyncState.markSynced(values, number(document, "updatedAt"));
        upsertReference(database,
                        TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES,
                        document, values);
      }
    });
  }

  private static void applyServices(Context context,
                                    List<DocumentSnapshot> documents) {
    withDatabase(context, database -> {
      for (DocumentSnapshot document : documents) {
        ContentValues values = new ContentValues();
        long categoryId = referenceLocalId(
            database, TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES,
            document.getString("categoryRemoteId"),
            document.getLong("categoryId"));
        if (categoryId <= 0)
          continue;
        values.put(TechFixDatabaseHelper.CATEGORY_ID, categoryId);
        values.put(TechFixDatabaseHelper.NAME, text(document, "name"));
        values.put(TechFixDatabaseHelper.DESCRIPTION,
                   text(document, "description"));
        values.put(TechFixDatabaseHelper.BASE_PRICE_CENTS,
                   number(document, "basePriceCents"));
        values.put(TechFixDatabaseHelper.ESTIMATED_MINUTES,
                   number(document, "estimatedMinutes"));
        values.put(TechFixDatabaseHelper.ACTIVE, bool(document, "active"));
        LocalSyncState.markSynced(values, number(document, "updatedAt"));
        upsertReference(database,
                        TechFixDatabaseHelper.TABLE_REPAIR_SERVICES,
                        document, values);
      }
    });
  }

  private static void applyTechnicians(Context context,
                                       List<DocumentSnapshot> documents) {
    withDatabase(context, database -> {
      for (DocumentSnapshot document : documents) {
        ContentValues values = new ContentValues();
        long branchId = referenceLocalId(
            database, TechFixDatabaseHelper.TABLE_BRANCHES,
            document.getString("branchRemoteId"),
            document.getLong("branchId"));
        if (branchId <= 0)
          continue;
        values.put(TechFixDatabaseHelper.BRANCH_ID, branchId);
        values.put(TechFixDatabaseHelper.FULL_NAME, text(document, "fullName"));
        values.put(TechFixDatabaseHelper.EMAIL, text(document, "email"));
        values.put(TechFixDatabaseHelper.PHONE, text(document, "phone"));
        values.put(TechFixDatabaseHelper.SPECIALTY,
                   text(document, "specialty"));
        values.put(TechFixDatabaseHelper.ACTIVE, bool(document, "active"));
        LocalSyncState.markSynced(values, number(document, "updatedAt"));
        upsertReference(database, TechFixDatabaseHelper.TABLE_TECHNICIANS,
                        document, values);
      }
    });
  }

  private static void applyParts(Context context,
                                 List<DocumentSnapshot> documents) {
    withDatabase(context, database -> {
      for (DocumentSnapshot document : documents) {
        ContentValues values = new ContentValues();
        long branchId = referenceLocalId(
            database, TechFixDatabaseHelper.TABLE_BRANCHES,
            document.getString("branchRemoteId"),
            document.getLong("branchId"));
        if (branchId <= 0)
          continue;
        values.put(TechFixDatabaseHelper.BRANCH_ID, branchId);
        Long categoryRemoteLocalId = nullableReferenceLocalId(
            database, TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES,
            document.getString("categoryRemoteId"),
            document.getLong("categoryId"));
        putNullableLong(values, TechFixDatabaseHelper.CATEGORY_ID,
                        categoryRemoteLocalId);
        values.put(TechFixDatabaseHelper.NAME, text(document, "name"));
        values.put(TechFixDatabaseHelper.SKU, text(document, "sku"));
        values.put(TechFixDatabaseHelper.UNIT_PRICE_CENTS,
                   number(document, "unitPriceCents"));
        values.put(TechFixDatabaseHelper.QUANTITY_AVAILABLE,
                   number(document, "quantityAvailable"));
        values.put(TechFixDatabaseHelper.ACTIVE, bool(document, "active"));
        LocalSyncState.markSynced(values, number(document, "updatedAt"));
        upsertReference(database, TechFixDatabaseHelper.TABLE_SPARE_PARTS,
                        document, values);
      }
    });
  }

  private static void applyServicePartRequirements(
      Context context, List<DocumentSnapshot> documents) {
    withDatabase(context, database -> {
      for (DocumentSnapshot document : documents) {
        long serviceId = referenceLocalId(
            database, TechFixDatabaseHelper.TABLE_REPAIR_SERVICES,
            document.getString("serviceRemoteId"),
            document.getLong("serviceId"));
        long partId = referenceLocalId(
            database, TechFixDatabaseHelper.TABLE_SPARE_PARTS,
            document.getString("sparePartRemoteId"),
            document.getLong("sparePartId"));
        if (serviceId <= 0 || partId <= 0)
          continue;
        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.SERVICE_ID, serviceId);
        values.put(TechFixDatabaseHelper.SPARE_PART_ID, partId);
        values.put(TechFixDatabaseHelper.REQUIRED_QUANTITY,
                   Math.max(1, number(document, "requiredQuantity")));
        values.put(TechFixDatabaseHelper.ACTIVE, bool(document, "active"));
        LocalSyncState.markSynced(values, number(document, "updatedAt"));
        upsertReference(
            database, TechFixDatabaseHelper.TABLE_SERVICE_PART_REQUIREMENTS,
            document, values);
      }
    });
  }

  private static void applyAppointments(Context context,
                                        List<DocumentSnapshot> documents) {
    TechFixDatabaseHelper helper = new TechFixDatabaseHelper(context);
    SQLiteDatabase database = helper.getWritableDatabase();
    database.beginTransaction();
    try {
      for (DocumentSnapshot document : documents) {
        String email = text(document, "customerEmail");
        User user = helper.getOrCreateFirebaseUser(
            email.contains("@") ? email.substring(0, email.indexOf('@'))
                                : "Firebase customer",
            email.isEmpty() ? "remote." + document.getId() + "@techfix.local"
                            : email,
            document.getString("customerUid"));
        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.REMOTE_ID, document.getId());
        values.put(TechFixDatabaseHelper.USER_ID, user.getId());
        putNullableText(values, TechFixDatabaseHelper.CUSTOMER_UID,
                        document.getString("customerUid"));
        Long branchId = nullableReferenceLocalId(
            database, TechFixDatabaseHelper.TABLE_BRANCHES,
            firstText(document, "branchRemoteId", "branchDocumentId"),
            document.getLong("branchId"));
        Long technicianId = nullableReferenceLocalId(
            database, TechFixDatabaseHelper.TABLE_TECHNICIANS,
            firstText(document, "technicianDocumentId",
                      "technicianRemoteId"),
            document.getLong("technicianId"));
        long serviceId = referenceLocalId(
            database, TechFixDatabaseHelper.TABLE_REPAIR_SERVICES,
            document.getString("serviceRemoteId"),
            document.getLong("serviceId"));
        if (serviceId <= 0)
          continue;
        Long reservedPartId = nullableReferenceLocalId(
            database, TechFixDatabaseHelper.TABLE_SPARE_PARTS,
            firstText(document, "reservedPartId",
                      "reservedSparePartDocumentId"),
            document.getLong("reservedSparePartId"));
        putNullableLong(values, TechFixDatabaseHelper.BRANCH_ID, branchId);
        putNullableLong(values, TechFixDatabaseHelper.TECHNICIAN_ID,
                        technicianId);
        values.put(TechFixDatabaseHelper.SERVICE_ID, serviceId);
        putNullableLong(values, TechFixDatabaseHelper.RESERVED_PART_ID,
                        reservedPartId);
        values.put(TechFixDatabaseHelper.DEVICE_DETAILS,
                   text(document, "deviceDetails"));
        values.put(TechFixDatabaseHelper.PROBLEM_DESCRIPTION,
                   text(document, "problemDescription"));
        values.put(TechFixDatabaseHelper.STATUS, text(document, "status"));
        values.put(TechFixDatabaseHelper.APPOINTMENT_AT,
                   number(document, "appointmentAt"));
        putNullableDouble(values, TechFixDatabaseHelper.REQUEST_LATITUDE,
                          document.getDouble("requestLatitude"));
        putNullableDouble(values, TechFixDatabaseHelper.REQUEST_LONGITUDE,
                          document.getDouble("requestLongitude"));
        values.put(TechFixDatabaseHelper.CREATED_AT,
                   number(document, "createdAt"));
        LocalSyncState.markSynced(values, number(document, "updatedAt"));
        upsertByRemoteId(database, TechFixDatabaseHelper.TABLE_APPOINTMENTS,
                         document.getId(), values);
        syncLocalReservation(database, document, reservedPartId);
      }
      database.setTransactionSuccessful();
    } finally {
      database.endTransaction();
      helper.close();
    }
  }

  private static void applyUsers(Context context,
                                 List<DocumentSnapshot> documents) {
    try (TechFixDatabaseHelper helper = new TechFixDatabaseHelper(context)) {
      for (DocumentSnapshot document : documents) {
        String email = text(document, "email").trim();
        if (email.isEmpty())
          continue;
        String fullName = text(document, "fullName").trim();
        helper.getOrCreateFirebaseUser(
            fullName.isEmpty()
                ? (email.contains("@")
                       ? email.substring(0, email.indexOf('@'))
                       : "Firebase customer")
                : fullName,
            email, document.getId());
      }
    }
  }

  private static void applyPayments(Context context,
                                    List<DocumentSnapshot> documents) {
    withDatabase(context, database -> {
      for (DocumentSnapshot document : documents) {
        ContentValues values = new ContentValues();
        long appointmentId =
            appointmentId(database, document.getString("appointmentRemoteId"));
        if (appointmentId <= 0)
          continue;
        values.put(TechFixDatabaseHelper.REMOTE_ID, document.getId());
        values.put(TechFixDatabaseHelper.APPOINTMENT_ID, appointmentId);
        values.put(TechFixDatabaseHelper.AMOUNT_CENTS,
                   number(document, "amountCents"));
        values.put(TechFixDatabaseHelper.METHOD, text(document, "method"));
        values.put(TechFixDatabaseHelper.STATUS, text(document, "status"));
        putNullableText(values, TechFixDatabaseHelper.REFERENCE,
                        document.getString("reference"));
        putNullableLong(values, TechFixDatabaseHelper.PAID_AT,
                        document.getLong("paidAt"));
        values.put(TechFixDatabaseHelper.CREATED_AT,
                   number(document, "createdAt"));
        LocalSyncState.markSynced(values, number(document, "updatedAt"));
        upsertByRemoteId(database, TechFixDatabaseHelper.TABLE_PAYMENTS,
                         document.getId(), values);
      }
    });
  }

  private static void applyHistory(Context context,
                                   List<DocumentSnapshot> documents) {
    withDatabase(context, database -> {
      for (DocumentSnapshot document : documents) {
        ContentValues values = new ContentValues();
        long appointmentId =
            appointmentId(database, document.getString("appointmentRemoteId"));
        if (appointmentId <= 0)
          continue;
        values.put(TechFixDatabaseHelper.REMOTE_ID, document.getId());
        values.put(TechFixDatabaseHelper.APPOINTMENT_ID, appointmentId);
        values.put(TechFixDatabaseHelper.STATUS, text(document, "status"));
        values.put(TechFixDatabaseHelper.NOTES, text(document, "notes"));
        putNullableText(values, TechFixDatabaseHelper.IMAGE_PATH,
                        document.getString("imagePath"));
        values.put(TechFixDatabaseHelper.FEATURED,
                   bool(document, "featured"));
        values.put(TechFixDatabaseHelper.RECORDED_AT,
                   number(document, "recordedAt"));
        LocalSyncState.markSynced(values, number(document, "updatedAt"));
        upsertByRemoteId(database, TechFixDatabaseHelper.TABLE_REPAIR_HISTORY,
                         document.getId(), values);
      }
    });
  }

  private static void applyRepairSamples(
      Context context, List<DocumentSnapshot> documents) {
    withDatabase(context, database -> {
      List<String> remoteIds = new ArrayList<>();
      for (DocumentSnapshot document : documents) {
        String imagePath = text(document, "imagePath").trim();
        if (imagePath.isEmpty())
          continue;
        remoteIds.add(document.getId());
        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.REMOTE_ID, document.getId());
        values.put(TechFixDatabaseHelper.IMAGE_PATH, imagePath);
        values.put(TechFixDatabaseHelper.DEVICE_DETAILS,
                   text(document, "device"));
        values.put(TechFixDatabaseHelper.SERVICE_NAME,
                   text(document, "serviceName"));
        values.put(TechFixDatabaseHelper.BRANCH_NAME,
                   text(document, "branchName"));
        values.put(TechFixDatabaseHelper.UPDATED_AT,
                   number(document, "updatedAt"));
        int updated = database.update(
            TechFixDatabaseHelper.TABLE_REPAIR_SAMPLES, values,
            TechFixDatabaseHelper.REMOTE_ID + "=?",
            new String[] {document.getId()});
        if (updated == 0)
          database.insertOrThrow(TechFixDatabaseHelper.TABLE_REPAIR_SAMPLES,
                                 null, values);
      }
      deleteMissingSamples(database, remoteIds);
    });
  }

  private static void withDatabase(Context context, DatabaseConsumer consumer) {
    try (TechFixDatabaseHelper helper = new TechFixDatabaseHelper(context)) {
      SQLiteDatabase database = helper.getWritableDatabase();
      database.beginTransaction();
      try {
        consumer.accept(database);
        database.setTransactionSuccessful();
      } finally {
        database.endTransaction();
      }
    }
  }

  private static void upsertReference(SQLiteDatabase database, String table,
                                      DocumentSnapshot document,
                                      ContentValues values) {
    String remoteId = document.getId();
    values.put(TechFixDatabaseHelper.REMOTE_ID, remoteId);
    long remoteUpdatedAt = value(values, TechFixDatabaseHelper.UPDATED_AT);
    if (!shouldApplyRemote(database, table, TechFixDatabaseHelper.REMOTE_ID,
                           remoteId, remoteUpdatedAt))
      return;
    int updated = database.update(
        table, values, TechFixDatabaseHelper.REMOTE_ID + "=?",
        new String[] {remoteId});
    Long legacyId = document.getLong("localId");
    if (updated == 0 && legacyId != null &&
        remoteId.equals(String.valueOf(legacyId))) {
      updated = database.update(
          table, values, TechFixDatabaseHelper.ID + "=?",
          new String[] {String.valueOf(legacyId)});
    }
    if (updated == 0)
      database.insertOrThrow(table, null, values);
  }

  private static void upsertByRemoteId(SQLiteDatabase database, String table,
                                       String remoteId, ContentValues values) {
    long remoteUpdatedAt = value(values, TechFixDatabaseHelper.UPDATED_AT);
    if (!shouldApplyRemote(database, table, TechFixDatabaseHelper.REMOTE_ID,
                           remoteId, remoteUpdatedAt))
      return;
    int updated =
        database.update(table, values, TechFixDatabaseHelper.REMOTE_ID + "=?",
                        new String[] {remoteId});
    if (updated == 0)
      database.insertOrThrow(table, null, values);
  }

  private static long appointmentId(SQLiteDatabase database,
                                    String appointmentRemoteId) {
    if (appointmentRemoteId == null)
      return -1;
    try (android.database.Cursor cursor = database.query(
             TechFixDatabaseHelper.TABLE_APPOINTMENTS,
             new String[] {TechFixDatabaseHelper.ID},
             TechFixDatabaseHelper.REMOTE_ID + "=?",
             new String[] {appointmentRemoteId}, null, null, null, "1")) {
      return cursor.moveToFirst() ? cursor.getLong(0) : -1;
    }
  }

  private static long referenceLocalId(SQLiteDatabase database, String table,
                                       String remoteId, Long legacyId) {
    Long value = nullableReferenceLocalId(database, table, remoteId, legacyId);
    return value == null ? -1 : value;
  }

  private static Long nullableReferenceLocalId(SQLiteDatabase database,
                                               String table, String remoteId,
                                               Long legacyId) {
    if (remoteId != null && !remoteId.trim().isEmpty()) {
      try (android.database.Cursor cursor = database.query(
               table, new String[] {TechFixDatabaseHelper.ID},
               TechFixDatabaseHelper.REMOTE_ID + "=?",
               new String[] {remoteId.trim()}, null, null, null, "1")) {
        if (cursor.moveToFirst())
          return cursor.getLong(0);
      }
    }
    if (legacyId == null)
      return null;
    try (android.database.Cursor cursor = database.query(
             table, new String[] {TechFixDatabaseHelper.ID},
             TechFixDatabaseHelper.ID + "=?",
             new String[] {String.valueOf(legacyId)}, null, null, null, "1")) {
      return cursor.moveToFirst() ? cursor.getLong(0) : null;
    }
  }

  private static String firstText(DocumentSnapshot document,
                                  String firstField, String secondField) {
    String value = document.getString(firstField);
    return value == null || value.trim().isEmpty()
        ? document.getString(secondField)
        : value;
  }

  private static void syncLocalReservation(SQLiteDatabase database,
                                           DocumentSnapshot document,
                                           Long reservedPartId) {
    long appointmentId = appointmentId(database, document.getId());
    if (appointmentId <= 0)
      return;
    String appointmentStatus = text(document, "status");
    String reservationStatus = text(document, "partReservationState");
    if (reservationStatus.isEmpty()) {
      if ("CANCELLED".equals(appointmentStatus))
        reservationStatus = "RELEASED";
      else if ("COMPLETED".equals(appointmentStatus))
        reservationStatus = "CONSUMED";
      else
        reservationStatus = "RESERVED";
    }
    long updatedAt = number(document, "updatedAt");
    if (reservedPartId == null) {
      ContentValues released = new ContentValues();
      released.put(TechFixDatabaseHelper.RESERVATION_STATUS,
                   "COMPLETED".equals(appointmentStatus) ? "CONSUMED"
                                                         : "RELEASED");
      released.put(TechFixDatabaseHelper.UPDATED_AT, updatedAt);
      released.put(TechFixDatabaseHelper.SYNC_DIRTY, 0);
      database.update(
          TechFixDatabaseHelper.TABLE_APPOINTMENT_PART_RESERVATIONS,
          released, TechFixDatabaseHelper.APPOINTMENT_ID + "=?",
          new String[] {String.valueOf(appointmentId)});
      return;
    }

    ContentValues oldReservations = new ContentValues();
    oldReservations.put(TechFixDatabaseHelper.RESERVATION_STATUS, "RELEASED");
    oldReservations.put(TechFixDatabaseHelper.UPDATED_AT, updatedAt);
    oldReservations.put(TechFixDatabaseHelper.SYNC_DIRTY, 0);
    database.update(
        TechFixDatabaseHelper.TABLE_APPOINTMENT_PART_RESERVATIONS,
        oldReservations,
        TechFixDatabaseHelper.APPOINTMENT_ID + "=? AND " +
            TechFixDatabaseHelper.SPARE_PART_ID + "<>? AND " +
            TechFixDatabaseHelper.RESERVATION_STATUS + "='RESERVED'",
        new String[] {String.valueOf(appointmentId),
                      String.valueOf(reservedPartId)});

    String partRemoteId = referenceRemoteId(
        database, TechFixDatabaseHelper.TABLE_SPARE_PARTS, reservedPartId);
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.REMOTE_ID,
               document.getId() + ":" + partRemoteId);
    values.put(TechFixDatabaseHelper.APPOINTMENT_ID, appointmentId);
    values.put(TechFixDatabaseHelper.SPARE_PART_ID, reservedPartId);
    values.put(TechFixDatabaseHelper.RESERVED_QUANTITY, 1);
    values.put(TechFixDatabaseHelper.RESERVATION_STATUS, reservationStatus);
    values.put(TechFixDatabaseHelper.CREATED_AT,
               number(document, "assignedAt") > 0
                   ? number(document, "assignedAt")
                   : updatedAt);
    values.put(TechFixDatabaseHelper.UPDATED_AT, updatedAt);
    values.put(TechFixDatabaseHelper.SYNC_DIRTY, 0);
    int updated = database.update(
        TechFixDatabaseHelper.TABLE_APPOINTMENT_PART_RESERVATIONS, values,
        TechFixDatabaseHelper.APPOINTMENT_ID + "=? AND " +
            TechFixDatabaseHelper.SPARE_PART_ID + "=?",
        new String[] {String.valueOf(appointmentId),
                      String.valueOf(reservedPartId)});
    if (updated == 0)
      database.insertOrThrow(
          TechFixDatabaseHelper.TABLE_APPOINTMENT_PART_RESERVATIONS, null,
          values);
  }

  private static String referenceRemoteId(SQLiteDatabase database,
                                          String table, long localId) {
    try (android.database.Cursor cursor = database.query(
             table, new String[] {TechFixDatabaseHelper.REMOTE_ID},
             TechFixDatabaseHelper.ID + "=?",
             new String[] {String.valueOf(localId)}, null, null, null, "1")) {
      return cursor.moveToFirst() && !cursor.isNull(0) ? cursor.getString(0)
                                                       : String.valueOf(localId);
    }
  }

  private static void deleteMissingSamples(SQLiteDatabase database,
                                           List<String> remoteIds) {
    List<String> stale = new ArrayList<>();
    try (android.database.Cursor cursor = database.query(
             TechFixDatabaseHelper.TABLE_REPAIR_SAMPLES,
             new String[] {TechFixDatabaseHelper.REMOTE_ID}, null, null, null,
             null, null)) {
      while (cursor.moveToNext()) {
        String remoteId = cursor.getString(0);
        if (!remoteIds.contains(remoteId))
          stale.add(remoteId);
      }
    }
    for (String remoteId : stale)
      database.delete(TechFixDatabaseHelper.TABLE_REPAIR_SAMPLES,
                      TechFixDatabaseHelper.REMOTE_ID + "=?",
                      new String[] {remoteId});
  }

  private static long localId(DocumentSnapshot document) {
    Long value = document.getLong("localId");
    if (value == null)
      throw new IllegalArgumentException(
          "Remote document is missing localId: " + document.getId());
    return value;
  }

  private static long number(DocumentSnapshot document, String field) {
    Object value = document.get(field);
    if (value instanceof Number)
      return ((Number)value).longValue();
    if (value instanceof Timestamp)
      return ((Timestamp)value).toDate().getTime();
    if (value instanceof java.util.Date)
      return ((java.util.Date)value).getTime();
    return 0;
  }

  private static double decimal(DocumentSnapshot document, String field) {
    Object value = document.get(field);
    return value instanceof Number ? ((Number)value).doubleValue() : 0;
  }

  private static String text(DocumentSnapshot document, String field) {
    String value = document.getString(field);
    return value == null ? "" : value;
  }

  private static int bool(DocumentSnapshot document, String field) {
    Boolean value = document.getBoolean(field);
    return Boolean.TRUE.equals(value) ? 1 : 0;
  }

  private static void putNullableLong(ContentValues values, String field,
                                      Long value) {
    if (value == null)
      values.putNull(field);
    else
      values.put(field, value);
  }

  private static void putNullableText(ContentValues values, String field,
                                      String value) {
    if (value == null || value.trim().isEmpty())
      values.putNull(field);
    else
      values.put(field, value.trim());
  }

  private static void putNullableDouble(ContentValues values, String field,
                                        Double value) {
    if (value == null)
      values.putNull(field);
    else
      values.put(field, value);
  }

  private static boolean shouldApplyRemote(SQLiteDatabase database,
                                           String table,
                                           String identityColumn,
                                           String identity,
                                           long remoteUpdatedAt) {
    try (android.database.Cursor cursor = database.query(
             table,
             new String[] {TechFixDatabaseHelper.SYNC_DIRTY,
                           TechFixDatabaseHelper.UPDATED_AT},
             identityColumn + "=?", new String[] {identity}, null, null, null,
             "1")) {
      if (!cursor.moveToFirst())
        return true;
      boolean dirty = cursor.getInt(0) == 1;
      long localUpdatedAt = cursor.getLong(1);
      return !dirty || remoteUpdatedAt >= localUpdatedAt;
    }
  }

  private static long value(ContentValues values, String key) {
    Long value = values.getAsLong(key);
    return value == null ? 0 : value;
  }

  private interface DocumentConsumer {
    void accept(List<DocumentSnapshot> documents);
  }

  private interface DatabaseConsumer {
    void accept(SQLiteDatabase database);
  }

  public interface DataObserver {
    void onDataChanged();
  }
}
