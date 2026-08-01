package com.example.techie;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.security.GeneralSecurityException;
import java.util.Locale;

public class UserDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "techfix.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_FULL_NAME = "full_name";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD_HASH = "password_hash";
    private static final String COLUMN_PASSWORD_SALT = "password_salt";
    private static final String COLUMN_CREATED_AT = "created_at";

    public UserDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(
                "CREATE TABLE " + TABLE_USERS + " (" +
                        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_FULL_NAME + " TEXT NOT NULL, " +
                        COLUMN_EMAIL + " TEXT NOT NULL UNIQUE COLLATE NOCASE, " +
                        COLUMN_PASSWORD_HASH + " TEXT NOT NULL, " +
                        COLUMN_PASSWORD_SALT + " TEXT NOT NULL, " +
                        COLUMN_CREATED_AT + " INTEGER NOT NULL" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        // Version 1 is the initial schema. Future schema changes should use migrations here.
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
            values.put(COLUMN_FULL_NAME, fullName.trim());
            values.put(COLUMN_EMAIL, normalizedEmail);
            values.put(COLUMN_PASSWORD_HASH, passwordHash.getHash());
            values.put(COLUMN_PASSWORD_SALT, passwordHash.getSalt());
            values.put(COLUMN_CREATED_AT, System.currentTimeMillis());

            long userId = database.insertOrThrow(TABLE_USERS, null, values);
            return userId == -1
                    ? RegistrationResult.ERROR
                    : RegistrationResult.SUCCESS;
        } catch (SQLiteConstraintException exception) {
            return RegistrationResult.EMAIL_ALREADY_EXISTS;
        } catch (GeneralSecurityException | RuntimeException exception) {
            return RegistrationResult.ERROR;
        }
    }

    public AuthenticationResult authenticate(String email, String password) {
        String[] columns = {
                COLUMN_ID,
                COLUMN_FULL_NAME,
                COLUMN_EMAIL,
                COLUMN_PASSWORD_HASH,
                COLUMN_PASSWORD_SALT
        };

        try (Cursor cursor = getReadableDatabase().query(
                TABLE_USERS,
                columns,
                COLUMN_EMAIL + " = ?",
                new String[]{normalizeEmail(email)},
                null,
                null,
                null,
                "1"
        )) {
            if (!cursor.moveToFirst()) {
                return AuthenticationResult.invalidCredentials();
            }

            String storedHash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_HASH));
            String storedSalt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_SALT));
            if (!PasswordHasher.verify(password, storedSalt, storedHash)) {
                return AuthenticationResult.invalidCredentials();
            }

            User user = new User(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULL_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL))
            );
            return AuthenticationResult.success(user);
        } catch (GeneralSecurityException | RuntimeException exception) {
            return AuthenticationResult.error();
        }
    }

    private boolean emailExists(SQLiteDatabase database, String email) {
        try (Cursor cursor = database.query(
                TABLE_USERS,
                new String[]{COLUMN_ID},
                COLUMN_EMAIL + " = ?",
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

        public Status getStatus() {
            return status;
        }

        public User getUser() {
            return user;
        }
    }
}
