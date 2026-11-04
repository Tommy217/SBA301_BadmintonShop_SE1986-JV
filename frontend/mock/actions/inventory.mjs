import { randomUUID } from 'node:crypto';

const check = (condition, message) => {
  if (!condition) throw new Error(message);
};
const INT_MAX = 2_147_483_647;
const DECIMAL_12_MAX = 999_999_999_999;
const DECIMAL_14_MAX = 99_999_999_999_999;
const id = () => randomUUID();
const userId = (value) => {
  if (value == null || value === '') return null;
  const normalized = String(value);
  if (!/^\d+$/.test(normalized)) return null;
  const number = Number(normalized);
  return Number.isSafeInteger(number) ? number : null;
};
const validText = (value, length, label, required = false) => {
  const result = String(value ?? '').trim();
  check(!required || result.length > 0, `Vui lòng nhập ${label}.`);
  check(result.length <= length, `${label} tối đa ${length} ký tự.`);
  return result || null;
};

export function prepareInventoryData(db) {
  const wasMissing = [
    'suppliers',
    'goods_receipts',
    'goods_receipt_items',
    'inventory_movements',
    'stock_audits',
    'stock_audit_items',
    'stock_alerts',
  ].some((table) => !Array.isArray(db[table]));
  db.suppliers ||= [];
  db.goods_receipts ||= [];
  db.goods_receipt_items ||= [];
  db.inventory_movements ||= [];
  db.stock_audits ||= [];
  db.stock_audit_items ||= [];
  db.stock_alerts ||= [];
  return syncStockAlerts(db) || wasMissing;
}

function syncStockAlerts(db) {
  let changed = false;
  const now = new Date().toISOString();
  for (const variant of db.product_variants || []) {
    const activeAlert = db.stock_alerts.find(
      (alert) => String(alert.variant_id) === String(variant.id) && alert.status === 'open',
    );
    const threshold = Number(variant.low_stock_threshold ?? 5);
    const needsAlert = variant.is_active && Number(variant.stock_quantity) <= threshold;
    if (needsAlert && !activeAlert) {
      db.stock_alerts.push({
        id: id(),
        variant_id: variant.id,
        stock_at_alert: Number(variant.stock_quantity),
        threshold,
        status: 'open',
        created_at: now,
        resolved_at: null,
      });
      changed = true;
    } else if (activeAlert && !needsAlert) {
      activeAlert.status = 'resolved';
      activeAlert.resolved_at = now;
      changed = true;
    } else if (
      activeAlert &&
      (Number(activeAlert.stock_at_alert) !== Number(variant.stock_quantity) ||
        Number(activeAlert.threshold) !== threshold)
    ) {
      activeAlert.stock_at_alert = Number(variant.stock_quantity);
      activeAlert.threshold = threshold;
      changed = true;
    }
  }
  return changed;
}

function recordMovement(db, variant, movement, actor, now) {
  db.inventory_movements.push({
    id: id(),
    variant_id: variant.id,
    movement_type: movement.type,
    quantity: movement.quantity,
    balance_after: Number(variant.stock_quantity),
    ref_type: movement.ref_type ?? null,
    ref_id: movement.ref_id ?? null,
    created_by: userId(actor),
    note: movement.note ?? null,
    created_at: now,
  });
}

function receiveGoods(db, action, actor, now) {
  const supplierId =
    action.supplier_id == null || action.supplier_id === '' ? null : Number(action.supplier_id);
  check(
    supplierId === null || db.suppliers.some((supplier) => supplier.id === supplierId),
    'Nhà cung cấp không hợp lệ.',
  );
  check(
    Array.isArray(action.items) && action.items.length > 0,
    'Chọn ít nhất một SKU để nhập kho.',
  );
  const seenVariants = new Set();
  const items = action.items.map((item) => {
    const variant = db.product_variants.find(
      (entry) => String(entry.id) === String(item.variant_id),
    );
    const quantity = Number(item.quantity);
    const unitCost = Number(item.unit_cost);
    check(variant, 'Không tìm thấy SKU cần nhập.');
    check(!seenVariants.has(String(variant.id)), 'Không nhập trùng SKU trong cùng phiếu.');
    seenVariants.add(String(variant.id));
    check(
      Number.isSafeInteger(quantity) && quantity > 0 && quantity <= INT_MAX,
      'Số lượng nhập phải là số nguyên dương hợp lệ.',
    );
    check(
      Number.isSafeInteger(unitCost) && unitCost >= 0 && unitCost <= DECIMAL_12_MAX,
      'Đơn giá nhập vượt phạm vi DECIMAL(12,0).',
    );
    check(
      Number.isSafeInteger(Number(variant.stock_quantity) + quantity) &&
        Number(variant.stock_quantity) + quantity <= INT_MAX,
      'Số lượng tồn kho vượt giới hạn.',
    );
    return { variant, quantity, unit_cost: unitCost };
  });
  const note = validText(action.note, 300, 'ghi chú');
  const receiptId = id();
  const receipt = {
    id: receiptId,
    receipt_code: `GR-${Date.now()}-${receiptId.slice(0, 6).toUpperCase()}`,
    supplier_id: supplierId,
    received_by: actor ?? null,
    total_cost: items.reduce((sum, item) => sum + item.quantity * item.unit_cost, 0),
    note,
    received_at: now,
  };
  check(
    Number.isSafeInteger(receipt.total_cost) && receipt.total_cost <= DECIMAL_14_MAX,
    'Tổng giá trị phiếu nhập vượt phạm vi DECIMAL(14,0).',
  );
  db.goods_receipts.push(receipt);
  for (const item of items) {
    const receiptItem = {
      id: id(),
      receipt_id: receiptId,
      variant_id: item.variant.id,
      quantity: item.quantity,
      unit_cost: item.unit_cost,
    };
    db.goods_receipt_items.push(receiptItem);
    item.variant.stock_quantity += item.quantity;
    recordMovement(
      db,
      item.variant,
      {
        type: 'stock_in',
        quantity: item.quantity,
        ref_type: 'goods_receipt',
        ref_id: receiptId,
        note,
      },
      actor,
      now,
    );
  }
}

function startAudit(db, action, actor, now) {
  const auditId = id();
  const audit = {
    id: auditId,
    audit_code: `AUD-${Date.now()}-${auditId.slice(0, 6).toUpperCase()}`,
    status: 'draft',
    conducted_by: actor ?? null,
    note: validText(action.note, 300, 'ghi chú'),
    started_at: now,
    completed_at: null,
  };
  db.stock_audits.push(audit);
  for (const variant of db.product_variants || []) {
    db.stock_audit_items.push({
      id: id(),
      audit_id: auditId,
      variant_id: variant.id,
      system_qty: Number(variant.stock_quantity),
      counted_qty: Number(variant.stock_quantity),
      diff_qty: 0,
      note: null,
    });
  }
}

function updateAudit(db, action) {
  const audit = db.stock_audits.find((item) => String(item.id) === String(action.audit_id));
  check(audit?.status === 'draft', 'Chỉ cập nhật được phiếu kiểm kê nháp.');
  check(Array.isArray(action.items), 'Danh sách số lượng kiểm kê không hợp lệ.');
  const auditItems = db.stock_audit_items.filter(
    (item) => String(item.audit_id) === String(audit.id),
  );
  const updates = action.items.map((input) => {
    const item = auditItems.find((entry) => String(entry.variant_id) === String(input.variant_id));
    const counted = Number(input.counted_qty);
    check(item, 'SKU không thuộc phiếu kiểm kê này.');
    check(
      Number.isSafeInteger(counted) && counted >= 0 && counted <= INT_MAX,
      'Số lượng thực đếm phải là số nguyên không âm hợp lệ.',
    );
    return {
      item,
      counted,
      note: validText(input.note, 200, 'ghi chú SKU'),
    };
  });
  for (const update of updates) {
    update.item.counted_qty = update.counted;
    update.item.diff_qty = update.counted - Number(update.item.system_qty);
    update.item.note = update.note;
  }
}

function completeAudit(db, action, actor, now) {
  const audit = db.stock_audits.find((item) => String(item.id) === String(action.audit_id));
  check(audit?.status === 'draft', 'Chỉ hoàn tất được phiếu kiểm kê nháp.');
  const entries = db.stock_audit_items.filter((item) => String(item.audit_id) === String(audit.id));
  const changes = entries.map((entry) => {
    const variant = db.product_variants.find(
      (item) => String(item.id) === String(entry.variant_id),
    );
    check(variant, 'Một SKU trong phiếu kiểm kê không còn tồn tại.');
    check(
      Number(variant.stock_quantity) === Number(entry.system_qty),
      `Tồn kho SKU ${variant.sku} đã thay đổi sau khi bắt đầu kiểm kê. Hãy tạo phiếu kiểm kê mới.`,
    );
    return { entry, variant, difference: Number(entry.counted_qty) - Number(entry.system_qty) };
  });
  for (const { entry, variant, difference } of changes) {
    if (!difference) continue;
    variant.stock_quantity = Number(entry.counted_qty);
    recordMovement(
      db,
      variant,
      {
        type: 'audit_adjust',
        quantity: difference,
        ref_type: 'stock_audit',
        ref_id: audit.id,
        note: entry.note || audit.note || 'Điều chỉnh theo kết quả kiểm kê',
      },
      actor,
      now,
    );
  }
  audit.status = 'completed';
  audit.completed_at = now;
}

function manualAdjust(db, action, actor, now) {
  const variant = db.product_variants.find((item) => String(item.id) === String(action.variant_id));
  check(variant, 'Không tìm thấy SKU.');
  const nextQuantity = Number(action.counted_qty);
  check(
    Number.isSafeInteger(nextQuantity) && nextQuantity >= 0 && nextQuantity <= INT_MAX,
    'Tồn kho mới phải là số nguyên không âm hợp lệ.',
  );
  const note = validText(action.note, 300, 'lý do điều chỉnh', true);
  const difference = nextQuantity - Number(variant.stock_quantity);
  check(difference !== 0, 'Tồn kho mới phải khác số lượng hiện tại.');
  variant.stock_quantity = nextQuantity;
  recordMovement(
    db,
    variant,
    {
      type: 'manual_adjust',
      quantity: difference,
      ref_type: 'manual',
      note,
    },
    actor,
    now,
  );
}

export function manageInventory(db, action, actor = null) {
  prepareInventoryData(db);
  const now = new Date().toISOString();
  if (action.type === 'inventory_receive') {
    receiveGoods(db, action, actor, now);
  } else if (action.type === 'inventory_audit_start') {
    startAudit(db, action, actor, now);
  } else if (action.type === 'inventory_audit_update') {
    updateAudit(db, action);
  } else if (action.type === 'inventory_audit_complete') {
    completeAudit(db, action, actor, now);
  } else if (action.type === 'inventory_audit_cancel') {
    const audit = db.stock_audits.find((item) => String(item.id) === String(action.audit_id));
    check(audit?.status === 'draft', 'Chỉ hủy được phiếu kiểm kê nháp.');
    audit.status = 'cancelled';
  } else if (action.type === 'inventory_adjust') {
    manualAdjust(db, action, actor, now);
  } else {
    throw new Error('Thao tác kho không hợp lệ.');
  }
  syncStockAlerts(db);
}

export function recordOrderStockOut(db, order, actor, now) {
  db.inventory_movements ||= [];
  for (const item of (db.order_items || []).filter(
    (entry) => String(entry.order_id) === String(order.id),
  )) {
    const variant = db.product_variants.find(
      (entry) => String(entry.id) === String(item.variant_id),
    );
    if (!variant) continue;
    recordMovement(
      db,
      variant,
      {
        type: 'stock_out',
        quantity: -Number(item.quantity),
        ref_type: 'order',
        ref_id: order.id,
        note: `Xuất kho theo đơn ${order.order_code}`,
      },
      actor,
      now,
    );
  }
}

export function recordOrderStockReturn(db, order, item, actor, now) {
  db.inventory_movements ||= [];
  const variant = db.product_variants.find((entry) => String(entry.id) === String(item.variant_id));
  if (!variant) return;
  recordMovement(
    db,
    variant,
    {
      type: 'return_in',
      quantity: Number(item.quantity),
      ref_type: 'order',
      ref_id: order.id,
      note: `Nhập trả kho khi đơn ${order.order_code} chuyển sang ${order.status}`,
    },
    actor,
    now,
  );
}

export function recordInitialStock(db, variant, actor) {
  if (Number(variant.stock_quantity) <= 0) return;
  db.inventory_movements ||= [];
  recordMovement(
    db,
    variant,
    {
      type: 'manual_adjust',
      quantity: Number(variant.stock_quantity),
      ref_type: 'manual',
      note: 'Tồn kho ban đầu khi tạo SKU',
    },
    actor,
    new Date().toISOString(),
  );
}
