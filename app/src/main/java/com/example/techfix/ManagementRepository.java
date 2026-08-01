package com.example.techfix;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.techfix.data.model.AppointmentStatus;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class ManagementRepository implements AutoCloseable {
  private static final String ACTIVE_APPOINTMENTS =
      "('PENDING','ASSIGNED','IN_PROGRESS',"
      + "'WAITING_FOR_PARTS','READY_FOR_PAYMENT')";
  private final Context appContext;
  private final TechFixDatabaseHelper helper;

  public ManagementRepository(Context context) {
    appContext = context.getApplicationContext();
    helper = new TechFixDatabaseHelper(appContext);
  }

  public List<ManagementRecord> getRecords(String module, String branch) {
    switch (module) {
    case ManagementModuleActivity.BRANCHES:
      return getBranches();
    case ManagementModuleActivity.CATEGORIES:
      return getCategories();
    case ManagementModuleActivity.TECHNICIANS:
      return getTechnicians(branch);
    case ManagementModuleActivity.PRICES:
      return getServices();
    case ManagementModuleActivity.PARTS:
      return getParts(branch);
    case ManagementModuleActivity.IMAGES:
      return getRepairImages(branch);
    case ManagementModuleActivity.PAYMENTS:
      return getPayments(branch);
    case ManagementModuleActivity.STATUSES:
      return getAppointments(branch, true);
    case ManagementModuleActivity.APPOINTMENTS:
    default:
      return getAppointments(branch, false);
    }
  }

  public ModuleSummary getSummary(String module, String branch) {
    List<ManagementRecord> records = getRecords(module, branch);
    switch (module) {
    case ManagementModuleActivity.BRANCHES:
      return new ModuleSummary(String.valueOf(records.size()),
                               "service branches", "LOCATIONS");
    case ManagementModuleActivity.CATEGORIES:
      return new ModuleSummary(String.valueOf(records.size()),
                               "device categories", "CATALOG");
    case ManagementModuleActivity.TECHNICIANS:
      return new ModuleSummary(String.valueOf(records.size()), "technicians",
                               "LIVE DATA");
    case ManagementModuleActivity.PRICES:
      return new ModuleSummary(String.valueOf(records.size()),
                               "active repair services", "CATALOG");
    case ManagementModuleActivity.PARTS:
      long units = queryLong(
          "SELECT COALESCE(SUM(p." + TechFixDatabaseHelper.QUANTITY_AVAILABLE +
              "),0) FROM " + TechFixDatabaseHelper.TABLE_SPARE_PARTS +
              " p LEFT JOIN " + TechFixDatabaseHelper.TABLE_BRANCHES +
              " b ON b." + TechFixDatabaseHelper.ID + "=p." +
              TechFixDatabaseHelper.BRANCH_ID + " WHERE 1=1" +
              branchWhere(branch, "b"),
          branchArgs(branch));
      return new ModuleSummary(String.valueOf(units),
                               "available spare-part units", "LIVE STOCK");
    case ManagementModuleActivity.IMAGES:
      return new ModuleSummary(String.valueOf(records.size()),
                               "saved repair images", "GALLERY");
    case ManagementModuleActivity.PAYMENTS:
      long paid = paidTotal(branch);
      return new ModuleSummary(formatPrice(paid), "payments received",
                               "LIVE TOTAL");
    case ManagementModuleActivity.STATUSES:
      return new ModuleSummary(String.valueOf(records.size()),
                               "repairs requiring tracking", "LIVE QUEUE");
    case ManagementModuleActivity.APPOINTMENTS:
    default:
      return new ModuleSummary(String.valueOf(records.size()),
                               "repair appointments", "LIVE QUEUE");
    }
  }

  public DashboardStats getDashboardStats() {
    long active = queryLong("SELECT COUNT(*) FROM " +
                                TechFixDatabaseHelper.TABLE_APPOINTMENTS +
                                " WHERE " + TechFixDatabaseHelper.STATUS +
                                " IN " + ACTIVE_APPOINTMENTS,
                            null);
    long ready = queryLong(
        "SELECT COUNT(*) FROM " + TechFixDatabaseHelper.TABLE_APPOINTMENTS +
            " WHERE " + TechFixDatabaseHelper.STATUS + "='READY_FOR_PAYMENT'",
        null);
    long lowStock = queryLong(
        "SELECT COUNT(*) FROM " + TechFixDatabaseHelper.TABLE_SPARE_PARTS +
            " WHERE " + TechFixDatabaseHelper.ACTIVE + "=1 AND " +
            TechFixDatabaseHelper.QUANTITY_AVAILABLE + "<=5",
        null);
    long technicians = queryLong(
        "SELECT COUNT(*) FROM " + TechFixDatabaseHelper.TABLE_TECHNICIANS +
            " WHERE " + TechFixDatabaseHelper.ACTIVE + "=1",
        null);
    long paid = paidTotal("All");
    return new DashboardStats(active, ready, lowStock, technicians, paid,
                              getRecentActivity());
  }

  public boolean updateAppointmentStatus(long appointmentId,
                                         AppointmentStatus status) {
    SQLiteDatabase database = helper.getWritableDatabase();
    boolean changed = false;
    database.beginTransaction();
    try {
      ContentValues appointment = new ContentValues();
      appointment.put(TechFixDatabaseHelper.STATUS, status.name());
      int updated =
          database.update(TechFixDatabaseHelper.TABLE_APPOINTMENTS, appointment,
                          TechFixDatabaseHelper.ID + "=?",
                          new String[] {String.valueOf(appointmentId)});
      if (updated != 1)
        return false;
      ContentValues history = new ContentValues();
      history.put(TechFixDatabaseHelper.APPOINTMENT_ID, appointmentId);
      history.put(TechFixDatabaseHelper.STATUS, status.name());
      history.put(TechFixDatabaseHelper.NOTES,
                  "Status updated by management to " + statusLabel(status) +
                      ".");
      history.putNull(TechFixDatabaseHelper.IMAGE_PATH);
      history.put(TechFixDatabaseHelper.RECORDED_AT,
                  System.currentTimeMillis());
      database.insertOrThrow(TechFixDatabaseHelper.TABLE_REPAIR_HISTORY, null,
                             history);
      database.setTransactionSuccessful();
      changed = true;
    } finally {
      database.endTransaction();
    }
    if (changed)
      FirebaseSyncScheduler.enqueueNow(appContext);
    return changed;
  }

  public boolean setTechnicianActive(long technicianId, boolean active) {
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.ACTIVE, active ? 1 : 0);
    return updateById(TechFixDatabaseHelper.TABLE_TECHNICIANS, technicianId,
                      values) == 1;
  }

  public boolean setBranchActive(long branchId, boolean active) {
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.ACTIVE, active ? 1 : 0);
    return updateById(TechFixDatabaseHelper.TABLE_BRANCHES, branchId, values) ==
        1;
  }

  public boolean setCategoryActive(long categoryId, boolean active) {
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.ACTIVE, active ? 1 : 0);
    return updateById(TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES, categoryId,
                      values) == 1;
  }

  public boolean updateServicePrice(long serviceId, long rupees) {
    if (rupees < 0)
      throw new IllegalArgumentException("Price cannot be negative.");
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.BASE_PRICE_CENTS,
               Math.multiplyExact(rupees, 100));
    return updateById(TechFixDatabaseHelper.TABLE_REPAIR_SERVICES, serviceId,
                      values) == 1;
  }

  public boolean updatePartQuantity(long partId, int quantity) {
    if (quantity < 0)
      throw new IllegalArgumentException("Quantity cannot be negative.");
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.QUANTITY_AVAILABLE, quantity);
    return updateById(TechFixDatabaseHelper.TABLE_SPARE_PARTS, partId,
                      values) == 1;
  }

  public boolean markPaymentPaid(long paymentId) {
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.STATUS, "PAID");
    values.put(TechFixDatabaseHelper.PAID_AT, System.currentTimeMillis());
    return updateById(TechFixDatabaseHelper.TABLE_PAYMENTS, paymentId,
                      values) == 1;
  }

  public boolean featureRepairImage(long historyId) {
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.NOTES, "Featured repair image");
    return updateById(TechFixDatabaseHelper.TABLE_REPAIR_HISTORY, historyId,
                      values) == 1;
  }

  public long addRepairImage(long appointmentId, String imagePath) {
    if (imagePath == null || imagePath.trim().isEmpty())
      throw new IllegalArgumentException("Repair image is required.");
    AppointmentStatus status = currentStatus(appointmentId);
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.APPOINTMENT_ID, appointmentId);
    values.put(TechFixDatabaseHelper.STATUS, status.name());
    values.put(TechFixDatabaseHelper.NOTES,
               "Repair image added by management.");
    values.put(TechFixDatabaseHelper.IMAGE_PATH, imagePath.trim());
    values.put(TechFixDatabaseHelper.RECORDED_AT, System.currentTimeMillis());
    return insertAndSync(TechFixDatabaseHelper.TABLE_REPAIR_HISTORY, values);
  }

  public long createWalkInAppointment(String deviceDetails, String branchName) {
    String device = requireText(deviceDetails, "Device details");
    SQLiteDatabase database = helper.getWritableDatabase();
    long userId = firstId(database, TechFixDatabaseHelper.TABLE_USERS);
    long serviceId =
        firstId(database, TechFixDatabaseHelper.TABLE_REPAIR_SERVICES);
    long branchId = findBranchId(database, branchName);
    if (userId <= 0)
      throw new IllegalStateException(
          "Create a customer account before adding an appointment.");
    if (serviceId <= 0 || branchId <= 0)
      throw new IllegalStateException(
          "Service and branch reference data is unavailable.");
    long appointmentId;
    database.beginTransaction();
    try {
      long now = System.currentTimeMillis();
      ContentValues appointment = new ContentValues();
      appointment.put(TechFixDatabaseHelper.USER_ID, userId);
      appointment.put(TechFixDatabaseHelper.BRANCH_ID, branchId);
      appointment.putNull(TechFixDatabaseHelper.TECHNICIAN_ID);
      appointment.put(TechFixDatabaseHelper.SERVICE_ID, serviceId);
      appointment.put(TechFixDatabaseHelper.DEVICE_DETAILS, device);
      appointment.put(TechFixDatabaseHelper.PROBLEM_DESCRIPTION,
                      "Walk-in repair request");
      appointment.put(TechFixDatabaseHelper.STATUS,
                      AppointmentStatus.PENDING.name());
      appointment.put(TechFixDatabaseHelper.APPOINTMENT_AT, now + 60 * 60_000L);
      appointment.put(TechFixDatabaseHelper.CREATED_AT, now);
      appointmentId = database.insertOrThrow(
          TechFixDatabaseHelper.TABLE_APPOINTMENTS, null, appointment);
      ContentValues history = new ContentValues();
      history.put(TechFixDatabaseHelper.APPOINTMENT_ID, appointmentId);
      history.put(TechFixDatabaseHelper.STATUS,
                  AppointmentStatus.PENDING.name());
      history.put(TechFixDatabaseHelper.NOTES,
                  "Walk-in appointment created by management.");
      history.putNull(TechFixDatabaseHelper.IMAGE_PATH);
      history.put(TechFixDatabaseHelper.RECORDED_AT, now);
      database.insertOrThrow(TechFixDatabaseHelper.TABLE_REPAIR_HISTORY, null,
                             history);
      database.setTransactionSuccessful();
    } finally {
      database.endTransaction();
    }
    FirebaseSyncScheduler.enqueueNow(appContext);
    return appointmentId;
  }

  public long addTechnician(String fullName, String branchName) {
    String name = requireText(fullName, "Technician name");
    SQLiteDatabase database = helper.getWritableDatabase();
    long branchId = findBranchId(database, branchName);
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.BRANCH_ID, branchId);
    values.put(TechFixDatabaseHelper.FULL_NAME, name);
    values.put(TechFixDatabaseHelper.EMAIL,
               "technician." + System.currentTimeMillis() + "@techfix.local");
    values.put(TechFixDatabaseHelper.PHONE, "Not provided");
    values.put(TechFixDatabaseHelper.SPECIALTY, "Phone and tablet repairs");
    values.put(TechFixDatabaseHelper.ACTIVE, 1);
    return insertAndSync(TechFixDatabaseHelper.TABLE_TECHNICIANS, values);
  }

  public long addBranch(String name, String address, String phone,
                        double latitude, double longitude) {
    if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)
      throw new IllegalArgumentException("Enter valid branch coordinates.");
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.NAME, requireText(name, "Branch name"));
    values.put(TechFixDatabaseHelper.ADDRESS,
               requireText(address, "Branch address"));
    values.put(TechFixDatabaseHelper.PHONE, requireText(phone, "Branch phone"));
    values.put(TechFixDatabaseHelper.LATITUDE, latitude);
    values.put(TechFixDatabaseHelper.LONGITUDE, longitude);
    values.put(TechFixDatabaseHelper.ACTIVE, 1);
    return insertAndSync(TechFixDatabaseHelper.TABLE_BRANCHES, values);
  }

  public long addCategory(String name, String description) {
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.NAME, requireText(name, "Category name"));
    values.put(TechFixDatabaseHelper.DESCRIPTION,
               requireText(description, "Category description"));
    values.put(TechFixDatabaseHelper.ACTIVE, 1);
    return insertAndSync(TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES, values);
  }

  public long addService(String serviceName) {
    String name = requireText(serviceName, "Service name");
    SQLiteDatabase database = helper.getWritableDatabase();
    long categoryId =
        firstId(database, TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES);
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.CATEGORY_ID, categoryId);
    values.put(TechFixDatabaseHelper.NAME, name);
    values.put(TechFixDatabaseHelper.DESCRIPTION,
               "Service added through management.");
    values.put(TechFixDatabaseHelper.BASE_PRICE_CENTS, 0);
    values.put(TechFixDatabaseHelper.ESTIMATED_MINUTES, 60);
    values.put(TechFixDatabaseHelper.ACTIVE, 1);
    return insertAndSync(TechFixDatabaseHelper.TABLE_REPAIR_SERVICES, values);
  }

  public long addSparePart(String partName, String branchName) {
    String name = requireText(partName, "Spare part name");
    SQLiteDatabase database = helper.getWritableDatabase();
    long branchId = findBranchId(database, branchName);
    long categoryId =
        firstId(database, TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES);
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.BRANCH_ID, branchId);
    values.put(TechFixDatabaseHelper.CATEGORY_ID, categoryId);
    values.put(TechFixDatabaseHelper.NAME, name);
    values.put(TechFixDatabaseHelper.SKU, "NEW-" + System.currentTimeMillis());
    values.put(TechFixDatabaseHelper.UNIT_PRICE_CENTS, 0);
    values.put(TechFixDatabaseHelper.QUANTITY_AVAILABLE, 0);
    values.put(TechFixDatabaseHelper.ACTIVE, 1);
    return insertAndSync(TechFixDatabaseHelper.TABLE_SPARE_PARTS, values);
  }

  public long createPendingPayment(long appointmentId) {
    SQLiteDatabase database = helper.getWritableDatabase();
    String sql = "SELECT s." + TechFixDatabaseHelper.BASE_PRICE_CENTS +
                 " FROM " + TechFixDatabaseHelper.TABLE_APPOINTMENTS +
                 " a JOIN " + TechFixDatabaseHelper.TABLE_REPAIR_SERVICES +
                 " s ON s." + TechFixDatabaseHelper.ID + "=a." +
                 TechFixDatabaseHelper.SERVICE_ID + " WHERE a." +
                 TechFixDatabaseHelper.ID + "=?";
    long amount;
    try (Cursor cursor = database.rawQuery(
             sql, new String[] {String.valueOf(appointmentId)})) {
      if (!cursor.moveToFirst())
        throw new IllegalArgumentException("Appointment not found.");
      amount = cursor.getLong(0);
    }
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.APPOINTMENT_ID, appointmentId);
    values.put(TechFixDatabaseHelper.AMOUNT_CENTS, amount);
    values.put(TechFixDatabaseHelper.METHOD, "CASH");
    values.put(TechFixDatabaseHelper.STATUS, "PENDING");
    values.putNull(TechFixDatabaseHelper.REFERENCE);
    values.putNull(TechFixDatabaseHelper.PAID_AT);
    values.put(TechFixDatabaseHelper.CREATED_AT, System.currentTimeMillis());
    return insertAndSync(TechFixDatabaseHelper.TABLE_PAYMENTS, values);
  }

  public List<AppointmentChoice> getAppointmentChoices() {
    List<AppointmentChoice> choices = new ArrayList<>();
    String sql = "SELECT a." + TechFixDatabaseHelper.ID + ",a." +
                 TechFixDatabaseHelper.DEVICE_DETAILS + ",a." +
                 TechFixDatabaseHelper.STATUS + " FROM " +
                 TechFixDatabaseHelper.TABLE_APPOINTMENTS + " a ORDER BY a." +
                 TechFixDatabaseHelper.CREATED_AT + " DESC";
    try (Cursor cursor = helper.getReadableDatabase().rawQuery(sql, null)) {
      while (cursor.moveToNext()) {
        choices.add(new AppointmentChoice(
            cursor.getLong(0),
            "#TF-" + (1000 + cursor.getLong(0)) + " · " + cursor.getString(1),
            AppointmentStatus.valueOf(cursor.getString(2))));
      }
    }
    return choices;
  }

  private List<ManagementRecord> getAppointments(String branch,
                                                 boolean activeOnly) {
    List<ManagementRecord> records = new ArrayList<>();
    String sql =
        "SELECT a." + TechFixDatabaseHelper.ID + ",a." +
        TechFixDatabaseHelper.STATUS + ",a." +
        TechFixDatabaseHelper.DEVICE_DETAILS + ",a." +
        TechFixDatabaseHelper.PROBLEM_DESCRIPTION + ",a." +
        TechFixDatabaseHelper.APPOINTMENT_AT + ",s." +
        TechFixDatabaseHelper.NAME + ",b." + TechFixDatabaseHelper.NAME +
        ",COALESCE(t." + TechFixDatabaseHelper.FULL_NAME +
        ",'Unassigned') FROM " + TechFixDatabaseHelper.TABLE_APPOINTMENTS +
        " a JOIN " + TechFixDatabaseHelper.TABLE_REPAIR_SERVICES +
        " s ON s." + TechFixDatabaseHelper.ID + "=a." +
        TechFixDatabaseHelper.SERVICE_ID + " LEFT JOIN " +
        TechFixDatabaseHelper.TABLE_BRANCHES + " b ON b." +
        TechFixDatabaseHelper.ID + "=a." + TechFixDatabaseHelper.BRANCH_ID +
        " LEFT JOIN " + TechFixDatabaseHelper.TABLE_TECHNICIANS + " t ON t." +
        TechFixDatabaseHelper.ID + "=a." + TechFixDatabaseHelper.TECHNICIAN_ID +
        " WHERE 1=1" +
        (activeOnly ? " AND a." + TechFixDatabaseHelper.STATUS + " IN " +
                          ACTIVE_APPOINTMENTS
                    : "") +
        branchWhere(branch, "b") + " ORDER BY a." +
        TechFixDatabaseHelper.APPOINTMENT_AT + " DESC";
    try (Cursor cursor =
             helper.getReadableDatabase().rawQuery(sql, branchArgs(branch))) {
      while (cursor.moveToNext()) {
        AppointmentStatus status =
            AppointmentStatus.valueOf(cursor.getString(1));
        String branchName = nullable(cursor.getString(6), "Unassigned branch");
        records.add(new ManagementRecord(
            cursor.getLong(0), "#TF-" + (1000 + cursor.getLong(0)),
            statusLabel(status),
            cursor.getString(2) + " · " + cursor.getString(5),
            branchName + " · " + cursor.getString(7) + " · " +
                formatDateTime(cursor.getLong(4)),
            cursor.getString(3), "UPDATE", branchName, null));
      }
    }
    return records;
  }

  private List<ManagementRecord> getBranches() {
    List<ManagementRecord> records = new ArrayList<>();
    try (Cursor cursor = helper.getReadableDatabase().query(
             TechFixDatabaseHelper.TABLE_BRANCHES, null, null, null, null, null,
             TechFixDatabaseHelper.NAME)) {
      while (cursor.moveToNext()) {
        boolean active = cursor.getInt(cursor.getColumnIndexOrThrow(
                             TechFixDatabaseHelper.ACTIVE)) == 1;
        String name = cursor.getString(
            cursor.getColumnIndexOrThrow(TechFixDatabaseHelper.NAME));
        records.add(new ManagementRecord(
            cursor.getLong(
                cursor.getColumnIndexOrThrow(TechFixDatabaseHelper.ID)),
            name.substring(0, Math.min(3, name.length()))
                .toUpperCase(Locale.US),
            active ? "ACTIVE" : "INACTIVE", name,
            cursor.getString(
                cursor.getColumnIndexOrThrow(TechFixDatabaseHelper.ADDRESS)),
            cursor.getString(
                cursor.getColumnIndexOrThrow(TechFixDatabaseHelper.PHONE)),
            active ? "DISABLE" : "ENABLE", name, null));
      }
    }
    return records;
  }

  private List<ManagementRecord> getCategories() {
    List<ManagementRecord> records = new ArrayList<>();
    try (Cursor cursor = helper.getReadableDatabase().query(
             TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES, null, null, null,
             null, null, TechFixDatabaseHelper.NAME)) {
      while (cursor.moveToNext()) {
        boolean active = cursor.getInt(cursor.getColumnIndexOrThrow(
                             TechFixDatabaseHelper.ACTIVE)) == 1;
        String name = cursor.getString(
            cursor.getColumnIndexOrThrow(TechFixDatabaseHelper.NAME));
        records.add(new ManagementRecord(
            cursor.getLong(
                cursor.getColumnIndexOrThrow(TechFixDatabaseHelper.ID)),
            name.substring(0, Math.min(4, name.length()))
                .toUpperCase(Locale.US),
            active ? "ACTIVE" : "INACTIVE", name, "Service classification",
            cursor.getString(cursor.getColumnIndexOrThrow(
                TechFixDatabaseHelper.DESCRIPTION)),
            active ? "DISABLE" : "ENABLE", "All", null));
      }
    }
    return records;
  }

  private List<ManagementRecord> getTechnicians(String branch) {
    List<ManagementRecord> records = new ArrayList<>();
    String sql =
        "SELECT t." + TechFixDatabaseHelper.ID + ",t." +
        TechFixDatabaseHelper.FULL_NAME + ",t." +
        TechFixDatabaseHelper.SPECIALTY + ",t." + TechFixDatabaseHelper.ACTIVE +
        ",b." + TechFixDatabaseHelper.NAME + ",(SELECT COUNT(*) FROM " +
        TechFixDatabaseHelper.TABLE_APPOINTMENTS + " a WHERE a." +
        TechFixDatabaseHelper.TECHNICIAN_ID + "=t." + TechFixDatabaseHelper.ID +
        " AND a." + TechFixDatabaseHelper.STATUS + " IN " +
        ACTIVE_APPOINTMENTS + ") FROM " +
        TechFixDatabaseHelper.TABLE_TECHNICIANS + " t JOIN " +
        TechFixDatabaseHelper.TABLE_BRANCHES + " b ON b." +
        TechFixDatabaseHelper.ID + "=t." + TechFixDatabaseHelper.BRANCH_ID +
        " WHERE 1=1" + branchWhere(branch, "b") + " ORDER BY t." +
        TechFixDatabaseHelper.FULL_NAME;
    try (Cursor cursor =
             helper.getReadableDatabase().rawQuery(sql, branchArgs(branch))) {
      while (cursor.moveToNext()) {
        boolean active = cursor.getInt(3) == 1;
        int jobs = cursor.getInt(5);
        String status = active ? (jobs > 0 ? "BUSY" : "AVAILABLE") : "OFF DUTY";
        records.add(new ManagementRecord(
            cursor.getLong(0), initials(cursor.getString(1)), status,
            cursor.getString(1) + " · " + cursor.getString(2),
            cursor.getString(4),
            active ? jobs + " active repair" + (jobs == 1 ? "" : "s")
                   : "Not receiving assignments",
            "MANAGE", cursor.getString(4), null));
      }
    }
    return records;
  }

  private List<ManagementRecord> getServices() {
    List<ManagementRecord> records = new ArrayList<>();
    String sql =
        "SELECT s." + TechFixDatabaseHelper.ID + ",s." +
        TechFixDatabaseHelper.NAME + ",s." +
        TechFixDatabaseHelper.BASE_PRICE_CENTS + ",s." +
        TechFixDatabaseHelper.ESTIMATED_MINUTES + ",s." +
        TechFixDatabaseHelper.ACTIVE + ",c." + TechFixDatabaseHelper.NAME +
        " FROM " + TechFixDatabaseHelper.TABLE_REPAIR_SERVICES + " s JOIN " +
        TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES + " c ON c." +
        TechFixDatabaseHelper.ID + "=s." + TechFixDatabaseHelper.CATEGORY_ID +
        " ORDER BY c." + TechFixDatabaseHelper.NAME + ",s." +
        TechFixDatabaseHelper.NAME;
    try (Cursor cursor = helper.getReadableDatabase().rawQuery(sql, null)) {
      while (cursor.moveToNext()) {
        records.add(new ManagementRecord(
            cursor.getLong(0), cursor.getString(5).toUpperCase(),
            cursor.getInt(4) == 1 ? "ACTIVE" : "INACTIVE", cursor.getString(1),
            cursor.getString(5) + " · " + cursor.getInt(3) + " minute estimate",
            formatPrice(cursor.getLong(2)), "EDIT", "All", null));
      }
    }
    return records;
  }

  private List<ManagementRecord> getParts(String branch) {
    List<ManagementRecord> records = new ArrayList<>();
    String sql =
        "SELECT p." + TechFixDatabaseHelper.ID + ",p." +
        TechFixDatabaseHelper.SKU + ",p." + TechFixDatabaseHelper.NAME + ",p." +
        TechFixDatabaseHelper.QUANTITY_AVAILABLE + ",b." +
        TechFixDatabaseHelper.NAME + ",COALESCE(c." +
        TechFixDatabaseHelper.NAME + ",'General') FROM " +
        TechFixDatabaseHelper.TABLE_SPARE_PARTS + " p JOIN " +
        TechFixDatabaseHelper.TABLE_BRANCHES + " b ON b." +
        TechFixDatabaseHelper.ID + "=p." + TechFixDatabaseHelper.BRANCH_ID +
        " LEFT JOIN " + TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES +
        " c ON c." + TechFixDatabaseHelper.ID + "=p." +
        TechFixDatabaseHelper.CATEGORY_ID + " WHERE p." +
        TechFixDatabaseHelper.ACTIVE + "=1" + branchWhere(branch, "b") +
        " ORDER BY p." + TechFixDatabaseHelper.NAME;
    try (Cursor cursor =
             helper.getReadableDatabase().rawQuery(sql, branchArgs(branch))) {
      while (cursor.moveToNext()) {
        int quantity = cursor.getInt(3);
        records.add(new ManagementRecord(
            cursor.getLong(0), cursor.getString(1),
            quantity <= 5 ? "LOW STOCK" : "IN STOCK", cursor.getString(2),
            cursor.getString(5) + " · " + cursor.getString(4),
            quantity + " units available", "ADJUST", cursor.getString(4),
            null));
      }
    }
    return records;
  }

  private List<ManagementRecord> getRepairImages(String branch) {
    List<ManagementRecord> records = new ArrayList<>();
    String sql =
        "SELECT h." + TechFixDatabaseHelper.ID + ",h." +
        TechFixDatabaseHelper.NOTES + ",h." + TechFixDatabaseHelper.IMAGE_PATH +
        ",h." + TechFixDatabaseHelper.RECORDED_AT + ",a." +
        TechFixDatabaseHelper.ID + ",a." +
        TechFixDatabaseHelper.DEVICE_DETAILS + ",s." +
        TechFixDatabaseHelper.NAME + ",COALESCE(b." +
        TechFixDatabaseHelper.NAME + ",'Unassigned') FROM " +
        TechFixDatabaseHelper.TABLE_REPAIR_HISTORY + " h JOIN " +
        TechFixDatabaseHelper.TABLE_APPOINTMENTS + " a ON a." +
        TechFixDatabaseHelper.ID + "=h." +
        TechFixDatabaseHelper.APPOINTMENT_ID + " JOIN " +
        TechFixDatabaseHelper.TABLE_REPAIR_SERVICES + " s ON s." +
        TechFixDatabaseHelper.ID + "=a." + TechFixDatabaseHelper.SERVICE_ID +
        " LEFT JOIN " + TechFixDatabaseHelper.TABLE_BRANCHES + " b ON b." +
        TechFixDatabaseHelper.ID + "=a." + TechFixDatabaseHelper.BRANCH_ID +
        " WHERE h." + TechFixDatabaseHelper.IMAGE_PATH + " IS NOT NULL" +
        branchWhere(branch, "b") + " ORDER BY h." +
        TechFixDatabaseHelper.RECORDED_AT + " DESC";
    try (Cursor cursor =
             helper.getReadableDatabase().rawQuery(sql, branchArgs(branch))) {
      while (cursor.moveToNext()) {
        boolean featured =
            cursor.getString(1).toLowerCase(Locale.US).contains("featured");
        records.add(new ManagementRecord(
            cursor.getLong(0), "#TF-" + (1000 + cursor.getLong(4)),
            featured ? "FEATURED" : "PUBLISHED",
            cursor.getString(5) + " · " + cursor.getString(6),
            cursor.getString(7), "Added " + formatDate(cursor.getLong(3)),
            "FEATURE", cursor.getString(7), cursor.getString(2)));
      }
    }
    return records;
  }

  private List<ManagementRecord> getPayments(String branch) {
    List<ManagementRecord> records = new ArrayList<>();
    String sql =
        "SELECT p." + TechFixDatabaseHelper.ID + ",p." +
        TechFixDatabaseHelper.STATUS + ",p." +
        TechFixDatabaseHelper.AMOUNT_CENTS + ",p." +
        TechFixDatabaseHelper.METHOD + ",p." +
        TechFixDatabaseHelper.CREATED_AT + ",a." + TechFixDatabaseHelper.ID +
        ",COALESCE(b." + TechFixDatabaseHelper.NAME + ",'Unassigned') FROM " +
        TechFixDatabaseHelper.TABLE_PAYMENTS + " p JOIN " +
        TechFixDatabaseHelper.TABLE_APPOINTMENTS + " a ON a." +
        TechFixDatabaseHelper.ID + "=p." +
        TechFixDatabaseHelper.APPOINTMENT_ID + " LEFT JOIN " +
        TechFixDatabaseHelper.TABLE_BRANCHES + " b ON b." +
        TechFixDatabaseHelper.ID + "=a." + TechFixDatabaseHelper.BRANCH_ID +
        " WHERE 1=1" + branchWhere(branch, "b") + " ORDER BY p." +
        TechFixDatabaseHelper.CREATED_AT + " DESC";
    try (Cursor cursor =
             helper.getReadableDatabase().rawQuery(sql, branchArgs(branch))) {
      while (cursor.moveToNext()) {
        String status = cursor.getString(1);
        String branchName = cursor.getString(6);
        records.add(new ManagementRecord(
            cursor.getLong(0), "#PAY-" + cursor.getLong(0), status,
            formatPrice(cursor.getLong(2)) + " · #TF-" +
                (1000 + cursor.getLong(5)),
            statusLabel(cursor.getString(3)) + " · " + branchName,
            "Created " + formatDate(cursor.getLong(4)),
            "PAID".equals(status) ? "RECEIPT" : "MARK PAID", branchName,
            null));
      }
    }
    return records;
  }

  private List<String> getRecentActivity() {
    List<String> activity = new ArrayList<>();
    String sql = "SELECT h." + TechFixDatabaseHelper.STATUS + ",h." +
                 TechFixDatabaseHelper.NOTES + ",h." +
                 TechFixDatabaseHelper.RECORDED_AT + ",h." +
                 TechFixDatabaseHelper.APPOINTMENT_ID + " FROM " +
                 TechFixDatabaseHelper.TABLE_REPAIR_HISTORY + " h ORDER BY h." +
                 TechFixDatabaseHelper.RECORDED_AT + " DESC LIMIT 2";
    try (Cursor cursor = helper.getReadableDatabase().rawQuery(sql, null)) {
      while (cursor.moveToNext()) {
        activity.add("#TF-" + (1000 + cursor.getLong(3)) + " · " +
                     statusLabel(cursor.getString(0)) + "\n" +
                     cursor.getString(1) + " · " +
                     formatDate(cursor.getLong(2)));
      }
    }
    return activity;
  }

  private AppointmentStatus currentStatus(long appointmentId) {
    try (Cursor cursor = helper.getReadableDatabase().query(
             TechFixDatabaseHelper.TABLE_APPOINTMENTS,
             new String[] {TechFixDatabaseHelper.STATUS},
             TechFixDatabaseHelper.ID + "=?",
             new String[] {String.valueOf(appointmentId)}, null, null, null,
             "1")) {
      if (!cursor.moveToFirst())
        throw new IllegalArgumentException("Appointment not found.");
      return AppointmentStatus.valueOf(cursor.getString(0));
    }
  }

  private long paidTotal(String branch) {
    String sql = "SELECT COALESCE(SUM(p." + TechFixDatabaseHelper.AMOUNT_CENTS +
                 "),0) FROM " + TechFixDatabaseHelper.TABLE_PAYMENTS +
                 " p JOIN " + TechFixDatabaseHelper.TABLE_APPOINTMENTS +
                 " a ON a." + TechFixDatabaseHelper.ID + "=p." +
                 TechFixDatabaseHelper.APPOINTMENT_ID + " LEFT JOIN " +
                 TechFixDatabaseHelper.TABLE_BRANCHES + " b ON b." +
                 TechFixDatabaseHelper.ID + "=a." +
                 TechFixDatabaseHelper.BRANCH_ID + " WHERE p." +
                 TechFixDatabaseHelper.STATUS + "='PAID'" +
                 branchWhere(branch, "b");
    return queryLong(sql, branchArgs(branch));
  }

  private String branchWhere(String branch, String alias) {
    return branch == null || "All".equals(branch)
        ? ""
        : " AND " + alias + "." + TechFixDatabaseHelper.NAME + "=?";
  }

  private String[] branchArgs(String branch) {
    return branch == null || "All".equals(branch) ? null
                                                    : new String[] {branch};
  }

  private long queryLong(String sql, String[] arguments) {
    try (Cursor cursor =
             helper.getReadableDatabase().rawQuery(sql, arguments)) {
      return cursor.moveToFirst() ? cursor.getLong(0) : 0;
    }
  }

  private int updateById(String table, long id, ContentValues values) {
    int updated = helper.getWritableDatabase().update(
        table, values, TechFixDatabaseHelper.ID + "=?",
        new String[] {String.valueOf(id)});
    if (updated == 1)
      FirebaseSyncScheduler.enqueueNow(appContext);
    return updated;
  }

  private long insertAndSync(String table, ContentValues values) {
    long id = helper.getWritableDatabase().insertOrThrow(table, null, values);
    FirebaseSyncScheduler.enqueueNow(appContext);
    return id;
  }

  private long findBranchId(SQLiteDatabase database, String branchName) {
    String name = branchName == null || "All".equals(branchName) ? "Colombo"
                                                                   : branchName;
    try (Cursor cursor =
             database.query(TechFixDatabaseHelper.TABLE_BRANCHES,
                            new String[] {TechFixDatabaseHelper.ID},
                            TechFixDatabaseHelper.NAME + "=?",
                            new String[] {name}, null, null, null, "1")) {
      if (!cursor.moveToFirst())
        throw new IllegalArgumentException("Selected branch is unavailable.");
      return cursor.getLong(0);
    }
  }

  private long firstId(SQLiteDatabase database, String table) {
    try (Cursor cursor = database.query(
             table, new String[] {TechFixDatabaseHelper.ID}, null, null, null,
             null, TechFixDatabaseHelper.ID, "1")) {
      return cursor.moveToFirst() ? cursor.getLong(0) : -1;
    }
  }

  private String requireText(String value, String label) {
    if (value == null || value.trim().isEmpty())
      throw new IllegalArgumentException(label + " is required.");
    return value.trim();
  }

  private String initials(String value) {
    String[] words = value.trim().split("\\s+");
    StringBuilder initials = new StringBuilder();
    for (String word : words) {
      if (!word.isEmpty() && initials.length() < 2)
        initials.append(Character.toUpperCase(word.charAt(0)));
    }
    return initials.toString();
  }

  public static String formatPrice(long cents) {
    return "LKR " +
        NumberFormat.getIntegerInstance(Locale.US).format(cents / 100);
  }

  private String formatDateTime(long timestamp) {
    return new SimpleDateFormat("dd MMM · h:mm a", Locale.US)
        .format(new Date(timestamp));
  }

  private String formatDate(long timestamp) {
    return new SimpleDateFormat("dd MMM yyyy", Locale.US)
        .format(new Date(timestamp));
  }

  public static String statusLabel(AppointmentStatus status) {
    return statusLabel(status.name());
  }

  public static String statusLabel(String value) {
    String text = value.toLowerCase(Locale.US).replace('_', ' ');
    return Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }

  private String nullable(String value, String fallback) {
    return value == null || value.trim().isEmpty() ? fallback : value;
  }

  @Override
  public void close() {
    helper.close();
  }

  public static final class ManagementRecord {
    public final long id;
    public final String code;
    public final String status;
    public final String title;
    public final String meta;
    public final String detail;
    public final String action;
    public final String branch;
    public final String imagePath;

    ManagementRecord(long id, String code, String status, String title,
                     String meta, String detail, String action, String branch,
                     String imagePath) {
      this.id = id;
      this.code = code;
      this.status = status;
      this.title = title;
      this.meta = meta;
      this.detail = detail;
      this.action = action;
      this.branch = branch;
      this.imagePath = imagePath;
    }
  }

  public static final class ModuleSummary {
    public final String metric;
    public final String label;
    public final String trend;

    ModuleSummary(String metric, String label, String trend) {
      this.metric = metric;
      this.label = label;
      this.trend = trend;
    }
  }

  public static final class DashboardStats {
    public final long activeRepairs;
    public final long readyRepairs;
    public final long lowStockParts;
    public final long activeTechnicians;
    public final long paidCents;
    public final List<String> recentActivity;

    DashboardStats(long activeRepairs, long readyRepairs, long lowStockParts,
                   long activeTechnicians, long paidCents,
                   List<String> recentActivity) {
      this.activeRepairs = activeRepairs;
      this.readyRepairs = readyRepairs;
      this.lowStockParts = lowStockParts;
      this.activeTechnicians = activeTechnicians;
      this.paidCents = paidCents;
      this.recentActivity = recentActivity;
    }
  }

  public static final class AppointmentChoice {
    public final long id;
    public final String label;
    public final AppointmentStatus status;

    AppointmentChoice(long id, String label, AppointmentStatus status) {
      this.id = id;
      this.label = label;
      this.status = status;
    }
  }
}
