package com.example.techfixv2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context) {
        super(context, "TechFix.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, email TEXT, role TEXT)");
        db.execSQL("CREATE TABLE repairs (id INTEGER PRIMARY KEY AUTOINCREMENT, repair_id TEXT, device TEXT, status TEXT, cost TEXT, date TEXT)");

        // test codes. ((((((( deleteeeeeeeeeeeeeeeeeee)
        db.execSQL("INSERT INTO repairs (repair_id, device, status, cost, date) VALUES ('#TF-1042', 'iPhone 13 Pro Screen', 'Completed', 'LKR 18,500', '2026-08-09')");
        db.execSQL("INSERT INTO repairs (repair_id, device, status, cost, date) VALUES ('#TF-1038', 'MacBook Pro M1 Keyboard', 'In Progress', 'LKR 28,500', '2026-08-08')");
        db.execSQL("INSERT INTO repairs (repair_id, device, status, cost, date) VALUES ('#TF-1011', 'iPad Air 4 Battery', 'Pending', 'LKR 12,200', '2026-08-05')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS repairs");
        onCreate(db);
    }

    // Insert to local
    public boolean insertUser(String name, String email, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email != null ? email.trim().toLowerCase() : "");
        values.put("role", role);

        long result = db.insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }

    // Get role
    public String getUserRole(String email) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT role FROM users WHERE email=?", new String[]{cleanEmail});
        if (cursor != null && cursor.moveToFirst()) {
            String role = cursor.getString(0);
            cursor.close();
            return role;
        }
        if (cursor != null) {
            cursor.close();
        }
        return null;
    }

    // Get unmae
    public String getUserName(String email) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM users WHERE email=?", new String[]{cleanEmail});
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(0);
            cursor.close();
            return name;
        }
        if (cursor != null) {
            cursor.close();
        }
        return "User";
    }

    // Insert repair local
    public boolean addRepair(String repairId, String device, String status, String cost, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("repair_id", repairId);
        values.put("device", device);
        values.put("status", status);
        values.put("cost", cost);
        values.put("date", date);

        long result = db.insert("repairs", null, values);
        return result != -1;
    }

    // Get all repairs offline
    public Cursor getAllRepairs() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM repairs ORDER BY id DESC", null);
    }
}
