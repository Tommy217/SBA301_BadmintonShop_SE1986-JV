import { calculateVoucher } from '../src/modules/shop/utils/voucherRules.js';
import { deleteCatalog, updateManagedOrder } from './actions/management.mjs';
import { catalogs } from '../src/modules/management/constants.js';
import { catalogView, saveCatalog } from './actions/catalog.mjs';

import {
  prepareInventoryData,
  recordOrderStockOut,
} from './actions/inventory.mjs';
import { createServer } from 'node:http';
import { readFileSync, writeFileSync, renameSync } from 'node:fs';
import { randomUUID } from 'node:crypto';
import { fileURLToPath } from 'node:url';

const dbPath = fileURLToPath(new URL('./db.json', import.meta.url));
function view(db, includeManagement = true) {
  const {
    products,
    product_variants,
    carts,
    cart_items,
    orders,
    order_items,
    payments,
    order_status_history,
    support_tickets,
    ticket_items,
    deliveries,
    invoices,
    invoice_items,
    invoice_deliveries,
    suppliers,
    goods_receipts,
    goods_receipt_items,
    inventory_movements,
    stock_audits,
    stock_audit_items,
    stock_alerts,
  } = db;
  return {
    products,
    product_variants,
    carts,
    cart_items,
    orders,
    order_items,
    payments,
    order_status_history,
    support_tickets,
    ticket_items,
    deliveries: deliveries || [],
    invoices: includeManagement ? invoices || [] : [],
    invoice_items: includeManagement ? invoice_items || [] : [],
    invoice_deliveries: includeManagement ? invoice_deliveries || [] : [],
    suppliers: includeManagement ? suppliers || [] : [],
    goods_receipts: includeManagement ? goods_receipts || [] : [],
    goods_receipt_items: includeManagement ? goods_receipt_items || [] : [],
    inventory_movements: includeManagement ? inventory_movements || [] : [],
    stock_audits: includeManagement ? stock_audits || [] : [],
    stock_audit_items: includeManagement ? stock_audit_items || [] : [],
    stock_alerts: includeManagement ? stock_alerts || [] : [],
    ...catalogView(db),
  };
}
function check(condition, message) {
  if (!condition) throw new Error(message);
}
export function mutate(db, action, liveCatalog = null) {
  const now = new Date().toISOString();
  const cart = db.carts[0];
  const allVariants = [
    ...(liveCatalog?.product_variants || []),
    ...(db.product_variants || []),
  ];
  const allProducts = [
    ...(liveCatalog?.products || []),
    ...(db.products || []),
  ];
  const variant = (id) =>
    allVariants.find((v) => {
      const product = allProducts.find((p) => p.id === v.product_id);
      return (
        v.id === id &&
        v.is_active &&
        (!product || product.is_active) &&
        !(db.brands || []).some((b) => b.id === product?.brand_id && !b.is_active) &&
        !(db.categories || []).some((c) => c.id === product?.category_id && !c.is_active)
      );
    }) || allVariants.find((v) => v.id === id);
  if (action.type === 'add') {
    const v = variant(action.variant_id);
    check(v, 'Không tìm thấy biến thể sản phẩm.');
    const item = db.cart_items.find((i) => i.variant_id === v.id);
    check((item?.quantity || 0) + 1 <= v.stock_quantity, 'Số lượng vượt quá tồn kho.');
    if (item) {
      item.quantity++;
      item.is_selected = true;
    } else
      db.cart_items.push({
        id: randomUUID(),
        cart_id: cart.id,
        variant_id: v.id,
        quantity: 1,
        is_selected: true,
        added_at: now,
      });
  } else if (action.type === 'update' || action.type === 'remove') {
    const item = db.cart_items.find((i) => i.id === action.id);
    check(item, 'Sản phẩm không còn trong giỏ.');
    if (action.type === 'remove') db.cart_items = db.cart_items.filter((i) => i.id !== item.id);
    else {
      if (action.quantity !== undefined) {
        check(
          Number.isInteger(action.quantity) &&
            action.quantity > 0 &&
            action.quantity <= variant(item.variant_id).stock_quantity,
          'Số lượng không hợp lệ hoặc vượt quá tồn kho.',
        );
        item.quantity = action.quantity;
      }
      if (typeof action.is_selected === 'boolean') item.is_selected = action.is_selected;
    }
  } else if (action.type === 'select_all') {
    check(typeof action.selected === 'boolean', 'Lựa chọn không hợp lệ.');
    db.cart_items.forEach((i) => {
      i.is_selected = action.selected;
    });
  } else if (action.type === 'checkout') {
    const receiver_name = String(action.receiver_name || '').trim();
    const receiver_phone = String(action.receiver_phone || '').trim();
    const shipping_address = String(action.shipping_address || '').trim();
    const note = String(action.note || '').trim();
    check(
      receiver_name.length > 0 && receiver_name.length <= 150,
      'Vui lòng nhập tên người nhận (tối đa 150 ký tự).',
    );
    check(/^(0|\+84)[0-9]{9}$/.test(receiver_phone), 'Số điện thoại không hợp lệ.');
    check(
      shipping_address.length >= 10 && shipping_address.length <= 500,
      'Địa chỉ cần từ 10 đến 500 ký tự.',
    );
    check(note.length <= 500, 'Ghi chú tối đa 500 ký tự.');
    const direct = action.source === 'direct';
    check(direct || action.source === 'cart', 'Vui lòng đặt hàng từ sản phẩm hoặc giỏ hàng.');
    const paymentMethod = action.payment_method || 'cod';
    check(['cod', 'prepaid'].includes(paymentMethod), 'Phương thức thanh toán không hợp lệ.');
    const gateway = paymentMethod === 'prepaid' ? action.gateway || 'mbbank' : null;
    check(
      !gateway || ['mbbank', 'momo', 'card'].includes(gateway),
      'Phương thức trả trước không hợp lệ.',
    );
    const selectedIds = action.cart_item_ids;
    if (!direct && selectedIds !== undefined) {
      check(
        Array.isArray(selectedIds) &&
          selectedIds.length > 0 &&
          new Set(selectedIds).size === selectedIds.length,
        'Chọn ít nhất một sản phẩm để đặt hàng.',
      );
      check(
        selectedIds.every((id) => db.cart_items.some((i) => i.id === id && i.cart_id === cart.id)),
        'Sản phẩm đã thay đổi trong giỏ. Vui lòng chọn lại.',
      );
    }
    const items = direct
      ? [{ variant_id: action.variant_id, quantity: action.quantity }]
      : db.cart_items.filter((i) => (selectedIds ? selectedIds.includes(i.id) : i.is_selected));
    for (const i of items)
      check(Number.isInteger(i.quantity) && i.quantity > 0, 'Số lượng không hợp lệ.');
    check(items.length, 'Chọn ít nhất một sản phẩm để đặt hàng.');
    for (const i of items)
      check(
        variant(i.variant_id) && i.quantity <= variant(i.variant_id).stock_quantity,
        'Sản phẩm đã hết hoặc không đủ tồn kho.',
      );
    const subtotal = items.reduce((s, i) => s + variant(i.variant_id).price * i.quantity, 0);
    const { voucher, discount } = calculateVoucher(db, action.voucher_code, items, cart.user_id);
    const shipping_fee = 0;
    const id = randomUUID();
    const order = {
      id,
      order_code: 'BS-' + Date.now() + '-' + id.slice(0, 4),
      user_id: cart.user_id,
      channel: 'online',
      status: 'pending',
      payment_status: 'unpaid',
      receiver_name,
      receiver_phone,
      shipping_address,
      voucher_id: voucher?.id || null,
      voucher_code: voucher?.code || null,
      subtotal,
      discount_amount: discount,
      shipping_fee,
      total_amount: subtotal - discount + shipping_fee,
      note,
      created_at: now,
      updated_at: now,
    };
    db.orders.push(order);
    if (voucher) {
      voucher.used_count++;
      (db.voucher_usages ||= []).push({
        id: randomUUID(),
        voucher_id: voucher.id,
        user_id: cart.user_id,
        order_id: order.id,
        discount_amount: discount,
        used_at: now,
      });
    }
    for (const i of items) {
      const v = variant(i.variant_id),
        p = db.products.find((p) => p.id === v.product_id);
      db.order_items.push({
        id: randomUUID(),
        order_id: id,
        variant_id: v.id,
        product_id: p.id,
        product_name: p.name,
        variant_info: v.color + ' / ' + v.size,
        unit_price: v.price,
        quantity: i.quantity,
        line_total: v.price * i.quantity,
      });
      v.stock_quantity -= i.quantity;
    }
    recordOrderStockOut(db, order, cart.user_id, now);
    db.payments.push({
      id: randomUUID(),
      order_id: id,
      method:
        paymentMethod === 'prepaid'
          ? gateway === 'momo'
            ? 'ewallet'
            : gateway === 'card'
              ? 'card'
              : 'qr'
          : 'cod',
      gateway,
      status: 'pending',
      amount: order.total_amount,
      created_at: now,
      updated_at: now,
    });
    db.order_status_history.push({
      id: randomUUID(),
      order_id: id,
      from_status: null,
      to_status: 'pending',
      changed_by: cart.user_id,
      note: 'Khách hàng đặt hàng',
      created_at: now,
    });
    if (!direct)
      db.cart_items = db.cart_items.filter((i) => !items.some((selected) => selected.id === i.id));
  } else if (action.type === 'create_support_ticket') {
    const order = db.orders.find((item) => item.id === action.order_id);
    const ticketType = String(action.ticket_type || '');
    const isUc15 = ticketType === 'REFUND';
    const reason = String(action.reason || '').trim();
    const description = String(action.description || '').trim();
    check(order, 'Không tìm thấy đơn hàng.');
    check(
      ticketType === 'REFUND',
      'Loại phiếu hỗ trợ không hợp lệ.',
    );
    check(
      reason.length > 0 && reason.length <= 300,
      'Lý do phiếu hỗ trợ là bắt buộc và tối đa 300 ký tự.',
    );
    check(description.length <= 5000, 'Mô tả phiếu hỗ trợ tối đa 5000 ký tự.');
    check(Array.isArray(db.support_tickets), 'Dữ liệu phiếu hỗ trợ không hợp lệ.');
    const id = randomUUID();
    db.support_tickets.push({
      id,
      ticket_code: 'ST-' + Date.now() + '-' + id.slice(0, 4),
      user_id: order.user_id,
      order_id: order.id,
      ticket_type: ticketType,
      status: isUc15 ? 'PENDING' : 'open',
      reason,
      description,
      refund_qr_url: action.refund_qr_url || null,
      evidence_urls: Array.isArray(action.evidence_urls) ? action.evidence_urls : [],
      refund_amount: null,
      cause: null,
      reject_reason: null,
      resolution_type: null,
      proposed_resolution: null,
      proposed_refund_amount: null,
      handled_by: null,
      reviewed_by: null,
      reviewed_at: null,
      approved_by: null,
      approved_at: null,
      resolved_at: null,
      created_at: now,
      updated_at: now,
    });
    if (Array.isArray(action.items) && action.items.length > 0) {
      if (!Array.isArray(db.ticket_items)) db.ticket_items = [];
      action.items.forEach((it) => {
        db.ticket_items.push({
          id: randomUUID(),
          ticket_id: id,
          order_item_id: it.order_item_id,
          quantity: it.quantity || 1,
        });
      });
    }
    if (isUc15) {
      if (!Array.isArray(db.ticket_status_history)) db.ticket_status_history = [];
      db.ticket_status_history.push({
        id: randomUUID(),
        ticket_id: id,
        from_status: null,
        to_status: 'PENDING',
        note: 'Khách hàng tạo phiếu hỗ trợ',
        created_at: now,
      });
    }
  } else if (action.type === 'review_support_ticket') {
    const ticket = db.support_tickets.find((item) => item.id === action.ticket_id);
    check(ticket, 'Không tìm thấy phiếu hỗ trợ.');
    const act = action.action || 'approve';
    if (!Array.isArray(db.ticket_status_history)) db.ticket_status_history = [];
    if (act === 'approve') {
      ticket.status = 'AWAITING_RETURN';
      ticket.cause = action.cause || 'STORE_FAULT';
      ticket.reviewed_by = action.actor_id || 'staff';
      ticket.reviewed_at = now;
      db.ticket_status_history.push({
        id: randomUUID(),
        ticket_id: ticket.id,
        from_status: 'PENDING',
        to_status: 'AWAITING_RETURN',
        note: action.note || 'Nhân viên đã duyệt sơ bộ tiếp nhận',
        created_at: now,
      });
    } else {
      ticket.status = 'REJECTED';
      ticket.reject_reason = action.reject_reason || 'Từ chối tiếp nhận';
      ticket.reviewed_by = action.actor_id || 'staff';
      ticket.reviewed_at = now;
      db.ticket_status_history.push({
        id: randomUUID(),
        ticket_id: ticket.id,
        from_status: 'PENDING',
        to_status: 'REJECTED',
        note: action.reject_reason,
        created_at: now,
      });
    }
    ticket.updated_at = now;
  } else if (action.type === 'inspect_support_ticket') {
    const ticket = db.support_tickets.find((item) => item.id === action.ticket_id);
    check(ticket, 'Không tìm thấy phiếu hỗ trợ.');
    if (!Array.isArray(db.ticket_inspections)) db.ticket_inspections = [];
    const result = action.result || 'PASSED';
    db.ticket_inspections.push({
      id: randomUUID(),
      ticket_id: ticket.id,
      staff_id: action.staff_id || 'staff',
      result,
      note: action.note || '',
      inspected_at: now,
    });
    if (result === 'FAILED') {
      ticket.status = 'INSPECTION_FAILED';
    } else {
      if (ticket.cause === 'CUSTOMER_PREFERENCE') {
        ticket.status = 'REJECTED';
        ticket.reject_reason = 'Sản phẩm đạt chuẩn nhưng lý do là nhu cầu cá nhân. Cửa hàng chỉ hoàn tiền khi lỗi từ phía shop.';
      } else {
        const order = db.orders.find((o) => o.id === ticket.order_id);
        const ticketItems = (db.ticket_items || []).filter((ti) => ti.ticket_id === ticket.id);
        let itemsSum = 0;
        ticketItems.forEach((ti) => {
          const oi = (db.order_items || []).find((x) => x.id === ti.order_item_id);
          if (oi) itemsSum += (Number(oi.unit_price) || 0) * (Number(ti.quantity) || 1);
        });
        const shipFee = ticket.cause === 'STORE_FAULT' && order ? Number(order.shipping_fee || 0) : 0;
        const totalRefund = itemsSum + shipFee;
        ticket.refund_amount = totalRefund;

        if (totalRefund >= 3000000) {
          ticket.status = 'PENDING_APPROVAL';
        } else {
          ticket.status = 'PENDING_TRANSFER';
        }
      }
    }
    ticket.updated_at = now;
  } else if (action.type === 'approve_refund_support_ticket') {
    const ticket = db.support_tickets.find((item) => item.id === action.ticket_id);
    check(ticket, 'Không tìm thấy phiếu hỗ trợ.');
    check((ticket.refund_amount || 0) < 3000000, 'Số tiền từ 3.000.000đ phải do Quản lý (Manager) phê duyệt.');
    ticket.status = 'PENDING_TRANSFER';
    ticket.approved_by = action.staff_id || 'staff';
    ticket.approved_at = now;
    ticket.updated_at = now;
  } else if (action.type === 'manager_decision_support_ticket') {
    const ticket = db.support_tickets.find((item) => item.id === action.ticket_id);
    check(ticket, 'Không tìm thấy phiếu hỗ trợ.');
    const decision = action.decision || 'approve';
    if (decision === 'approve') {
      ticket.status = 'PENDING_TRANSFER';
      ticket.approved_by = action.manager_id || 'manager-1';
      ticket.approved_at = now;
    } else {
      ticket.status = 'REJECTED';
      ticket.reject_reason = action.reject_reason || 'Quản lý từ chối hoàn tiền';
    }
    ticket.updated_at = now;
  } else if (action.type === 'refund_transfer_support_ticket') {
    const ticket = db.support_tickets.find((item) => item.id === action.ticket_id);
    check(ticket, 'Không tìm thấy phiếu hỗ trợ.');
    check(action.bank_reference, 'Mã giao dịch ngân hàng là bắt buộc.');
    if (!Array.isArray(db.refund_records)) db.refund_records = [];
    db.refund_records.push({
      id: randomUUID(),
      ticket_id: ticket.id,
      bank_reference: action.bank_reference,
      amount: ticket.refund_amount || 0,
      note: action.note || '',
      transferred_at: now,
    });
    ticket.status = 'REFUNDED';
    ticket.resolved_at = now;
    ticket.updated_at = now;
  } else if (action.type === 'cancel_support_ticket') {
    const ticket = db.support_tickets.find((item) => item.id === action.ticket_id);
    check(ticket, 'Không tìm thấy phiếu hỗ trợ.');
    check(ticket.status === 'PENDING', 'Chỉ có thể hủy phiếu khi đang ở trạng thái chờ duyệt (PENDING).');
    ticket.status = 'CANCELLED';
    ticket.updated_at = now;
  } else if (action.type === 'update_support_ticket') {
    const ticket = db.support_tickets.find((item) => item.id === action.ticket_id);
    const status = String(action.status || '');
    const resolutionType = action.resolution_type ?? null;
    const proposedResolution = String(action.proposed_resolution || '').trim();
    const handledBy = String(action.handled_by || '').trim();
    const refundAmount = action.proposed_refund_amount ?? null;
    check(ticket, 'Không tìm thấy phiếu hỗ trợ.');
    check(
      ['under_review', 'approved', 'rejected', 'resolved', 'cancelled'].includes(status),
      'Trạng thái xử lý phiếu hỗ trợ không hợp lệ.',
    );
    check(
      resolutionType === null || ['refund', 'repair', 'none'].includes(resolutionType),
      'Hướng xử lý phiếu hỗ trợ không hợp lệ.',
    );
    check(proposedResolution.length <= 5000, 'Phản hồi phiếu hỗ trợ tối đa 5000 ký tự.');
    check(
      !['approved', 'rejected', 'resolved'].includes(status) || proposedResolution.length > 0,
      'Vui lòng nhập phản hồi trước khi chốt kết quả xử lý.',
    );
    check(
      refundAmount === null ||
        (typeof refundAmount === 'number' && Number.isFinite(refundAmount) && refundAmount >= 0),
      'Số tiền hoàn dự kiến không hợp lệ.',
    );
    check(handledBy.length <= 200, 'Thông tin nhân viên xử lý không hợp lệ.');
    ticket.status = status;
    ticket.resolution_type = resolutionType;
    ticket.proposed_resolution = proposedResolution || null;
    ticket.proposed_refund_amount = resolutionType === 'refund' ? refundAmount : null;
    ticket.handled_by = handledBy || ticket.handled_by || null;
    ticket.reviewed_at = ticket.reviewed_at || now;
    ticket.resolved_at = status === 'resolved' ? now : null;
    ticket.updated_at = now;
  } else if (action.type === 'start_payment') {
    const order = db.orders.find((o) => o.id === action.order_id);
    check(
      order && order.status !== 'cancelled' && order.payment_status === 'unpaid',
      'Đơn hàng không thể thanh toán.',
    );
    const payment = db.payments.find((p) => p.order_id === order.id);
    check(payment && payment.method !== 'cod', 'Đơn hàng này thanh toán khi nhận hàng.');
    check(
      ['mbbank', 'momo', 'card'].includes(action.gateway),
      'Phương thức trả trước không hợp lệ.',
    );
    payment.method =
      action.gateway === 'momo' ? 'ewallet' : action.gateway === 'card' ? 'card' : 'qr';
    payment.gateway = action.gateway;
    payment.updated_at = now;
  } else if (action.type === 'sync_carrier_status') {
    if (!Array.isArray(db.deliveries)) db.deliveries = [];
    const delivery = db.deliveries.find((d) => d.id === action.delivery_id);
    check(delivery, 'Không tìm thấy vận đơn.');
    if (delivery.status === 'pending_pickup') {
      delivery.status = 'in_transit';
      delivery.carrier_status_text = 'Đã lấy hàng - Đang luân chuyển qua kho trung chuyển';
      delivery.shipped_at = delivery.shipped_at || now;
      delivery.tracking_logs.unshift({
        id: 'log-' + randomUUID().slice(0, 8),
        timestamp: now,
        status: 'in_transit',
        title: 'ĐVVC lấy hàng thành công',
        location: 'Kho lấy hàng của Shop',
        note: `Bưu tá ${delivery.carrier.toUpperCase()} đã tiếp nhận kiện hàng và đưa về kho trung chuyển.`,
      });
    } else if (delivery.status === 'in_transit') {
      delivery.status = 'out_for_delivery';
      delivery.carrier_status_text = 'Bưu tá ĐVVC đang trên đường đi giao cho người nhận';
      delivery.shipper_name = delivery.shipper_name || 'Nguyễn Văn Tuấn (Bưu tá ĐVVC)';
      delivery.shipper_phone = delivery.shipper_phone || '0987.654.321';
      delivery.tracking_logs.unshift({
        id: 'log-' + randomUUID().slice(0, 8),
        timestamp: now,
        status: 'out_for_delivery',
        title: 'Bưu tá đang phát hàng',
        location: 'Bưu cục phát gần nhất',
        note: `Bưu tá ${delivery.shipper_name} (${delivery.shipper_phone}) đang đi giao.`,
      });
    } else if (delivery.status === 'out_for_delivery') {
      delivery.status = 'delivered';
      delivery.carrier_status_text = 'Giao hàng thành công - Khách hàng đã nhận và ký xác nhận';
      delivery.delivered_at = now;
      delivery.tracking_logs.unshift({
        id: 'log-' + randomUUID().slice(0, 8),
        timestamp: now,
        status: 'delivered',
        title: 'Giao thành công',
        location: delivery.shipping_address,
        note: 'Khách hàng đã nhận kiện hàng nguyên vẹn và hoàn tất đối soát.',
      });
      const order = db.orders.find((o) => o.id === delivery.order_id);
      if (order) {
        order.status = 'completed';
        if (order.payment_status === 'unpaid') {
          order.payment_status = 'paid';
          const payment = db.payments.find((p) => p.order_id === order.id);
          if (payment) payment.status = 'completed';
        }
        db.order_status_history.push({
          id: randomUUID(),
          order_id: order.id,
          from_status: 'shipping',
          to_status: 'completed',
          changed_by: 'system_carrier_sync',
          note: `ĐVVC ${delivery.carrier.toUpperCase()} xác nhận giao thành công mã ${delivery.tracking_number}`,
          created_at: now,
        });
      }
    }
    delivery.last_synced_at = now;
    delivery.updated_at = now;
  } else if (action.type === 'create_shipment') {
    if (!Array.isArray(db.deliveries)) db.deliveries = [];
    const order = db.orders.find((o) => o.id === action.order_id);
    check(order, 'Không tìm thấy đơn hàng.');
    const existing = db.deliveries.find((d) => d.order_id === order.id);
    check(!existing, 'Đơn hàng này đã được tạo vận đơn trước đó.');
    const carrier = String(action.carrier || 'fastship').toLowerCase();
    check(
      ['fastship', 'ghn', 'ghtk', 'viettelpost'].includes(carrier),
      'Đơn vị vận chuyển không hợp lệ.',
    );
    const serviceType = String(action.service_type || 'standard');
    const trackingNumber = `${carrier.toUpperCase()}-${Date.now().toString().slice(-6)}${order.id.slice(0, 4).toUpperCase()}`;
    const id = 'del-' + randomUUID().slice(0, 8);
    const newDelivery = {
      id,
      order_id: order.id,
      order_code: order.order_code,
      tracking_number: trackingNumber,
      carrier,
      service_type: serviceType,
      shipping_fee: order.shipping_fee || 0,
      cod_amount: order.payment_status === 'paid' ? 0 : order.total_amount,
      status: 'pending_pickup',
      carrier_status_text: `Đã tạo vận đơn trên hệ thống ${carrier.toUpperCase()} - Chờ bưu tá lấy hàng`,
      receiver_name: order.receiver_name,
      receiver_phone: order.receiver_phone,
      shipping_address: order.shipping_address,
      note: action.note || order.note || '',
      shipper_name: null,
      shipper_phone: null,
      estimated_delivery_date: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString(),
      shipped_at: null,
      delivered_at: null,
      last_synced_at: now,
      created_at: now,
      updated_at: now,
      tracking_logs: [
        {
          id: 'log-' + randomUUID().slice(0, 8),
          timestamp: now,
          status: 'pending_pickup',
          title: 'Tạo vận đơn thành công',
          location: `Cổng kết nối API ${carrier.toUpperCase()}`,
          note: `Shop đã đẩy đơn sang ${carrier.toUpperCase()} và nhận mã vận đơn ${trackingNumber}. Đang chờ bưu tá đến lấy.`,
        },
      ],
    };
    db.deliveries.push(newDelivery);
    order.status = 'shipping';
    db.order_status_history.push({
      id: randomUUID(),
      order_id: order.id,
      from_status: 'pending',
      to_status: 'shipping',
      changed_by: cart.user_id,
      note: `Tạo vận đơn ${carrier.toUpperCase()} (${trackingNumber})`,
      created_at: now,
    });
  } else throw new Error('Thao tác không hợp lệ.');
  cart.updated_at = now;
  return db;
}
export function createApiHandler(databasePath = dbPath, backendUrl = process.env.VITE_BACKEND_URL || 'http://localhost:8080') {
  return async (req, res) => {
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    if (req.url !== '/api/shop') {
      res.writeHead(404);
      res.end(JSON.stringify({ message: 'Không tìm thấy API.' }));
      return;
    }
    try {
      if (!['GET', 'POST'].includes(req.method)) {
        res.writeHead(405);
        res.end('{}');
        return;
      }
      let body = '';
      for await (const chunk of req) {
        body += chunk;
        check(body.length < 25 * 1024 * 1024, 'Yêu cầu quá lớn.');
      }
      // Read after receiving the body; synchronous mutation and rename keep each write together.
      const db = JSON.parse(readFileSync(databasePath, 'utf8'));
      const token = req.headers.authorization?.replace(/^Bearer /, '');
      let sessionUser = null;
      if (token) {
        const identity = await fetch(backendUrl + '/api/auth/me', {
          headers: { Authorization: 'Bearer ' + token }, signal: AbortSignal.timeout(5000),
        });
        if (identity.ok) sessionUser = await identity.json();
        else if (identity.status !== 401) throw new Error('Không xác minh được phiên đăng nhập với backend.');
      }
      const authenticated = Boolean(sessionUser);
      const canViewManagement = sessionUser?.roles?.some((role) =>
        ['ADMIN', 'MANAGER', 'STAFF'].includes(role.toUpperCase()),
      );
      if ((token && !authenticated) || (req.method === 'POST' && !authenticated)) {
        res.writeHead(401);
        res.end(
          JSON.stringify({
            message: 'Vui lòng đăng nhập để sử dụng chức năng này.',
          }),
        );
        return;
      }
      if (req.method === 'GET' && !authenticated) {
        res.end(
          JSON.stringify({
            products: db.products,
            product_variants: db.product_variants,
            brands: db.brands || [],
            categories: db.categories || [],
            product_images: db.product_images || [],
            carts: [],
            cart_items: [],
            orders: [],
            order_items: [],
            payments: [],
            order_status_history: [],
            support_tickets: [],
            ticket_items: [],
            deliveries: [],
            invoices: [],
            invoice_items: [],
            invoice_deliveries: [],
          }),
        );
        return;
      }
      const inventoryChanged = prepareInventoryData(db);
      if (inventoryChanged) {
        writeFileSync(databasePath + '.tmp', JSON.stringify(db, null, 2) + '\n');
        renameSync(databasePath + '.tmp', databasePath);
      }
      if (req.method === 'POST') {
        const action = JSON.parse(body);
        if (action.type === 'manager_decision_support_ticket' && !sessionUser.roles.some(role => ['ADMIN', 'MANAGER'].includes(role))) {
          res.writeHead(403);
          res.end(JSON.stringify({ message: 'Chỉ quản lý được quyết định xử lý phiếu.' }));
          return;
        }
        const invoiceAction = action.type?.startsWith('invoice_');
        const inventoryAction = action.type?.startsWith('inventory_');
        if (
          [
            'review_support_ticket',
            'inspect_support_ticket',
            'approve_refund_support_ticket',
            'manager_decision_support_ticket',
            'refund_transfer_support_ticket',
            'create_shipment',
            'sync_carrier_status',
            'update_support_ticket',
            'catalog_save',
            'catalog_delete',
            'manager_update_order',
          ].includes(action.type) ||
          invoiceAction ||
          inventoryAction
        ) {
          const actor = sessionUser;
          const allowedRoles = ['ADMIN', 'MANAGER', 'STAFF'];
          if (!actor?.roles?.some((role) => allowedRoles.includes(role.toUpperCase()))) {
            res.writeHead(403);
            res.end(
              JSON.stringify({
                message: 'Bạn không có quyền quản lý dữ liệu cửa hàng.',
              }),
            );
            return;
          }
        }
        if (action.type === 'catalog_save' || action.type === 'catalog_delete') {
          throw new Error('Dữ liệu catalog được quản lý trực tiếp qua Spring Boot Backend (http://localhost:8080/api). Không còn lưu qua mock server.');
        } else if (action.type === 'manager_update_order') {
          const actor = sessionUser;
          updateManagedOrder(db, action, actor.id || actor.email);
        } else if (action.type?.startsWith('invoice_')) {
          throw new Error('Quản lý hóa đơn đã chuyển sang http://localhost:8080/api/invoices.');
        } else if (action.type?.startsWith('inventory_')) {
          throw new Error('Quản lý kho đã chuyển sang http://localhost:8080/api/inventory.');
        } else {
          let liveCatalog = null;
          try {
                const [variantsRes, productsRes] = await Promise.all([
              fetch(`${backendUrl}/api/products/variants`),
              fetch(`${backendUrl}/api/products`),
            ]);
            if (variantsRes.ok && productsRes.ok) {
              liveCatalog = {
                product_variants: await variantsRes.json(),
                products: await productsRes.json(),
              };
            }
          } catch {}
          mutate(db, action, liveCatalog);
        }
        prepareInventoryData(db);
        writeFileSync(databasePath + '.tmp', JSON.stringify(db, null, 2) + '\n');
        renameSync(databasePath + '.tmp', databasePath);
      }
      res.end(JSON.stringify(view(db, canViewManagement)));
    } catch (error) {
      res.writeHead(400);
      res.end(JSON.stringify({ message: error.message }));
    }
  };
}
export function startServer(port = 3000, databasePath = dbPath) {
  const server = createServer(createApiHandler(databasePath));
  server.listen(port, '127.0.0.1', () =>
    console.log('Mock API: http://127.0.0.1:' + server.address().port + '/api/shop'),
  );
  return server;
}
if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1])
  startServer(Number(process.env.PORT || 3000));
