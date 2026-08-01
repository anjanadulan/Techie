package com.example.techfix;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.example.techfix.data.model.AppointmentStatus;
import java.util.List;

public class RepairTrackingActivity extends CustomerScreen {
  public static final String EXTRA_APPOINTMENT_ID = "appointment_id";
  private CustomerRepository repository;
  private SessionManager sessionManager;

  public static void open(Activity activity, long appointmentId) {
    Intent intent = new Intent(activity, RepairTrackingActivity.class);
    intent.putExtra(EXTRA_APPOINTMENT_ID, appointmentId);
    activity.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    showCustomerLayout(R.layout.activity_repair_tracking);
    repository = new CustomerRepository(this);
    sessionManager = new SessionManager(this);
    findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    bindAppointment();
  }

  private void bindAppointment() {
    long appointmentId = getIntent().getLongExtra(EXTRA_APPOINTMENT_ID, -1);
    CustomerRepository.AppointmentItem appointment = appointmentId > 0
        ? repository.getAppointment(sessionManager.getUserId(), appointmentId)
        : repository.getLatestActiveAppointment(sessionManager.getUserId());
    if (appointment == null) {
      finish();
      return;
    }

    ((TextView) findViewById(R.id.tvTrackingId)).setText("Repair #TF-" + appointment.id);
    ((TextView) findViewById(R.id.tvTrackingStatus))
        .setText(CustomerRepository.statusLabel(appointment.status));
    ((TextView) findViewById(R.id.tvTrackingDevice)).setText(appointment.deviceDetails);
    ((TextView) findViewById(R.id.tvTrackingService)).setText(appointment.serviceName);
    ((TextView) findViewById(R.id.tvTrackingEstimate))
        .setText("Appointment · " + CustomerRepository.formatDateTime(appointment.appointmentAt));
    ((TextView) findViewById(R.id.tvTechnicianName))
        .setText(appointment.technicianName == null ? "Awaiting assignment"
                                                    : appointment.technicianName);
    ((TextView) findViewById(R.id.tvTechnicianMeta))
        .setText((appointment.technicianName == null ? "TechFix team" : "Assigned technician")
            + " · "
            + (appointment.branchName == null ? "Branch pending" : appointment.branchName));
    renderTimeline(appointment);
  }

  private void renderTimeline(CustomerRepository.AppointmentItem appointment) {
    String[] titles = {"Appointment submitted", "Technician assigned", "Repair in progress",
        "Ready for payment", "Repair completed"};
    int[] markerIds = {R.id.timelineMarker1, R.id.timelineMarker2, R.id.timelineMarker3,
        R.id.timelineMarker4, R.id.timelineMarker5};
    int[] titleIds = {R.id.timelineTitle1, R.id.timelineTitle2, R.id.timelineTitle3,
        R.id.timelineTitle4, R.id.timelineTitle5};
    int[] subtitleIds = {R.id.timelineSubtitle1, R.id.timelineSubtitle2, R.id.timelineSubtitle3,
        R.id.timelineSubtitle4, R.id.timelineSubtitle5};
    int currentStage = stageFor(appointment.status);
    List<CustomerRepository.HistoryItem> history = repository.getRepairHistory(appointment.id);
    String latestNote =
        history.isEmpty() ? appointment.problemDescription : history.get(history.size() - 1).notes;

    for (int index = 0; index < titles.length; index++) {
      TextView marker = findViewById(markerIds[index]);
      ((TextView) findViewById(titleIds[index])).setText(titles[index]);
      if (index <= currentStage) {
        marker.setBackgroundResource(R.drawable.bg_customer_chip_selected);
        marker.setText("✓");
        marker.setTextColor(getColor(R.color.white));
      } else {
        marker.setBackgroundResource(R.drawable.bg_customer_chip);
        marker.setText(String.valueOf(index + 1));
        marker.setTextColor(getColor(R.color.customer_muted));
      }
      ((TextView) findViewById(subtitleIds[index]))
          .setText(index == currentStage ? latestNote
                  : index < currentStage ? "Completed"
                                         : "Pending");
    }
  }

  private int stageFor(AppointmentStatus status) {
    switch (status) {
      case ASSIGNED:
        return 1;
      case IN_PROGRESS:
      case WAITING_FOR_PARTS:
        return 2;
      case READY_FOR_PAYMENT:
        return 3;
      case COMPLETED:
        return 4;
      case CANCELLED:
      case PENDING:
      default:
        return 0;
    }
  }

  @Override
  protected void onDestroy() {
    if (repository != null)
      repository.close();
    super.onDestroy();
  }
}
