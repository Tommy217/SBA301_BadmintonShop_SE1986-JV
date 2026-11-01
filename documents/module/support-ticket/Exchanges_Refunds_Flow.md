# UC15 – Exchanges & Refunds (Simplified Flow for Implementation)

Project: Badminton Shop (SWD392-G5). This document is the implementation brief for UC15. It overrides older versions of UC15, the Complaint & Returns activity diagram, and CR-BR-05/16/23/26/28/29.

## 1. Scope decisions (read first)

- **COD is removed from the system.** Every order is paid online through the Payment Gateway, so every refundable transaction has status `Paid` or `Partially Refunded`.
- **Refunds are manual.** After approval, Staff transfers the money by scanning the QR code that the Customer uploaded when creating the ticket, then records the bank transfer reference in the system.
- **No E-Wallet, no Payout API, no Payment Gateway call for refunds.** Removed: wallet linking, wallet selection, wallet binding validation, automatic refund trigger, "Refund Pending Verification" timeout state.
- **QR image is always uploaded when creating a ticket** (both `REFUND` and `EXCHANGE`). No bank name / account number / account holder text fields.
- **Exchange is 1:1 same-price only** (e.g. different size or color of the same product). No price difference and no extra shipping fee collected by the system.
- **No stock reservation for the replacement.** Availability is checked once, after the returned product passes inspection.
- **Cause of the request** (`STORE_FAULT` or `CUSTOMER_PREFERENCE`) is set by Staff at initial review. It decides the shipping-fee reimbursement and what happens when an exchange replacement is out of stock.
- Keep the 3,000,000 VND approval threshold (Staff below, Manager at or above).
- The refund transfer step reuses UC13 Manage Payment (Process Refund, manual path), applicable to all orders.

## 2. Actors

| Actor | Role in UC15 |
|---|---|
| Customer | Create ticket (Refund / Exchange) with QR, view own tickets and details, cancel ticket before processing, ship the product back |
| Staff | Initial review and cause marking, receive and inspect returned product, approve refund below 3,000,000 VND, ship exchange product, transfer refund manually and record reference |
| Manager | Review Staff inspection result and approve / reject refund of 3,000,000 VND or more |

No external actors (Payment Gateway and E-Wallet Platform are not part of UC15). Notifications reuse the existing notification module (UC9).

## 3. Ticket statuses and transitions

Statuses: `PENDING`, `REJECTED`, `AWAITING_RETURN`, `INSPECTION_FAILED`, `PENDING_APPROVAL`, `PENDING_TRANSFER`, `REFUNDED`, `COMPLETED`, `CANCELLED`.

**Refund ticket**

```
PENDING --Staff rejects (reason)--> REJECTED
PENDING --Staff approves initial review (sets cause)--> AWAITING_RETURN
PENDING --Customer cancels--> CANCELLED
AWAITING_RETURN --Inspection FAILED (reason)--> INSPECTION_FAILED          (product shipped back)
AWAITING_RETURN --Inspection PASSED, amount < 3,000,000--> PENDING_TRANSFER (Staff approves)
AWAITING_RETURN --Inspection PASSED, amount >= 3,000,000--> PENDING_APPROVAL
PENDING_APPROVAL --Manager approves--> PENDING_TRANSFER
PENDING_APPROVAL --Manager rejects (reason)--> REJECTED                    (product shipped back)
PENDING_TRANSFER --Staff records transfer--> REFUNDED
```

**Exchange ticket**

```
PENDING --Staff rejects (reason)--> REJECTED
PENDING --Staff approves initial review (sets cause)--> AWAITING_RETURN
PENDING --Customer cancels--> CANCELLED
AWAITING_RETURN --Inspection FAILED (reason)--> INSPECTION_FAILED          (product shipped back)
AWAITING_RETURN --PASSED, replacement available--> COMPLETED               (Staff ships replacement)
AWAITING_RETURN --PASSED, replacement unavailable, cause = STORE_FAULT--> follows the Refund path
                    (PENDING_TRANSFER if amount < 3,000,000, else PENDING_APPROVAL, then REFUNDED)
AWAITING_RETURN --PASSED, replacement unavailable, cause = CUSTOMER_PREFERENCE--> CANCELLED
                    (original product shipped back, no refund)
```

Rules for transitions:
- A Customer can cancel only while status is `PENDING` (before processing begins).
- An exchange ticket that moves to the refund path keeps `type = EXCHANGE`, is not inspected again, and ends in `REFUNDED`.
- Every status change is written to the status history and the audit log, and triggers a Customer notification.
- Terminal statuses: `REJECTED`, `INSPECTION_FAILED`, `REFUNDED`, `COMPLETED`, `CANCELLED`.

## 4. Main flows

### 4.1 Create ticket (Customer)
Input: order, selected order items with quantities, request type (`REFUND` or `EXCHANGE`), reason, evidence photos/videos, and a **QR image** for receiving money. For `EXCHANGE`, also the requested replacement variant (same product, same price) per item.

System validation (reject with a clear message if any fails):
1. Customer is authenticated and owns the order.
2. Order is delivered and the request is within **7 days** of the confirmed delivery date.
3. The order has **no existing ticket** (at most one ticket per order, any status).
4. Each selected item belongs to the order, and requested quantity is at most the purchased quantity.
5. Reason and at least one evidence file are present.
6. QR image is present and is a valid image file (JPEG/PNG, within the size limit used by evidence upload), for both ticket types. The UI marks the QR upload as a **required field** so a normal user cannot submit without it; the backend check is only a safeguard against direct API calls (no extra error flow needs to be drawn in the diagrams).
7. For `EXCHANGE`: the replacement variant is the same price as the original item.

On success: create ticket with status `PENDING`, link items, evidence and QR, notify Customer, and make the ticket visible in the Staff queue.

### 4.2 Initial review (Staff, SLA 48 hours)
- Staff opens the ticket queue and views the ticket, order, items, evidence and QR.
- Reject (reason required) → `REJECTED`, notify Customer.
- Approve → Staff must set the **cause** (`STORE_FAULT` or `CUSTOMER_PREFERENCE`) → `AWAITING_RETURN`, notify Customer with return instructions.
- Cause meaning: `STORE_FAULT` = manufacturing defect, wrong product, damaged before use, missing accessories, product not matching description, or other store error. `CUSTOMER_PREFERENCE` = size/model change or similar personal choice.
- Staff may also request additional evidence outside the system by rejecting with a reason (no separate status).

### 4.3 Return and inspection (Staff)
- Customer ships the product back (outside the system).
- Staff confirms receipt and records the inspection result: `PASSED` or `FAILED`, with a note.
- `FAILED` → `INSPECTION_FAILED`, product shipped back, notify Customer. For refunds this blocks any approval or transfer.

### 4.4 Refund approval (Refund tickets, and Exchange tickets converted to refund)
After a `PASSED` inspection:
1. System calculates the refund amount: sum of the paid line amounts of the selected items for the requested quantities, plus shipping-fee reimbursement only when cause = `STORE_FAULT`.
2. Validate: refund amount must not exceed `amount paid − already refunded`. Otherwise reject.
3. Threshold:
   - **Below 3,000,000 VND**: authorized Staff approves → `PENDING_TRANSFER`.
   - **3,000,000 VND or more**: ticket → `PENDING_APPROVAL`. Manager reviews the Staff inspection result and approves (→ `PENDING_TRANSFER`) or rejects with reason (→ `REJECTED`, product shipped back).
4. Record the approving actor and decision. The threshold is enforced by the system based on the validated refund amount, not the order value.

### 4.5 Manual refund transfer (Staff, via UC13)
1. Staff opens the ticket in `PENDING_TRANSFER` (or UC13 View Transaction List → Process Refund) and sees the Customer's uploaded QR and the approved amount.
2. Staff scans the QR with a banking app and transfers the approved amount.
3. Staff enters the **bank transfer reference** (required, unique) and confirms.
4. System, in one atomic transaction:
   - creates exactly one `refund_record` for the ticket (reject if one already exists),
   - updates the payment transaction: `Paid → Partially Refunded` or `Refunded` (total refunded never exceeds the amount paid),
   - sets the ticket to `REFUNDED`,
   - writes the audit log and notifies the Customer.
5. Expected completion for the Customer: 3–7 business days after approval.

### 4.6 Exchange completion (Staff)
After a `PASSED` inspection on an `EXCHANGE` ticket, Staff checks that the replacement variant is available:
- **Available:** Staff ships the replacement, enters the shipping info, ticket → `COMPLETED`, notify Customer. Stock of the replacement is deducted using the existing inventory logic (UC18 Deduct Stock / Update Inventory).
- **Unavailable and cause = `STORE_FAULT`:** the ticket moves to the refund path (section 4.4), using the Customer's uploaded QR. No new inspection and no extra ticket.
- **Unavailable and cause = `CUSTOMER_PREFERENCE`:** ticket → `CANCELLED`, original product shipped back, notify Customer. **No refund** (a voluntary refund is not offered just because the Customer changes their mind or wants a different size or model).
- The system never starts a refund automatically only because the replacement is unavailable; a refund happens only through the cause rule above and the normal approval and manual transfer.

### 4.7 Customer cancels ticket
- Only when status is `PENDING`. Otherwise show an error.
- Success → `CANCELLED`, notify Customer, write audit log.

### 4.8 Customer views tickets
List own tickets (type, status, created date) and open a ticket detail (items, evidence, status history, rejection reason, refund result). A Customer can only see their own tickets.

## 5. Business rules (updated set for this implementation)

Kept as in the BusinessRule file: CR-BR-01 to 04, 06 to 15, 17 to 22, 24, 25, 27. Changed or removed:

| ID | Status | New content |
|---|---|---|
| CR-BR-05 | Modified | Exchange only when the original product meets return eligibility, the replacement is available, **and the replacement has the same price as the original**. No price difference or extra fee is collected by the system. |
| CR-BR-16 | Rewritten | All approved refunds are transferred **manually by Staff** to the account behind the QR image uploaded by the Customer. The system does not call any gateway refund or payout API. |
| CR-BR-23 | Modified | If the replacement is unavailable after a satisfactory inspection: when the cause is `STORE_FAULT` the ticket follows the refund approval and manual transfer rules; when the cause is `CUSTOMER_PREFERENCE` the ticket is cancelled, the original product is returned, and no refund is made. No refund is started automatically. |
| CR-BR-26 | Rewritten | A Customer must upload a QR image for receiving money when creating any ticket (Refund or Exchange). The Customer does not enter bank account text details. |
| CR-BR-28 | Rewritten | When satisfactory inspection and the required Staff or Manager approval are confirmed, the ticket moves to `PENDING_TRANSFER`. Staff executes the transfer manually and records the transfer reference. Exactly one refund record is created per ticket. |
| CR-BR-29 | Removed | E-Wallet binding authorization no longer exists. |

Related existing rules that now depend on the cause: CR-BR-21 (return shipping fee is covered by the store for `STORE_FAULT`, by the Customer for `CUSTOMER_PREFERENCE`; the system only records the cause and does not collect any fee).

Key rules to enforce in code:
- One ticket per order (DB unique constraint on `order_id` plus service-level check).
- Return window 7 days from confirmed delivery.
- Evidence and QR required when creating a ticket.
- Cause is required when Staff approves the initial review.
- Refund amount ≤ amount paid − already refunded.
- Threshold 3,000,000 VND: below → Staff, at or above → Manager only.
- Refund and approval require a `PASSED` inspection.
- No duplicate refund: one `refund_record` per ticket and unique `bank_reference`.
- Tickets are processed independently of each other.
- Evidence and QR files are accessible only to the ticket owner, Staff and Manager.
- Every ticket action, approval decision, refund operation and status change is audited; important status changes notify the Customer.

## 6. Data model (suggested, MySQL naming)

```
support_ticket
  id PK, ticket_code UNIQUE, order_id UNIQUE FK, customer_id FK,
  type ENUM('REFUND','EXCHANGE'), reason TEXT, status VARCHAR,
  cause ENUM('STORE_FAULT','CUSTOMER_PREFERENCE') NULL,   -- set at initial review approval
  reject_reason TEXT NULL, refund_amount DECIMAL NULL,
  refund_qr_url VARCHAR NOT NULL,                         -- required for both types
  reviewed_by FK NULL, approved_by FK NULL, approved_at DATETIME NULL,
  created_at, updated_at

support_ticket_item
  id PK, ticket_id FK, order_item_id FK, quantity INT,
  replacement_variant_id FK NULL                          -- EXCHANGE only

ticket_evidence
  id PK, ticket_id FK, file_url, file_type, uploaded_at

ticket_status_history
  id PK, ticket_id FK, from_status, to_status, actor_id FK, note, created_at

ticket_inspection
  id PK, ticket_id FK UNIQUE, staff_id FK, result ENUM('PASSED','FAILED'),
  note TEXT, inspected_at

refund_record
  id PK, ticket_id FK UNIQUE, transaction_id FK, amount DECIMAL,
  bank_reference VARCHAR UNIQUE, transferred_by FK, transferred_at DATETIME
```

Reuse existing tables and services for: orders, payment transactions, notifications, audit log, file upload, inventory.

## 7. API outline (adapt to project conventions)

Customer:
- `POST /api/support-tickets` (multipart: items, type, reason, evidence files, QR file, replacement variants for exchange)
- `GET /api/support-tickets` (own tickets)
- `GET /api/support-tickets/{id}`
- `POST /api/support-tickets/{id}/cancel`

Staff:
- `GET /api/staff/support-tickets` (filter by type, status)
- `POST /api/staff/support-tickets/{id}/review` (approve with `cause`, or reject with reason)
- `POST /api/staff/support-tickets/{id}/inspection` (PASSED / FAILED + note)
- `POST /api/staff/support-tickets/{id}/approve-refund` (amount below 3,000,000 only)
- `POST /api/staff/support-tickets/{id}/ship-exchange` (replacement available)
- `POST /api/staff/support-tickets/{id}/replacement-unavailable` (system applies the cause rule: `STORE_FAULT` → refund path, `CUSTOMER_PREFERENCE` → `CANCELLED`)
- `POST /api/staff/support-tickets/{id}/refund-transfer` (bank_reference; calls the UC13 manual refund logic)

Manager:
- `POST /api/manager/support-tickets/{id}/decision` (approve / reject + reason, amount at or above 3,000,000 only)

Authorization: enforce roles server-side; the Staff approve endpoint must reject amounts at or above the threshold.

## 8. Exceptions and error handling

| Case | System behavior |
|---|---|
| Missing evidence or missing QR | Normally blocked by required fields in the UI; the backend also rejects creation as a safeguard |
| Return window expired or policy violated | Reject with explanation |
| Order already has a ticket | Reject creation |
| Quantity exceeds purchased quantity | Reject |
| Exchange replacement has a different price | Reject creation |
| Staff approves review without cause | Reject the action |
| Refund amount exceeds remaining refundable amount | Reject |
| Inspection failed or product not yet received | Block approval and transfer |
| Approval conditions not met | Block transfer |
| Duplicate refund attempt (record or reference exists) | Block, no change |
| Cancel when status is not `PENDING` | Error message |
| Replacement unavailable | Apply cause rule (refund path or `CANCELLED`) |
| QR image unreadable when Staff scans it | Staff rejects or contacts the Customer outside the system (no extra status) |
| DB or system error during an update | Roll back, statuses unchanged |

## 9. Changes required in UC13 (Manage Payment)

- Remove COD flows (COD Pending / Collected / Settled, Notify COD Collection, Reconcile COD Payments, Switch to COD) if COD is removed project-wide.
- Process Refund: keep only the manual path (former ALTER-7a), applicable to **all** orders. Remove the gateway refund API path and the "Refund Pending Verification" handling for refunds.
- Process Refund must verify the ticket is `PENDING_TRANSFER` and the refund amount equals the approved amount.

## 10. Diagram changes (for the team, not for coding)

- Use case diagram: remove E-Wallet Platform and Payment Gateway actors, remove Link E-Wallet / Select Refund E-Wallet / Validate Wallet Binding & Eligibility. Replace "Process Refund (Payout to E-Wallet)" with "Process Manual Refund" that includes "Record Transfer Reference".
- Returns activity diagram: first Customer step becomes "attach refund QR"; remove the Payment Gateway lane; the last step becomes Staff "Transfer refund via QR and enter reference" followed by System "Record refund and update ticket".
- Exchange activity diagram: add QR upload at ticket creation, a Staff step "set cause" at review, and after inspection a decision "replacement available?" with branches: ship replacement, or (cause = store fault) go to the refund approval path, or (cause = customer preference) cancel and ship the original product back.
- Context diagram: remove wallet and refund-payout flows.

## 11. Out of scope for this implementation

E-Wallet linking, Payout API, automatic refund trigger, refund timeout reconciliation, COD, price difference or shipping fee collection for exchanges, manual bank text entry by Customer, QR generation or QR content validation, stock reservation for replacement products.

## 12. Acceptance checklist

- [ ] Customer cannot create a second ticket for the same order.
- [ ] A ticket (Refund or Exchange) cannot be created without a QR image.
- [ ] An Exchange ticket cannot be created with a different-price replacement.
- [ ] Customer can cancel only in `PENDING`.
- [ ] Staff rejection requires a reason and notifies the Customer.
- [ ] Staff approval of the initial review requires a cause.
- [ ] Failed inspection blocks refund and sets `INSPECTION_FAILED`.
- [ ] Refund below 3,000,000 is approved by Staff; at or above requires Manager; wrong role is rejected by the API.
- [ ] Refund amount never exceeds paid minus already refunded; shipping reimbursement is added only for `STORE_FAULT`.
- [ ] Staff transfer requires a unique bank reference and creates exactly one `refund_record`.
- [ ] Transaction status becomes `Partially Refunded` or `Refunded` correctly after transfer.
- [ ] Exchange with replacement available ends in `COMPLETED` and deducts stock.
- [ ] Exchange with replacement unavailable and `STORE_FAULT` goes to the refund path without a second inspection.
- [ ] Exchange with replacement unavailable and `CUSTOMER_PREFERENCE` ends in `CANCELLED` with no refund.
- [ ] Every status change is in the history and audit log and notifies the Customer.
- [ ] Customers cannot view other Customers' tickets, evidence or QR files.
