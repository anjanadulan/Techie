package com.example.techfix;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public final class FirebaseRealtimeSync {
  private static final List<ListenerRegistration> listeners = new ArrayList<>();

  private FirebaseRealtimeSync() {}

  public static synchronized void start(Context context) {
    stop();
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null)
      return;
    Context appContext = context.getApplicationContext();
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    listen(firestore.collection("branches"),
           documents -> applyBranches(appContext, documents));
    listen(firestore.collection("deviceCategories"),
           documents -> applyCategories(appContext, documents));
    listen(firestore.collection("services"),
           documents -> applyServices(appContext, documents));
    listen(firestore.collection("technicians"),
           documents -> applyTechnicians(appContext, documents));
    listen(firestore.collection("spareParts"),
           documents -> applyParts(appContext, documents));

    firestore.collection("users")
        .document(user.getUid())
        .get()
        .addOnSuccessListener(profile -> {
          FirebaseUser currentUser =
              FirebaseAuth.getInstance().getCurrentUser();
          if (currentUser == null ||
              !currentUser.getUid().equals(user.getUid()))
            return;
          boolean manager = "manager".equals(profile.getString("role"));
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
          listen(appointments,
                 documents -> applyAppointments(appContext, documents));
          listen(payments, documents -> applyPayments(appContext, documents));
          listen(history, documents -> applyHistory(appContext, documents));
        });
  }

  public static synchronized void stop() {
    for (ListenerRegistration listener : listeners)
      listener.remove();
    listeners.clear();
  }

  private static synchronized void listen(Query query,
                                          DocumentConsumer consumer) {
    listeners.add(query.addSnapshotListener((snapshot, error) -> {
      if (error == null && snapshot != null) {
        try {
          consumer.accept(snapshot.getDocuments());
        } catch (RuntimeException ignored) {
          // A later snapshot or periodic upload can repair malformed or
          // out-of-order local data.
        }
      }
    }));
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
        upsert(database, TechFixDatabaseHelper.TABLE_BRANCHES,
               localId(document), values);
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
        upsert(database, TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES,
               localId(document), values);
      }
    });
  }

  private static void applyServices(Context context,
                                    List<DocumentSnapshot> documents) {
    withDatabase(context, database -> {
      for (DocumentSnapshot document : documents) {
        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.CATEGORY_ID,
                   number(document, "categoryId"));
        values.put(TechFixDatabaseHelper.NAME, text(document, "name"));
        values.put(TechFixDatabaseHelper.DESCRIPTION,
                   text(document, "description"));
        values.put(TechFixDatabaseHelper.BASE_PRICE_CENTS,
                   number(document, "basePriceCents"));
        values.put(TechFixDatabaseHelper.ESTIMATED_MINUTES,
                   number(document, "estimatedMinutes"));
        values.put(TechFixDatabaseHelper.ACTIVE, bool(document, "active"));
        upsert(database, TechFixDatabaseHelper.TABLE_REPAIR_SERVICES,
               localId(document), values);
      }
    });
  }

  private static void applyTechnicians(Context context,
                                       List<DocumentSnapshot> documents) {
    withDatabase(context, database -> {
      for (DocumentSnapshot document : documents) {
        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.BRANCH_ID,
                   number(document, "branchId"));
        values.put(TechFixDatabaseHelper.FULL_NAME, text(document, "fullName"));
        values.put(TechFixDatabaseHelper.EMAIL, text(document, "email"));
        values.put(TechFixDatabaseHelper.PHONE, text(document, "phone"));
        values.put(TechFixDatabaseHelper.SPECIALTY,
                   text(document, "specialty"));
        values.put(TechFixDatabaseHelper.ACTIVE, bool(document, "active"));
        upsert(database, TechFixDatabaseHelper.TABLE_TECHNICIANS,
               localId(document), values);
      }
    });
  }

  private static void applyParts(Context context,
                                 List<DocumentSnapshot> documents) {
    withDatabase(context, database -> {
      for (DocumentSnapshot document : documents) {
        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.BRANCH_ID,
                   number(document, "branchId"));
        putNullableLong(values, TechFixDatabaseHelper.CATEGORY_ID,
                        document.getLong("categoryId"));
        values.put(TechFixDatabaseHelper.NAME, text(document, "name"));
        values.put(TechFixDatabaseHelper.SKU, text(document, "sku"));
        values.put(TechFixDatabaseHelper.UNIT_PRICE_CENTS,
                   number(document, "unitPriceCents"));
        values.put(TechFixDatabaseHelper.QUANTITY_AVAILABLE,
                   number(document, "quantityAvailable"));
        values.put(TechFixDatabaseHelper.ACTIVE, bool(document, "active"));
        upsert(database, TechFixDatabaseHelper.TABLE_SPARE_PARTS,
               localId(document), values);
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
                            : email);
        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.USER_ID, user.getId());
        putNullableLong(values, TechFixDatabaseHelper.BRANCH_ID,
                        document.getLong("branchId"));
        putNullableLong(values, TechFixDatabaseHelper.TECHNICIAN_ID,
                        document.getLong("technicianId"));
        values.put(TechFixDatabaseHelper.SERVICE_ID,
                   number(document, "serviceId"));
        values.put(TechFixDatabaseHelper.DEVICE_DETAILS,
                   text(document, "deviceDetails"));
        values.put(TechFixDatabaseHelper.PROBLEM_DESCRIPTION,
                   text(document, "problemDescription"));
        values.put(TechFixDatabaseHelper.STATUS, text(document, "status"));
        values.put(TechFixDatabaseHelper.APPOINTMENT_AT,
                   number(document, "appointmentAt"));
        values.put(TechFixDatabaseHelper.CREATED_AT,
                   number(document, "createdAt"));
        upsert(database, TechFixDatabaseHelper.TABLE_APPOINTMENTS,
               localId(document), values);
      }
      database.setTransactionSuccessful();
    } finally {
      database.endTransaction();
      helper.close();
    }
  }

  private static void applyPayments(Context context,
                                    List<DocumentSnapshot> documents) {
    withDatabase(context, database -> {
      for (DocumentSnapshot document : documents) {
        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.APPOINTMENT_ID,
                   number(document, "appointmentId"));
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
        upsert(database, TechFixDatabaseHelper.TABLE_PAYMENTS,
               localId(document), values);
      }
    });
  }

  private static void applyHistory(Context context,
                                   List<DocumentSnapshot> documents) {
    withDatabase(context, database -> {
      for (DocumentSnapshot document : documents) {
        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.APPOINTMENT_ID,
                   number(document, "appointmentId"));
        values.put(TechFixDatabaseHelper.STATUS, text(document, "status"));
        values.put(TechFixDatabaseHelper.NOTES, text(document, "notes"));
        putNullableText(values, TechFixDatabaseHelper.IMAGE_PATH,
                        document.getString("imagePath"));
        values.put(TechFixDatabaseHelper.RECORDED_AT,
                   number(document, "recordedAt"));
        upsert(database, TechFixDatabaseHelper.TABLE_REPAIR_HISTORY,
               localId(document), values);
      }
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

  private static void upsert(SQLiteDatabase database, String table, long id,
                             ContentValues values) {
    int updated =
        database.update(table, values, TechFixDatabaseHelper.ID + "=?",
                        new String[] {String.valueOf(id)});
    if (updated == 0) {
      values.put(TechFixDatabaseHelper.ID, id);
      database.insertOrThrow(table, null, values);
    }
  }

  private static long localId(DocumentSnapshot document) {
    Long value = document.getLong("localId");
    if (value == null)
      throw new IllegalArgumentException(
          "Remote document is missing localId: " + document.getId());
    return value;
  }

  private static long number(DocumentSnapshot document, String field) {
    Long value = document.getLong(field);
    return value == null ? 0 : value;
  }

  private static double decimal(DocumentSnapshot document, String field) {
    Double value = document.getDouble(field);
    return value == null ? 0 : value;
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

  private interface DocumentConsumer {
    void accept(List<DocumentSnapshot> documents);
  }

  private interface DatabaseConsumer {
    void accept(SQLiteDatabase database);
  }
}
