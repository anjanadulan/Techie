package com.example.techfix;

import static org.junit.Assert.assertEquals;

import com.example.techfix.data.model.AppointmentStatus;
import org.junit.Test;

public class ManagementRepositoryTest {
  @Test
  public void statusLabelFormatsStoredAppointmentStatus() {
    assertEquals(
        "Ready for payment", ManagementRepository.statusLabel(AppointmentStatus.READY_FOR_PAYMENT));
  }

  @Test
  public void formatPriceConvertsStoredCentsToRupees() {
    assertEquals("LKR 8,500", ManagementRepository.formatPrice(850000));
  }
}
