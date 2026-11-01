CREATE DATABASE badminton_shop COLLATE Vietnamese_CI_AS;
GO
USE badminton_shop;
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- =====================================================================
-- 1-3. USERS, AUTHENTICATION, ROLES & PERMISSIONS
-- =====================================================================
CREATE TABLE users (
  id             BIGINT IDENTITY(1,1) PRIMARY KEY,
  email          VARCHAR(190) NOT NULL UNIQUE,
  phone          VARCHAR(20) NULL,
  password_hash  VARCHAR(255) NOT NULL,
  full_name      NVARCHAR(150) NOT NULL,
  gender         VARCHAR(20) NULL CHECK (gender IN ('male','female','other')),
  birth_date     DATE NULL,
  avatar_url     VARCHAR(500),
  status         VARCHAR(20) NOT NULL DEFAULT 'active' CHECK (status IN ('active','inactive','locked')), -- Deactivate User
  email_verified_at DATETIME2(0) NULL,
  last_login_at  DATETIME2(0) NULL,
  created_at     DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  updated_at     DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  INDEX idx_users_status (status)
);

CREATE TABLE password_reset_tokens (               -- Forgot Password
  id          BIGINT IDENTITY(1,1) PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  token_hash  VARCHAR(255) NOT NULL,
  expires_at  DATETIME2(0) NOT NULL,
  used_at     DATETIME2(0) NULL,
  created_at  DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_prt_user (user_id)
);

CREATE TABLE addresses (                            -- Store/update customer profile
  id            BIGINT IDENTITY(1,1) PRIMARY KEY,
  user_id       BIGINT NOT NULL,
  receiver_name NVARCHAR(150) NOT NULL,
  phone         VARCHAR(20)  NOT NULL,
  province      NVARCHAR(100) NOT NULL,
  district      NVARCHAR(100) NOT NULL,
  ward          NVARCHAR(100),
  street        NVARCHAR(255) NOT NULL,
  is_default    BIT NOT NULL DEFAULT 0,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE roles (                                -- Customer, Staff, Manager, Admin
  id    INT IDENTITY(1,1) PRIMARY KEY,
  code  VARCHAR(30) NOT NULL UNIQUE,
  name  NVARCHAR(80) NOT NULL
);

CREATE TABLE permissions (
  id    INT IDENTITY(1,1) PRIMARY KEY,
  code  VARCHAR(80) NOT NULL UNIQUE,                -- vd: order.confirm, product.edit
  name  NVARCHAR(150) NOT NULL
);

CREATE TABLE role_permissions (                     -- Edit/revoke permissions
  role_id       INT NOT NULL,
  permission_id INT NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  FOREIGN KEY (role_id)       REFERENCES roles(id)       ON DELETE CASCADE,
  FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE TABLE user_roles (                           -- Assign roles
  user_id BIGINT NOT NULL,
  role_id INT NOT NULL,
  assigned_by BIGINT NULL,
  assigned_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  PRIMARY KEY (user_id, role_id),
  FOREIGN KEY (user_id)     REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (role_id)     REFERENCES roles(id) ON DELETE CASCADE,
  FOREIGN KEY (assigned_by) REFERENCES users(id)
);

CREATE TABLE audit_logs (                           -- View activity logs / System Monitoring
  id          BIGINT IDENTITY(1,1) PRIMARY KEY,
  user_id     BIGINT NULL,
  action      VARCHAR(80) NOT NULL,                 -- login, order.confirm, user.deactivate...
  entity_type VARCHAR(50),
  entity_id   BIGINT,
  detail      NVARCHAR(MAX) NULL CHECK (ISJSON(detail) = 1),
  ip_address  VARCHAR(45),
  user_agent  VARCHAR(300),
  created_at  DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
  INDEX idx_audit_user_time (user_id, created_at),
  INDEX idx_audit_entity (entity_type, entity_id)
);

-- =====================================================================
-- 4. CATALOG: BRAND, CATEGORY, PRODUCT, VOUCHER
-- =====================================================================
CREATE TABLE brands (
  id        INT IDENTITY(1,1) PRIMARY KEY,
  name      NVARCHAR(100) NOT NULL UNIQUE,
  slug      VARCHAR(120) NOT NULL UNIQUE,
  logo_url  VARCHAR(500),
  is_active BIT NOT NULL DEFAULT 1
);

CREATE TABLE categories (
  id        INT IDENTITY(1,1) PRIMARY KEY,
  parent_id INT NULL,
  name      NVARCHAR(100) NOT NULL,
  slug      VARCHAR(120) NOT NULL UNIQUE,
  is_active BIT NOT NULL DEFAULT 1,
  FOREIGN KEY (parent_id) REFERENCES categories(id)
);

CREATE TABLE products (
  id           BIGINT IDENTITY(1,1) CONSTRAINT PK_products PRIMARY KEY,
  category_id  INT NOT NULL,
  brand_id     INT NOT NULL,
  product_type VARCHAR(20) NOT NULL CHECK (product_type IN ('racket','shoes','apparel')),
  name         NVARCHAR(255) NOT NULL,
  slug         VARCHAR(280) NOT NULL UNIQUE,         -- dùng cho Share product (link)
  description  NVARCHAR(MAX),
  base_price   DECIMAL(12,0) NOT NULL,
  is_active    BIT NOT NULL DEFAULT 1,
  created_by   BIGINT NULL,
  created_at   DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  updated_at   DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (category_id) REFERENCES categories(id),
  FOREIGN KEY (brand_id)    REFERENCES brands(id),
  FOREIGN KEY (created_by)  REFERENCES users(id),
  INDEX idx_products_type_brand (product_type, brand_id)
);

CREATE TABLE racket_specs (
  product_id      BIGINT PRIMARY KEY,
  balance_type    VARCHAR(20) NOT NULL CHECK (balance_type IN ('head_heavy','even','head_light')),
  flex            VARCHAR(20) NOT NULL CHECK (flex IN ('stiff','medium','flexible')),
  play_style      VARCHAR(20) NOT NULL CHECK (play_style IN ('attack','defense','all_round')),
  max_tension_lbs DECIMAL(4,1),
  material        NVARCHAR(150),
  shaft_diameter  DECIMAL(3,1),
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE shoe_specs (
  product_id  BIGINT PRIMARY KEY,
  gender      VARCHAR(20) NOT NULL CHECK (gender IN ('male','female','unisex')),
  sole_type   NVARCHAR(100),
  technology  NVARCHAR(200),
  foot_width  VARCHAR(20) NOT NULL DEFAULT 'regular' CHECK (foot_width IN ('narrow','regular','wide')),
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE apparel_specs (
  product_id   BIGINT PRIMARY KEY,
  gender       VARCHAR(20) NOT NULL CHECK (gender IN ('male','female','unisex','kids')),
  apparel_kind VARCHAR(20) NOT NULL CHECK (apparel_kind IN ('shirt','shorts','skirt','dress','jacket','socks','other')),
  material     NVARCHAR(150),
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Vợt: size='4U-G5' | Giày: size='42' | Áo/quần: size='M'
CREATE TABLE product_variants (
  id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
  product_id          BIGINT NOT NULL,
  sku                 VARCHAR(60) NOT NULL UNIQUE,
  barcode             VARCHAR(60) NULL,
  color               NVARCHAR(50),
  size                VARCHAR(30),
  price               DECIMAL(12,0) NOT NULL,
  cost_price          DECIMAL(12,0) NOT NULL DEFAULT 0,
  stock_quantity      INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
  low_stock_threshold INT NOT NULL DEFAULT 5,        -- Low-stock warning
  is_active           BIT NOT NULL DEFAULT 1,
  CONSTRAINT uq_variant UNIQUE (product_id, color, size),
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE product_images (
  id         BIGINT IDENTITY(1,1) PRIMARY KEY,
  product_id BIGINT NOT NULL,
  variant_id BIGINT NULL,
  url        VARCHAR(500) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);

CREATE TABLE product_price_history (                -- Pricing (phục vụ báo cáo giá/khuyến mãi)
  id          BIGINT IDENTITY(1,1) PRIMARY KEY,
  variant_id  BIGINT NOT NULL,
  old_price   DECIMAL(12,0) NOT NULL,
  new_price   DECIMAL(12,0) NOT NULL,
  changed_by  BIGINT NULL,
  changed_at  DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
  FOREIGN KEY (changed_by) REFERENCES users(id)
);

CREATE TABLE vouchers (
  id             INT IDENTITY(1,1) PRIMARY KEY,
  code           VARCHAR(40) NOT NULL UNIQUE,
  name           NVARCHAR(150) NOT NULL,
  description    NVARCHAR(500),
  discount_type  VARCHAR(20) NOT NULL CHECK (discount_type IN ('percent','fixed')),
  discount_value DECIMAL(12,0) NOT NULL,
  min_order      DECIMAL(12,0) NOT NULL DEFAULT 0,
  max_discount   DECIMAL(12,0) NULL,
  usage_limit    INT NULL,
  per_user_limit INT NOT NULL DEFAULT 1,
  used_count     INT NOT NULL DEFAULT 0,
  applies_to     VARCHAR(20) NOT NULL DEFAULT 'all' CHECK (applies_to IN ('all','category','brand','product')),
  starts_at      DATETIME2(0) NOT NULL,
  ends_at        DATETIME2(0) NOT NULL,
  is_active      BIT NOT NULL DEFAULT 1,
  created_by     BIGINT NULL,
  FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE voucher_scopes (                       -- phạm vi áp dụng khi applies_to != 'all'
  id         INT IDENTITY(1,1) PRIMARY KEY,
  voucher_id INT NOT NULL,
  scope_type VARCHAR(20) NOT NULL CHECK (scope_type IN ('category','brand','product')),
  scope_id   BIGINT NOT NULL,
  FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE CASCADE,
  CONSTRAINT uq_voucher_scope UNIQUE (voucher_id, scope_type, scope_id)
);

-- =====================================================================
-- 5. CART
-- =====================================================================
CREATE TABLE carts (
  id         BIGINT IDENTITY(1,1) PRIMARY KEY,
  user_id    BIGINT NOT NULL UNIQUE,
  voucher_id INT NULL,                              -- Apply Voucher
  updated_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
  FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE SET NULL
);

CREATE TABLE cart_items (
  id         BIGINT IDENTITY(1,1) PRIMARY KEY,
  cart_id    BIGINT NOT NULL,
  variant_id BIGINT NOT NULL,
  quantity   INT NOT NULL CHECK (quantity > 0),
  is_selected BIT NOT NULL DEFAULT 1,        -- Select items to checkout
  added_at   DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  CONSTRAINT uq_cart_variant UNIQUE (cart_id, variant_id),
  FOREIGN KEY (cart_id)    REFERENCES carts(id) ON DELETE CASCADE,
  FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE
);

-- =====================================================================
-- 6. ORDERS
-- =====================================================================
CREATE TABLE orders (
  id               BIGINT IDENTITY(1,1) PRIMARY KEY,
  order_code       VARCHAR(30) NOT NULL UNIQUE,
  user_id          BIGINT NULL,                      -- NULL = khách mua tại quầy
  channel          VARCHAR(20) NOT NULL DEFAULT 'online' CHECK (channel IN ('online','in_store')),
  status           VARCHAR(20)
                   NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','confirmed','preparing','shipping','completed','cancelled','returned')),
  payment_status   VARCHAR(20) NOT NULL DEFAULT 'unpaid' CHECK (payment_status IN ('unpaid','paid','partially_refunded','refunded')),
  receiver_name    NVARCHAR(150),
  receiver_phone   VARCHAR(20),
  shipping_address NVARCHAR(500),
  voucher_id       INT NULL,
  voucher_code     VARCHAR(40),
  subtotal         DECIMAL(12,0) NOT NULL,
  discount_amount  DECIMAL(12,0) NOT NULL DEFAULT 0,
  shipping_fee     DECIMAL(12,0) NOT NULL DEFAULT 0,
  total_amount     DECIMAL(12,0) NOT NULL,
  note             NVARCHAR(500),
  confirmed_by     BIGINT NULL,                      -- Staff Confirm order
  confirmed_at     DATETIME2(0) NULL,
  cancelled_by     BIGINT NULL,                      -- Customer/Staff cancels order
  cancelled_at     DATETIME2(0) NULL,
  cancel_reason    NVARCHAR(300),
  created_at       DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  updated_at       DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (user_id)      REFERENCES users(id),
  FOREIGN KEY (voucher_id)   REFERENCES vouchers(id),
  FOREIGN KEY (confirmed_by) REFERENCES users(id),
  FOREIGN KEY (cancelled_by) REFERENCES users(id),
  INDEX idx_orders_status_created (status, created_at),
  INDEX idx_orders_user (user_id, created_at)        -- Purchase History
);

CREATE TABLE order_items (
  id           BIGINT IDENTITY(1,1) PRIMARY KEY,
  order_id     BIGINT NOT NULL,
  variant_id   BIGINT NOT NULL,
  product_id   BIGINT NOT NULL,
  product_name NVARCHAR(255) NOT NULL,                -- snapshot
  variant_info NVARCHAR(100),                         -- snapshot "Đỏ / 42"
  unit_price   DECIMAL(12,0) NOT NULL,
  quantity     INT NOT NULL CHECK (quantity > 0),
  line_total   DECIMAL(12,0) NOT NULL,
  FOREIGN KEY (order_id)   REFERENCES orders(id) ON DELETE CASCADE,
  FOREIGN KEY (variant_id) REFERENCES product_variants(id),
  FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE order_status_history (                 -- Order Status Tracking
  id          BIGINT IDENTITY(1,1) PRIMARY KEY,
  order_id    BIGINT NOT NULL,
  from_status VARCHAR(20),
  to_status   VARCHAR(20) NOT NULL,
  changed_by  BIGINT NULL,
  note        NVARCHAR(300),
  created_at  DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (order_id)   REFERENCES orders(id) ON DELETE CASCADE,
  FOREIGN KEY (changed_by) REFERENCES users(id),
  INDEX idx_osh_order (order_id, created_at)
);

CREATE TABLE voucher_usages (                       -- Voucher Using
  id              BIGINT IDENTITY(1,1) PRIMARY KEY,
  voucher_id      INT NOT NULL,
  user_id         BIGINT NULL,
  order_id        BIGINT NOT NULL,
  discount_amount DECIMAL(12,0) NOT NULL,
  used_at         DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (voucher_id) REFERENCES vouchers(id),
  FOREIGN KEY (user_id)    REFERENCES users(id),
  FOREIGN KEY (order_id)   REFERENCES orders(id) ON DELETE CASCADE,
  CONSTRAINT uq_voucher_order UNIQUE (voucher_id, order_id)
);

-- =====================================================================
-- 11. COMPLAINTS & RETURNS (tạo trước vì refunds tham chiếu)
-- =====================================================================
CREATE TABLE support_tickets (
  id                     BIGINT IDENTITY(1,1) PRIMARY KEY,
  ticket_code            VARCHAR(30) NOT NULL UNIQUE,
  user_id                BIGINT NOT NULL,
  order_id               BIGINT NOT NULL,
  ticket_type            VARCHAR(20) NOT NULL CHECK (ticket_type IN ('return','exchange','warranty','complaint')),
  status                 VARCHAR(20)
                         NOT NULL DEFAULT 'open' CHECK (status IN ('open','under_review','approved','rejected','resolved','cancelled')),
  reason                 NVARCHAR(300) NOT NULL,
  description            NVARCHAR(MAX),
  resolution_type        VARCHAR(20) NULL CHECK (resolution_type IN ('refund','exchange','repair','none')),
  proposed_resolution    NVARCHAR(MAX) NULL,                  -- Staff proposes resolution/refund
  proposed_refund_amount DECIMAL(12,0) NULL,
  handled_by             BIGINT NULL,
  reviewed_at            DATETIME2(0) NULL,
  resolved_at            DATETIME2(0) NULL,
  created_at             DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  updated_at             DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (user_id)    REFERENCES users(id),
  FOREIGN KEY (order_id)   REFERENCES orders(id),
  FOREIGN KEY (handled_by) REFERENCES users(id),
  INDEX idx_ticket_status (status, created_at)
);

CREATE TABLE ticket_items (
  id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
  ticket_id           BIGINT NOT NULL,
  order_item_id       BIGINT NOT NULL,
  quantity            INT NOT NULL CHECK (quantity > 0),
  exchange_variant_id BIGINT NULL,                   -- đổi sang size/màu khác
  FOREIGN KEY (ticket_id)           REFERENCES support_tickets(id) ON DELETE CASCADE,
  FOREIGN KEY (order_item_id)       REFERENCES order_items(id),
  FOREIGN KEY (exchange_variant_id) REFERENCES product_variants(id)
);

CREATE TABLE ticket_attachments (                   -- Attach defect evidences
  id         BIGINT IDENTITY(1,1) PRIMARY KEY,
  ticket_id  BIGINT NOT NULL,
  file_url   VARCHAR(500) NOT NULL,
  file_type  VARCHAR(20) NOT NULL DEFAULT 'image' CHECK (file_type IN ('image','video','document')),
  created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE
);

CREATE TABLE ticket_comments (
  id         BIGINT IDENTITY(1,1) PRIMARY KEY,
  ticket_id  BIGINT NOT NULL,
  author_id  BIGINT NOT NULL,
  message    NVARCHAR(MAX) NOT NULL,
  created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE,
  FOREIGN KEY (author_id) REFERENCES users(id)
);

-- =====================================================================
-- 7. PAYMENT
-- =====================================================================
CREATE TABLE payments (
  id              BIGINT IDENTITY(1,1) PRIMARY KEY,
  order_id        BIGINT NOT NULL,
  method          VARCHAR(20) NOT NULL CHECK (method IN ('cod','qr','card','ewallet','bank_transfer','cash')),
  gateway         VARCHAR(30),                       -- vnpay, momo, payos...
  status          VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','paid','failed','cancelled','refunded')),
  amount          DECIMAL(12,0) NOT NULL,
  gateway_txn_ref VARCHAR(100),
  qr_code_url     VARCHAR(500),                      -- Display QR code / payment slip
  paid_at         DATETIME2(0) NULL,
  cancelled_at    DATETIME2(0) NULL,
  created_at      DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  updated_at      DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  INDEX idx_pay_status (status),
  INDEX idx_pay_txn (gateway_txn_ref)
);

CREATE TABLE payment_transactions (                 -- log gửi/nhận với Payment Gateway
  id          BIGINT IDENTITY(1,1) PRIMARY KEY,
  payment_id  BIGINT NOT NULL,
  direction   VARCHAR(20) NOT NULL CHECK (direction IN ('request','response','callback','refund_request','refund_response')),
  payload     NVARCHAR(MAX) CHECK (ISJSON(payload) = 1),
  http_status INT,
  created_at  DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);

CREATE TABLE refunds (                              -- Refund Processing / Refund Trigger
  id           BIGINT IDENTITY(1,1) PRIMARY KEY,
  payment_id   BIGINT NOT NULL,
  order_id     BIGINT NOT NULL,
  ticket_id    BIGINT NULL,
  amount       DECIMAL(12,0) NOT NULL,
  status       VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','processing','completed','failed')),
  gateway_ref  VARCHAR(100),
  reason       NVARCHAR(300),
  requested_by BIGINT NULL,
  requested_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  processed_at DATETIME2(0) NULL,
  FOREIGN KEY (payment_id)   REFERENCES payments(id),
  FOREIGN KEY (order_id)     REFERENCES orders(id),
  FOREIGN KEY (ticket_id)    REFERENCES support_tickets(id) ON DELETE SET NULL,
  FOREIGN KEY (requested_by) REFERENCES users(id)
);

-- =====================================================================
-- 8. SHIPPING & DELIVERY
-- =====================================================================
CREATE TABLE carriers (
  id        INT IDENTITY(1,1) PRIMARY KEY,
  code      VARCHAR(30) NOT NULL UNIQUE,             -- ghn, ghtk, viettelpost
  name      NVARCHAR(100) NOT NULL,
  is_active BIT NOT NULL DEFAULT 1
);

CREATE TABLE shipments (
  id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
  order_id              BIGINT NOT NULL,
  carrier_id            INT NULL,
  tracking_code         VARCHAR(80),
  status                VARCHAR(20)
                        NOT NULL DEFAULT 'preparing' CHECK (status IN ('preparing','picked_up','in_transit','delivered','failed','returned')),
  cod_amount            DECIMAL(12,0) NOT NULL DEFAULT 0,
  cod_collected_at      DATETIME2(0) NULL,               -- Carrier confirms delivery, COD collected
  shipped_at            DATETIME2(0) NULL,
  delivered_at          DATETIME2(0) NULL,
  customer_confirmed_at DATETIME2(0) NULL,               -- Delivery Confirmation
  created_at            DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (order_id)   REFERENCES orders(id) ON DELETE CASCADE,
  FOREIGN KEY (carrier_id) REFERENCES carriers(id) ON DELETE SET NULL,
  INDEX idx_ship_tracking (tracking_code)
);

CREATE TABLE shipment_events (                      -- Delivery Status Sync
  id          BIGINT IDENTITY(1,1) PRIMARY KEY,
  shipment_id BIGINT NOT NULL,
  status      VARCHAR(40) NOT NULL,
  description NVARCHAR(300),
  location    NVARCHAR(200),
  occurred_at DATETIME2(0) NOT NULL,
  FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE,
  INDEX idx_se_shipment (shipment_id, occurred_at)
);

-- =====================================================================
-- 9. INVENTORY
-- =====================================================================
CREATE TABLE suppliers (
  id      INT IDENTITY(1,1) PRIMARY KEY,
  name    NVARCHAR(150) NOT NULL,
  phone   VARCHAR(20),
  email   VARCHAR(190),
  address NVARCHAR(300)
);

CREATE TABLE goods_receipts (                       -- Stock In
  id           BIGINT IDENTITY(1,1) PRIMARY KEY,
  receipt_code VARCHAR(30) NOT NULL UNIQUE,
  supplier_id  INT NULL,
  received_by  BIGINT NULL,
  total_cost   DECIMAL(14,0) NOT NULL DEFAULT 0,
  note         NVARCHAR(300),
  received_at  DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL,
  FOREIGN KEY (received_by) REFERENCES users(id)
);

CREATE TABLE goods_receipt_items (
  id         BIGINT IDENTITY(1,1) PRIMARY KEY,
  receipt_id BIGINT NOT NULL,
  variant_id BIGINT NOT NULL,
  quantity   INT NOT NULL CHECK (quantity > 0),
  unit_cost  DECIMAL(12,0) NOT NULL,
  FOREIGN KEY (receipt_id) REFERENCES goods_receipts(id) ON DELETE CASCADE,
  FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);

CREATE TABLE inventory_movements (                  -- sổ cái: Stock In / Stock Out / Audit
  id            BIGINT IDENTITY(1,1) PRIMARY KEY,
  variant_id    BIGINT NOT NULL,
  movement_type VARCHAR(20) NOT NULL CHECK (movement_type IN ('stock_in','stock_out','return_in','audit_adjust','manual_adjust')),
  quantity      INT NOT NULL,                        -- dương = nhập, âm = xuất
  balance_after INT NOT NULL,
  ref_type      VARCHAR(20) NULL CHECK (ref_type IN ('goods_receipt','order','ticket','stock_audit','manual')),
  ref_id        BIGINT NULL,
  created_by    BIGINT NULL,
  note          NVARCHAR(300),
  created_at    DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (variant_id) REFERENCES product_variants(id),
  FOREIGN KEY (created_by) REFERENCES users(id),
  INDEX idx_im_variant_time (variant_id, created_at)
);

CREATE TABLE stock_audits (                         -- Stock Audit (kiểm kê định kỳ)
  id            BIGINT IDENTITY(1,1) PRIMARY KEY,
  audit_code    VARCHAR(30) NOT NULL UNIQUE,
  status        VARCHAR(20) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft','completed','cancelled')),
  conducted_by  BIGINT NULL,
  note          NVARCHAR(300),
  started_at    DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  completed_at  DATETIME2(0) NULL,
  FOREIGN KEY (conducted_by) REFERENCES users(id)
);

CREATE TABLE stock_audit_items (
  id          BIGINT IDENTITY(1,1) PRIMARY KEY,
  audit_id    BIGINT NOT NULL,
  variant_id  BIGINT NOT NULL,
  system_qty  INT NOT NULL,
  counted_qty INT NOT NULL,
  diff_qty    AS (counted_qty - system_qty) PERSISTED,
  note        NVARCHAR(200),
  FOREIGN KEY (audit_id)   REFERENCES stock_audits(id) ON DELETE CASCADE,
  FOREIGN KEY (variant_id) REFERENCES product_variants(id),
  CONSTRAINT uq_audit_variant UNIQUE (audit_id, variant_id)
);

CREATE TABLE stock_alerts (                         -- Stock Alerts
  id             BIGINT IDENTITY(1,1) PRIMARY KEY,
  variant_id     BIGINT NOT NULL,
  stock_at_alert INT NOT NULL,
  threshold      INT NOT NULL,
  status         VARCHAR(20) NOT NULL DEFAULT 'open' CHECK (status IN ('open','resolved')),
  created_at     DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  resolved_at    DATETIME2(0) NULL,
  FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
  INDEX idx_alert_status (status)
);

-- =====================================================================
-- 10. INVOICES
-- =====================================================================
CREATE TABLE invoices (
  id                BIGINT IDENTITY(1,1) PRIMARY KEY,
  invoice_no        VARCHAR(30) NOT NULL UNIQUE,
  order_id          BIGINT NOT NULL,
  invoice_type      VARCHAR(20) NOT NULL DEFAULT 'sale' CHECK (invoice_type IN ('sale','adjustment')),  -- Invoice Adjustment
  parent_invoice_id BIGINT NULL,                     -- hóa đơn gốc khi điều chỉnh/thay thế
  status            VARCHAR(20) NOT NULL DEFAULT 'issued' CHECK (status IN ('issued','cancelled','replaced')),
  buyer_name        NVARCHAR(150),
  buyer_email       VARCHAR(190),
  buyer_address     NVARCHAR(500),
  buyer_tax_code    VARCHAR(30),
  subtotal          DECIMAL(12,0) NOT NULL,
  discount_amount   DECIMAL(12,0) NOT NULL DEFAULT 0,
  shipping_fee      DECIMAL(12,0) NOT NULL DEFAULT 0,
  total_amount      DECIMAL(12,0) NOT NULL,            -- âm nếu là hóa đơn điều chỉnh giảm
  reason            NVARCHAR(300),
  pdf_url           VARCHAR(500),                      -- Print/export PDF
  issued_by         BIGINT NULL,                       -- NULL = tự động khi đơn đã thanh toán
  issued_at         DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (order_id)          REFERENCES orders(id),
  FOREIGN KEY (parent_invoice_id) REFERENCES invoices(id),
  FOREIGN KEY (issued_by)         REFERENCES users(id),
  INDEX idx_inv_order (order_id)
);

CREATE TABLE invoice_items (
  id           BIGINT IDENTITY(1,1) PRIMARY KEY,
  invoice_id   BIGINT NOT NULL,
  description  NVARCHAR(255) NOT NULL,
  unit_price   DECIMAL(12,0) NOT NULL,
  quantity     INT NOT NULL,
  line_total   DECIMAL(12,0) NOT NULL,
  FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

CREATE TABLE invoice_deliveries (                   -- Send e-invoice via email
  id         BIGINT IDENTITY(1,1) PRIMARY KEY,
  invoice_id BIGINT NOT NULL,
  channel    VARCHAR(20) NOT NULL DEFAULT 'email' CHECK (channel IN ('email','download')),
  recipient  VARCHAR(190),
  status     VARCHAR(20) NOT NULL DEFAULT 'queued' CHECK (status IN ('queued','sent','failed')),
  sent_at    DATETIME2(0) NULL,
  FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);
GO

-- =====================================================================
-- UNIQUE INDEX có điều kiện (cho phép nhiều NULL như MySQL)
-- =====================================================================
CREATE UNIQUE INDEX uq_users_phone       ON users(phone)               WHERE phone    IS NOT NULL;
CREATE UNIQUE INDEX uq_variants_barcode  ON product_variants(barcode)  WHERE barcode  IS NOT NULL;
GO

-- =====================================================================
-- FULL-TEXT SEARCH cho products.name (thay cho FULLTEXT INDEX của MySQL)
-- Truy vấn: WHERE CONTAINS(name, N'"vợt*"')  hoặc  FREETEXT(name, N'vợt yonex')
-- =====================================================================
IF FULLTEXTSERVICEPROPERTY('IsFullTextInstalled') = 1
BEGIN
    IF NOT EXISTS (SELECT 1 FROM sys.fulltext_catalogs WHERE name = 'ft_badminton')
        EXEC('CREATE FULLTEXT CATALOG ft_badminton AS DEFAULT');
    EXEC('CREATE FULLTEXT INDEX ON products(name) KEY INDEX PK_products ON ft_badminton');
END
ELSE
    PRINT N'Full-Text Search chưa được cài, bỏ qua full-text index (có thể dùng LIKE).';
GO

-- =====================================================================
-- TRIGGER thay cho ON UPDATE CURRENT_TIMESTAMP
-- =====================================================================
CREATE TRIGGER trg_users_updated_at ON users
AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(updated_at) RETURN;
    UPDATE t SET updated_at = SYSDATETIME()
    FROM users t JOIN inserted i ON i.id = t.id;
END;
GO
CREATE TRIGGER trg_products_updated_at ON products
AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(updated_at) RETURN;
    UPDATE t SET updated_at = SYSDATETIME()
    FROM products t JOIN inserted i ON i.id = t.id;
END;
GO
CREATE TRIGGER trg_carts_updated_at ON carts
AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(updated_at) RETURN;
    UPDATE t SET updated_at = SYSDATETIME()
    FROM carts t JOIN inserted i ON i.id = t.id;
END;
GO
CREATE TRIGGER trg_orders_updated_at ON orders
AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(updated_at) RETURN;
    UPDATE t SET updated_at = SYSDATETIME()
    FROM orders t JOIN inserted i ON i.id = t.id;
END;
GO
CREATE TRIGGER trg_support_tickets_updated_at ON support_tickets
AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(updated_at) RETURN;
    UPDATE t SET updated_at = SYSDATETIME()
    FROM support_tickets t JOIN inserted i ON i.id = t.id;
END;
GO
CREATE TRIGGER trg_payments_updated_at ON payments
AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(updated_at) RETURN;
    UPDATE t SET updated_at = SYSDATETIME()
    FROM payments t JOIN inserted i ON i.id = t.id;
END;
GO

-- =====================================================================
-- 14. REPORTS & ANALYTICS (VIEW)
-- =====================================================================
-- Doanh thu theo ngày
CREATE VIEW v_revenue_daily AS
SELECT CAST(o.created_at AS DATE) AS [day],
       COUNT(*)            AS orders,
       SUM(o.total_amount) AS revenue
FROM orders o
WHERE o.status = 'completed'
GROUP BY CAST(o.created_at AS DATE);
GO

-- Doanh thu theo tháng
CREATE VIEW v_revenue_monthly AS
SELECT CONVERT(CHAR(7), o.created_at, 126) AS [month],      -- 'yyyy-MM'
       COUNT(*)            AS orders,
       SUM(o.total_amount) AS revenue
FROM orders o
WHERE o.status = 'completed'
GROUP BY CONVERT(CHAR(7), o.created_at, 126);
GO

-- Sản phẩm bán chạy (khi truy vấn dùng:  SELECT * FROM v_best_selling_products ORDER BY units_sold DESC)
CREATE VIEW v_best_selling_products AS
SELECT p.id AS product_id, p.name, p.product_type,
       SUM(oi.quantity)   AS units_sold,
       SUM(oi.line_total) AS revenue
FROM order_items oi
JOIN orders   o ON o.id = oi.order_id AND o.status = 'completed'
JOIN products p ON p.id = oi.product_id
GROUP BY p.id, p.name, p.product_type;
GO

-- Tồn kho hiện tại + cảnh báo
CREATE VIEW v_stock_level AS
SELECT v.id AS variant_id, v.sku, p.name AS product_name, p.product_type,
       v.color, v.size, v.stock_quantity, v.low_stock_threshold,
       CASE WHEN v.stock_quantity <= v.low_stock_threshold THEN 1 ELSE 0 END AS is_low_stock,
       v.stock_quantity * v.cost_price AS stock_value
FROM product_variants v
JOIN products p ON p.id = v.product_id;
GO

-- Hiệu suất nhân viên (số đơn xác nhận, xử lý ticket, phiếu nhập kho)
CREATE VIEW v_staff_performance AS
SELECT u.id AS staff_id, u.full_name,
       (SELECT COUNT(*) FROM orders o WHERE o.confirmed_by = u.id)         AS orders_confirmed,
       (SELECT COUNT(*) FROM support_tickets t WHERE t.handled_by = u.id)  AS tickets_handled,
       (SELECT COUNT(*) FROM goods_receipts g WHERE g.received_by = u.id)  AS receipts_created
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id AND r.code IN ('staff','manager','admin');
GO

-- Tăng trưởng người dùng lũy kế (Active Subscriber Accumulation)
CREATE VIEW v_user_growth AS
SELECT d.[day],
       d.new_users,
       SUM(d.new_users) OVER (ORDER BY d.[day]) AS cumulative_users
FROM (SELECT CAST(created_at AS DATE) AS [day], COUNT(*) AS new_users
      FROM users WHERE status = 'active' GROUP BY CAST(created_at AS DATE)) d;
GO

-- Hiệu quả khuyến mãi (gộp pricing/promotion)
CREATE VIEW v_voucher_performance AS
SELECT v.id AS voucher_id, v.code, v.name,
       COUNT(vu.id) AS times_used,
       COALESCE(SUM(vu.discount_amount), 0) AS total_discount,
       COALESCE(SUM(o.total_amount), 0)     AS revenue_with_voucher
FROM vouchers v
LEFT JOIN voucher_usages vu ON vu.voucher_id = v.id
LEFT JOIN orders o ON o.id = vu.order_id
GROUP BY v.id, v.code, v.name;
GO

-- =====================================================================
-- DỮ LIỆU KHỞI TẠO
-- =====================================================================
INSERT INTO roles (code, name) VALUES
  ('customer', N'Khách hàng'), ('staff', N'Nhân viên'),
  ('manager',  N'Quản lý'),    ('admin', N'Quản trị viên');

INSERT INTO permissions (code, name) VALUES
  ('catalog.manage',   N'Quản lý danh mục/sản phẩm/voucher'),
  ('order.confirm',    N'Xác nhận/cập nhật đơn hàng'),
  ('payment.refund',   N'Xử lý hoàn tiền'),
  ('inventory.manage', N'Quản lý kho'),
  ('invoice.manage',   N'Quản lý hóa đơn'),
  ('ticket.handle',    N'Xử lý khiếu nại/đổi trả'),
  ('report.view',      N'Xem báo cáo'),
  ('user.manage',      N'Quản lý người dùng & phân quyền'),
  ('log.view',         N'Xem nhật ký hoạt động');
GO
