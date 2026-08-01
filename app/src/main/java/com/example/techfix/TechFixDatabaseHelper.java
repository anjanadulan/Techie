package com.example.techfix;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Locale;
import java.util.UUID;

public class TechFixDatabaseHelper extends SQLiteOpenHelper {
  static final String DATABASE_NAME = "techfix.db";
  static final int DATABASE_VERSION = 7;

  static final String TABLE_USERS = "users";
  static final String TABLE_BRANCHES = "branches";
  static final String TABLE_TECHNICIANS = "technicians";
  static final String TABLE_DEVICE_CATEGORIES = "device_categories";
  static final String TABLE_REPAIR_SERVICES = "repair_services";
  static final String TABLE_SPARE_PARTS = "spare_parts";
  static final String TABLE_APPOINTMENTS = "appointments";
  static final String TABLE_PAYMENTS = "payments";
  static final String TABLE_REPAIR_HISTORY = "repair_history";
  static final String TABLE_SERVICE_PART_REQUIREMENTS =
      "service_part_requirements";
  static final String TABLE_APPOINTMENT_PART_RESERVATIONS =
      "appointment_part_reservations";
  static final String TABLE_REPAIR_SAMPLES = "repair_samples";

  static final String ID = "_id";
  static final String FULL_NAME = "full_name";
  static final String EMAIL = "email";
  static final String FIREBASE_UID = "firebase_uid";
  static final String PASSWORD_HASH = "password_hash";
  static final String PASSWORD_SALT = "password_salt";
  static final String NAME = "name";
  static final String ADDRESS = "address";
  static final String PHONE = "phone";
  static final String LATITUDE = "latitude";
  static final String LONGITUDE = "longitude";
  static final String ACTIVE = "active";
  static final String BRANCH_ID = "branch_id";
  static final String CATEGORY_ID = "category_id";
  static final String TECHNICIAN_ID = "technician_id";
  static final String USER_ID = "user_id";
  static final String SERVICE_ID = "service_id";
  static final String SPARE_PART_ID = "spare_part_id";
  static final String RESERVED_PART_ID = "reserved_part_id";
  static final String APPOINTMENT_ID = "appointment_id";
  static final String SPECIALTY = "specialty";
  static final String DESCRIPTION = "description";
  static final String BASE_PRICE_CENTS = "base_price_cents";
  static final String ESTIMATED_MINUTES = "estimated_minutes";
  static final String SKU = "sku";
  static final String UNIT_PRICE_CENTS = "unit_price_cents";
  static final String QUANTITY_AVAILABLE = "quantity_available";
  static final String DEVICE_DETAILS = "device_details";
  static final String PROBLEM_DESCRIPTION = "problem_description";
  static final String STATUS = "status";
  static final String APPOINTMENT_AT = "appointment_at";
  static final String REQUEST_LATITUDE = "request_latitude";
  static final String REQUEST_LONGITUDE = "request_longitude";
  static final String AMOUNT_CENTS = "amount_cents";
  static final String METHOD = "method";
  static final String REFERENCE = "reference";
  static final String PAID_AT = "paid_at";
  static final String NOTES = "notes";
  static final String IMAGE_PATH = "image_path";
  static final String FEATURED = "featured";
  static final String REQUIRED_QUANTITY = "required_quantity";
  static final String RESERVED_QUANTITY = "reserved_quantity";
  static final String RESERVATION_STATUS = "reservation_status";
  static final String SERVICE_NAME = "service_name";
  static final String BRANCH_NAME = "branch_name";
  static final String RECORDED_AT = "recorded_at";
  static final String CREATED_AT = "created_at";
  static final String REMOTE_ID = "remote_id";
  static final String CUSTOMER_UID = "customer_uid";
  static final String SYNC_DIRTY = "sync_dirty";
  static final String UPDATED_AT = "updated_at";

  private static final String APPOINTMENT_STATUS_CHECK =
      " CHECK (" + STATUS + " IN ('PENDING','ASSIGNED','IN_PROGRESS',"
      + "'WAITING_FOR_PARTS','READY_FOR_PAYMENT','COMPLETED','CANCELLED'))";
  private static final String PAYMENT_METHOD_CHECK =
      " CHECK (" + METHOD + " IN ('CASH','CARD','BANK_TRANSFER','ONLINE'))";
  private static final String PAYMENT_STATUS_CHECK =
      " CHECK (" + STATUS + " IN ('PENDING','PAID','FAILED','REFUNDED'))";
  private static final String RESERVATION_STATUS_CHECK =
      " CHECK (" + RESERVATION_STATUS +
      " IN ('RESERVED','CONSUMED','RELEASED'))";
  private static final long SEED_UPDATED_AT = 1L;

  public TechFixDatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onConfigure(SQLiteDatabase database) {
    super.onConfigure(database);
    database.setForeignKeyConstraintsEnabled(true);
  }

  @Override
  public void onCreate(SQLiteDatabase database) {
    createUsersTable(database);
    createCoreTables(database);
    migrateToV7(database);
    seedReferenceData(database);
    seedDefaultPartRequirements(database);
  }

  @Override
  public void onUpgrade(SQLiteDatabase database, int oldVersion,
                        int newVersion) {
    database.beginTransaction();
    try {
      if (oldVersion < 2) {
        createCoreTables(database);
      }
      if (oldVersion < 5) {
        addRemoteIdentifiers(database);
      }
      if (oldVersion < 6) {
        addSyncMetadata(database);
      }
      if (oldVersion < 7) {
        migrateToV7(database);
      }
      if (oldVersion < 4) {
        seedReferenceData(database);
      }
      if (oldVersion < 7) {
        seedDefaultPartRequirements(database);
      }
      database.setTransactionSuccessful();
    } finally {
      database.endTransaction();
    }
  }

  private void createUsersTable(SQLiteDatabase database) {
    database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" + ID +
                     " INTEGER PRIMARY KEY AUTOINCREMENT, " + FULL_NAME +
                     " TEXT NOT NULL, " + EMAIL +
                     " TEXT NOT NULL UNIQUE COLLATE NOCASE, " +
                     FIREBASE_UID + " TEXT, " +
                     PASSWORD_HASH + " TEXT NOT NULL, " + PASSWORD_SALT +
                     " TEXT NOT NULL, " + CREATED_AT + " INTEGER NOT NULL"
                     + ")");
    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_firebase_uid " +
                     "ON " + TABLE_USERS + "(" + FIREBASE_UID + ")");
  }

  private void createCoreTables(SQLiteDatabase database) {
    database.execSQL(
        "CREATE TABLE IF NOT EXISTS " + TABLE_BRANCHES + " (" + ID +
        " INTEGER PRIMARY KEY AUTOINCREMENT, " + REMOTE_ID +
        " TEXT NOT NULL, " + NAME +
        " TEXT NOT NULL UNIQUE COLLATE NOCASE, " + ADDRESS +
        " TEXT NOT NULL, " + PHONE + " TEXT NOT NULL, " + LATITUDE +
        " REAL NOT NULL, " + LONGITUDE + " REAL NOT NULL, " + ACTIVE +
        " INTEGER NOT NULL DEFAULT 1 CHECK (" + ACTIVE + " IN (0, 1)), " +
        UPDATED_AT + " INTEGER NOT NULL DEFAULT 0, " + SYNC_DIRTY +
        " INTEGER NOT NULL DEFAULT 1 CHECK (" + SYNC_DIRTY + " IN (0, 1))"
        + ")");

    database.execSQL(
        "CREATE TABLE IF NOT EXISTS " + TABLE_TECHNICIANS + " (" + ID +
        " INTEGER PRIMARY KEY AUTOINCREMENT, " + REMOTE_ID +
        " TEXT NOT NULL, " + BRANCH_ID +
        " INTEGER NOT NULL, " + FULL_NAME + " TEXT NOT NULL, " + EMAIL +
        " TEXT NOT NULL UNIQUE COLLATE NOCASE, " + PHONE +
        " TEXT NOT NULL, " + SPECIALTY + " TEXT NOT NULL, " + ACTIVE +
        " INTEGER NOT NULL DEFAULT 1 CHECK (" + ACTIVE + " IN (0, 1)), "
        + UPDATED_AT + " INTEGER NOT NULL DEFAULT 0, " + SYNC_DIRTY +
        " INTEGER NOT NULL DEFAULT 1 CHECK (" + SYNC_DIRTY + " IN (0, 1)), "
        + "FOREIGN KEY (" + BRANCH_ID + ") REFERENCES " + TABLE_BRANCHES +
        "(" + ID + ") ON UPDATE CASCADE ON DELETE RESTRICT"
        + ")");

    database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_DEVICE_CATEGORIES +
                     " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     REMOTE_ID + " TEXT NOT NULL, " + NAME +
                     " TEXT NOT NULL UNIQUE COLLATE NOCASE, " +
                     DESCRIPTION + " TEXT NOT NULL, " + ACTIVE +
                     " INTEGER NOT NULL DEFAULT 1 CHECK (" + ACTIVE +
                     " IN (0, 1)), " + UPDATED_AT +
                     " INTEGER NOT NULL DEFAULT 0, " + SYNC_DIRTY +
                     " INTEGER NOT NULL DEFAULT 1 CHECK (" + SYNC_DIRTY +
                     " IN (0, 1))"
                     + ")");

    database.execSQL(
        "CREATE TABLE IF NOT EXISTS " + TABLE_REPAIR_SERVICES + " (" + ID +
        " INTEGER PRIMARY KEY AUTOINCREMENT, " + REMOTE_ID +
        " TEXT NOT NULL, " + CATEGORY_ID +
        " INTEGER NOT NULL, " + NAME + " TEXT NOT NULL, " + DESCRIPTION +
        " TEXT NOT NULL, " + BASE_PRICE_CENTS + " INTEGER NOT NULL CHECK (" +
        BASE_PRICE_CENTS + " >= 0), " + ESTIMATED_MINUTES +
        " INTEGER NOT NULL CHECK (" + ESTIMATED_MINUTES + " > 0), " + ACTIVE +
        " INTEGER NOT NULL DEFAULT 1 CHECK (" + ACTIVE + " IN (0, 1)), "
        + UPDATED_AT + " INTEGER NOT NULL DEFAULT 0, " + SYNC_DIRTY +
        " INTEGER NOT NULL DEFAULT 1 CHECK (" + SYNC_DIRTY + " IN (0, 1)), "
        + "UNIQUE (" + CATEGORY_ID + ", " + NAME + "), "
        + "FOREIGN KEY (" + CATEGORY_ID + ") REFERENCES " +
        TABLE_DEVICE_CATEGORIES + "(" + ID +
        ") ON UPDATE CASCADE ON DELETE RESTRICT"
        + ")");

    database.execSQL(
        "CREATE TABLE IF NOT EXISTS " + TABLE_SPARE_PARTS + " (" + ID +
        " INTEGER PRIMARY KEY AUTOINCREMENT, " + REMOTE_ID +
        " TEXT NOT NULL, " + BRANCH_ID +
        " INTEGER NOT NULL, " + CATEGORY_ID + " INTEGER, " + NAME +
        " TEXT NOT NULL, " + SKU + " TEXT NOT NULL, " + UNIT_PRICE_CENTS +
        " INTEGER NOT NULL CHECK (" + UNIT_PRICE_CENTS + " >= 0), " +
        QUANTITY_AVAILABLE + " INTEGER NOT NULL DEFAULT 0 CHECK (" +
        QUANTITY_AVAILABLE + " >= 0), " + ACTIVE +
        " INTEGER NOT NULL DEFAULT 1 CHECK (" + ACTIVE + " IN (0, 1)), "
        + UPDATED_AT + " INTEGER NOT NULL DEFAULT 0, " + SYNC_DIRTY +
        " INTEGER NOT NULL DEFAULT 1 CHECK (" + SYNC_DIRTY + " IN (0, 1)), "
        + "UNIQUE (" + BRANCH_ID + ", " + SKU + "), "
        + "FOREIGN KEY (" + BRANCH_ID + ") REFERENCES " + TABLE_BRANCHES +
        "(" + ID + ") ON UPDATE CASCADE ON DELETE RESTRICT, "
        + "FOREIGN KEY (" + CATEGORY_ID + ") REFERENCES " +
        TABLE_DEVICE_CATEGORIES + "(" + ID +
        ") ON UPDATE CASCADE ON DELETE SET NULL"
        + ")");

    database.execSQL(
        "CREATE TABLE IF NOT EXISTS " + TABLE_APPOINTMENTS + " (" + ID +
        " INTEGER PRIMARY KEY AUTOINCREMENT, " + REMOTE_ID +
        " TEXT NOT NULL UNIQUE, " + CUSTOMER_UID + " TEXT, " + USER_ID +
        " INTEGER NOT NULL, " + BRANCH_ID + " INTEGER, " + TECHNICIAN_ID +
        " INTEGER, " + SERVICE_ID + " INTEGER NOT NULL, " +
        RESERVED_PART_ID + " INTEGER, " + DEVICE_DETAILS + " TEXT NOT NULL, " +
        PROBLEM_DESCRIPTION + " TEXT NOT NULL, " + STATUS + " TEXT NOT NULL" +
        APPOINTMENT_STATUS_CHECK + ", " + APPOINTMENT_AT +
        " INTEGER NOT NULL, " + REQUEST_LATITUDE + " REAL, " +
        REQUEST_LONGITUDE + " REAL, " + CREATED_AT + " INTEGER NOT NULL, " +
        UPDATED_AT + " INTEGER NOT NULL, " + SYNC_DIRTY +
        " INTEGER NOT NULL DEFAULT 1 CHECK (" + SYNC_DIRTY + " IN (0, 1)), "
        + "FOREIGN KEY (" + USER_ID + ") REFERENCES " + TABLE_USERS + "(" +
        ID + ") ON UPDATE CASCADE ON DELETE RESTRICT, "
        + "FOREIGN KEY (" + BRANCH_ID + ") REFERENCES " + TABLE_BRANCHES +
        "(" + ID + ") ON UPDATE CASCADE ON DELETE RESTRICT, "
        + "FOREIGN KEY (" + TECHNICIAN_ID + ") REFERENCES " +
        TABLE_TECHNICIANS + "(" + ID +
        ") ON UPDATE CASCADE ON DELETE RESTRICT, "
        + "FOREIGN KEY (" + SERVICE_ID + ") REFERENCES " +
        TABLE_REPAIR_SERVICES + "(" + ID +
        ") ON UPDATE CASCADE ON DELETE RESTRICT, "
        + "FOREIGN KEY (" + RESERVED_PART_ID + ") REFERENCES " +
        TABLE_SPARE_PARTS + "(" + ID +
        ") ON UPDATE CASCADE ON DELETE SET NULL"
        + ")");

    database.execSQL(
        "CREATE TABLE IF NOT EXISTS " + TABLE_PAYMENTS + " (" + ID +
        " INTEGER PRIMARY KEY AUTOINCREMENT, " + REMOTE_ID +
        " TEXT NOT NULL UNIQUE, " + APPOINTMENT_ID + " INTEGER NOT NULL, " +
        AMOUNT_CENTS + " INTEGER NOT NULL CHECK (" + AMOUNT_CENTS + " >= 0), " +
        METHOD + " TEXT NOT NULL" + PAYMENT_METHOD_CHECK + ", " + STATUS +
        " TEXT NOT NULL" + PAYMENT_STATUS_CHECK + ", " + REFERENCE +
        " TEXT, " + PAID_AT + " INTEGER, " + CREATED_AT +
        " INTEGER NOT NULL, " + UPDATED_AT + " INTEGER NOT NULL, " +
        SYNC_DIRTY + " INTEGER NOT NULL DEFAULT 1 CHECK (" + SYNC_DIRTY +
        " IN (0, 1)), "
        + "FOREIGN KEY (" + APPOINTMENT_ID + ") REFERENCES " +
        TABLE_APPOINTMENTS + "(" + ID + ") ON UPDATE CASCADE ON DELETE CASCADE"
        + ")");

    database.execSQL(
        "CREATE TABLE IF NOT EXISTS " + TABLE_REPAIR_HISTORY + " (" + ID +
        " INTEGER PRIMARY KEY AUTOINCREMENT, " + REMOTE_ID +
        " TEXT NOT NULL UNIQUE, " + APPOINTMENT_ID + " INTEGER NOT NULL, " + STATUS +
        " TEXT NOT NULL" + APPOINTMENT_STATUS_CHECK + ", " + NOTES +
        " TEXT NOT NULL, " + IMAGE_PATH + " TEXT, " + FEATURED +
        " INTEGER NOT NULL DEFAULT 0 CHECK (" + FEATURED + " IN (0, 1)), " +
        RECORDED_AT +
        " INTEGER NOT NULL, " + UPDATED_AT + " INTEGER NOT NULL, " +
        SYNC_DIRTY + " INTEGER NOT NULL DEFAULT 1 CHECK (" + SYNC_DIRTY +
        " IN (0, 1)), "
        + "FOREIGN KEY (" + APPOINTMENT_ID + ") REFERENCES " +
        TABLE_APPOINTMENTS + "(" + ID + ") ON UPDATE CASCADE ON DELETE CASCADE"
        + ")");

  }

  private void createIndexes(SQLiteDatabase database) {
    database.execSQL("CREATE INDEX IF NOT EXISTS idx_technicians_branch ON " +
                     TABLE_TECHNICIANS + "(" + BRANCH_ID + ")");
    database.execSQL("CREATE INDEX IF NOT EXISTS idx_services_category ON " +
                     TABLE_REPAIR_SERVICES + "(" + CATEGORY_ID + ")");
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_parts_branch_category ON " +
        TABLE_SPARE_PARTS + "(" + BRANCH_ID + ", " + CATEGORY_ID + ")");
    database.execSQL("CREATE INDEX IF NOT EXISTS idx_appointments_user ON " +
                     TABLE_APPOINTMENTS + "(" + USER_ID + ", " + CREATED_AT +
                     ")");
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_appointments_customer_uid ON " +
        TABLE_APPOINTMENTS + "(" + CUSTOMER_UID + ")");
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_appointments_branch_status ON " +
        TABLE_APPOINTMENTS + "(" + BRANCH_ID + ", " + STATUS + ")");
    database.execSQL("CREATE INDEX IF NOT EXISTS idx_history_appointment ON " +
                     TABLE_REPAIR_HISTORY + "(" + APPOINTMENT_ID + ", " +
                     RECORDED_AT + ")");
    database.execSQL("CREATE INDEX IF NOT EXISTS idx_appointments_sync_dirty ON " +
                     TABLE_APPOINTMENTS + "(" + SYNC_DIRTY + ")");
    database.execSQL("CREATE INDEX IF NOT EXISTS idx_payments_sync_dirty ON " +
                     TABLE_PAYMENTS + "(" + SYNC_DIRTY + ")");
    database.execSQL("CREATE INDEX IF NOT EXISTS idx_history_sync_dirty ON " +
                     TABLE_REPAIR_HISTORY + "(" + SYNC_DIRTY + ")");
    database.execSQL("CREATE INDEX IF NOT EXISTS idx_branches_sync_dirty ON " +
                     TABLE_BRANCHES + "(" + SYNC_DIRTY + ")");
    database.execSQL("CREATE INDEX IF NOT EXISTS idx_categories_sync_dirty ON " +
                     TABLE_DEVICE_CATEGORIES + "(" + SYNC_DIRTY + ")");
    database.execSQL("CREATE INDEX IF NOT EXISTS idx_services_sync_dirty ON " +
                     TABLE_REPAIR_SERVICES + "(" + SYNC_DIRTY + ")");
    database.execSQL("CREATE INDEX IF NOT EXISTS idx_technicians_sync_dirty ON " +
                     TABLE_TECHNICIANS + "(" + SYNC_DIRTY + ")");
    database.execSQL("CREATE INDEX IF NOT EXISTS idx_parts_sync_dirty ON " +
                     TABLE_SPARE_PARTS + "(" + SYNC_DIRTY + ")");
  }

  private void migrateToV7(SQLiteDatabase database) {
    addReferenceIdentityColumn(database, TABLE_BRANCHES);
    addReferenceIdentityColumn(database, TABLE_DEVICE_CATEGORIES);
    addReferenceIdentityColumn(database, TABLE_REPAIR_SERVICES);
    addReferenceIdentityColumn(database, TABLE_TECHNICIANS);
    addReferenceIdentityColumn(database, TABLE_SPARE_PARTS);

    if (!hasColumn(database, TABLE_APPOINTMENTS, RESERVED_PART_ID)) {
      database.execSQL("ALTER TABLE " + TABLE_APPOINTMENTS + " ADD COLUMN " +
                       RESERVED_PART_ID + " INTEGER REFERENCES " +
                       TABLE_SPARE_PARTS + "(" + ID +
                       ") ON UPDATE CASCADE ON DELETE SET NULL");
    }
    if (!hasColumn(database, TABLE_REPAIR_HISTORY, FEATURED)) {
      database.execSQL("ALTER TABLE " + TABLE_REPAIR_HISTORY + " ADD COLUMN " +
                       FEATURED + " INTEGER NOT NULL DEFAULT 0 CHECK (" +
                       FEATURED + " IN (0, 1))");
      database.execSQL("UPDATE " + TABLE_REPAIR_HISTORY + " SET " + FEATURED +
                       "=1 WHERE lower(" + NOTES + ") LIKE '%featured%'");
    }

    createIntegrityTables(database);
    backfillReferenceRemoteIds(database);
    removeDuplicatePayments(database);
    createIndexes(database);
    createIntegrityIndexes(database);
  }

  private void addReferenceIdentityColumn(SQLiteDatabase database,
                                          String table) {
    if (!hasColumn(database, table, REMOTE_ID)) {
      database.execSQL("ALTER TABLE " + table + " ADD COLUMN " + REMOTE_ID +
                       " TEXT");
    }
    addReferenceSyncColumns(database, table);
  }

  private void createIntegrityTables(SQLiteDatabase database) {
    database.execSQL(
        "CREATE TABLE IF NOT EXISTS " + TABLE_SERVICE_PART_REQUIREMENTS +
        " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + REMOTE_ID +
        " TEXT NOT NULL UNIQUE, " + SERVICE_ID + " INTEGER NOT NULL, " +
        SPARE_PART_ID + " INTEGER NOT NULL, " + REQUIRED_QUANTITY +
        " INTEGER NOT NULL DEFAULT 1 CHECK (" + REQUIRED_QUANTITY +
        " > 0), " + ACTIVE + " INTEGER NOT NULL DEFAULT 1 CHECK (" + ACTIVE +
        " IN (0, 1)), " + UPDATED_AT + " INTEGER NOT NULL, " + SYNC_DIRTY +
        " INTEGER NOT NULL DEFAULT 1 CHECK (" + SYNC_DIRTY +
        " IN (0, 1)), UNIQUE (" + SERVICE_ID + ", " + SPARE_PART_ID +
        "), FOREIGN KEY (" + SERVICE_ID + ") REFERENCES " +
        TABLE_REPAIR_SERVICES + "(" + ID +
        ") ON UPDATE CASCADE ON DELETE CASCADE, FOREIGN KEY (" +
        SPARE_PART_ID + ") REFERENCES " + TABLE_SPARE_PARTS + "(" + ID +
        ") ON UPDATE CASCADE ON DELETE CASCADE)");

    database.execSQL(
        "CREATE TABLE IF NOT EXISTS " + TABLE_APPOINTMENT_PART_RESERVATIONS +
        " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + REMOTE_ID +
        " TEXT NOT NULL UNIQUE, " + APPOINTMENT_ID + " INTEGER NOT NULL, " +
        SPARE_PART_ID + " INTEGER NOT NULL, " + RESERVED_QUANTITY +
        " INTEGER NOT NULL DEFAULT 1 CHECK (" + RESERVED_QUANTITY +
        " > 0), " + RESERVATION_STATUS + " TEXT NOT NULL" +
        RESERVATION_STATUS_CHECK + ", " + CREATED_AT +
        " INTEGER NOT NULL, " + UPDATED_AT + " INTEGER NOT NULL, " +
        SYNC_DIRTY + " INTEGER NOT NULL DEFAULT 1 CHECK (" + SYNC_DIRTY +
        " IN (0, 1)), UNIQUE (" + APPOINTMENT_ID + ", " + SPARE_PART_ID +
        "), FOREIGN KEY (" + APPOINTMENT_ID + ") REFERENCES " +
        TABLE_APPOINTMENTS + "(" + ID +
        ") ON UPDATE CASCADE ON DELETE CASCADE, FOREIGN KEY (" +
        SPARE_PART_ID + ") REFERENCES " + TABLE_SPARE_PARTS + "(" + ID +
        ") ON UPDATE CASCADE ON DELETE RESTRICT)");

    database.execSQL(
        "CREATE TABLE IF NOT EXISTS " + TABLE_REPAIR_SAMPLES + " (" +
        REMOTE_ID + " TEXT PRIMARY KEY NOT NULL, " + IMAGE_PATH +
        " TEXT NOT NULL, " + DEVICE_DETAILS +
        " TEXT NOT NULL DEFAULT '', " + SERVICE_NAME +
        " TEXT NOT NULL DEFAULT '', " + BRANCH_NAME +
        " TEXT NOT NULL DEFAULT '', " + UPDATED_AT + " INTEGER NOT NULL)");
  }

  private void backfillReferenceRemoteIds(SQLiteDatabase database) {
    backfillSimpleRemoteIds(database, TABLE_BRANCHES, NAME, "branch");
    backfillSimpleRemoteIds(database, TABLE_DEVICE_CATEGORIES, NAME,
                            "device-category");
    backfillSimpleRemoteIds(database, TABLE_TECHNICIANS, EMAIL, "technician");

    String serviceSql =
        "SELECT s." + ID + ", s." + NAME + ", c." + REMOTE_ID +
        " FROM " + TABLE_REPAIR_SERVICES + " s JOIN " +
        TABLE_DEVICE_CATEGORIES + " c ON c." + ID + "=s." + CATEGORY_ID;
    try (Cursor cursor = database.rawQuery(serviceSql, null)) {
      while (cursor.moveToNext()) {
        updateReferenceIdentity(
            database, TABLE_REPAIR_SERVICES, cursor.getLong(0),
            stableRemoteId("service", cursor.getString(2) + "|" +
                                          cursor.getString(1)));
      }
    }

    String partSql =
        "SELECT p." + ID + ", p." + SKU + ", b." + REMOTE_ID + " FROM " +
        TABLE_SPARE_PARTS + " p JOIN " + TABLE_BRANCHES + " b ON b." + ID +
        "=p." + BRANCH_ID;
    try (Cursor cursor = database.rawQuery(partSql, null)) {
      while (cursor.moveToNext()) {
        updateReferenceIdentity(
            database, TABLE_SPARE_PARTS, cursor.getLong(0),
            stableRemoteId("spare-part", cursor.getString(2) + "|" +
                                             cursor.getString(1)));
      }
    }
  }

  private void backfillSimpleRemoteIds(SQLiteDatabase database, String table,
                                       String naturalKeyColumn,
                                       String entityName) {
    try (Cursor cursor = database.query(
             table, new String[] {ID, naturalKeyColumn}, null, null, null, null,
             null)) {
      while (cursor.moveToNext()) {
        updateReferenceIdentity(
            database, table, cursor.getLong(0),
            stableRemoteId(entityName, cursor.getString(1)));
      }
    }
  }

  private void updateReferenceIdentity(SQLiteDatabase database, String table,
                                       long localId, String remoteId) {
    String selectedRemoteId = remoteId;
    try (Cursor collision = database.query(
             table, new String[] {ID}, REMOTE_ID + " = ? AND " + ID + " <> ?",
             new String[] {remoteId, String.valueOf(localId)}, null, null, null,
             "1")) {
      if (collision.moveToFirst())
        selectedRemoteId = UUID.randomUUID().toString();
    }
    ContentValues values = new ContentValues();
    values.put(REMOTE_ID, selectedRemoteId);
    values.put(SYNC_DIRTY, 1);
    database.update(table, values, ID + " = ?",
                    new String[] {String.valueOf(localId)});
    database.execSQL("UPDATE " + table + " SET " + UPDATED_AT + "=? WHERE " +
                         ID + "=? AND " + UPDATED_AT + "<=0",
                     new Object[] {SEED_UPDATED_AT, localId});
  }

  private void removeDuplicatePayments(SQLiteDatabase database) {
    database.execSQL(
        "DELETE FROM " + TABLE_PAYMENTS + " WHERE " + ID +
        " NOT IN (SELECT (SELECT winner." + ID + " FROM " + TABLE_PAYMENTS +
        " winner WHERE winner." + APPOINTMENT_ID + "=grouped." +
        APPOINTMENT_ID + " ORDER BY CASE WHEN winner." + STATUS +
        "='PAID' THEN 0 ELSE 1 END, winner." + CREATED_AT +
        " DESC, winner." + ID + " DESC LIMIT 1) FROM " + TABLE_PAYMENTS +
        " grouped GROUP BY grouped." + APPOINTMENT_ID + ")");
    database.execSQL("DROP INDEX IF EXISTS idx_payments_appointment");
  }

  private void createIntegrityIndexes(SQLiteDatabase database) {
    createUniqueRemoteIndex(database, TABLE_BRANCHES, "branches");
    createUniqueRemoteIndex(database, TABLE_DEVICE_CATEGORIES, "categories");
    createUniqueRemoteIndex(database, TABLE_REPAIR_SERVICES, "services");
    createUniqueRemoteIndex(database, TABLE_TECHNICIANS, "technicians");
    createUniqueRemoteIndex(database, TABLE_SPARE_PARTS, "parts");
    createUniqueRemoteIndex(database, TABLE_SERVICE_PART_REQUIREMENTS,
                            "requirements");
    createUniqueRemoteIndex(database, TABLE_APPOINTMENT_PART_RESERVATIONS,
                            "reservations");
    database.execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS idx_payments_appointment_unique ON " +
        TABLE_PAYMENTS + "(" + APPOINTMENT_ID + ")");
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_appointments_reserved_part ON " +
        TABLE_APPOINTMENTS + "(" + RESERVED_PART_ID + ")");
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_history_featured ON " +
        TABLE_REPAIR_HISTORY + "(" + FEATURED + ", " + RECORDED_AT + ")");
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_requirements_service_active ON " +
        TABLE_SERVICE_PART_REQUIREMENTS + "(" + SERVICE_ID + ", " + ACTIVE +
        ")");
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_requirements_part ON " +
        TABLE_SERVICE_PART_REQUIREMENTS + "(" + SPARE_PART_ID + ")");
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_requirements_sync_dirty ON " +
        TABLE_SERVICE_PART_REQUIREMENTS + "(" + SYNC_DIRTY + ")");
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_reservations_appointment_status ON " +
        TABLE_APPOINTMENT_PART_RESERVATIONS + "(" + APPOINTMENT_ID + ", " +
        RESERVATION_STATUS + ")");
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_reservations_part_status ON " +
        TABLE_APPOINTMENT_PART_RESERVATIONS + "(" + SPARE_PART_ID + ", " +
        RESERVATION_STATUS + ")");
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_reservations_sync_dirty ON " +
        TABLE_APPOINTMENT_PART_RESERVATIONS + "(" + SYNC_DIRTY + ")");
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_repair_samples_updated ON " +
        TABLE_REPAIR_SAMPLES + "(" + UPDATED_AT + " DESC)");
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_repair_samples_service ON " +
        TABLE_REPAIR_SAMPLES + "(" + SERVICE_NAME + ")");
    database.execSQL(
        "CREATE INDEX IF NOT EXISTS idx_repair_samples_branch ON " +
        TABLE_REPAIR_SAMPLES + "(" + BRANCH_NAME + ")");
  }

  private void createUniqueRemoteIndex(SQLiteDatabase database, String table,
                                       String indexName) {
    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_" + indexName +
                     "_remote_id ON " + table + "(" + REMOTE_ID + ")");
  }

  private String stableRemoteId(String entityName, String naturalKey) {
    String normalizedKey = naturalKey == null ? "" : naturalKey.trim();
    String source = "techfix:" + entityName + ":" +
                    normalizedKey.toLowerCase(Locale.ROOT);
    return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8))
        .toString();
  }

  private void addRemoteIdentifiers(SQLiteDatabase database) {
    String[] tables = {TABLE_APPOINTMENTS, TABLE_PAYMENTS,
                       TABLE_REPAIR_HISTORY};
    for (String table : tables) {
      if (!hasColumn(database, table, REMOTE_ID))
        database.execSQL("ALTER TABLE " + table + " ADD COLUMN " + REMOTE_ID +
                         " TEXT");
      database.execSQL("UPDATE " + table + " SET " + REMOTE_ID +
                       "=lower(hex(randomblob(16))) WHERE " + REMOTE_ID +
                       " IS NULL");
      database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_" + table +
                       "_remote_id ON " + table + "(" + REMOTE_ID + ")");
    }
  }

  private void addSyncMetadata(SQLiteDatabase database) {
    if (!hasColumn(database, TABLE_USERS, FIREBASE_UID))
      database.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " +
                       FIREBASE_UID + " TEXT");
    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_firebase_uid " +
                     "ON " + TABLE_USERS + "(" + FIREBASE_UID + ")");

    if (!hasColumn(database, TABLE_APPOINTMENTS, CUSTOMER_UID))
      database.execSQL("ALTER TABLE " + TABLE_APPOINTMENTS + " ADD COLUMN " +
                       CUSTOMER_UID + " TEXT");
    if (!hasColumn(database, TABLE_APPOINTMENTS, REQUEST_LATITUDE))
      database.execSQL("ALTER TABLE " + TABLE_APPOINTMENTS + " ADD COLUMN " +
                       REQUEST_LATITUDE + " REAL");
    if (!hasColumn(database, TABLE_APPOINTMENTS, REQUEST_LONGITUDE))
      database.execSQL("ALTER TABLE " + TABLE_APPOINTMENTS + " ADD COLUMN " +
                       REQUEST_LONGITUDE + " REAL");
    addReferenceSyncColumns(database, TABLE_BRANCHES);
    addReferenceSyncColumns(database, TABLE_DEVICE_CATEGORIES);
    addReferenceSyncColumns(database, TABLE_REPAIR_SERVICES);
    addReferenceSyncColumns(database, TABLE_TECHNICIANS);
    addReferenceSyncColumns(database, TABLE_SPARE_PARTS);
    addSyncColumns(database, TABLE_APPOINTMENTS, CREATED_AT);
    addSyncColumns(database, TABLE_PAYMENTS, CREATED_AT);
    addSyncColumns(database, TABLE_REPAIR_HISTORY, RECORDED_AT);
  }

  private void addReferenceSyncColumns(SQLiteDatabase database, String table) {
    if (!hasColumn(database, table, UPDATED_AT))
      database.execSQL("ALTER TABLE " + table + " ADD COLUMN " + UPDATED_AT +
                       " INTEGER NOT NULL DEFAULT 0");
    if (!hasColumn(database, table, SYNC_DIRTY))
      database.execSQL("ALTER TABLE " + table + " ADD COLUMN " + SYNC_DIRTY +
                       " INTEGER NOT NULL DEFAULT 0 CHECK (" + SYNC_DIRTY +
                       " IN (0, 1))");
  }

  private void addSyncColumns(SQLiteDatabase database, String table,
                              String timestampColumn) {
    if (!hasColumn(database, table, UPDATED_AT))
      database.execSQL("ALTER TABLE " + table + " ADD COLUMN " + UPDATED_AT +
                       " INTEGER NOT NULL DEFAULT 0");
    if (!hasColumn(database, table, SYNC_DIRTY))
      database.execSQL("ALTER TABLE " + table + " ADD COLUMN " + SYNC_DIRTY +
                       " INTEGER NOT NULL DEFAULT 1 CHECK (" + SYNC_DIRTY +
                       " IN (0, 1))");
    database.execSQL("UPDATE " + table + " SET " + UPDATED_AT + "=" +
                     timestampColumn + " WHERE " + UPDATED_AT + "=0");
  }

  private boolean hasColumn(SQLiteDatabase database, String table,
                            String column) {
    try (Cursor cursor =
             database.rawQuery("PRAGMA table_info(" + table + ")", null)) {
      int nameIndex = cursor.getColumnIndexOrThrow("name");
      while (cursor.moveToNext())
        if (column.equals(cursor.getString(nameIndex)))
          return true;
      return false;
    }
  }

  private void seedReferenceData(SQLiteDatabase database) {
    long colomboId =
        insertBranch(database, "Colombo", "42 Galle Road, Colombo 03",
                     "+94 11 234 5678", 6.9271, 79.8612);
    long galleId = insertBranch(database, "Galle", "18 Wakwella Road, Galle",
                                "+94 91 223 4567", 6.0535, 80.2210);

    long phoneId = insertCategory(database, "Phone",
                                  "Smartphone hardware and software repairs");
    long laptopId = insertCategory(database, "Laptop",
                                   "Laptop diagnostics and component repairs");
    long tabletId = insertCategory(
        database, "Tablet", "Tablet display, battery, and charging repairs");
    long desktopId = insertCategory(
        database, "Desktop", "Desktop computer diagnostics and upgrades");

    insertService(database, phoneId, "Screen replacement",
                  "Restore a cracked, flickering, or unresponsive display "
                      + "with a quality-tested replacement.",
                  850000, 90);
    insertService(database, phoneId, "Battery replacement",
                  "Replace a weak or swollen battery and verify safe "
                      + "charging performance.",
                  600000, 60);
    insertService(
        database, laptopId, "Keyboard repair",
        "Repair or replace unresponsive laptop keyboard keys and connectors.",
        450000, 90);
    insertService(database, desktopId, "Device diagnostics",
                  "Complete hardware and software diagnostics with a clear "
                      + "repair estimate.",
                  250000, 45);
    insertService(
        database, tabletId, "Charging port repair",
        "Inspect and repair loose, damaged, or non-responsive charging ports.",
        700000, 90);

    insertTechnician(database, colomboId, "Nimal Perera", "nimal@techfix.lk",
                     "+94 77 123 4567", "Phone and tablet repairs");
    insertTechnician(database, colomboId, "Tharindu Silva",
                     "tharindu@techfix.lk", "+94 77 234 5678",
                     "Laptop and desktop repairs");
    insertTechnician(database, galleId, "Sachini Fernando",
                     "sachini@techfix.lk", "+94 77 345 6789",
                     "Phone and tablet repairs");
    insertTechnician(database, galleId, "Kavindu Jayasekara",
                     "kavindu@techfix.lk", "+94 77 456 7890",
                     "Laptop and desktop repairs");

    insertSparePart(database, colomboId, phoneId, "Universal OLED display",
                    "COL-PH-DIS-01", 650000, 8);
    insertSparePart(database, colomboId, phoneId, "Phone battery pack",
                    "COL-PH-BAT-01", 400000, 12);
    insertSparePart(database, galleId, phoneId, "Universal OLED display",
                    "GAL-PH-DIS-01", 650000, 5);
    insertSparePart(database, galleId, laptopId, "Laptop keyboard assembly",
                    "GAL-LP-KEY-01", 350000, 4);
    insertSparePart(database, colomboId, tabletId, "Tablet charging port",
                    "COL-TB-CHG-01", 480000, 6);
    insertSparePart(database, galleId, tabletId, "Tablet charging port",
                    "GAL-TB-CHG-01", 480000, 3);
  }

  private void seedDefaultPartRequirements(SQLiteDatabase database) {
    long screenServiceId = findId(database, TABLE_REPAIR_SERVICES, NAME,
                                  "Screen replacement");
    long batteryServiceId = findId(database, TABLE_REPAIR_SERVICES, NAME,
                                   "Battery replacement");
    long keyboardServiceId = findId(database, TABLE_REPAIR_SERVICES, NAME,
                                    "Keyboard repair");
    long chargingServiceId = findId(database, TABLE_REPAIR_SERVICES, NAME,
                                    "Charging port repair");

    insertServicePartRequirement(
        database, screenServiceId,
        findId(database, TABLE_SPARE_PARTS, SKU, "COL-PH-DIS-01"), 1);
    insertServicePartRequirement(
        database, screenServiceId,
        findId(database, TABLE_SPARE_PARTS, SKU, "GAL-PH-DIS-01"), 1);
    insertServicePartRequirement(
        database, batteryServiceId,
        findId(database, TABLE_SPARE_PARTS, SKU, "COL-PH-BAT-01"), 1);
    insertServicePartRequirement(
        database, keyboardServiceId,
        findId(database, TABLE_SPARE_PARTS, SKU, "GAL-LP-KEY-01"), 1);
    insertServicePartRequirement(
        database, chargingServiceId,
        findId(database, TABLE_SPARE_PARTS, SKU, "COL-TB-CHG-01"), 1);
    insertServicePartRequirement(
        database, chargingServiceId,
        findId(database, TABLE_SPARE_PARTS, SKU, "GAL-TB-CHG-01"), 1);
  }

  private long insertBranch(SQLiteDatabase database, String name,
                            String address, String phone, double latitude,
                            double longitude) {
    ContentValues values = new ContentValues();
    values.put(NAME, name);
    values.put(ADDRESS, address);
    values.put(PHONE, phone);
    values.put(LATITUDE, latitude);
    values.put(LONGITUDE, longitude);
    values.put(ACTIVE, 1);
    putSeedSyncState(values, "branch", name);
    database.insertWithOnConflict(TABLE_BRANCHES, null, values,
                                  SQLiteDatabase.CONFLICT_IGNORE);
    return findId(database, TABLE_BRANCHES, NAME, name);
  }

  private long insertCategory(SQLiteDatabase database, String name,
                              String description) {
    ContentValues values = new ContentValues();
    values.put(NAME, name);
    values.put(DESCRIPTION, description);
    values.put(ACTIVE, 1);
    putSeedSyncState(values, "device-category", name);
    database.insertWithOnConflict(TABLE_DEVICE_CATEGORIES, null, values,
                                  SQLiteDatabase.CONFLICT_IGNORE);
    return findId(database, TABLE_DEVICE_CATEGORIES, NAME, name);
  }

  private long insertService(SQLiteDatabase database, long categoryId,
                             String name, String description, long priceCents,
                             int estimatedMinutes) {
    ContentValues values = new ContentValues();
    values.put(CATEGORY_ID, categoryId);
    values.put(NAME, name);
    values.put(DESCRIPTION, description);
    values.put(BASE_PRICE_CENTS, priceCents);
    values.put(ESTIMATED_MINUTES, estimatedMinutes);
    values.put(ACTIVE, 1);
    putSeedSyncState(values, "service",
                     findRemoteId(database, TABLE_DEVICE_CATEGORIES,
                                  categoryId) + "|" + name);
    database.insertWithOnConflict(TABLE_REPAIR_SERVICES, null, values,
                                  SQLiteDatabase.CONFLICT_IGNORE);
    return findId(database, TABLE_REPAIR_SERVICES, NAME, name);
  }

  private void insertTechnician(SQLiteDatabase database, long branchId,
                                String fullName, String email, String phone,
                                String specialty) {
    ContentValues values = new ContentValues();
    values.put(BRANCH_ID, branchId);
    values.put(FULL_NAME, fullName);
    values.put(EMAIL, email);
    values.put(PHONE, phone);
    values.put(SPECIALTY, specialty);
    values.put(ACTIVE, 1);
    putSeedSyncState(values, "technician", email);
    database.insertWithOnConflict(TABLE_TECHNICIANS, null, values,
                                  SQLiteDatabase.CONFLICT_IGNORE);
  }

  private long insertSparePart(SQLiteDatabase database, long branchId,
                               long categoryId, String name, String sku,
                               long priceCents, int quantity) {
    ContentValues values = new ContentValues();
    values.put(BRANCH_ID, branchId);
    values.put(CATEGORY_ID, categoryId);
    values.put(NAME, name);
    values.put(SKU, sku);
    values.put(UNIT_PRICE_CENTS, priceCents);
    values.put(QUANTITY_AVAILABLE, quantity);
    values.put(ACTIVE, 1);
    putSeedSyncState(values, "spare-part",
                     findRemoteId(database, TABLE_BRANCHES, branchId) + "|" +
                         sku);
    database.insertWithOnConflict(TABLE_SPARE_PARTS, null, values,
                                  SQLiteDatabase.CONFLICT_IGNORE);
    return findId(database, TABLE_SPARE_PARTS, SKU, sku);
  }

  private void insertServicePartRequirement(SQLiteDatabase database,
                                            long serviceId, long sparePartId,
                                            int requiredQuantity) {
    if (serviceId < 1 || sparePartId < 1)
      return;
    String serviceRemoteId =
        findRemoteId(database, TABLE_REPAIR_SERVICES, serviceId);
    String partRemoteId = findRemoteId(database, TABLE_SPARE_PARTS, sparePartId);
    if (serviceRemoteId == null || partRemoteId == null)
      return;

    ContentValues values = new ContentValues();
    values.put(REMOTE_ID, stableRemoteId("service-part-requirement",
                                         serviceRemoteId + "|" + partRemoteId));
    values.put(SERVICE_ID, serviceId);
    values.put(SPARE_PART_ID, sparePartId);
    values.put(REQUIRED_QUANTITY, requiredQuantity);
    values.put(ACTIVE, 1);
    values.put(UPDATED_AT, SEED_UPDATED_AT);
    values.put(SYNC_DIRTY, 1);
    database.insertWithOnConflict(TABLE_SERVICE_PART_REQUIREMENTS, null, values,
                                  SQLiteDatabase.CONFLICT_IGNORE);
  }

  private void putSeedSyncState(ContentValues values, String entityName,
                                String naturalKey) {
    values.put(REMOTE_ID, stableRemoteId(entityName, naturalKey));
    values.put(UPDATED_AT, SEED_UPDATED_AT);
    values.put(SYNC_DIRTY, 1);
  }

  private String findRemoteId(SQLiteDatabase database, String table,
                              long localId) {
    if (localId < 1)
      return null;
    try (Cursor cursor = database.query(
             table, new String[] {REMOTE_ID}, ID + " = ?",
             new String[] {String.valueOf(localId)}, null, null, null, "1")) {
      return cursor.moveToFirst() ? cursor.getString(0) : null;
    }
  }

  private long findId(SQLiteDatabase database, String table, String column,
                      String value) {
    try (Cursor cursor =
             database.query(table, new String[] {ID}, column + " = ?",
                            new String[] {value}, null, null, null, "1")) {
      return cursor.moveToFirst() ? cursor.getLong(0) : -1;
    }
  }

  public RegistrationResult registerUser(String fullName, String email,
                                         String password) {
    String normalizedEmail = normalizeEmail(email);

    try {
      SQLiteDatabase database = getWritableDatabase();
      if (emailExists(database, normalizedEmail)) {
        return RegistrationResult.EMAIL_ALREADY_EXISTS;
      }

      PasswordHasher.PasswordHash passwordHash =
          PasswordHasher.create(password);
      ContentValues values = new ContentValues();
      values.put(FULL_NAME, fullName.trim());
      values.put(EMAIL, normalizedEmail);
      values.put(PASSWORD_HASH, passwordHash.getHash());
      values.put(PASSWORD_SALT, passwordHash.getSalt());
      values.put(CREATED_AT, System.currentTimeMillis());

      long userId = database.insertOrThrow(TABLE_USERS, null, values);
      return userId == -1 ? RegistrationResult.ERROR
                          : RegistrationResult.SUCCESS;
    } catch (SQLiteConstraintException exception) {
      return RegistrationResult.EMAIL_ALREADY_EXISTS;
    } catch (GeneralSecurityException | RuntimeException exception) {
      return RegistrationResult.ERROR;
    }
  }

  public AuthenticationResult authenticate(String email, String password) {
    String[] columns = {ID, FULL_NAME, EMAIL, PASSWORD_HASH, PASSWORD_SALT};

    try (Cursor cursor = getReadableDatabase().query(
             TABLE_USERS, columns, EMAIL + " = ?",
             new String[] {normalizeEmail(email)}, null, null, null, "1")) {
      if (!cursor.moveToFirst()) {
        return AuthenticationResult.invalidCredentials();
      }

      String storedHash =
          cursor.getString(cursor.getColumnIndexOrThrow(PASSWORD_HASH));
      String storedSalt =
          cursor.getString(cursor.getColumnIndexOrThrow(PASSWORD_SALT));
      if (!PasswordHasher.verify(password, storedSalt, storedHash)) {
        return AuthenticationResult.invalidCredentials();
      }

      User user =
          new User(cursor.getLong(cursor.getColumnIndexOrThrow(ID)),
                   cursor.getString(cursor.getColumnIndexOrThrow(FULL_NAME)),
                   cursor.getString(cursor.getColumnIndexOrThrow(EMAIL)));
      return AuthenticationResult.success(user);
    } catch (GeneralSecurityException | RuntimeException exception) {
      return AuthenticationResult.error();
    }
  }

  public User getOrCreateFirebaseUser(String fullName, String email) {
    return getOrCreateFirebaseUser(fullName, email, null);
  }

  public User getOrCreateFirebaseUser(String fullName, String email,
                                      String firebaseUid) {
    String normalizedEmail = normalizeEmail(email);
    SQLiteDatabase database = getWritableDatabase();
    boolean hasFirebaseUid =
        firebaseUid != null && !firebaseUid.trim().isEmpty();
    String selection = hasFirebaseUid
                           ? FIREBASE_UID + " = ? OR (" + FIREBASE_UID +
                                 " IS NULL AND " + EMAIL + " = ?)"
                           : EMAIL + " = ?";
    String[] selectionArguments = hasFirebaseUid
                                      ? new String[] {firebaseUid.trim(),
                                                      normalizedEmail}
                                      : new String[] {normalizedEmail};
    try (Cursor cursor = database.query(
             TABLE_USERS, new String[] {ID, FULL_NAME, EMAIL}, selection,
             selectionArguments, null, null, null, "1")) {
      if (cursor.moveToFirst()) {
        long localUserId =
            cursor.getLong(cursor.getColumnIndexOrThrow(ID));
        String storedName =
            cursor.getString(cursor.getColumnIndexOrThrow(FULL_NAME));
        String storedEmail =
            cursor.getString(cursor.getColumnIndexOrThrow(EMAIL));
        ContentValues profile = new ContentValues();
        if (fullName != null && !fullName.trim().isEmpty() &&
            !fullName.trim().equals(storedName)) {
          profile.put(FULL_NAME, fullName.trim());
          storedName = fullName.trim();
        }
        if (!normalizedEmail.equals(storedEmail) &&
            !emailBelongsToAnotherUser(database, normalizedEmail,
                                       localUserId)) {
          profile.put(EMAIL, normalizedEmail);
          storedEmail = normalizedEmail;
        }
        if (hasFirebaseUid)
          profile.put(FIREBASE_UID, firebaseUid.trim());
        if (profile.size() > 0)
          database.update(TABLE_USERS, profile, ID + " = ?",
                          new String[] {String.valueOf(localUserId)});
        return new User(localUserId, storedName, storedEmail);
      }
    }

    try {
      PasswordHasher.PasswordHash unusablePassword =
          PasswordHasher.create(UUID.randomUUID().toString());
      ContentValues values = new ContentValues();
      values.put(FULL_NAME, fullName == null || fullName.trim().isEmpty()
                                ? normalizedEmail.split("@")[0]
                                : fullName.trim());
      values.put(EMAIL, normalizedEmail);
      if (firebaseUid != null && !firebaseUid.trim().isEmpty())
        values.put(FIREBASE_UID, firebaseUid.trim());
      values.put(PASSWORD_HASH, unusablePassword.getHash());
      values.put(PASSWORD_SALT, unusablePassword.getSalt());
      values.put(CREATED_AT, System.currentTimeMillis());
      long userId = database.insertOrThrow(TABLE_USERS, null, values);
      return new User(userId, values.getAsString(FULL_NAME), normalizedEmail);
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException(
          "Unable to create the local Firebase profile.", exception);
    }
  }

  private boolean emailExists(SQLiteDatabase database, String email) {
    try (Cursor cursor =
             database.query(TABLE_USERS, new String[] {ID}, EMAIL + " = ?",
                            new String[] {email}, null, null, null, "1")) {
      return cursor.moveToFirst();
    }
  }

  private boolean emailBelongsToAnotherUser(SQLiteDatabase database,
                                            String email, long userId) {
    try (Cursor cursor = database.query(
             TABLE_USERS, new String[] {ID}, EMAIL + " = ? AND " + ID +
                 " <> ?",
             new String[] {email, String.valueOf(userId)}, null, null, null,
             "1")) {
      return cursor.moveToFirst();
    }
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  public enum RegistrationResult { SUCCESS, EMAIL_ALREADY_EXISTS, ERROR }

  public static final class AuthenticationResult {
    public enum Status { SUCCESS, INVALID_CREDENTIALS, ERROR }

    private final Status status;
    private final User user;

    private AuthenticationResult(Status status, User user) {
      this.status = status;
      this.user = user;
    }

    static AuthenticationResult success(User user) {
      return new AuthenticationResult(Status.SUCCESS, user);
    }

    static AuthenticationResult invalidCredentials() {
      return new AuthenticationResult(Status.INVALID_CREDENTIALS, null);
    }

    static AuthenticationResult error() {
      return new AuthenticationResult(Status.ERROR, null);
    }

    public Status getStatus() { return status; }
    public User getUser() { return user; }
  }
}
