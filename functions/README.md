# TechFix Firebase Functions

Trusted Firestore operations for repair assignment, manager updates, and
payments. The module targets Node.js 22 and uses second-generation Firebase
Functions.

## Exports

- `assignRepairAppointment`: Firestore `appointments/{id}` creation trigger.
- `autoAssignAppointment`: manager-only callable that retries centralized
  branch, technician and spare-part assignment for a pending request.
- `reassignAppointment`: manager-only callable technician reassignment.
- `updateRepairStatus`: manager-only callable status update with audit history.
- `processPayment`: Firestore `payments/{id}` write trigger. It processes new
  and retried `PENDING` requests and ignores finalized records.

## Client write contract

Customers create appointments with `status: "PENDING"`, `customerUid`,
`serviceId`, `appointmentAt`, `requestLatitude`, and `requestLongitude`.
Assignment fields are server-owned.

Customers create payments with `status: "PENDING"`, `customerUid`,
`appointmentRemoteId`, and `method`. Amount, reference, final status and paid
time are server-owned.

Manager callables accept Firestore appointment document IDs. Technician and
branch inputs may be either the Firestore document ID or the shared `localId`.
Successful assignment stores `reservedPartId` as the Firestore spare-part
document ID; cancellation uses `reservationReleased` to return its stock only
once.

Manager authorization comes from `users/{uid}.role == "manager"`. Reference
documents are expected to retain their shared numeric `localId` because the
current Android SQLite cache stores branch, technician, service and part
relationships as numeric IDs.

The catalog scan intentionally assumes TechFix's small two-branch coursework
dataset. A production-scale deployment should replace collection scans with
slot/reservation documents and indexed queries.
