package com.example.techfix;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.techfix.data.model.AppointmentStatus;
import com.example.techfix.data.model.PaymentMethod;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class CustomerRepository implements AutoCloseable {
  private static final String ACTIVE_STATUSES =
      "('PENDING','ASSIGNED','IN_PROGRESS','WAITING_FOR_PARTS','READY_FOR_"
      + "PAYMENT')";

  private final Context appContext;
  private final TechFixDatabaseHelper helper;

  public CustomerRepository(Context context) {
    appContext = context.getApplicationContext();
    helper = new TechFixDatabaseHelper(appContext);
    helper.getWritableDatabase();
  }

  public List<ServiceItem> getServices(String query) {
    List<ServiceItem> services = new ArrayList<>();
    String normalized = query == null ? "" : query.trim();
    String selection = "s." + TechFixDatabaseHelper.ACTIVE + " = 1";
    List<String> arguments = new ArrayList<>();
    if (!normalized.isEmpty()) {
      selection += " AND (s." + TechFixDatabaseHelper.NAME + " LIKE ? OR c." +
                   TechFixDatabaseHelper.NAME + " LIKE ?)";
      arguments.add("%" + normalized + "%");
      arguments.add("%" + normalized + "%");
    }

    String sql = "SELECT s." + TechFixDatabaseHelper.ID + ", s." +
                 TechFixDatabaseHelper.CATEGORY_ID + ", s." +
                 TechFixDatabaseHelper.NAME + " service_name, s." +
                 TechFixDatabaseHelper.DESCRIPTION + ", s." +
                 TechFixDatabaseHelper.BASE_PRICE_CENTS + ", s." +
                 TechFixDatabaseHelper.ESTIMATED_MINUTES + ", c." +
                 TechFixDatabaseHelper.NAME + " category_name FROM " +
                 TechFixDatabaseHelper.TABLE_REPAIR_SERVICES + " s JOIN " +
                 TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES + " c ON c." +
                 TechFixDatabaseHelper.ID + " = s." +
                 TechFixDatabaseHelper.CATEGORY_ID + " WHERE " + selection +
                 " ORDER BY s." + TechFixDatabaseHelper.ID;

    try (Cursor cursor = helper.getReadableDatabase().rawQuery(
             sql, arguments.toArray(new String[0]))) {
      while (cursor.moveToNext()) {
        services.add(mapService(cursor));
      }
    }
    return services;
  }

  public ServiceItem getService(long serviceId) {
    String sql = "SELECT s." + TechFixDatabaseHelper.ID + ", s." +
                 TechFixDatabaseHelper.CATEGORY_ID + ", s." +
                 TechFixDatabaseHelper.NAME + " service_name, s." +
                 TechFixDatabaseHelper.DESCRIPTION + ", s." +
                 TechFixDatabaseHelper.BASE_PRICE_CENTS + ", s." +
                 TechFixDatabaseHelper.ESTIMATED_MINUTES + ", c." +
                 TechFixDatabaseHelper.NAME + " category_name FROM " +
                 TechFixDatabaseHelper.TABLE_REPAIR_SERVICES + " s JOIN " +
                 TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES + " c ON c." +
                 TechFixDatabaseHelper.ID + " = s." +
                 TechFixDatabaseHelper.CATEGORY_ID + " WHERE s." +
                 TechFixDatabaseHelper.ID + " = ? LIMIT 1";
    try (Cursor cursor = helper.getReadableDatabase().rawQuery(
             sql, new String[] {String.valueOf(serviceId)})) {
      return cursor.moveToFirst() ? mapService(cursor) : null;
    }
  }

  public List<BranchItem> getBranches() {
    List<BranchItem> branches = new ArrayList<>();
    try (Cursor cursor = helper.getReadableDatabase().query(
             TechFixDatabaseHelper.TABLE_BRANCHES,
             new String[] {TechFixDatabaseHelper.ID, TechFixDatabaseHelper.NAME,
                           TechFixDatabaseHelper.ADDRESS,
                           TechFixDatabaseHelper.PHONE},
             TechFixDatabaseHelper.ACTIVE + " = 1", null, null, null,
             TechFixDatabaseHelper.NAME)) {
      while (cursor.moveToNext()) {
        branches.add(new BranchItem(cursor.getLong(0), cursor.getString(1),
                                    cursor.getString(2), cursor.getString(3)));
      }
    }
    return branches;
  }

  public long createAppointment(long userId, long serviceId,
                                String deviceDetails, String problemDescription,
                                long appointmentAt, String imagePath,
                                double latitude, double longitude) {
    if (userId <= 0 || serviceId <= 0) {
      throw new IllegalArgumentException("User and service are required.");
    }
    if (deviceDetails == null || deviceDetails.trim().isEmpty()) {
      throw new IllegalArgumentException("Device details are required.");
    }
    if (problemDescription == null || problemDescription.trim().isEmpty()) {
      throw new IllegalArgumentException("Problem description is required.");
    }
    if (appointmentAt <= System.currentTimeMillis()) {
      throw new IllegalArgumentException("Choose a future appointment time.");
    }

    SQLiteDatabase database = helper.getWritableDatabase();
    long appointmentId;
    database.beginTransaction();
    try {
      AssignmentOption assignment = findBestAssignment(
          database, serviceId, latitude, longitude, appointmentAt);
      reserveSparePart(database, assignment.sparePartId);
      AppointmentStatus status = AppointmentStatus.ASSIGNED;
      long now = System.currentTimeMillis();
      ContentValues values = new ContentValues();
      values.put(TechFixDatabaseHelper.USER_ID, userId);
      values.put(TechFixDatabaseHelper.BRANCH_ID, assignment.branchId);
      values.put(TechFixDatabaseHelper.TECHNICIAN_ID, assignment.technicianId);
      values.put(TechFixDatabaseHelper.SERVICE_ID, serviceId);
      values.put(TechFixDatabaseHelper.DEVICE_DETAILS, deviceDetails.trim());
      values.put(TechFixDatabaseHelper.PROBLEM_DESCRIPTION,
                 problemDescription.trim());
      values.put(TechFixDatabaseHelper.STATUS, status.name());
      values.put(TechFixDatabaseHelper.APPOINTMENT_AT, appointmentAt);
      values.put(TechFixDatabaseHelper.CREATED_AT, now);
      appointmentId = database.insertOrThrow(
          TechFixDatabaseHelper.TABLE_APPOINTMENTS, null, values);

      ContentValues history = new ContentValues();
      history.put(TechFixDatabaseHelper.APPOINTMENT_ID, appointmentId);
      history.put(TechFixDatabaseHelper.STATUS, status.name());
      history.put(TechFixDatabaseHelper.NOTES,
                  "Assigned to " + assignment.technicianName + " at " +
                      assignment.branchName +
                      (" based on distance, technician availability, and "
                       + "parts stock."));
      if (imagePath != null && !imagePath.trim().isEmpty()) {
        history.put(TechFixDatabaseHelper.IMAGE_PATH, imagePath.trim());
      }
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

  public AssignmentOption findBestAssignment(long serviceId, double latitude,
                                             double longitude,
                                             long appointmentAt) {
    return findBestAssignment(helper.getReadableDatabase(), serviceId, latitude,
                              longitude, appointmentAt);
  }

  public AppointmentItem getLatestActiveAppointment(long userId) {
    List<AppointmentItem> appointments = queryAppointments(
        "a." + TechFixDatabaseHelper.USER_ID + " = ? AND a." +
            TechFixDatabaseHelper.STATUS + " IN " + ACTIVE_STATUSES,
        new String[] {String.valueOf(userId)},
        "a." + TechFixDatabaseHelper.CREATED_AT + " DESC", "1");
    return appointments.isEmpty() ? null : appointments.get(0);
  }

  public AppointmentItem getAppointment(long userId, long appointmentId) {
    List<AppointmentItem> appointments = queryAppointments(
        "a." + TechFixDatabaseHelper.USER_ID + " = ? AND a." +
            TechFixDatabaseHelper.ID + " = ?",
        new String[] {String.valueOf(userId), String.valueOf(appointmentId)},
        null, "1");
    return appointments.isEmpty() ? null : appointments.get(0);
  }

  public List<AppointmentItem> getAppointments(long userId) {
    return queryAppointments("a." + TechFixDatabaseHelper.USER_ID + " = ?",
                             new String[] {String.valueOf(userId)},
                             "a." + TechFixDatabaseHelper.CREATED_AT + " DESC",
                             null);
  }

  public int countActiveAppointments(long userId) {
    return countAppointments(userId, " IN " + ACTIVE_STATUSES);
  }

  public int countCompletedAppointments(long userId) {
    return countAppointments(userId, " = 'COMPLETED'");
  }

  public int countDistinctDevices(long userId) {
    String sql = "SELECT COUNT(DISTINCT " +
                 TechFixDatabaseHelper.DEVICE_DETAILS + ") FROM " +
                 TechFixDatabaseHelper.TABLE_APPOINTMENTS + " WHERE " +
                 TechFixDatabaseHelper.USER_ID + " = ?";
    try (Cursor cursor = helper.getReadableDatabase().rawQuery(
             sql, new String[] {String.valueOf(userId)})) {
      return cursor.moveToFirst() ? cursor.getInt(0) : 0;
    }
  }

  public List<HistoryItem> getRepairHistory(long appointmentId) {
    List<HistoryItem> history = new ArrayList<>();
    try (Cursor cursor = helper.getReadableDatabase().query(
             TechFixDatabaseHelper.TABLE_REPAIR_HISTORY, null,
             TechFixDatabaseHelper.APPOINTMENT_ID + " = ?",
             new String[] {String.valueOf(appointmentId)}, null, null,
             TechFixDatabaseHelper.RECORDED_AT + " ASC")) {
      while (cursor.moveToNext()) {
        history.add(new HistoryItem(
            AppointmentStatus.valueOf(cursor.getString(
                cursor.getColumnIndexOrThrow(TechFixDatabaseHelper.STATUS))),
            cursor.getString(
                cursor.getColumnIndexOrThrow(TechFixDatabaseHelper.NOTES)),
            cursor.getLong(cursor.getColumnIndexOrThrow(
                TechFixDatabaseHelper.RECORDED_AT))));
      }
    }
    return history;
  }

  public PaymentItem getPayment(long userId, long appointmentId) {
    String sql =
        "SELECT p." + TechFixDatabaseHelper.ID + ",p." +
        TechFixDatabaseHelper.AMOUNT_CENTS + ",p." +
        TechFixDatabaseHelper.METHOD + ",p." + TechFixDatabaseHelper.STATUS +
        ",p." + TechFixDatabaseHelper.REFERENCE + ",p." +
        TechFixDatabaseHelper.PAID_AT + " FROM " +
        TechFixDatabaseHelper.TABLE_PAYMENTS + " p JOIN " +
        TechFixDatabaseHelper.TABLE_APPOINTMENTS + " a ON a." +
        TechFixDatabaseHelper.ID + "=p." +
        TechFixDatabaseHelper.APPOINTMENT_ID + " WHERE a." +
        TechFixDatabaseHelper.USER_ID + "=? AND a." + TechFixDatabaseHelper.ID +
        "=? ORDER BY p." + TechFixDatabaseHelper.CREATED_AT + " DESC LIMIT 1";
    try (Cursor cursor = helper.getReadableDatabase().rawQuery(
             sql, new String[] {String.valueOf(userId),
                                String.valueOf(appointmentId)})) {
      return cursor.moveToFirst() ? mapPayment(cursor) : null;
    }
  }

  public PaymentItem processPayment(long userId, long appointmentId,
                                    PaymentMethod method) {
    if (method == null || method == PaymentMethod.CASH)
      throw new IllegalArgumentException(
          "Choose card, bank transfer, or online payment.");
    SQLiteDatabase database = helper.getWritableDatabase();
    database.beginTransaction();
    PaymentItem result;
    try {
      String appointmentSql =
          "SELECT a." + TechFixDatabaseHelper.STATUS + ",s." +
          TechFixDatabaseHelper.BASE_PRICE_CENTS + " FROM " +
          TechFixDatabaseHelper.TABLE_APPOINTMENTS + " a JOIN " +
          TechFixDatabaseHelper.TABLE_REPAIR_SERVICES + " s ON s." +
          TechFixDatabaseHelper.ID + "=a." + TechFixDatabaseHelper.SERVICE_ID +
          " WHERE a." + TechFixDatabaseHelper.ID + "=? AND a." +
          TechFixDatabaseHelper.USER_ID + "=?";
      String appointmentStatus;
      long amountCents;
      try (Cursor appointment = database.rawQuery(
               appointmentSql, new String[] {String.valueOf(appointmentId),
                                             String.valueOf(userId)})) {
        if (!appointment.moveToFirst())
          throw new IllegalArgumentException("Repair appointment not found.");
        appointmentStatus = appointment.getString(0);
        amountCents = appointment.getLong(1);
      }
      if (!AppointmentStatus.READY_FOR_PAYMENT.name().equals(appointmentStatus))
        throw new IllegalArgumentException(
            "Payment becomes available when the repair is ready.");

      String reference =
          "TFX-" + appointmentId + "-" + System.currentTimeMillis();
      long paidAt = System.currentTimeMillis();
      ContentValues values = new ContentValues();
      values.put(TechFixDatabaseHelper.APPOINTMENT_ID, appointmentId);
      values.put(TechFixDatabaseHelper.AMOUNT_CENTS, amountCents);
      values.put(TechFixDatabaseHelper.METHOD, method.name());
      values.put(TechFixDatabaseHelper.STATUS, "PAID");
      values.put(TechFixDatabaseHelper.REFERENCE, reference);
      values.put(TechFixDatabaseHelper.PAID_AT, paidAt);
      values.put(TechFixDatabaseHelper.CREATED_AT, paidAt);

      long paymentId;
      try (Cursor existing = database.query(
               TechFixDatabaseHelper.TABLE_PAYMENTS,
               new String[] {TechFixDatabaseHelper.ID,
                             TechFixDatabaseHelper.STATUS},
               TechFixDatabaseHelper.APPOINTMENT_ID + "=?",
               new String[] {String.valueOf(appointmentId)}, null, null,
               TechFixDatabaseHelper.CREATED_AT + " DESC", "1")) {
        if (existing.moveToFirst()) {
          paymentId = existing.getLong(0);
          if ("PAID".equals(existing.getString(1))) {
            database.setTransactionSuccessful();
            return getPayment(userId, appointmentId);
          }
          database.update(TechFixDatabaseHelper.TABLE_PAYMENTS, values,
                          TechFixDatabaseHelper.ID + "=?",
                          new String[] {String.valueOf(paymentId)});
        } else {
          paymentId = database.insertOrThrow(
              TechFixDatabaseHelper.TABLE_PAYMENTS, null, values);
        }
      }

      ContentValues history = new ContentValues();
      history.put(TechFixDatabaseHelper.APPOINTMENT_ID, appointmentId);
      history.put(TechFixDatabaseHelper.STATUS,
                  AppointmentStatus.READY_FOR_PAYMENT.name());
      history.put(TechFixDatabaseHelper.NOTES,
                  "Payment received via " +
                      method.name().toLowerCase(Locale.US).replace('_', ' ') +
                      ".");
      history.putNull(TechFixDatabaseHelper.IMAGE_PATH);
      history.put(TechFixDatabaseHelper.RECORDED_AT, paidAt);
      database.insertOrThrow(TechFixDatabaseHelper.TABLE_REPAIR_HISTORY, null,
                             history);
      result = new PaymentItem(paymentId, amountCents, method.name(), "PAID",
                               reference, paidAt);
      database.setTransactionSuccessful();
    } finally {
      database.endTransaction();
    }
    FirebaseSyncScheduler.enqueueNow(appContext);
    return result;
  }

  public List<GalleryItem> getFeaturedRepairImages() {
    List<GalleryItem> images = new ArrayList<>();
    String sql =
        "SELECT h." + TechFixDatabaseHelper.ID + ",h." +
        TechFixDatabaseHelper.IMAGE_PATH + ",a." +
        TechFixDatabaseHelper.DEVICE_DETAILS + ",s." +
        TechFixDatabaseHelper.NAME + ",COALESCE(b." +
        TechFixDatabaseHelper.NAME + ",'TechFix'),h." +
        TechFixDatabaseHelper.RECORDED_AT + " FROM " +
        TechFixDatabaseHelper.TABLE_REPAIR_HISTORY + " h JOIN " +
        TechFixDatabaseHelper.TABLE_APPOINTMENTS + " a ON a." +
        TechFixDatabaseHelper.ID + "=h." +
        TechFixDatabaseHelper.APPOINTMENT_ID + " JOIN " +
        TechFixDatabaseHelper.TABLE_REPAIR_SERVICES + " s ON s." +
        TechFixDatabaseHelper.ID + "=a." + TechFixDatabaseHelper.SERVICE_ID +
        " LEFT JOIN " + TechFixDatabaseHelper.TABLE_BRANCHES + " b ON b." +
        TechFixDatabaseHelper.ID + "=a." + TechFixDatabaseHelper.BRANCH_ID +
        " WHERE h." + TechFixDatabaseHelper.IMAGE_PATH +
        " IS NOT NULL AND LOWER(h." + TechFixDatabaseHelper.NOTES +
        ") LIKE '%featured%' ORDER BY h." +
        TechFixDatabaseHelper.RECORDED_AT + " DESC";
    try (Cursor cursor = helper.getReadableDatabase().rawQuery(sql, null)) {
      while (cursor.moveToNext()) {
        images.add(new GalleryItem(cursor.getLong(0), cursor.getString(1),
                                   cursor.getString(2), cursor.getString(3),
                                   cursor.getString(4), cursor.getLong(5)));
      }
    }
    return images;
  }

  private PaymentItem mapPayment(Cursor cursor) {
    return new PaymentItem(cursor.getLong(0), cursor.getLong(1),
                           cursor.getString(2), cursor.getString(3),
                           cursor.getString(4),
                           cursor.isNull(5) ? null : cursor.getLong(5));
  }

  private List<AppointmentItem> queryAppointments(String selection,
                                                  String[] args, String orderBy,
                                                  String limit) {
    List<AppointmentItem> appointments = new ArrayList<>();
    String sql =
        "SELECT a." + TechFixDatabaseHelper.ID + ", a." +
        TechFixDatabaseHelper.DEVICE_DETAILS + ", a." +
        TechFixDatabaseHelper.PROBLEM_DESCRIPTION + ", a." +
        TechFixDatabaseHelper.STATUS + ", a." +
        TechFixDatabaseHelper.APPOINTMENT_AT + ", a." +
        TechFixDatabaseHelper.CREATED_AT + ", s." + TechFixDatabaseHelper.NAME +
        " service_name, s." + TechFixDatabaseHelper.BASE_PRICE_CENTS +
        ", b." + TechFixDatabaseHelper.NAME + " branch_name, b." +
        TechFixDatabaseHelper.ADDRESS + " branch_address, b." +
        TechFixDatabaseHelper.PHONE + " branch_phone, t." +
        TechFixDatabaseHelper.FULL_NAME + " technician_name, t." +
        TechFixDatabaseHelper.PHONE + " technician_phone FROM " +
        TechFixDatabaseHelper.TABLE_APPOINTMENTS + " a JOIN " +
        TechFixDatabaseHelper.TABLE_REPAIR_SERVICES + " s ON s." +
        TechFixDatabaseHelper.ID + " = a." + TechFixDatabaseHelper.SERVICE_ID +
        " LEFT JOIN " + TechFixDatabaseHelper.TABLE_BRANCHES + " b ON b." +
        TechFixDatabaseHelper.ID + " = a." + TechFixDatabaseHelper.BRANCH_ID +
        " LEFT JOIN " + TechFixDatabaseHelper.TABLE_TECHNICIANS + " t ON t." +
        TechFixDatabaseHelper.ID + " = a." +
        TechFixDatabaseHelper.TECHNICIAN_ID + " WHERE " + selection +
        (orderBy == null ? "" : " ORDER BY " + orderBy) +
        (limit == null ? "" : " LIMIT " + limit);
    try (Cursor cursor = helper.getReadableDatabase().rawQuery(sql, args)) {
      while (cursor.moveToNext())
        appointments.add(mapAppointment(cursor));
    }
    return appointments;
  }

  private int countAppointments(long userId, String statusClause) {
    String sql = "SELECT COUNT(*) FROM " +
                 TechFixDatabaseHelper.TABLE_APPOINTMENTS + " WHERE " +
                 TechFixDatabaseHelper.USER_ID + " = ? AND " +
                 TechFixDatabaseHelper.STATUS + statusClause;
    try (Cursor cursor = helper.getReadableDatabase().rawQuery(
             sql, new String[] {String.valueOf(userId)})) {
      return cursor.moveToFirst() ? cursor.getInt(0) : 0;
    }
  }

  private AssignmentOption findBestAssignment(SQLiteDatabase database,
                                              long serviceId, double latitude,
                                              double longitude,
                                              long appointmentAt) {
    ServiceCapacity service = getServiceCapacity(database, serviceId);
    if (service == null)
      throw new IllegalArgumentException("Selected service is unavailable.");

    AssignmentOption best = null;
    try (Cursor branches = database.query(
             TechFixDatabaseHelper.TABLE_BRANCHES,
             new String[] {TechFixDatabaseHelper.ID, TechFixDatabaseHelper.NAME,
                           TechFixDatabaseHelper.ADDRESS,
                           TechFixDatabaseHelper.LATITUDE,
                           TechFixDatabaseHelper.LONGITUDE},
             TechFixDatabaseHelper.ACTIVE + " = 1", null, null, null, null)) {
      while (branches.moveToNext()) {
        long branchId = branches.getLong(0);
        TechnicianSlot technician =
            findAvailableTechnician(database, branchId, service.categoryName,
                                    appointmentAt, service.estimatedMinutes);
        if (technician == null)
          continue;
        String partKeyword = requiredPartKeyword(service.serviceName);
        Long sparePartId =
            partKeyword == null
                ? null
                : findRequiredPart(database, branchId, service.categoryId,
                                   partKeyword);
        if (partKeyword != null && sparePartId == null)
          continue;
        double distance = distanceKm(latitude, longitude, branches.getDouble(3),
                                     branches.getDouble(4));
        AssignmentOption option = new AssignmentOption(
            branchId, branches.getString(1), branches.getString(2),
            technician.id, technician.name, sparePartId, distance);
        if (best == null || option.distanceKm < best.distanceKm)
          best = option;
      }
    }
    if (best == null) {
      throw new IllegalArgumentException(
          "No branch currently has both an available technician and the "
          + "required parts.");
    }
    return best;
  }

  private ServiceCapacity getServiceCapacity(SQLiteDatabase database,
                                             long serviceId) {
    String sql = "SELECT s." + TechFixDatabaseHelper.CATEGORY_ID + ", s." +
                 TechFixDatabaseHelper.NAME + ", s." +
                 TechFixDatabaseHelper.ESTIMATED_MINUTES + ", c." +
                 TechFixDatabaseHelper.NAME + " FROM " +
                 TechFixDatabaseHelper.TABLE_REPAIR_SERVICES + " s JOIN " +
                 TechFixDatabaseHelper.TABLE_DEVICE_CATEGORIES + " c ON c." +
                 TechFixDatabaseHelper.ID + " = s." +
                 TechFixDatabaseHelper.CATEGORY_ID + " WHERE s." +
                 TechFixDatabaseHelper.ID + " = ? AND s." +
                 TechFixDatabaseHelper.ACTIVE + " = 1";
    try (Cursor cursor =
             database.rawQuery(sql, new String[] {String.valueOf(serviceId)})) {
      return cursor.moveToFirst()
          ? new ServiceCapacity(cursor.getLong(0), cursor.getString(1),
                                cursor.getInt(2), cursor.getString(3))
          : null;
    }
  }

  private TechnicianSlot findAvailableTechnician(SQLiteDatabase database,
                                                 long branchId,
                                                 String categoryName,
                                                 long appointmentAt,
                                                 int estimatedMinutes) {
    String sql = "SELECT t." + TechFixDatabaseHelper.ID + ", t." +
                 TechFixDatabaseHelper.FULL_NAME + " FROM " +
                 TechFixDatabaseHelper.TABLE_TECHNICIANS + " t WHERE t." +
                 TechFixDatabaseHelper.BRANCH_ID + " = ? AND t." +
                 TechFixDatabaseHelper.ACTIVE + " = 1 AND LOWER(t." +
                 TechFixDatabaseHelper.SPECIALTY +
                 ") LIKE ? AND NOT EXISTS (SELECT 1 FROM " +
                 TechFixDatabaseHelper.TABLE_APPOINTMENTS + " a WHERE a." +
                 TechFixDatabaseHelper.TECHNICIAN_ID + " = t." +
                 TechFixDatabaseHelper.ID + " AND a." +
                 TechFixDatabaseHelper.STATUS + " IN " + ACTIVE_STATUSES +
                 " AND ABS(a." + TechFixDatabaseHelper.APPOINTMENT_AT +
                 " - ?) < ?) ORDER BY t." + TechFixDatabaseHelper.ID +
                 " LIMIT 1";
    long conflictWindow = Math.max(60, estimatedMinutes + 30) * 60_000L;
    try (Cursor cursor = database.rawQuery(
             sql, new String[] {String.valueOf(branchId),
                                "%" + categoryName.toLowerCase(Locale.US) + "%",
                                String.valueOf(appointmentAt),
                                String.valueOf(conflictWindow)})) {
      return cursor.moveToFirst()
          ? new TechnicianSlot(cursor.getLong(0), cursor.getString(1))
          : null;
    }
  }

  private Long findRequiredPart(SQLiteDatabase database, long branchId,
                                long categoryId, String keyword) {
    String selection = TechFixDatabaseHelper.BRANCH_ID + " = ? AND " +
                       TechFixDatabaseHelper.CATEGORY_ID + " = ? AND " +
                       TechFixDatabaseHelper.ACTIVE + " = 1 AND " +
                       TechFixDatabaseHelper.QUANTITY_AVAILABLE +
                       " > 0 AND LOWER(" + TechFixDatabaseHelper.NAME +
                       ") LIKE ?";
    try (Cursor cursor = database.query(
             TechFixDatabaseHelper.TABLE_SPARE_PARTS,
             new String[] {TechFixDatabaseHelper.ID}, selection,
             new String[] {String.valueOf(branchId), String.valueOf(categoryId),
                           "%" + keyword + "%"},
             null, null, null, "1")) {
      return cursor.moveToFirst() ? cursor.getLong(0) : null;
    }
  }

  private void reserveSparePart(SQLiteDatabase database, Long sparePartId) {
    if (sparePartId == null)
      return;
    int quantity;
    try (Cursor cursor = database.query(
             TechFixDatabaseHelper.TABLE_SPARE_PARTS,
             new String[] {TechFixDatabaseHelper.QUANTITY_AVAILABLE},
             TechFixDatabaseHelper.ID + " = ?",
             new String[] {String.valueOf(sparePartId)}, null, null, null,
             "1")) {
      if (!cursor.moveToFirst() || cursor.getInt(0) <= 0) {
        throw new IllegalArgumentException(
            "The required spare part is no longer available.");
      }
      quantity = cursor.getInt(0);
    }
    ContentValues values = new ContentValues();
    values.put(TechFixDatabaseHelper.QUANTITY_AVAILABLE, quantity - 1);
    int updated = database.update(
        TechFixDatabaseHelper.TABLE_SPARE_PARTS, values,
        TechFixDatabaseHelper.ID + " = ? AND " +
            TechFixDatabaseHelper.QUANTITY_AVAILABLE + " = ?",
        new String[] {String.valueOf(sparePartId), String.valueOf(quantity)});
    if (updated != 1) {
      throw new IllegalArgumentException(
          "The required spare part was reserved by another request.");
    }
  }

  private String requiredPartKeyword(String serviceName) {
    String name = serviceName.toLowerCase(Locale.US);
    if (name.contains("screen"))
      return "display";
    if (name.contains("battery"))
      return "battery";
    if (name.contains("keyboard"))
      return "keyboard";
    if (name.contains("charging"))
      return "charging";
    return null;
  }

  public static double distanceKm(double fromLatitude, double fromLongitude,
                                  double toLatitude, double toLongitude) {
    double earthRadiusKm = 6371.0088;
    double latitudeDelta = Math.toRadians(toLatitude - fromLatitude);
    double longitudeDelta = Math.toRadians(toLongitude - fromLongitude);
    double startLatitude = Math.toRadians(fromLatitude);
    double endLatitude = Math.toRadians(toLatitude);
    double haversine =
        Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2) +
        Math.cos(startLatitude) * Math.cos(endLatitude) *
            Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
    return earthRadiusKm * 2 *
        Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
  }

  private ServiceItem mapService(Cursor cursor) {
    return new ServiceItem(cursor.getLong(0), cursor.getLong(1),
                           cursor.getString(2), cursor.getString(3),
                           cursor.getLong(4), cursor.getInt(5),
                           cursor.getString(6));
  }

  private AppointmentItem mapAppointment(Cursor cursor) {
    return new AppointmentItem(
        cursor.getLong(0), cursor.getString(1), cursor.getString(2),
        AppointmentStatus.valueOf(cursor.getString(3)), cursor.getLong(4),
        cursor.getLong(5), cursor.getString(6), cursor.getLong(7),
        cursor.getString(8), cursor.getString(9), cursor.getString(10),
        cursor.getString(11), cursor.getString(12));
  }

  public static String formatPrice(long cents) {
    return "LKR " +
        NumberFormat.getIntegerInstance(Locale.US).format(cents / 100);
  }

  public static String formatDate(long timestamp) {
    return new SimpleDateFormat("dd MMM yyyy", Locale.US)
        .format(new Date(timestamp));
  }

  public static String formatDateTime(long timestamp) {
    return new SimpleDateFormat("dd MMM yyyy · h:mm a", Locale.US)
        .format(new Date(timestamp));
  }

  public static String statusLabel(AppointmentStatus status) {
    String lower = status.name().toLowerCase(Locale.US).replace('_', ' ');
    return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
  }

  @Override
  public void close() {
    helper.close();
  }

  private static final class ServiceCapacity {
    final long categoryId;
    final String serviceName;
    final int estimatedMinutes;
    final String categoryName;

    ServiceCapacity(long categoryId, String serviceName, int estimatedMinutes,
                    String categoryName) {
      this.categoryId = categoryId;
      this.serviceName = serviceName;
      this.estimatedMinutes = estimatedMinutes;
      this.categoryName = categoryName;
    }
  }

  private static final class TechnicianSlot {
    final long id;
    final String name;

    TechnicianSlot(long id, String name) {
      this.id = id;
      this.name = name;
    }
  }

  public static final class AssignmentOption {
    public final long branchId;
    public final String branchName;
    public final String branchAddress;
    public final long technicianId;
    public final String technicianName;
    public final Long sparePartId;
    public final double distanceKm;

    AssignmentOption(long branchId, String branchName, String branchAddress,
                     long technicianId, String technicianName, Long sparePartId,
                     double distanceKm) {
      this.branchId = branchId;
      this.branchName = branchName;
      this.branchAddress = branchAddress;
      this.technicianId = technicianId;
      this.technicianName = technicianName;
      this.sparePartId = sparePartId;
      this.distanceKm = distanceKm;
    }
  }

  public static final class ServiceItem {
    public final long id;
    public final long categoryId;
    public final String name;
    public final String description;
    public final long priceCents;
    public final int estimatedMinutes;
    public final String categoryName;

    ServiceItem(long id, long categoryId, String name, String description,
                long priceCents, int estimatedMinutes, String categoryName) {
      this.id = id;
      this.categoryId = categoryId;
      this.name = name;
      this.description = description;
      this.priceCents = priceCents;
      this.estimatedMinutes = estimatedMinutes;
      this.categoryName = categoryName;
    }
  }

  public static final class BranchItem {
    public final long id;
    public final String name;
    public final String address;
    public final String phone;

    BranchItem(long id, String name, String address, String phone) {
      this.id = id;
      this.name = name;
      this.address = address;
      this.phone = phone;
    }
  }

  public static final class AppointmentItem {
    public final long id;
    public final String deviceDetails;
    public final String problemDescription;
    public final AppointmentStatus status;
    public final long appointmentAt;
    public final long createdAt;
    public final String serviceName;
    public final long priceCents;
    public final String branchName;
    public final String branchAddress;
    public final String branchPhone;
    public final String technicianName;
    public final String technicianPhone;

    AppointmentItem(long id, String deviceDetails, String problemDescription,
                    AppointmentStatus status, long appointmentAt,
                    long createdAt, String serviceName, long priceCents,
                    String branchName, String branchAddress, String branchPhone,
                    String technicianName, String technicianPhone) {
      this.id = id;
      this.deviceDetails = deviceDetails;
      this.problemDescription = problemDescription;
      this.status = status;
      this.appointmentAt = appointmentAt;
      this.createdAt = createdAt;
      this.serviceName = serviceName;
      this.priceCents = priceCents;
      this.branchName = branchName;
      this.branchAddress = branchAddress;
      this.branchPhone = branchPhone;
      this.technicianName = technicianName;
      this.technicianPhone = technicianPhone;
    }
  }

  public static final class PaymentItem {
    public final long id;
    public final long amountCents;
    public final String method;
    public final String status;
    public final String reference;
    public final Long paidAt;

    PaymentItem(long id, long amountCents, String method, String status,
                String reference, Long paidAt) {
      this.id = id;
      this.amountCents = amountCents;
      this.method = method;
      this.status = status;
      this.reference = reference;
      this.paidAt = paidAt;
    }
  }

  public static final class GalleryItem {
    public final long id;
    public final String imagePath;
    public final String device;
    public final String service;
    public final String branch;
    public final long recordedAt;

    GalleryItem(long id, String imagePath, String device, String service,
                String branch, long recordedAt) {
      this.id = id;
      this.imagePath = imagePath;
      this.device = device;
      this.service = service;
      this.branch = branch;
      this.recordedAt = recordedAt;
    }
  }

  public static final class HistoryItem {
    public final AppointmentStatus status;
    public final String notes;
    public final long recordedAt;

    HistoryItem(AppointmentStatus status, String notes, long recordedAt) {
      this.status = status;
      this.notes = notes;
      this.recordedAt = recordedAt;
    }
  }
}
