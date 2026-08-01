package com.example.techfix;

import static org.junit.Assert.assertEquals;

import com.example.techfix.data.model.AppointmentStatus;
import org.junit.Test;

public class CustomerRepositoryTest {
  @Test
  public void formatsStoredCentsAsSriLankanPrice() {
    assertEquals("LKR 8,500", CustomerRepository.formatPrice(850000));
  }

  @Test
  public void formatsDatabaseStatusForCustomers() {
    assertEquals(
        "Ready for payment", CustomerRepository.statusLabel(AppointmentStatus.READY_FOR_PAYMENT));
  }
}
