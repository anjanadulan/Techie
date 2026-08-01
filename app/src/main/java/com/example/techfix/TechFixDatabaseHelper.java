package com.example.techfix;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.GeneralSecurityException;
import java.util.Locale;

public class TechFixDatabaseHelper extends SQLiteOpenHelper {

    static final String DATABASE_NAME = "techfix.db";
    static final int DATABASE_VERSION = 2;

    static final String TABLE_USERS = "users";
    static final String TABLE_BRANCHES = "branches";
    static final String TABLE_TECHNICIANS = "technicians";
    static final String TABLE_DEVICE_CATEGORIES = "device_categories";
    static final String TABLE_REPAIR_SERVICES = "repair_services";
    static final String TABLE_SPARE_PARTS = "spare_parts";
    static final String TABLE_APPOINTMENTS = "appointments";
    static final String TABLE_PAYMENTS = "payments";
    static final String TABLE_REPAIR_HISTORY = "repair_history";

    static final String ID = "_id";
    static final String FULL_NAME = "full_name";
    static final String EMAIL = "email";
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
    static final String AMOUNT_CENTS = "amount_cents";
    static final String METHOD = "method";
    static final String REFERENCE = "reference";
    static final String PAID_AT = "paid_at";
    static final String NOTES = "notes";
    static final String IMAGE_PATH = "image_path";
    static final String RECORDED_AT = "recorded_at";
    static final String CREATED_AT = "created_at";

    private static final String APPOINTMENT_STATUS_CHECK =
            " CHECK (" + STATUS + " IN ('PENDING','ASSIGNED','IN_PROGRESS'," +
                    "'WAITING_FOR_PARTS','READY_FOR_PAYMENT','COMPLETED','CANCELLED'))";
    private static final String PAYMENT_METHOD_CHECK =
            " CHECK (" + METHOD + " IN ('CASH','CARD','BANK_TRANSFER','ONLINE'))";
    private static final String PAYMENT_STATUS_CHECK =
            " CHECK (" + STATUS + " IN ('PENDING','PAID','FAILED','REFUNDED'))";

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
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.beginTransaction();
        try {
            if (oldVersion < 2) {
                createCoreTables(database);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private void createUsersTable(SQLiteDatabase database) {
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                        ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        FULL_NAME + " TEXT NOT NULL, " +
                        EMAIL + " TEXT NOT NULL UNIQUE COLLATE NOCASE, " +
                        PASSWORD_HASH + " TEXT NOT NULL, " +
                        PASSWORD_SALT + " TEXT NOT NULL, " +
                        CREATED_AT + " INTEGER NOT NULL" +
                        ")"
        );
    }

    private void createCoreTables(SQLiteDatabase database) {
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_BRANCHES + " (" +
                        ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        NAME + " TEXT NOT NULL UNIQUE COLLATE NOCASE, " +
                        ADDRESS + " TEXT NOT NULL, " +
                        PHONE + " TEXT NOT NULL, " +
                        LATITUDE + " REAL NOT NULL, " +
                        LONGITUDE + " REAL NOT NULL, " +
                        ACTIVE + " INTEGER NOT NULL DEFAULT 1 CHECK (" + ACTIVE + " IN (0, 1))" +
                        ")"
        );

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_TECHNICIANS + " (" +
                        ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        BRANCH_ID + " INTEGER NOT NULL, " +
                        FULL_NAME + " TEXT NOT NULL, " +
                        EMAIL + " TEXT NOT NULL UNIQUE COLLATE NOCASE, " +
                        PHONE + " TEXT NOT NULL, " +
                        SPECIALTY + " TEXT NOT NULL, " +
                        ACTIVE + " INTEGER NOT NULL DEFAULT 1 CHECK (" + ACTIVE + " IN (0, 1)), " +
                        "FOREIGN KEY (" + BRANCH_ID + ") REFERENCES " + TABLE_BRANCHES +
                        "(" + ID + ") ON UPDATE CASCADE ON DELETE RESTRICT" +
                        ")"
        );

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_DEVICE_CATEGORIES + " (" +
                        ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        NAME + " TEXT NOT NULL UNIQUE COLLATE NOCASE, " +
                        DESCRIPTION + " TEXT NOT NULL, " +
                        ACTIVE + " INTEGER NOT NULL DEFAULT 1 CHECK (" + ACTIVE + " IN (0, 1))" +
                        ")"
        );

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_REPAIR_SERVICES + " (" +
                        ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        CATEGORY_ID + " INTEGER NOT NULL, " +
                        NAME + " TEXT NOT NULL, " +
                        DESCRIPTION + " TEXT NOT NULL, " +
                        BASE_PRICE_CENTS + " INTEGER NOT NULL CHECK (" + BASE_PRICE_CENTS + " >= 0), " +
                        ESTIMATED_MINUTES + " INTEGER NOT NULL CHECK (" + ESTIMATED_MINUTES + " > 0), " +
                        ACTIVE + " INTEGER NOT NULL DEFAULT 1 CHECK (" + ACTIVE + " IN (0, 1)), " +
                        "UNIQUE (" + CATEGORY_ID + ", " + NAME + "), " +
                        "FOREIGN KEY (" + CATEGORY_ID + ") REFERENCES " + TABLE_DEVICE_CATEGORIES +
                        "(" + ID + ") ON UPDATE CASCADE ON DELETE RESTRICT" +
                        ")"
        );

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_SPARE_PARTS + " (" +
                        ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        BRANCH_ID + " INTEGER NOT NULL, " +
                        CATEGORY_ID + " INTEGER, " +
                        NAME + " TEXT NOT NULL, " +
                        SKU + " TEXT NOT NULL, " +
                        UNIT_PRICE_CENTS + " INTEGER NOT NULL CHECK (" + UNIT_PRICE_CENTS + " >= 0), " +
                        QUANTITY_AVAILABLE + " INTEGER NOT NULL DEFAULT 0 CHECK (" + QUANTITY_AVAILABLE + " >= 0), " +
                        ACTIVE + " INTEGER NOT NULL DEFAULT 1 CHECK (" + ACTIVE + " IN (0, 1)), " +
                        "UNIQUE (" + BRANCH_ID + ", " + SKU + "), " +
                        "FOREIGN KEY (" + BRANCH_ID + ") REFERENCES " + TABLE_BRANCHES +
                        "(" + ID + ") ON UPDATE CASCADE ON DELETE RESTRICT, " +
                        "FOREIGN KEY (" + CATEGORY_ID + ") REFERENCES " + TABLE_DEVICE_CATEGORIES +
                        "(" + ID + ") ON UPDATE CASCADE ON DELETE SET NULL" +
                        ")"
        );

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_APPOINTMENTS + " (" +
                        ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        USER_ID + " INTEGER NOT NULL, " +
                        BRANCH_ID + " INTEGER, " +
                        TECHNICIAN_ID + " INTEGER, " +
                        SERVICE_ID + " INTEGER NOT NULL, " +
                        DEVICE_DETAILS + " TEXT NOT NULL, " +
                        PROBLEM_DESCRIPTION + " TEXT NOT NULL, " +
                        STATUS + " TEXT NOT NULL" + APPOINTMENT_STATUS_CHECK + ", " +
                        APPOINTMENT_AT + " INTEGER NOT NULL, " +
                        CREATED_AT + " INTEGER NOT NULL, " +
                        "FOREIGN KEY (" + USER_ID + ") REFERENCES " + TABLE_USERS +
                        "(" + ID + ") ON UPDATE CASCADE ON DELETE RESTRICT, " +
                        "FOREIGN KEY (" + BRANCH_ID + ") REFERENCES " + TABLE_BRANCHES +
                        "(" + ID + ") ON UPDATE CASCADE ON DELETE RESTRICT, " +
                        "FOREIGN KEY (" + TECHNICIAN_ID + ") REFERENCES " + TABLE_TECHNICIANS +
                        "(" + ID + ") ON UPDATE CASCADE ON DELETE RESTRICT, " +
                        "FOREIGN KEY (" + SERVICE_ID + ") REFERENCES " + TABLE_REPAIR_SERVICES +
                        "(" + ID + ") ON UPDATE CASCADE ON DELETE RESTRICT" +
                        ")"
        );

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_PAYMENTS + " (" +
                        ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        APPOINTMENT_ID + " INTEGER NOT NULL, " +
                        AMOUNT_CENTS + " INTEGER NOT NULL CHECK (" + AMOUNT_CENTS + " >= 0), " +
                        METHOD + " TEXT NOT NULL" + PAYMENT_METHOD_CHECK + ", " +
                        STATUS + " TEXT NOT NULL" + PAYMENT_STATUS_CHECK + ", " +
                        REFERENCE + " TEXT, " +
                        PAID_AT + " INTEGER, " +
                        CREATED_AT + " INTEGER NOT NULL, " +
                        "FOREIGN KEY (" + APPOINTMENT_ID + ") REFERENCES " + TABLE_APPOINTMENTS +
                        "(" + ID + ") ON UPDATE CASCADE ON DELETE CASCADE" +
                        ")"
        );

        database.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_REPAIR_HISTORY + " (" +
                        ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        APPOINTMENT_ID + " INTEGER NOT NULL, " +
                        STATUS + " TEXT NOT NULL" + APPOINTMENT_STATUS_CHECK + ", " +
                        NOTES + " TEXT NOT NULL, " +
                        IMAGE_PATH + " TEXT, " +
                        RECORDED_AT + " INTEGER NOT NULL, " +
                        "FOREIGN KEY (" + APPOINTMENT_ID + ") REFERENCES " + TABLE_APPOINTMENTS +
                        "(" + ID + ") ON UPDATE CASCADE ON DELETE CASCADE" +
                        ")"
        );

        createIndexes(database);
    }

    private void createIndexes(SQLiteDatabase database) {
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_technicians_branch ON " +
                TABLE_TECHNICIANS + "(" + BRANCH_ID + ")");
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_services_category ON " +
                TABLE_REPAIR_SERVICES + "(" + CATEGORY_ID + ")");
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_parts_branch_category ON " +
                TABLE_SPARE_PARTS + "(" + BRANCH_ID + ", " + CATEGORY_ID + ")");
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_appointments_user ON " +
                TABLE_APPOINTMENTS + "(" + USER_ID + ", " + CREATED_AT + ")");
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_appointments_branch_status ON " +
                TABLE_APPOINTMENTS + "(" + BRANCH_ID + ", " + STATUS + ")");
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_payments_appointment ON " +
                TABLE_PAYMENTS + "(" + APPOINTMENT_ID + ")");
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_history_appointment ON " +
                TABLE_REPAIR_HISTORY + "(" + APPOINTMENT_ID + ", " + RECORDED_AT + ")");
    }

    public RegistrationResult registerUser(String fullName, String email, String password) {
        String normalizedEmail = normalizeEmail(email);

        try {
            SQLiteDatabase database = getWritableDatabase();
            if (emailExists(database, normalizedEmail)) {
                return RegistrationResult.EMAIL_ALREADY_EXISTS;
            }

            PasswordHasher.PasswordHash passwordHash = PasswordHasher.create(password);
            ContentValues values = new ContentValues();
            values.put(FULL_NAME, fullName.trim());
            values.put(EMAIL, normalizedEmail);
            values.put(PASSWORD_HASH, passwordHash.getHash());
            values.put(PASSWORD_SALT, passwordHash.getSalt());
            values.put(CREATED_AT, System.currentTimeMillis());

            long userId = database.insertOrThrow(TABLE_USERS, null, values);
            return userId == -1 ? RegistrationResult.ERROR : RegistrationResult.SUCCESS;
        } catch (SQLiteConstraintException exception) {
            return RegistrationResult.EMAIL_ALREADY_EXISTS;
        } catch (GeneralSecurityException | RuntimeException exception) {
            return RegistrationResult.ERROR;
        }
    }

    public AuthenticationResult authenticate(String email, String password) {
        String[] columns = {ID, FULL_NAME, EMAIL, PASSWORD_HASH, PASSWORD_SALT};

        try (Cursor cursor = getReadableDatabase().query(
                TABLE_USERS,
                columns,
                EMAIL + " = ?",
                new String[]{normalizeEmail(email)},
                null,
                null,
                null,
                "1"
        )) {
            if (!cursor.moveToFirst()) {
                return AuthenticationResult.invalidCredentials();
            }

            String storedHash = cursor.getString(cursor.getColumnIndexOrThrow(PASSWORD_HASH));
            String storedSalt = cursor.getString(cursor.getColumnIndexOrThrow(PASSWORD_SALT));
            if (!PasswordHasher.verify(password, storedSalt, storedHash)) {
                return AuthenticationResult.invalidCredentials();
            }

            User user = new User(
                    cursor.getLong(cursor.getColumnIndexOrThrow(ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(FULL_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(EMAIL))
            );
            return AuthenticationResult.success(user);
        } catch (GeneralSecurityException | RuntimeException exception) {
            return AuthenticationResult.error();
        }
    }

    private boolean emailExists(SQLiteDatabase database, String email) {
        try (Cursor cursor = database.query(
                TABLE_USERS,
                new String[]{ID},
                EMAIL + " = ?",
                new String[]{email},
                null,
                null,
                null,
                "1"
        )) {
            return cursor.moveToFirst();
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public enum RegistrationResult {
        SUCCESS,
        EMAIL_ALREADY_EXISTS,
        ERROR
    }

    public static final class AuthenticationResult {
        public enum Status {
            SUCCESS,
            INVALID_CREDENTIALS,
            ERROR
        }

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
