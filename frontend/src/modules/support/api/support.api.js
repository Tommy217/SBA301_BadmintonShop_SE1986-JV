import { sendShopAction } from '@/app/providers/shop/shop.api';

export const supportApi = {
  // Customer APIs
  createTicket: (payload) => sendShopAction({ type: 'create_support_ticket', ...payload }),
  cancelTicket: (ticketId) => sendShopAction({ type: 'cancel_support_ticket', ticket_id: ticketId }),

  // Staff APIs
  staffReviewTicket: ({ ticketId, action, cause, rejectReason, note }) =>
    sendShopAction({
      type: 'review_support_ticket',
      ticket_id: ticketId,
      action, // 'approve' | 'reject'
      cause, // 'STORE_FAULT' | 'CUSTOMER_PREFERENCE'
      reject_reason: rejectReason,
      note,
    }),

  staffInspectTicket: ({ ticketId, result, note }) =>
    sendShopAction({
      type: 'inspect_support_ticket',
      ticket_id: ticketId,
      result, // 'PASSED' | 'FAILED'
      note,
    }),

  staffApproveRefund: ({ ticketId, note }) =>
    sendShopAction({
      type: 'approve_refund_support_ticket',
      ticket_id: ticketId,
      note,
    }),

  staffRecordRefundTransfer: ({ ticketId, bankReference, note }) =>
    sendShopAction({
      type: 'refund_transfer_support_ticket',
      ticket_id: ticketId,
      bank_reference: bankReference,
      note,
    }),

  // Manager APIs
  managerDecision: ({ ticketId, decision, rejectReason, note }) =>
    sendShopAction({
      type: 'manager_decision_support_ticket',
      ticket_id: ticketId,
      decision, // 'approve' | 'reject'
      reject_reason: rejectReason,
      note,
    }),

  updateThreshold: (threshold) =>
    sendShopAction({
      type: 'update_refund_threshold',
      threshold,
    }),

  updatePaymentTimeout: (timeoutMinutes) =>
    sendShopAction({
      type: 'update_payment_timeout',
      timeout_minutes: timeoutMinutes,
    }),
};
