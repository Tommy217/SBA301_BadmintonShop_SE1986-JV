# UC15 – Returns & Refunds (Implementation Specification)

Project: Badminton Shop (SWD392-G5). This document is the implementation brief for UC15.

## 1. Scope decisions

- **COD is removed from the system.** Every order is paid online through the Payment Gateway, so every refundable transaction has status `Paid` or `Partially Refunded`.
- **Refunds are manual.** After approval, Staff transfers the money by scanning the QR code that the Customer uploaded when creating the ticket, then records the bank transfer reference in the system.
- **No E-Wallet, no Payout API, no Payment Gateway call for refunds.** Removed: wallet linking, wallet selection, wallet binding validation, automatic refund trigger, "Refund Pending Verification" timeout state.
- **QR image is always uploaded when creating a ticket.** No bank name / account number / account holder text fields.
- **Scope is strictly Return & Refund (`REFUND`).** Exchange flow is not supported. Customers who need a different item or size receive a refund and may place a new order.
- **Cause of the request and refund policy:** The shop **only accepts returns and refunds for store fault (`STORE_FAULT`)**. If an item passes inspection but the cause is customer change of mind (`CUSTOMER_PREFERENCE`), the ticket is `REJECTED`, the product is returned to the customer, and no refund is issued.
- **No inventory restock:** Returned items are defective/store fault items set aside for manufacturer warranty/disposal, so they are not restocked into saleable inventory.
- **Approval threshold:** 3,000,000 VND (Staff below, Manager at or above).
- **Return logistics:** Customers ship the product back to the shop's warehouse address as instructed on the ticket detail page. Staff receives and inspects the item directly at the warehouse.

## 2. Actors

| Actor | Role in UC15 |
|---|---|
| Customer | Create ticket (Refund) with QR, view own tickets and details, cancel ticket before processing, ship the product back |
| Staff | Initial review and cause marking, receive and inspect returned product, approve refund below 3,000,000 VND, transfer refund manually and record bank reference |
| Manager | Review Staff inspection result and approve / reject refund of 3,000,000 VND or more |

## 3. Ticket statuses and transitions

Statuses: `PENDING`, `REJECTED`, `AWAITING_RETURN`, `INSPECTION_FAILED`, `PENDING_APPROVAL`, `PENDING_TRANSFER`, `REFUNDED`, `CANCELLED`.

```
PENDING --Staff rejects (reason)--> REJECTED
PENDING --Staff approves initial review (sets cause)--> AWAITING_RETURN
PENDING --Customer cancels--> CANCELLED
AWAITING_RETURN --Inspection FAILED (reason)--> INSPECTION_FAILED          (product shipped back)
AWAITING_RETURN --Inspection PASSED, cause = CUSTOMER_PREFERENCE--> REJECTED (product shipped back, no refund)
AWAITING_RETURN --Inspection PASSED, cause = STORE_FAULT, amount < 3,000,000--> PENDING_TRANSFER (Staff approves)
AWAITING_RETURN --Inspection PASSED, cause = STORE_FAULT, amount >= 3,000,000--> PENDING_APPROVAL
PENDING_APPROVAL --Manager approves--> PENDING_TRANSFER
PENDING_APPROVAL --Manager rejects (reason)--> REJECTED                    (product shipped back)
PENDING_TRANSFER --Staff records transfer--> REFUNDED
```

## 4. Step-by-step details

### 4.1 Ticket creation (Customer)
- Input: order, selected order items with quantities, reason, description, evidence photos/videos, and a **QR image** for receiving money.
- Validation:
  1. Authenticated user owns the order.
  2. Order is in `completed` status with payment status `paid`.
  3. Order was delivered within the 7-day return window.
  4. Order does not already have an active ticket.
  5. Selected items belong to the order and quantities do not exceed purchased quantities.
  6. QR image is uploaded.
  7. At least one evidence file is uploaded.
- Result: Ticket created in `PENDING` status.

### 4.2 Initial review (Staff, SLA 48h)
- Staff reviews the ticket reason and evidence files.
- Staff can:
  - **Approve**: Staff assigns cause (`STORE_FAULT` or `CUSTOMER_PREFERENCE`). Status moves to `AWAITING_RETURN`.
  - **Reject**: Staff enters a reject reason. Status moves to `REJECTED`.

### 4.3 Inspection (Staff)
- After receiving the package at the warehouse, Staff records the inspection result:
  - `FAILED`: Status moves to `INSPECTION_FAILED`. Item is returned to customer; ticket ends.
  - `PASSED`:
    - If cause is `CUSTOMER_PREFERENCE`: Status moves to `REJECTED`. Original item is returned to customer; no refund.
    - If cause is `STORE_FAULT`: System calculates refund amount (item price + initial shipping fee, capped at order total amount).
      - If amount < 3,000,000 VND: Staff approves $\rightarrow$ moves to `PENDING_TRANSFER`.
      - If amount $\ge$ 3,000,000 VND: Moves to `PENDING_APPROVAL` for Manager review.

### 4.4 Manager approval (Manager)
- For refund amounts $\ge$ 3,000,000 VND:
  - **Approve**: Status moves to `PENDING_TRANSFER`.
  - **Reject**: Manager provides reject reason $\rightarrow$ status moves to `REJECTED`.

### 4.5 Refund transfer (Staff)
- Staff views the Customer's uploaded QR code and exact refund amount.
- Staff scans the QR code via mobile banking and executes the transfer.
- Staff records the unique bank transaction reference (`bank_reference`) and optional note.
- System sets ticket status to `REFUNDED`, links payment ID, updates `payments.status = 'refunded'`, and updates order payment status to `refunded` (or `partially_refunded`).

## 5. API Endpoints

- `POST /api/support-tickets` (Customer creates ticket)
- `GET /api/support-tickets` (Customer lists own tickets)
- `GET /api/support-tickets/{id}` (Ticket detail)
- `POST /api/support-tickets/{id}/cancel` (Customer cancels ticket in PENDING)
- `GET /api/staff/support-tickets` (Staff queue)
- `POST /api/staff/support-tickets/{id}/review` (Staff initial review)
- `POST /api/staff/support-tickets/{id}/inspection` (Staff inspection)
- `POST /api/staff/support-tickets/{id}/approve-refund` (Staff approves refund < 3M)
- `POST /api/manager/support-tickets/{id}/decision` (Manager decision $\ge$ 3M)
- `POST /api/staff/support-tickets/{id}/refund-transfer` (Staff records manual transfer)
