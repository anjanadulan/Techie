package com.example.techfix;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.techfix.data.model.Appointment;
import com.example.techfix.data.model.AppointmentStatus;
import com.example.techfix.data.model.Branch;
import com.example.techfix.data.model.DeviceCategory;
import com.example.techfix.data.model.Payment;
import com.example.techfix.data.model.PaymentMethod;
import com.example.techfix.data.model.PaymentStatus;
import com.example.techfix.data.model.RepairHistory;
import com.example.techfix.data.model.RepairService;
import com.example.techfix.data.model.SparePart;
import com.example.techfix.data.model.Technician;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CoreDataRepository {

    private final TechFixDatabaseHelper databaseHelper;

    public CoreDataRepository(Context context) {
        databaseHelper = new TechFixDatabaseHelper(context.getApplicationContext());
    }

    public long saveBranch(Branch branch) {
        if (branch.getLatitude() < -90 || branch.getLatitude() > 90 ||
                branch.getLongitude() < -180 || branch.getLongitude() > 180) {
            throw new IllegalArgumentException("Branch coordinates are invalid.");
        }

        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.NAME, requireText(branch.getName(), "Branch name"));
        values.put(TechFixDatabaseHelper.ADDRESS, requireText(branch.getAddress(), "Branch address"));
        values.put(TechFixDatabaseHelper.PHONE, requireText(branch.getPhone(), "Branch phone"));
        values.put(TechFixDatabaseHelper.LATITUDE, branch.getLatitude());
        values.put(TechFixDatabaseHelper.LONGITUDE, branch.getLongitude());
        values.put(TechFixDatabaseHelper.ACTIVE, toDatabaseBoolean(branch.isActive()));
        return save(TechFixDatabaseHelper.TABLE_BRANCHES, branch.getId(), values);
    }

    public List<Branch> getBranches(boolean activeOnly) {
        return query(
                TechFixDatabaseHelper.TABLE_BRANCHES,
                activeOnly ? TechFixDatabaseHelper.ACTIVE + " = 1" : null,
                null,
                TechFixDatabaseHelper.NAME + " COLLATE NOCASE",
                cursor -> new Branch(
                        getLong(cursor, TechFixDatabaseHelper.ID),
                        getString(cursor, TechFixDatabaseHelper.NAME),
                        getString(cursor, TechFixDatabaseHelper.ADDRESS),
                        getString(cursor, TechFixDatabaseHelper.PHONE),
                        getDouble(cursor, TechFixDatabaseHelper.LATITUDE),
                        getDouble(cursor, TechFixDatabaseHelper.LONGITUDE),
                        getBoolean(cursor, TechFixDatabaseHelper.ACTIVE)
                )
        );
    }

    public int setBranchActive(long branchId, boolean active) {
        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.ACTIVE, toDatabaseBoolean(active));
        return updateById(TechFixDatabaseHelper.TABLE_BRANCHES, branchId, values);
    }

    public long saveTechnician(Technician technician) {
        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.BRANCH_ID, requirePositive(
                technician.getBranchId(), "Technician branch"));
        values.put(TechFixDatabaseHelper.FULL_NAME, requireText(
                technician.getFullName(), "Technician name"));
        values.put(TechFixDatabaseHelper.EMAIL, requireText(
                technician.getEmail(), "Technician email").toLowerCase(Locale.ROOT));
        values.put(TechFixDatabaseHelper.PHONE, requireText(
                technician.getPhone(), "Technician phone"));
        values.put(TechFixDatabaseHelper.SPECIALTY, requireText(
                technician.getSpecialty(), "Technician specialty"));
        values.put(TechFixDatabaseHelper.ACTIVE, toDatabaseBoolean(technician.isActive()));
        return save(TechFixDatabaseHelper.TABLE_TECHNICIANS, technician.getId(), values);
    }

    public List<Technician> getTechniciansForBranch(long branchId, boolean activeOnly) {
        String selection = TechFixDatabaseHelper.BRANCH_ID + " = ?" +
                (activeOnly ? " AND " + TechFixDatabaseHelper.ACTIVE + " = 1" : "");
        return query(
                TechFixDatabaseHelper.TABLE_TECHNICIANS,
                selection,
                new String[]{String.valueOf(branchId)},
                TechFixDatabaseHelper.FULL_NAME + " COLLATE NOCASE",
                cursor -> new Technician(
                        getLong(cursor, TechFixDatabaseHelper.ID),
                        getLong(cursor, TechFixDatabaseHelper.BRANCH_ID),
                        getString(cursor, TechFixDatabaseHelper.FULL_NAME),
                        getString(cursor, TechFixDatabaseHelper.EMAIL),
                        getString(cursor, TechFixDatabaseHelper.PHONE),
                        getString(cursor, TechFixDatabaseHelper.SPECIALTY),
                        getBoolean(cursor, TechFixDatabaseHelper.ACTIVE)
                )
        );
    }

    public long saveDeviceCategory(DeviceCategory category) {
        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.NAME, requireText(category.getName(), "Category name"));
        values.put(TechFixDatabaseHelper.DESCRIPTION, requireText(
                category.getDescription(), "Category description"));
        values.put(TechFixDatabaseHelper.ACTIVE, toDatabaseBoolean(category.isActive()));
        return save(TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES, category.getId(), values);
    }

    public List<DeviceCategory> getDeviceCategories(boolean activeOnly) {
        return query(
                TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES,
                activeOnly ? TechFixDatabaseHelper.ACTIVE + " = 1" : null,
                null,
                TechFixDatabaseHelper.NAME + " COLLATE NOCASE",
                cursor -> new DeviceCategory(
                        getLong(cursor, TechFixDatabaseHelper.ID),
                        getString(cursor, TechFixDatabaseHelper.NAME),
                        getString(cursor, TechFixDatabaseHelper.DESCRIPTION),
                        getBoolean(cursor, TechFixDatabaseHelper.ACTIVE)
                )
        );
    }

    public long saveRepairService(RepairService service) {
        requireNonNegative(service.getBasePriceCents(), "Service price");
        requirePositive(service.getEstimatedMinutes(), "Estimated minutes");

        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.CATEGORY_ID, requirePositive(
                service.getCategoryId(), "Service category"));
        values.put(TechFixDatabaseHelper.NAME, requireText(service.getName(), "Service name"));
        values.put(TechFixDatabaseHelper.DESCRIPTION, requireText(
                service.getDescription(), "Service description"));
        values.put(TechFixDatabaseHelper.BASE_PRICE_CENTS, service.getBasePriceCents());
        values.put(TechFixDatabaseHelper.ESTIMATED_MINUTES, service.getEstimatedMinutes());
        values.put(TechFixDatabaseHelper.ACTIVE, toDatabaseBoolean(service.isActive()));
        return save(TechFixDatabaseHelper.TABLE_REPAIR_SERVICES, service.getId(), values);
    }

    public List<RepairService> getRepairServicesForCategory(long categoryId, boolean activeOnly) {
        String selection = TechFixDatabaseHelper.CATEGORY_ID + " = ?" +
                (activeOnly ? " AND " + TechFixDatabaseHelper.ACTIVE + " = 1" : "");
        return query(
                TechFixDatabaseHelper.TABLE_REPAIR_SERVICES,
                selection,
                new String[]{String.valueOf(categoryId)},
                TechFixDatabaseHelper.NAME + " COLLATE NOCASE",
                cursor -> new RepairService(
                        getLong(cursor, TechFixDatabaseHelper.ID),
                        getLong(cursor, TechFixDatabaseHelper.CATEGORY_ID),
                        getString(cursor, TechFixDatabaseHelper.NAME),
                        getString(cursor, TechFixDatabaseHelper.DESCRIPTION),
                        getLong(cursor, TechFixDatabaseHelper.BASE_PRICE_CENTS),
                        getInt(cursor, TechFixDatabaseHelper.ESTIMATED_MINUTES),
                        getBoolean(cursor, TechFixDatabaseHelper.ACTIVE)
                )
        );
    }

    public long saveSparePart(SparePart sparePart) {
        requireNonNegative(sparePart.getUnitPriceCents(), "Spare part price");
        requireNonNegative(sparePart.getQuantityAvailable(), "Spare part quantity");

        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.BRANCH_ID, requirePositive(
                sparePart.getBranchId(), "Spare part branch"));
        putNullableLong(values, TechFixDatabaseHelper.CATEGORY_ID, sparePart.getCategoryId());
        values.put(TechFixDatabaseHelper.NAME, requireText(sparePart.getName(), "Spare part name"));
        values.put(TechFixDatabaseHelper.SKU, requireText(sparePart.getSku(), "Spare part SKU"));
        values.put(TechFixDatabaseHelper.UNIT_PRICE_CENTS, sparePart.getUnitPriceCents());
        values.put(TechFixDatabaseHelper.QUANTITY_AVAILABLE, sparePart.getQuantityAvailable());
        values.put(TechFixDatabaseHelper.ACTIVE, toDatabaseBoolean(sparePart.isActive()));
        return save(TechFixDatabaseHelper.TABLE_SPARE_PARTS, sparePart.getId(), values);
    }

    public List<SparePart> getSparePartsForBranch(long branchId, boolean activeOnly) {
        String selection = TechFixDatabaseHelper.BRANCH_ID + " = ?" +
                (activeOnly ? " AND " + TechFixDatabaseHelper.ACTIVE + " = 1" : "");
        return query(
                TechFixDatabaseHelper.TABLE_SPARE_PARTS,
                selection,
                new String[]{String.valueOf(branchId)},
                TechFixDatabaseHelper.NAME + " COLLATE NOCASE",
                cursor -> new SparePart(
                        getLong(cursor, TechFixDatabaseHelper.ID),
                        getLong(cursor, TechFixDatabaseHelper.BRANCH_ID),
                        getNullableLong(cursor, TechFixDatabaseHelper.CATEGORY_ID),
                        getString(cursor, TechFixDatabaseHelper.NAME),
                        getString(cursor, TechFixDatabaseHelper.SKU),
                        getLong(cursor, TechFixDatabaseHelper.UNIT_PRICE_CENTS),
                        getInt(cursor, TechFixDatabaseHelper.QUANTITY_AVAILABLE),
                        getBoolean(cursor, TechFixDatabaseHelper.ACTIVE)
                )
        );
    }

    public List<SparePart> getAvailableSpareParts(long branchId, Long categoryId) {
        String selection = TechFixDatabaseHelper.BRANCH_ID + " = ? AND " +
                TechFixDatabaseHelper.ACTIVE + " = 1 AND " +
                TechFixDatabaseHelper.QUANTITY_AVAILABLE + " > 0";
        List<String> arguments = new ArrayList<>();
        arguments.add(String.valueOf(branchId));
        if (categoryId != null) {
            selection += " AND " + TechFixDatabaseHelper.CATEGORY_ID + " = ?";
            arguments.add(String.valueOf(categoryId));
        }
        return query(
                TechFixDatabaseHelper.TABLE_SPARE_PARTS,
                selection,
                arguments.toArray(new String[0]),
                TechFixDatabaseHelper.NAME + " COLLATE NOCASE",
                cursor -> new SparePart(
                        getLong(cursor, TechFixDatabaseHelper.ID),
                        getLong(cursor, TechFixDatabaseHelper.BRANCH_ID),
                        getNullableLong(cursor, TechFixDatabaseHelper.CATEGORY_ID),
                        getString(cursor, TechFixDatabaseHelper.NAME),
                        getString(cursor, TechFixDatabaseHelper.SKU),
                        getLong(cursor, TechFixDatabaseHelper.UNIT_PRICE_CENTS),
                        getInt(cursor, TechFixDatabaseHelper.QUANTITY_AVAILABLE),
                        getBoolean(cursor, TechFixDatabaseHelper.ACTIVE)
                )
        );
    }

    public int updateSparePartQuantity(long sparePartId, int quantityAvailable) {
        requireNonNegative(quantityAvailable, "Spare part quantity");
        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.QUANTITY_AVAILABLE, quantityAvailable);
        return updateById(TechFixDatabaseHelper.TABLE_SPARE_PARTS, sparePartId, values);
    }

    public long saveAppointment(Appointment appointment) {
        requirePositive(appointment.getUserId(), "Appointment user");
        requirePositive(appointment.getServiceId(), "Appointment service");
        requirePositive(appointment.getAppointmentAt(), "Appointment time");
        if (appointment.getStatus() == null) {
            throw new IllegalArgumentException("Appointment status is required.");
        }

        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.USER_ID, appointment.getUserId());
        putNullableLong(values, TechFixDatabaseHelper.BRANCH_ID, appointment.getBranchId());
        putNullableLong(values, TechFixDatabaseHelper.TECHNICIAN_ID, appointment.getTechnicianId());
        values.put(TechFixDatabaseHelper.SERVICE_ID, appointment.getServiceId());
        values.put(TechFixDatabaseHelper.DEVICE_DETAILS, requireText(
                appointment.getDeviceDetails(), "Device details"));
        values.put(TechFixDatabaseHelper.PROBLEM_DESCRIPTION, requireText(
                appointment.getProblemDescription(), "Problem description"));
        values.put(TechFixDatabaseHelper.STATUS, appointment.getStatus().name());
        values.put(TechFixDatabaseHelper.APPOINTMENT_AT, appointment.getAppointmentAt());
        values.put(TechFixDatabaseHelper.CREATED_AT,
                appointment.getCreatedAt() > 0 ? appointment.getCreatedAt() : System.currentTimeMillis());
        return save(TechFixDatabaseHelper.TABLE_APPOINTMENTS, appointment.getId(), values);
    }

    public List<Appointment> getAppointmentsForUser(long userId) {
        return query(
                TechFixDatabaseHelper.TABLE_APPOINTMENTS,
                TechFixDatabaseHelper.USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                TechFixDatabaseHelper.APPOINTMENT_AT + " DESC",
                this::mapAppointment
        );
    }

    public List<Appointment> getAppointmentsForBranch(long branchId, AppointmentStatus status) {
        String selection = TechFixDatabaseHelper.BRANCH_ID + " = ?";
        List<String> arguments = new ArrayList<>();
        arguments.add(String.valueOf(branchId));
        if (status != null) {
            selection += " AND " + TechFixDatabaseHelper.STATUS + " = ?";
            arguments.add(status.name());
        }
        return query(
                TechFixDatabaseHelper.TABLE_APPOINTMENTS,
                selection,
                arguments.toArray(new String[0]),
                TechFixDatabaseHelper.APPOINTMENT_AT,
                this::mapAppointment
        );
    }

    public int assignAppointment(long appointmentId, long branchId, Long technicianId) {
        requirePositive(appointmentId, "Appointment");
        requirePositive(branchId, "Appointment branch");
        if (technicianId != null && !technicianBelongsToBranch(technicianId, branchId)) {
            throw new IllegalArgumentException(
                    "The selected technician does not belong to the selected branch.");
        }

        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(TechFixDatabaseHelper.BRANCH_ID, branchId);
            putNullableLong(values, TechFixDatabaseHelper.TECHNICIAN_ID, technicianId);
            values.put(TechFixDatabaseHelper.STATUS, AppointmentStatus.ASSIGNED.name());
            int updated = database.update(
                    TechFixDatabaseHelper.TABLE_APPOINTMENTS,
                    values,
                    TechFixDatabaseHelper.ID + " = ?",
                    new String[]{String.valueOf(appointmentId)}
            );
            if (updated != 1) {
                return 0;
            }

            ContentValues historyValues = new ContentValues();
            historyValues.put(TechFixDatabaseHelper.APPOINTMENT_ID, appointmentId);
            historyValues.put(TechFixDatabaseHelper.STATUS, AppointmentStatus.ASSIGNED.name());
            historyValues.put(TechFixDatabaseHelper.NOTES, "Appointment assigned to branch.");
            historyValues.putNull(TechFixDatabaseHelper.IMAGE_PATH);
            historyValues.put(TechFixDatabaseHelper.RECORDED_AT, System.currentTimeMillis());
            database.insertOrThrow(
                    TechFixDatabaseHelper.TABLE_REPAIR_HISTORY,
                    null,
                    historyValues
            );
            database.setTransactionSuccessful();
            return 1;
        } finally {
            database.endTransaction();
        }
    }

    public boolean updateAppointmentStatus(long appointmentId, AppointmentStatus status,
                                           String notes, String imagePath) {
        if (status == null) {
            throw new IllegalArgumentException("Appointment status is required.");
        }

        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            ContentValues appointmentValues = new ContentValues();
            appointmentValues.put(TechFixDatabaseHelper.STATUS, status.name());
            int updated = database.update(
                    TechFixDatabaseHelper.TABLE_APPOINTMENTS,
                    appointmentValues,
                    TechFixDatabaseHelper.ID + " = ?",
                    new String[]{String.valueOf(appointmentId)}
            );
            if (updated != 1) {
                return false;
            }

            ContentValues historyValues = new ContentValues();
            historyValues.put(TechFixDatabaseHelper.APPOINTMENT_ID, appointmentId);
            historyValues.put(TechFixDatabaseHelper.STATUS, status.name());
            historyValues.put(TechFixDatabaseHelper.NOTES, notes == null ? "" : notes.trim());
            putNullableText(historyValues, TechFixDatabaseHelper.IMAGE_PATH, imagePath);
            historyValues.put(TechFixDatabaseHelper.RECORDED_AT, System.currentTimeMillis());
            database.insertOrThrow(
                    TechFixDatabaseHelper.TABLE_REPAIR_HISTORY,
                    null,
                    historyValues
            );
            database.setTransactionSuccessful();
            return true;
        } finally {
            database.endTransaction();
        }
    }

    public long savePayment(Payment payment) {
        requirePositive(payment.getAppointmentId(), "Payment appointment");
        requireNonNegative(payment.getAmountCents(), "Payment amount");
        if (payment.getMethod() == null || payment.getStatus() == null) {
            throw new IllegalArgumentException("Payment method and status are required.");
        }

        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.APPOINTMENT_ID, payment.getAppointmentId());
        values.put(TechFixDatabaseHelper.AMOUNT_CENTS, payment.getAmountCents());
        values.put(TechFixDatabaseHelper.METHOD, payment.getMethod().name());
        values.put(TechFixDatabaseHelper.STATUS, payment.getStatus().name());
        putNullableText(values, TechFixDatabaseHelper.REFERENCE, payment.getReference());
        Long paidAt = payment.getPaidAt();
        if (payment.getStatus() == PaymentStatus.PAID && paidAt == null) {
            paidAt = System.currentTimeMillis();
        }
        putNullableLong(values, TechFixDatabaseHelper.PAID_AT, paidAt);
        values.put(TechFixDatabaseHelper.CREATED_AT,
                payment.getCreatedAt() > 0 ? payment.getCreatedAt() : System.currentTimeMillis());
        return save(TechFixDatabaseHelper.TABLE_PAYMENTS, payment.getId(), values);
    }

    public List<Payment> getPaymentsForAppointment(long appointmentId) {
        return query(
                TechFixDatabaseHelper.TABLE_PAYMENTS,
                TechFixDatabaseHelper.APPOINTMENT_ID + " = ?",
                new String[]{String.valueOf(appointmentId)},
                TechFixDatabaseHelper.CREATED_AT + " DESC",
                cursor -> new Payment(
                        getLong(cursor, TechFixDatabaseHelper.ID),
                        getLong(cursor, TechFixDatabaseHelper.APPOINTMENT_ID),
                        getLong(cursor, TechFixDatabaseHelper.AMOUNT_CENTS),
                        PaymentMethod.valueOf(getString(cursor, TechFixDatabaseHelper.METHOD)),
                        PaymentStatus.valueOf(getString(cursor, TechFixDatabaseHelper.STATUS)),
                        getNullableString(cursor, TechFixDatabaseHelper.REFERENCE),
                        getNullableLong(cursor, TechFixDatabaseHelper.PAID_AT),
                        getLong(cursor, TechFixDatabaseHelper.CREATED_AT)
                )
        );
    }

    public long addRepairHistory(RepairHistory history) {
        requirePositive(history.getAppointmentId(), "Repair history appointment");
        if (history.getStatus() == null) {
            throw new IllegalArgumentException("Repair history status is required.");
        }

        ContentValues values = new ContentValues();
        values.put(TechFixDatabaseHelper.APPOINTMENT_ID, history.getAppointmentId());
        values.put(TechFixDatabaseHelper.STATUS, history.getStatus().name());
        values.put(TechFixDatabaseHelper.NOTES,
                history.getNotes() == null ? "" : history.getNotes().trim());
        putNullableText(values, TechFixDatabaseHelper.IMAGE_PATH, history.getImagePath());
        values.put(TechFixDatabaseHelper.RECORDED_AT,
                history.getRecordedAt() > 0 ? history.getRecordedAt() : System.currentTimeMillis());
        return databaseHelper.getWritableDatabase().insertOrThrow(
                TechFixDatabaseHelper.TABLE_REPAIR_HISTORY,
                null,
                values
        );
    }

    public List<RepairHistory> getRepairHistory(long appointmentId) {
        return query(
                TechFixDatabaseHelper.TABLE_REPAIR_HISTORY,
                TechFixDatabaseHelper.APPOINTMENT_ID + " = ?",
                new String[]{String.valueOf(appointmentId)},
                TechFixDatabaseHelper.RECORDED_AT,
                cursor -> new RepairHistory(
                        getLong(cursor, TechFixDatabaseHelper.ID),
                        getLong(cursor, TechFixDatabaseHelper.APPOINTMENT_ID),
                        AppointmentStatus.valueOf(getString(cursor, TechFixDatabaseHelper.STATUS)),
                        getString(cursor, TechFixDatabaseHelper.NOTES),
                        getNullableString(cursor, TechFixDatabaseHelper.IMAGE_PATH),
                        getLong(cursor, TechFixDatabaseHelper.RECORDED_AT)
                )
        );
    }

    public void close() {
        databaseHelper.close();
    }

    private Appointment mapAppointment(Cursor cursor) {
        return new Appointment(
                getLong(cursor, TechFixDatabaseHelper.ID),
                getLong(cursor, TechFixDatabaseHelper.USER_ID),
                getNullableLong(cursor, TechFixDatabaseHelper.BRANCH_ID),
                getNullableLong(cursor, TechFixDatabaseHelper.TECHNICIAN_ID),
                getLong(cursor, TechFixDatabaseHelper.SERVICE_ID),
                getString(cursor, TechFixDatabaseHelper.DEVICE_DETAILS),
                getString(cursor, TechFixDatabaseHelper.PROBLEM_DESCRIPTION),
                AppointmentStatus.valueOf(getString(cursor, TechFixDatabaseHelper.STATUS)),
                getLong(cursor, TechFixDatabaseHelper.APPOINTMENT_AT),
                getLong(cursor, TechFixDatabaseHelper.CREATED_AT)
        );
    }

    private boolean technicianBelongsToBranch(long technicianId, long branchId) {
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                TechFixDatabaseHelper.TABLE_TECHNICIANS,
                new String[]{TechFixDatabaseHelper.ID},
                TechFixDatabaseHelper.ID + " = ? AND " +
                        TechFixDatabaseHelper.BRANCH_ID + " = ? AND " +
                        TechFixDatabaseHelper.ACTIVE + " = 1",
                new String[]{String.valueOf(technicianId), String.valueOf(branchId)},
                null,
                null,
                null,
                "1"
        )) {
            return cursor.moveToFirst();
        }
    }

    private long save(String table, long id, ContentValues values) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        if (id <= 0) {
            return database.insertOrThrow(table, null, values);
        }
        int updated = database.update(
                table,
                values,
                TechFixDatabaseHelper.ID + " = ?",
                new String[]{String.valueOf(id)}
        );
        return updated == 1 ? id : -1;
    }

    private int updateById(String table, long id, ContentValues values) {
        requirePositive(id, "Record ID");
        return databaseHelper.getWritableDatabase().update(
                table,
                values,
                TechFixDatabaseHelper.ID + " = ?",
                new String[]{String.valueOf(id)}
        );
    }

    private <T> List<T> query(String table, String selection, String[] selectionArgs,
                              String orderBy, CursorMapper<T> mapper) {
        List<T> results = new ArrayList<>();
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                table,
                null,
                selection,
                selectionArgs,
                null,
                null,
                orderBy
        )) {
            while (cursor.moveToNext()) {
                results.add(mapper.map(cursor));
            }
        }
        return results;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    private long requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }
        return value;
    }

    private long requireNonNegative(long value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative.");
        }
        return value;
    }

    private int toDatabaseBoolean(boolean value) {
        return value ? 1 : 0;
    }

    private void putNullableLong(ContentValues values, String key, Long value) {
        if (value == null) {
            values.putNull(key);
        } else {
            values.put(key, value);
        }
    }

    private void putNullableText(ContentValues values, String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            values.putNull(key);
        } else {
            values.put(key, value.trim());
        }
    }

    private long getLong(Cursor cursor, String column) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(column));
    }

    private int getInt(Cursor cursor, String column) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(column));
    }

    private double getDouble(Cursor cursor, String column) {
        return cursor.getDouble(cursor.getColumnIndexOrThrow(column));
    }

    private String getString(Cursor cursor, String column) {
        return cursor.getString(cursor.getColumnIndexOrThrow(column));
    }

    private String getNullableString(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? null : cursor.getString(index);
    }

    private Long getNullableLong(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? null : cursor.getLong(index);
    }

    private boolean getBoolean(Cursor cursor, String column) {
        return getInt(cursor, column) == 1;
    }

    private interface CursorMapper<T> {
        T map(Cursor cursor);
    }
}
