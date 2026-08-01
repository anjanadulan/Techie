package com.example.techfix;

import android.os.Handler;
import android.os.Looper;
import com.example.techfix.data.model.AppointmentStatus;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.HashMap;
import java.util.Map;

public final class FirebaseManagementApi {
  private static final String REGION = "us-central1";

  private final FirebaseFunctions functions =
      FirebaseFunctions.getInstance(REGION);
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  public void autoAssignAppointment(String appointmentId, Callback callback) {
    Map<String, Object> data = new HashMap<>();
    data.put("appointmentId", requireId(appointmentId));
    call("autoAssignAppointment", data, callback);
  }

  public void reassignAppointment(String appointmentId, long technicianId,
                                  Callback callback) {
    Map<String, Object> data = new HashMap<>();
    data.put("appointmentId", requireId(appointmentId));
    data.put("technicianId", technicianId);
    call("reassignAppointment", data, callback);
  }

  public void updateRepairStatus(String appointmentId,
                                 AppointmentStatus status, String notes,
                                 Callback callback) {
    if (status == null)
      throw new IllegalArgumentException("Repair status is required.");
    Map<String, Object> data = new HashMap<>();
    data.put("appointmentId", requireId(appointmentId));
    data.put("status", status.name());
    data.put("notes", notes == null ? "" : notes.trim());
    call("updateRepairStatus", data, callback);
  }

  private void call(String functionName, Map<String, Object> data,
                    Callback callback) {
    if (callback == null)
      throw new IllegalArgumentException("A management callback is required.");
    functions.getHttpsCallable(functionName).call(data).addOnCompleteListener(
        task -> mainHandler.post(() -> {
          if (task.isSuccessful()) {
            callback.onSuccess();
            return;
          }
          Exception error = task.getException();
          callback.onFailure(
              error == null
                  ? new IllegalStateException(
                        "The Firebase management action failed.")
                  : error);
        }));
  }

  private String requireId(String appointmentId) {
    if (appointmentId == null || appointmentId.trim().isEmpty())
      throw new IllegalArgumentException(
          "This appointment has not synchronized with Firebase yet.");
    return appointmentId.trim();
  }

  public interface Callback {
    void onSuccess();

    void onFailure(Exception error);
  }
}
