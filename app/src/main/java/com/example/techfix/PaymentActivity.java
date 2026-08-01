package com.example.techfix;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.example.techfix.data.model.AppointmentStatus;
import com.example.techfix.data.model.PaymentMethod;

public class PaymentActivity extends CustomerScreen {
  private static final String EXTRA_APPOINTMENT_ID = "payment_appointment_id";

  private CustomerRepository repository;
  private SessionManager sessionManager;
  private long appointmentId;
  private PaymentMethod selectedMethod = PaymentMethod.CARD;
  private boolean observingRealtime;
  private final FirebaseRealtimeSync.DataObserver dataObserver =
      () -> runOnUiThread(() -> {
        if (observingRealtime && repository != null && !isFinishing())
          bindPayment();
      });

  public static void open(Activity activity, long appointmentId) {
    Intent intent = new Intent(activity, PaymentActivity.class);
    intent.putExtra(EXTRA_APPOINTMENT_ID, appointmentId);
    activity.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    showCustomerLayout(R.layout.activity_payment);
    repository = new CustomerRepository(this);
    sessionManager = new SessionManager(this);
    appointmentId = getIntent().getLongExtra(EXTRA_APPOINTMENT_ID, -1);

    findViewById(R.id.btnPaymentBack).setOnClickListener(view -> finish());
    bindMethod(R.id.paymentMethodCard, PaymentMethod.CARD);
    bindMethod(R.id.paymentMethodBank, PaymentMethod.BANK_TRANSFER);
    bindMethod(R.id.paymentMethodOnline, PaymentMethod.ONLINE);
    findViewById(R.id.btnConfirmPayment)
        .setOnClickListener(view -> processPayment());
    updateMethodAppearance();
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (!observingRealtime) {
      FirebaseRealtimeSync.addObserver(dataObserver);
      observingRealtime = true;
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    bindPayment();
  }

  @Override
  protected void onStop() {
    if (observingRealtime) {
      FirebaseRealtimeSync.removeObserver(dataObserver);
      observingRealtime = false;
    }
    super.onStop();
  }

  private void bindMethod(int viewId, PaymentMethod method) {
    findViewById(viewId).setOnClickListener(view -> {
      selectedMethod = method;
      updateMethodAppearance();
    });
  }

  private void updateMethodAppearance() {
    updateMethod(R.id.paymentMethodCard, selectedMethod == PaymentMethod.CARD);
    updateMethod(R.id.paymentMethodBank,
                 selectedMethod == PaymentMethod.BANK_TRANSFER);
    updateMethod(R.id.paymentMethodOnline,
                 selectedMethod == PaymentMethod.ONLINE);
  }

  private void updateMethod(int viewId, boolean selected) {
    TextView method = findViewById(viewId);
    method.setBackgroundResource(selected ? R.drawable.bg_customer_chip_selected
                                          : R.drawable.bg_customer_chip);
    method.setTextColor(
        getColor(selected ? R.color.white : R.color.customer_text));
  }

  private void bindPayment() {
    CustomerRepository.AppointmentItem appointment =
        repository.getAppointment(sessionManager.getUserId(), appointmentId);
    if (appointment == null) {
      finish();
      return;
    }
    ((TextView)findViewById(R.id.tvPaymentRepair))
        .setText("Repair #TF-" + appointment.id);
    ((TextView)findViewById(R.id.tvPaymentDevice))
        .setText(appointment.deviceDetails + " · " + appointment.serviceName);
    ((TextView)findViewById(R.id.tvPaymentAmount))
        .setText(CustomerRepository.formatPrice(appointment.priceCents));

    CustomerRepository.PaymentItem payment =
        repository.getPayment(sessionManager.getUserId(), appointmentId);
    boolean paid = payment != null && "PAID".equals(payment.status);
    TextView status = findViewById(R.id.tvPaymentStatus);
    TextView confirm = findViewById(R.id.btnConfirmPayment);
    if (paid) {
      status.setVisibility(View.VISIBLE);
      status.setText("PAID · " + payment.method.replace('_', ' ') + "\n" +
                     payment.reference);
      confirm.setText("Payment complete");
      confirm.setEnabled(false);
    } else if (payment != null && "PENDING".equals(payment.status)) {
      status.setVisibility(View.VISIBLE);
      status.setText("Payment is being securely confirmed.");
      confirm.setText("Processing payment");
      confirm.setEnabled(false);
    } else if (appointment.status != AppointmentStatus.READY_FOR_PAYMENT) {
      status.setVisibility(View.VISIBLE);
      status.setText("Payment unlocks when the repair is ready.");
      confirm.setText("Not ready for payment");
      confirm.setEnabled(false);
    } else {
      status.setVisibility(payment != null && "FAILED".equals(payment.status)
                               ? View.VISIBLE
                               : View.GONE);
      if (payment != null && "FAILED".equals(payment.status))
        status.setText("Payment was not completed. Please try again.");
      confirm.setText("Confirm payment");
      confirm.setEnabled(true);
    }
  }

  private void processPayment() {
    try {
      CustomerRepository.PaymentItem payment = repository.processPayment(
          sessionManager.getUserId(), appointmentId, selectedMethod);
      Toast
          .makeText(this, "Payment submitted for confirmation.",
                    Toast.LENGTH_LONG)
          .show();
      bindPayment();
    } catch (RuntimeException exception) {
      Toast
          .makeText(this,
                    exception.getMessage() == null
                        ? "Unable to process payment."
                        : exception.getMessage(),
                    Toast.LENGTH_LONG)
          .show();
    }
  }

  @Override
  protected void onDestroy() {
    if (repository != null)
      repository.close();
    super.onDestroy();
  }
}
