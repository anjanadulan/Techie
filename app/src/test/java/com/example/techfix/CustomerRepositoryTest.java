package com.example.techfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

  @Test
  public void gpsDistancePrefersNearbySriLankanBranch() {
    double toColombo = CustomerRepository.distanceKm(6.90, 79.86, 6.9271, 79.8612);
    double toGalle = CustomerRepository.distanceKm(6.90, 79.86, 6.0535, 80.2210);
    assertTrue(toColombo < toGalle);
  }
}
