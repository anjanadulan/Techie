"use strict";

const crypto = require("node:crypto");
const {initializeApp} = require("firebase-admin/app");
const {FieldValue, getFirestore} = require("firebase-admin/firestore");
const {setGlobalOptions} = require("firebase-functions/v2");
const {
  onDocumentCreated,
  onDocumentWritten,
} = require("firebase-functions/v2/firestore");
const {HttpsError, onCall} = require("firebase-functions/v2/https");

initializeApp();
setGlobalOptions({region: "us-central1", maxInstances: 10});

const db = getFirestore();
const ACTIVE_REPAIR_STATUSES = new Set([
  "PENDING",
  "ASSIGNED",
  "IN_PROGRESS",
  "WAITING_FOR_PARTS",
  "READY_FOR_PAYMENT",
]);
const REPAIR_STATUSES = new Set([
  ...ACTIVE_REPAIR_STATUSES,
  "COMPLETED",
  "CANCELLED",
]);
const PAYMENT_METHODS = new Set([
  "CARD",
  "BANK_TRANSFER",
  "ONLINE",
  "CASH",
]);

exports.assignRepairAppointment = onDocumentCreated(
    "appointments/{appointmentId}",
    async (event) => {
      const appointmentId = event.params.appointmentId;
      const appointmentRef = db.collection("appointments").doc(appointmentId);
      await db.runTransaction((transaction) => assignPendingAppointment(
          transaction,
          appointmentRef,
          appointmentId,
          {eventId: event.id, unavailableIsError: false},
      ));
    },
);

exports.autoAssignAppointment = onCall(async (request) => {
  requireAuthentication(request.auth);
  const appointmentId = requiredId(request.data?.appointmentId, "appointmentId");
  const appointmentRef = db.collection("appointments").doc(appointmentId);
  return db.runTransaction(async (transaction) => {
    await requireManager(transaction, request.auth.uid);
    return assignPendingAppointment(
        transaction,
        appointmentRef,
        appointmentId,
        {eventId: null, unavailableIsError: true},
    );
  });
});

async function assignPendingAppointment(transaction, appointmentRef,
    appointmentId, options) {
  const appointmentSnapshot = await transaction.get(appointmentRef);
  if (!appointmentSnapshot.exists) {
    if (options.unavailableIsError) {
      throw new HttpsError("not-found", "Repair appointment not found.");
    }
    return null;
  }

  const appointment = appointmentSnapshot.data();
  if (options.eventId && appointment.assignmentEventId === options.eventId) {
    return {appointmentId, status: appointment.status};
  }
  if (appointment.status !== "PENDING") {
    if (appointment.status === "ASSIGNED") {
      return {appointmentId, status: appointment.status};
    }
    if (options.unavailableIsError) {
      throw new HttpsError(
          "failed-precondition",
          "Only pending appointments can be automatically assigned.",
      );
    }
    transaction.update(appointmentRef, {
      assignmentState: "REJECTED",
      assignmentError: "Appointments must be created with PENDING status.",
      assignmentEventId: options.eventId,
      updatedAt: FieldValue.serverTimestamp(),
    });
    return null;
  }

  const validationError = validatePendingAppointment(appointment);
  if (validationError) {
    return assignmentUnavailable(
        transaction, appointmentRef, options, validationError);
  }

  const catalog = await readAssignmentCatalog(transaction);
  const service = findReferenced(
      catalog.services,
      appointment.serviceRemoteId,
      appointment.serviceId,
  );
  if (!service || service.data.active !== true) {
    return assignmentUnavailable(
        transaction,
        appointmentRef,
        options,
        "The selected repair service is unavailable.",
    );
  }
  const category = findReferenced(
      catalog.categories,
      service.data.categoryRemoteId,
      service.data.categoryId,
  );
  if (!category || category.data.active !== true) {
    return assignmentUnavailable(
        transaction,
        appointmentRef,
        options,
        "The selected device category is unavailable.",
    );
  }
  const choice = chooseAssignment({
    appointmentId,
    appointment,
    service,
    category,
    ...catalog,
  });
  if (!choice) {
    return assignmentUnavailable(
        transaction,
        appointmentRef,
        options,
        "No branch currently has a compatible technician and required parts.",
    );
  }

  const now = Date.now();
  const appointmentUpdate = {
    status: "ASSIGNED",
    assignmentState: "ASSIGNED",
    assignmentError: FieldValue.delete(),
    assignmentSource: options.eventId ? "AUTO" : "MANAGER_AUTO",
    branchId: localId(choice.branch),
    technicianId: localId(choice.technician),
    branchDocumentId: choice.branch.id,
    branchRemoteId: choice.branch.id,
    technicianDocumentId: choice.technician.id,
    serviceRemoteId: service.id,
    categoryRemoteId: category.id,
    estimatedMinutes: serviceDuration(service.data),
    assignedAt: now,
    updatedAt: FieldValue.serverTimestamp(),
  };
  if (options.eventId) appointmentUpdate.assignmentEventId = options.eventId;
  transaction.update(choice.technician.ref, {
    scheduleVersion: FieldValue.increment(1),
    updatedAt: FieldValue.serverTimestamp(),
  });
  if (choice.part) {
    transaction.update(choice.part.ref, {
      quantityAvailable: FieldValue.increment(-1),
      updatedAt: FieldValue.serverTimestamp(),
    });
    appointmentUpdate.reservedSparePartId = localId(choice.part);
    appointmentUpdate.reservedSparePartDocumentId = choice.part.id;
    appointmentUpdate.reservedPartId = choice.part.id;
    appointmentUpdate.reservationReleased = false;
    appointmentUpdate.partReservationState = "RESERVED";
  }
  transaction.update(appointmentRef, appointmentUpdate);
  createHistory(transaction, {
    historyId: crypto.randomUUID(),
    appointmentId,
    appointment,
    status: "ASSIGNED",
    notes: `Automatically assigned to ${choice.technician.data.fullName || "a technician"} at ${choice.branch.data.name || "TechFix"}.`,
    recordedAt: now,
  });
  return {
    appointmentId,
    status: "ASSIGNED",
    branchId: localId(choice.branch),
    technicianId: localId(choice.technician),
  };
}

function assignmentUnavailable(transaction, appointmentRef, options, message) {
  if (options.unavailableIsError) {
    throw new HttpsError("failed-precondition", message);
  }
  const update = {
    assignmentState: "UNAVAILABLE",
    assignmentError: message,
    updatedAt: FieldValue.serverTimestamp(),
  };
  if (options.eventId) update.assignmentEventId = options.eventId;
  transaction.update(appointmentRef, update);
  return null;
}

exports.reassignAppointment = onCall(async (request) => {
  requireAuthentication(request.auth);
  const appointmentId = requiredId(request.data?.appointmentId, "appointmentId");
  const technicianId = requiredId(request.data?.technicianId, "technicianId");
  const requestedBranchId = optionalId(request.data?.branchId);
  const appointmentRef = db.collection("appointments").doc(appointmentId);
  const historyId = crypto.randomUUID();

  return db.runTransaction(async (transaction) => {
    await requireManager(transaction, request.auth.uid);
    const appointmentSnapshot = await transaction.get(appointmentRef);
    if (!appointmentSnapshot.exists) {
      throw new HttpsError("not-found", "Repair appointment not found.");
    }
    const appointment = appointmentSnapshot.data();
    if (appointment.status === "COMPLETED" || appointment.status === "CANCELLED") {
      throw new HttpsError(
          "failed-precondition",
          "Completed or cancelled appointments cannot be reassigned.",
      );
    }

    const catalog = await readAssignmentCatalog(transaction);
    const technician = findByLocalOrDocumentId(catalog.technicians, technicianId);
    if (!technician || technician.data.active !== true) {
      throw new HttpsError("failed-precondition", "Choose an active technician.");
    }
    const branch = findReferenced(
        catalog.branches,
        requestedBranchId ?? technician.data.branchRemoteId,
        requestedBranchId ?? technician.data.branchId,
    );
    if (!branch || branch.data.active !== true) {
      throw new HttpsError("failed-precondition", "Choose an active branch.");
    }
    if (!technicianBelongsToBranch(technician.data, branch)) {
      throw new HttpsError(
          "failed-precondition",
          "The technician does not belong to the selected branch.",
      );
    }

    const service = findReferenced(
        catalog.services,
        appointment.serviceRemoteId,
        appointment.serviceId,
    );
    if (!service || service.data.active !== true) {
      throw new HttpsError("failed-precondition", "Repair service is unavailable.");
    }
    const category = findReferenced(
        catalog.categories,
        service.data.categoryRemoteId,
        service.data.categoryId,
    );
    if (!category || category.data.active !== true) {
      throw new HttpsError(
          "failed-precondition",
          "The device category is unavailable.",
      );
    }
    if (!technicianSupports(technician.data, category)) {
      throw new HttpsError(
          "failed-precondition",
          "The technician is not compatible with this device category.",
      );
    }
    if (hasOverlap(
        catalog.activeAppointments,
        catalog.services,
        technician,
        appointmentId,
        number(appointment.appointmentAt),
        serviceDuration(service.data),
    )) {
      throw new HttpsError(
          "already-exists",
          "The technician already has an overlapping appointment.",
      );
    }

    const requiredKeyword = requiredPartKeyword(service.data.name);
    const existingPartDocumentId = appointment.reservedPartId ||
      appointment.reservedSparePartDocumentId;
    let existingPart = appointment.reservationReleased === true ? null :
      existingPartDocumentId ?
      catalog.parts.find((part) =>
        part.id === String(existingPartDocumentId)) :
      findByLocalOrDocumentId(catalog.parts, appointment.reservedSparePartId);
    if (!existingPart && appointment.reservationReleased !== true &&
        existingPartDocumentId) {
      const reservedPartSnapshot = await transaction.get(
          db.collection("spareParts").doc(String(existingPartDocumentId)),
      );
      if (reservedPartSnapshot.exists) {
        existingPart = row(reservedPartSnapshot);
      }
    }
    let selectedPart = null;
    if (requiredKeyword) {
      if (existingPart &&
          partBelongsToBranch(existingPart.data, branch) &&
          partMatches(existingPart, category, requiredKeyword)) {
        selectedPart = existingPart;
      } else {
        selectedPart = choosePart(
            catalog.parts,
            branch,
            category,
            requiredKeyword,
        );
        if (!selectedPart) {
          throw new HttpsError(
              "failed-precondition",
              "The selected branch does not have the required spare part.",
          );
        }
      }
    }

    const oldTechnician = appointment.technicianDocumentId ?
      catalog.technicians.find((item) =>
        item.id === String(appointment.technicianDocumentId)) :
      findByLocalOrDocumentId(catalog.technicians, appointment.technicianId);
    const now = Date.now();
    const nextStatus = appointment.status === "PENDING" ?
      "ASSIGNED" : appointment.status;

    transaction.update(technician.ref, {
      scheduleVersion: FieldValue.increment(1),
      updatedAt: FieldValue.serverTimestamp(),
    });
    if (oldTechnician && oldTechnician.id !== technician.id) {
      transaction.update(oldTechnician.ref, {
        scheduleVersion: FieldValue.increment(1),
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
    if (existingPart && (!selectedPart || existingPart.id !== selectedPart.id)) {
      transaction.update(existingPart.ref, {
        quantityAvailable: FieldValue.increment(1),
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
    if (selectedPart && (!existingPart || existingPart.id !== selectedPart.id)) {
      transaction.update(selectedPart.ref, {
        quantityAvailable: FieldValue.increment(-1),
        updatedAt: FieldValue.serverTimestamp(),
      });
    }

    const update = {
      status: nextStatus,
      assignmentState: "ASSIGNED",
      assignmentError: FieldValue.delete(),
      assignmentSource: "MANAGER",
      branchId: localId(branch),
      technicianId: localId(technician),
      branchDocumentId: branch.id,
      branchRemoteId: branch.id,
      technicianDocumentId: technician.id,
      serviceRemoteId: service.id,
      categoryRemoteId: category.id,
      estimatedMinutes: serviceDuration(service.data),
      assignedAt: now,
      updatedAt: FieldValue.serverTimestamp(),
    };
    if (selectedPart) {
      update.reservedSparePartId = localId(selectedPart);
      update.reservedSparePartDocumentId = selectedPart.id;
      update.reservedPartId = selectedPart.id;
      update.reservationReleased = false;
      update.partReservationState = "RESERVED";
    } else {
      update.reservedSparePartId = FieldValue.delete();
      update.reservedSparePartDocumentId = FieldValue.delete();
      update.reservedPartId = FieldValue.delete();
      update.reservationReleased = FieldValue.delete();
      update.partReservationState = FieldValue.delete();
    }
    transaction.update(appointmentRef, update);
    createHistory(transaction, {
      historyId,
      appointmentId,
      appointment,
      status: nextStatus,
      notes: `Assigned by management to ${technician.data.fullName || "a technician"} at ${branch.data.name || "TechFix"}.`,
      recordedAt: now,
    });
    return {
      appointmentId,
      status: nextStatus,
      branchId: localId(branch),
      technicianId: localId(technician),
    };
  });
});

exports.updateRepairStatus = onCall(async (request) => {
  requireAuthentication(request.auth);
  const appointmentId = requiredId(request.data?.appointmentId, "appointmentId");
  const nextStatus = String(request.data?.status || "").trim().toUpperCase();
  const suppliedNotes = String(request.data?.notes || "").trim();
  if (!REPAIR_STATUSES.has(nextStatus)) {
    throw new HttpsError("invalid-argument", "Unknown repair status.");
  }
  const appointmentRef = db.collection("appointments").doc(appointmentId);
  const historyId = crypto.randomUUID();

  return db.runTransaction(async (transaction) => {
    await requireManager(transaction, request.auth.uid);
    const appointmentSnapshot = await transaction.get(appointmentRef);
    if (!appointmentSnapshot.exists) {
      throw new HttpsError("not-found", "Repair appointment not found.");
    }
    const appointment = appointmentSnapshot.data();
    if (appointment.status === nextStatus) {
      throw new HttpsError("already-exists", "The repair already has this status.");
    }
    if (appointment.status === "CANCELLED" || appointment.status === "COMPLETED") {
      throw new HttpsError(
          "failed-precondition",
          "A terminal repair status cannot be changed.",
      );
    }
    if (nextStatus === "COMPLETED" && appointment.paymentStatus !== "PAID") {
      throw new HttpsError(
          "failed-precondition",
          "Payment must be completed before closing the repair.",
      );
    }

    const technicianDocumentId = appointment.technicianDocumentId ||
      appointment.technicianId;
    const technicianSnapshot = technicianDocumentId ?
      await transaction.get(
          db.collection("technicians")
              .doc(String(technicianDocumentId)),
      ) : null;
    const reservedPartId = appointment.reservedPartId ||
      appointment.reservedSparePartDocumentId;
    const partSnapshot = nextStatus === "CANCELLED" && reservedPartId ?
      await transaction.get(
          db.collection("spareParts")
              .doc(String(reservedPartId)),
      ) : null;

    const now = Date.now();
    const update = {
      status: nextStatus,
      updatedAt: FieldValue.serverTimestamp(),
    };
    if (nextStatus === "CANCELLED" && reservedPartId &&
        appointment.reservationReleased !== true && !partSnapshot?.exists) {
      throw new HttpsError(
          "failed-precondition",
          "The reserved spare part no longer exists.",
      );
    }
    if (nextStatus === "CANCELLED" && partSnapshot?.exists &&
        appointment.reservationReleased !== true) {
      transaction.update(partSnapshot.ref, {
        quantityAvailable: FieldValue.increment(1),
        updatedAt: FieldValue.serverTimestamp(),
      });
      update.partReservationState = "RELEASED";
      update.reservationReleased = true;
    } else if (nextStatus === "COMPLETED" &&
        appointment.partReservationState === "RESERVED") {
      update.partReservationState = "CONSUMED";
    }
    if (technicianSnapshot?.exists) {
      transaction.update(technicianSnapshot.ref, {
        scheduleVersion: FieldValue.increment(1),
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
    transaction.update(appointmentRef, update);
    createHistory(transaction, {
      historyId,
      appointmentId,
      appointment,
      status: nextStatus,
      notes: suppliedNotes || `Repair status updated by management to ${statusLabel(nextStatus)}.`,
      recordedAt: now,
    });
    return {appointmentId, status: nextStatus};
  });
});

exports.processPayment = onDocumentWritten(
    "payments/{paymentId}",
    async (event) => {
      const paymentId = event.params.paymentId;
      const paymentRef = db.collection("payments").doc(paymentId);
      const historyId = crypto.randomUUID();
      const processedEventId = event.id;

      await db.runTransaction(async (transaction) => {
        const paymentSnapshot = await transaction.get(paymentRef);
        if (!paymentSnapshot.exists) return;
        const payment = paymentSnapshot.data();
        if (payment.status !== "PENDING") return;
        if (!PAYMENT_METHODS.has(String(payment.method || "").toUpperCase())) {
          failPayment(transaction, paymentRef, processedEventId,
              "Unsupported payment method.");
          return;
        }
        const appointmentId = String(payment.appointmentRemoteId || "").trim();
        const customerUid = String(payment.customerUid || "").trim();
        if (!appointmentId || !customerUid) {
          failPayment(transaction, paymentRef, processedEventId,
              "Appointment and customer ownership are required.");
          return;
        }

        const appointmentRef = db.collection("appointments").doc(appointmentId);
        const appointmentSnapshot = await transaction.get(appointmentRef);
        if (!appointmentSnapshot.exists) {
          failPayment(transaction, paymentRef, processedEventId,
              "Repair appointment not found.");
          return;
        }
        const appointment = appointmentSnapshot.data();
        const servicesSnapshot = await transaction.get(
            db.collection("services"),
        );
        const service = findReferenced(
            rows(servicesSnapshot),
            appointment.serviceRemoteId,
            appointment.serviceId,
        );

        if (appointment.customerUid !== customerUid) {
          failPayment(transaction, paymentRef, processedEventId,
              "Payment ownership does not match the appointment.");
          return;
        }
        if (appointment.status !== "READY_FOR_PAYMENT") {
          failPayment(transaction, paymentRef, processedEventId,
              "The repair is not ready for payment.");
          return;
        }
        if (appointment.paymentStatus === "PAID") {
          failPayment(transaction, paymentRef, processedEventId,
              "This appointment has already been paid.");
          return;
        }
        if (!service || !nonNegativeInteger(service.data.basePriceCents)) {
          failPayment(transaction, paymentRef, processedEventId,
              "The authoritative service price is unavailable.");
          return;
        }

        const now = Date.now();
        const amountCents = service.data.basePriceCents;
        const reference = `TFX-${paymentId.slice(0, 8).toUpperCase()}-${now}`;
        transaction.update(paymentRef, {
          appointmentRemoteId: appointmentId,
          appointmentId: appointment.localId ?? null,
          customerUid,
          method: String(payment.method).toUpperCase(),
          amountCents,
          reference,
          status: "PAID",
          paidAt: now,
          paymentEventId: processedEventId,
          failureReason: FieldValue.delete(),
          updatedAt: FieldValue.serverTimestamp(),
        });
        transaction.update(appointmentRef, {
          paymentStatus: "PAID",
          paidPaymentId: paymentId,
          updatedAt: FieldValue.serverTimestamp(),
        });
        createHistory(transaction, {
          historyId,
          appointmentId,
          appointment,
          status: appointment.status,
          notes: `Payment received via ${String(payment.method).toLowerCase().replaceAll("_", " ")}.`,
          recordedAt: now,
        });
      });
    },
);

async function readAssignmentCatalog(transaction) {
  const branches = await transaction.get(
      db.collection("branches").where("active", "==", true),
  );
  const technicians = await transaction.get(
      db.collection("technicians").where("active", "==", true),
  );
  const parts = await transaction.get(
      db.collection("spareParts").where("active", "==", true),
  );
  const services = await transaction.get(db.collection("services"));
  const categories = await transaction.get(db.collection("deviceCategories"));
  const activeAppointments = await transaction.get(
      db.collection("appointments")
          .where("status", "in", [...ACTIVE_REPAIR_STATUSES]),
  );
  return {
    branches: rows(branches),
    technicians: rows(technicians),
    parts: rows(parts),
    services: rows(services),
    categories: rows(categories),
    activeAppointments: rows(activeAppointments),
  };
}

function chooseAssignment(input) {
  const branches = input.branches
      .filter((branch) => validCoordinates(
          branch.data.latitude,
          branch.data.longitude,
      ))
      .sort((left, right) => distanceKm(
          input.appointment.requestLatitude,
          input.appointment.requestLongitude,
          left.data.latitude,
          left.data.longitude,
      ) - distanceKm(
          input.appointment.requestLatitude,
          input.appointment.requestLongitude,
          right.data.latitude,
          right.data.longitude,
      ));
  const requiredKeyword = requiredPartKeyword(input.service.data.name);

  for (const branch of branches) {
    const part = requiredKeyword ? choosePart(
        input.parts,
        branch,
        input.category,
        requiredKeyword,
    ) : null;
    if (requiredKeyword && !part) continue;

    const candidates = input.technicians
        .filter((technician) =>
          technicianBelongsToBranch(technician.data, branch) &&
          technicianSupports(technician.data, input.category))
        .filter((technician) => !hasOverlap(
            input.activeAppointments,
            input.services,
            technician,
            input.appointmentId,
            number(input.appointment.appointmentAt),
            serviceDuration(input.service.data),
        ))
        .sort((left, right) => {
          const workloadDifference = activeWorkload(
              input.activeAppointments,
              left,
          ) - activeWorkload(input.activeAppointments, right);
          if (workloadDifference !== 0) return workloadDifference;
          return String(left.data.fullName || "")
              .localeCompare(String(right.data.fullName || ""));
        });
    if (candidates.length > 0) {
      return {branch, technician: candidates[0], part};
    }
  }
  return null;
}

function hasOverlap(appointments, services, technician, excludedAppointmentId,
    requestedStart, requestedDurationMinutes) {
  const requestedEnd = requestedStart + requestedDurationMinutes * 60_000;
  return appointments.some((existing) => {
    if (existing.id === excludedAppointmentId ||
        !appointmentUsesTechnician(existing.data, technician) ||
        !ACTIVE_REPAIR_STATUSES.has(existing.data.status)) return false;
    const existingStart = number(existing.data.appointmentAt);
    const existingService = findReferenced(
        services,
        existing.data.serviceRemoteId,
        existing.data.serviceId,
    );
    const existingDuration = number(existing.data.estimatedMinutes) ||
      serviceDuration(existingService?.data || {});
    const existingEnd = existingStart + existingDuration * 60_000;
    return requestedStart < existingEnd && requestedEnd > existingStart;
  });
}

function activeWorkload(appointments, technician) {
  return appointments.filter((appointment) =>
    appointmentUsesTechnician(appointment.data, technician) &&
    ACTIVE_REPAIR_STATUSES.has(appointment.data.status)).length;
}

function choosePart(parts, branch, category, keyword) {
  return parts
      .filter((part) =>
        partBelongsToBranch(part.data, branch) &&
        partMatches(part, category, keyword) &&
        nonNegativeInteger(part.data.quantityAvailable) &&
        part.data.quantityAvailable > 0)
      .sort((left, right) =>
        number(right.data.quantityAvailable) -
        number(left.data.quantityAvailable))[0] || null;
}

function partMatches(part, category, keyword) {
  return part.data.active === true &&
    referenceMatches(
        part.data.categoryRemoteId,
        part.data.categoryId,
        category,
    ) &&
    String(part.data.name || "").toLowerCase().includes(keyword);
}

function technicianSupports(technician, category) {
  const supported = technician.supportedCategoryIds ||
    technician.categoryIds || [];
  if (Array.isArray(supported) && supported.some((id) =>
    sameId(id, category.id) || sameId(id, localId(category)))) {
    return true;
  }
  const categoryName = String(category?.data?.name || "").trim().toLowerCase();
  return categoryName.length > 0 &&
    String(technician.specialty || "").toLowerCase().includes(categoryName);
}

function validatePendingAppointment(appointment) {
  if (!String(appointment.customerUid || "").trim()) {
    return "Customer ownership is required.";
  }
  if (appointment.serviceId === undefined || appointment.serviceId === null) {
    return "A repair service is required.";
  }
  if (!Number.isFinite(number(appointment.appointmentAt)) ||
      number(appointment.appointmentAt) <= Date.now()) {
    return "Choose a future appointment time.";
  }
  if (!validCoordinates(
      appointment.requestLatitude,
      appointment.requestLongitude,
  )) {
    return "Valid requestLatitude and requestLongitude values are required.";
  }
  return null;
}

function createHistory(transaction, details) {
  const historyRef = db.collection("repairHistory").doc(details.historyId);
  transaction.create(historyRef, {
    remoteId: details.historyId,
    appointmentRemoteId: details.appointmentId,
    appointmentId: details.appointment.localId ?? null,
    customerUid: details.appointment.customerUid,
    status: details.status,
    notes: details.notes,
    featured: false,
    imagePath: null,
    recordedAt: details.recordedAt,
    updatedAt: FieldValue.serverTimestamp(),
  });
}

function failPayment(transaction, paymentRef, eventId, message) {
  transaction.update(paymentRef, {
    status: "FAILED",
    failureReason: message,
    paymentEventId: eventId,
    updatedAt: FieldValue.serverTimestamp(),
  });
}

async function requireManager(transaction, uid) {
  const managerSnapshot = await transaction.get(
      db.collection("users").doc(uid),
  );
  if (!managerSnapshot.exists || managerSnapshot.data().role !== "manager") {
    throw new HttpsError("permission-denied", "Manager access is required.");
  }
}

function requireAuthentication(auth) {
  if (!auth?.uid) {
    throw new HttpsError("unauthenticated", "Sign in to continue.");
  }
}

function requiredId(value, name) {
  const id = String(value ?? "").trim();
  if (!id) throw new HttpsError("invalid-argument", `${name} is required.`);
  return id;
}

function optionalId(value) {
  const id = String(value ?? "").trim();
  return id || null;
}

function rows(snapshot) {
  return snapshot.docs.map(row);
}

function row(document) {
  return {
    id: document.id,
    ref: document.ref,
    data: document.data(),
  };
}

function findByLocalOrDocumentId(items, id) {
  if (id === undefined || id === null || id === "") return null;
  return items.find((item) => item.id === String(id)) ||
    items.find((item) => sameId(item.data.localId, id)) || null;
}

function findReferenced(items, remoteId, localReference) {
  return findByLocalOrDocumentId(items, remoteId) ||
    findByLocalOrDocumentId(items, localReference);
}

function referenceMatches(remoteId, localReference, target) {
  if (remoteId !== undefined && remoteId !== null && remoteId !== "") {
    if (sameId(remoteId, target.id)) return true;
  }
  return sameId(localReference, localId(target));
}

function technicianBelongsToBranch(technician, branch) {
  return referenceMatches(
      technician.branchRemoteId,
      technician.branchId,
      branch,
  );
}

function partBelongsToBranch(part, branch) {
  return referenceMatches(part.branchRemoteId, part.branchId, branch);
}

function appointmentUsesTechnician(appointment, technician) {
  const remoteId = appointment.technicianDocumentId ||
    appointment.technicianRemoteId;
  if (remoteId) return sameId(remoteId, technician.id);
  return sameId(appointment.technicianId, localId(technician));
}

function localId(item) {
  if (item?.data?.localId !== undefined && item.data.localId !== null) {
    return item.data.localId;
  }
  const parsed = Number(item?.id);
  return Number.isSafeInteger(parsed) ? parsed : item?.id;
}

function sameId(left, right) {
  return left !== undefined && left !== null &&
    right !== undefined && right !== null &&
    String(left) === String(right);
}

function serviceDuration(service) {
  const minutes = number(service?.estimatedMinutes);
  return Number.isFinite(minutes) && minutes > 0 ? minutes : 60;
}

function requiredPartKeyword(serviceName) {
  const name = String(serviceName || "").toLowerCase();
  if (name.includes("screen")) return "display";
  if (name.includes("battery")) return "battery";
  if (name.includes("keyboard")) return "keyboard";
  if (name.includes("charging")) return "charging";
  return null;
}

function number(value) {
  if (typeof value === "number") return value;
  if (value && typeof value.toMillis === "function") return value.toMillis();
  return Number(value);
}

function nonNegativeInteger(value) {
  return Number.isSafeInteger(value) && value >= 0;
}

function validCoordinates(latitude, longitude) {
  const lat = number(latitude);
  const lon = number(longitude);
  return Number.isFinite(lat) && lat >= -90 && lat <= 90 &&
    Number.isFinite(lon) && lon >= -180 && lon <= 180;
}

function distanceKm(fromLatitude, fromLongitude, toLatitude, toLongitude) {
  const earthRadiusKm = 6371.0088;
  const radians = (degrees) => degrees * Math.PI / 180;
  const latitudeDistance = radians(toLatitude - fromLatitude);
  const longitudeDistance = radians(toLongitude - fromLongitude);
  const startLatitude = radians(fromLatitude);
  const endLatitude = radians(toLatitude);
  const value = Math.sin(latitudeDistance / 2) ** 2 +
    Math.cos(startLatitude) * Math.cos(endLatitude) *
    Math.sin(longitudeDistance / 2) ** 2;
  return earthRadiusKm * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
}

function statusLabel(status) {
  return String(status).toLowerCase().replaceAll("_", " ");
}
