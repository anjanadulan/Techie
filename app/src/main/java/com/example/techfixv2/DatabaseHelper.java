package com.example.techfixv2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context) {
        super(context, "TechFix.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, email TEXT, role TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS repairs (id INTEGER PRIMARY KEY AUTOINCREMENT, repair_id TEXT, device TEXT, status TEXT, cost TEXT, date TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS payments (id INTEGER PRIMARY KEY AUTOINCREMENT, invoice_no TEXT, repair_id TEXT, customer TEXT, amount REAL, method TEXT, status TEXT, date TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS payments (id INTEGER PRIMARY KEY AUTOINCREMENT, invoice_no TEXT, repair_id TEXT, customer TEXT, amount REAL, method TEXT, status TEXT, date TEXT)");
        }
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

    // Insert payment local
    public boolean addPayment(String invoiceNo, String repairId, String customer, double amount, String method, String status, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("invoice_no", invoiceNo);
        values.put("repair_id", repairId);
        values.put("customer", customer);
        values.put("amount", amount);
        values.put("method", method);
        values.put("status", status);
        values.put("date", date);

        long result = db.insert("payments", null, values);
        return result != -1;
    }

    // Get payment for repair
    public Cursor getPaymentForRepair(String repairId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM payments WHERE repair_id=? ORDER BY id DESC LIMIT 1", new String[]{repairId});
    }

    // Get all payments offline
    public Cursor getAllPayments() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM payments ORDER BY id DESC", null);
    }
}
