-- Initial SQL Server schema, captured from badmintonshop.sql on 2026-11-09.
-- Includes required roles, settings and carriers; excludes demo accounts and business data.
-- Run only through Flyway against an empty database, or baseline an equivalent existing schema.
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- =====================================================================
-- 1-3. ROLES, USERS & AUTHENTICATION
-- =====================================================================
CREATE TABLE roles (                                -- Customer, Staff, Manager, Admin
  id    INT IDENTITY(1,1) PRIMARY KEY,
  code  VARCHAR(30) NOT NULL UNIQUE,
  name  NVARCHAR(80) NOT NULL
);

CREATE TABLE users (
  id                BIGINT IDENTITY(1,1) PRIMARY KEY,
  role_id           INT NOT NULL,
  email             VARCHAR(190) NOT NULL UNIQUE,
  phone             VARCHAR(20) NULL,
  password_hash     VARCHAR(255) NOT NULL,
  full_name         NVARCHAR(150) NOT NULL,
  gender            VARCHAR(20) NULL CHECK (gender IN ('male','female','other')),
  birth_date        DATE NULL,
  avatar_url        VARCHAR(500),
  status            VARCHAR(20) NOT NULL DEFAULT 'active' CHECK (status IN ('active','locked')),
  email_verified_at DATETIME2(0) NULL,
  last_login_at     DATETIME2(0) NULL,
  created_at        DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  updated_at        DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (role_id) REFERENCES roles(id),
  INDEX idx_users_role_id (role_id),
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

CREATE TABLE refresh_tokens (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(64) NOT NULL UNIQUE,
  expires_at DATETIME2(7) NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_refresh_tokens_user (user_id),
  INDEX idx_refresh_tokens_expiry (expires_at)
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
  logo_url  VARCHAR(MAX) NULL,       -- Existing remote / Cloudinary URL
  logo_data VARBINARY(MAX) NULL,     -- Bytes for newly uploaded logos
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
  url        VARCHAR(MAX) NULL,     -- Existing remote / Cloudinary URL
  image_data VARBINARY(MAX) NULL,    -- Bytes for newly uploaded images
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
-- 11. RETURNS & REFUNDS (tạo trước vì refunds tham chiếu)
-- =====================================================================
CREATE TABLE support_tickets (
  id                     BIGINT IDENTITY(1,1) PRIMARY KEY,
  ticket_code            VARCHAR(30) NOT NULL UNIQUE,
  user_id                BIGINT NOT NULL,
  order_id               BIGINT NOT NULL,
  ticket_type            VARCHAR(20) NOT NULL DEFAULT 'REFUND' CHECK (ticket_type IN ('REFUND')),
  status                 VARCHAR(20)
                         NOT NULL DEFAULT 'open' CHECK (status IN ('open','under_review','approved','rejected','resolved','cancelled')),
  reason                 NVARCHAR(300) NOT NULL,
  description            NVARCHAR(MAX),
  resolution_type        VARCHAR(20) NULL CHECK (resolution_type IN ('refund','none')),
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
  FOREIGN KEY (ticket_id)           REFERENCES support_tickets(id) ON DELETE CASCADE,
  FOREIGN KEY (order_item_id)       REFERENCES order_items(id)
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

CREATE TABLE system_settings (
  setting_key   VARCHAR(100) PRIMARY KEY,
  setting_value NVARCHAR(500) NOT NULL,
  description   NVARCHAR(255) NULL,
  updated_at    DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  updated_by    BIGINT NULL,
  FOREIGN KEY (updated_by) REFERENCES users(id)
);

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('refund_approval_threshold', '3000000', N'Ngưỡng hoàn tiền yêu cầu Quản lý phê duyệt (VND)'),
       ('payment_qr_timeout_minutes', '15', N'Thời gian hết hạn mã VietQR thanh toán đơn hàng (phút)');

-- =====================================================================
-- 8. SHIPPING & DELIVERY
-- =====================================================================
CREATE TABLE carriers (
  id                    INT IDENTITY(1,1) PRIMARY KEY,
  code                  VARCHAR(30) NOT NULL UNIQUE,             -- fastship, ghn, ghtk, viettelpost
  name                  NVARCHAR(100) NOT NULL,
  api_base_url          VARCHAR(255) NULL,                       -- vd: http://localhost:8081/api/v1 (API key để trong application.properties)
  tracking_url_template VARCHAR(500) NULL,                       -- vd: http://localhost:8082/track?code={code}
  hotline               VARCHAR(20) NULL,
  is_active             BIT NOT NULL DEFAULT 1
);

CREATE TABLE shipments (                            -- Vận đơn gửi sang ĐVVC (FastShip)
  id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
  order_id                BIGINT NOT NULL,
  carrier_id              INT NULL,
  partner_ref             VARCHAR(64) NULL,                    -- id vận đơn phía đối tác (FastShip trả về khi nhận yêu cầu)
  partner_version         BIGINT NULL,                         -- phiên bản vận đơn phía đối tác, bỏ qua webhook cũ đến muộn
  tracking_code           VARCHAR(80),                         -- mã vận đơn đối tác cấp
  tracking_url            VARCHAR(500),                        -- link tra cứu trên website đối tác
  service_type            VARCHAR(20) NOT NULL DEFAULT 'standard' CHECK (service_type IN ('standard','fast','express')),
  status                  VARCHAR(20)
                          NOT NULL DEFAULT 'requested' CHECK (status IN ('requested','rejected','pending_pickup','in_transit','out_for_delivery','delivered','delivery_failed','returned','cancelled')),
  carrier_status_text     NVARCHAR(300),                       -- mô tả trạng thái mới nhất từ đối tác
  shipping_fee            DECIMAL(12,0) NOT NULL DEFAULT 0,    -- cước đối tác báo
  cod_amount              DECIMAL(12,0) NOT NULL DEFAULT 0,
  cod_collected_at        DATETIME2(0) NULL,                   -- Carrier confirms delivery, COD collected
  weight_grams            INT NULL,
  note                    NVARCHAR(500),
  shipper_name            NVARCHAR(150),
  shipper_phone           VARCHAR(20),
  estimated_delivery_date DATETIME2(0) NULL,
  shipped_at              DATETIME2(0) NULL,                   -- bưu tá lấy hàng
  delivered_at            DATETIME2(0) NULL,
  cancelled_at            DATETIME2(0) NULL,
  cancel_reason           NVARCHAR(300),
  customer_confirmed_at   DATETIME2(0) NULL,                   -- Delivery Confirmation
  last_synced_at          DATETIME2(0) NULL,                   -- lần cuối nhận webhook / WebSocket / sync
  created_by              BIGINT NULL,                         -- staff tạo vận đơn
  created_at              DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  updated_at              DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
  FOREIGN KEY (order_id)   REFERENCES orders(id) ON DELETE CASCADE,
  FOREIGN KEY (carrier_id) REFERENCES carriers(id) ON DELETE SET NULL,
  FOREIGN KEY (created_by) REFERENCES users(id),
  INDEX idx_ship_tracking (tracking_code),
  INDEX idx_ship_partner_ref (partner_ref),
  INDEX idx_ship_order (order_id),
  INDEX idx_ship_status (status, created_at)
);

CREATE TABLE shipment_events (                      -- Delivery Status Sync (hành trình)
  id          BIGINT IDENTITY(1,1) PRIMARY KEY,
  shipment_id BIGINT NOT NULL,
  external_event_id VARCHAR(64) NULL,                -- eventId của webhook đối tác, chống ghi trùng
  status      VARCHAR(40) NOT NULL,                  -- trạng thái phía đối tác tại thời điểm đó
  title       NVARCHAR(150),                         -- vd: "Nhập kho trung chuyển"
  description NVARCHAR(300),
  location    NVARCHAR(200),
  source      VARCHAR(20) NOT NULL DEFAULT 'carrier' CHECK (source IN ('carrier','webhook','sync','manual')),
  occurred_at DATETIME2(0) NOT NULL,                 -- thời điểm xảy ra phía đối tác
  created_at  DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),  -- thời điểm shop nhận
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
CREATE UNIQUE INDEX uq_se_external_event ON shipment_events(external_event_id) WHERE external_event_id IS NOT NULL;
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
CREATE TRIGGER trg_shipments_updated_at ON shipments
AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF UPDATE(updated_at) RETURN;
    UPDATE t SET updated_at = SYSDATETIME()
    FROM shipments t JOIN inserted i ON i.id = t.id;
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
WHERE o.status = 'completed' AND o.payment_status = 'paid'
GROUP BY CAST(o.created_at AS DATE);
GO

-- Doanh thu theo tháng
CREATE VIEW v_revenue_monthly AS
SELECT CONVERT(CHAR(7), o.created_at, 126) AS [month],      -- 'yyyy-MM'
       COUNT(*)            AS orders,
       SUM(o.total_amount) AS revenue
FROM orders o
WHERE o.status = 'completed' AND o.payment_status = 'paid'
GROUP BY CONVERT(CHAR(7), o.created_at, 126);
GO

-- Sản phẩm bán chạy (khi truy vấn dùng:  SELECT * FROM v_best_selling_products ORDER BY units_sold DESC)
CREATE VIEW v_best_selling_products AS
SELECT p.id AS product_id, p.name, p.product_type,
       SUM(oi.quantity)   AS units_sold,
       SUM(oi.line_total) AS revenue
FROM order_items oi
JOIN orders   o ON o.id = oi.order_id AND o.status = 'completed' AND o.payment_status = 'paid'
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
JOIN roles r ON r.id = u.role_id AND r.code IN ('staff','manager','admin');
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

-- ===== Customer & Membership Report =====
-- Khách hàng mới theo ngày và lũy kế (chỉ tài khoản role customer)
CREATE VIEW v_customer_growth AS
SELECT d.[day],
       d.new_customers,
       SUM(d.new_customers) OVER (ORDER BY d.[day]) AS cumulative_customers
FROM (SELECT CAST(u.created_at AS DATE) AS [day], COUNT(*) AS new_customers
      FROM users u
      JOIN roles r ON r.id = u.role_id AND r.code = 'customer'
      GROUP BY CAST(u.created_at AS DATE)) d;
GO

-- Tổng hợp mua hàng theo từng khách (doanh thu tính đơn completed + paid)
CREATE VIEW v_customer_summary AS
SELECT u.id AS customer_id, u.full_name, u.email, u.phone, u.status,
       u.created_at AS registered_at,
       COUNT(o.id) AS total_orders,
       COALESCE(SUM(CASE WHEN o.status = 'completed' AND o.payment_status = 'paid' THEN 1 ELSE 0 END), 0) AS completed_orders,
       COALESCE(SUM(CASE WHEN o.status = 'completed' AND o.payment_status = 'paid' THEN o.total_amount ELSE 0 END), 0) AS total_spent,
       MIN(o.created_at) AS first_order_at,
       MAX(o.created_at) AS last_order_at
FROM users u
JOIN roles r ON r.id = u.role_id AND r.code = 'customer'
LEFT JOIN orders o ON o.user_id = u.id
GROUP BY u.id, u.full_name, u.email, u.phone, u.status, u.created_at;
GO

-- ===== Return & Refund Report =====
-- Số phiếu trả hàng/hoàn tiền theo tháng và loại. LOWER() để chạy được với cả giá trị thường lẫn HOA của module support ticket.
CREATE VIEW v_return_ticket_monthly AS
SELECT CONVERT(CHAR(7), t.created_at, 126) + '|' + LOWER(t.ticket_type) AS id,
       CONVERT(CHAR(7), t.created_at, 126) AS [month],
       LOWER(t.ticket_type) AS ticket_type,
       COUNT(*) AS total_tickets,
       SUM(CASE WHEN LOWER(t.status) IN ('resolved','refunded') THEN 1 ELSE 0 END) AS resolved_tickets,
       SUM(CASE WHEN LOWER(t.status) IN ('rejected','inspection_failed') THEN 1 ELSE 0 END) AS rejected_tickets,
       SUM(CASE WHEN LOWER(t.status) = 'cancelled' THEN 1 ELSE 0 END) AS cancelled_tickets
FROM support_tickets t
GROUP BY CONVERT(CHAR(7), t.created_at, 126), LOWER(t.ticket_type);
GO

-- Tiền hoàn theo tháng (chỉ khoản hoàn đã completed)
CREATE VIEW v_refund_monthly AS
SELECT CONVERT(CHAR(7), COALESCE(rf.processed_at, rf.requested_at), 126) AS [month],
       COUNT(*)       AS refund_count,
       SUM(rf.amount) AS refunded_amount
FROM refunds rf
WHERE rf.status = 'completed'
GROUP BY CONVERT(CHAR(7), COALESCE(rf.processed_at, rf.requested_at), 126);
GO
INSERT INTO roles (code, name) VALUES
  ('customer', N'Khách hàng'), ('staff', N'Nhân viên'),
  ('manager',  N'Quản lý'),    ('admin', N'Quản trị viên');
GO
INSERT INTO carriers (code, name, api_base_url, tracking_url_template, hotline, is_active) VALUES
  ('fastship',    N'FastShip Express (Đối Tác)', 'http://localhost:8081/api/v1', 'http://localhost:8082/track?code={code}', '19006868', 1),
  ('ghn',         N'Giao Hàng Nhanh (GHN)',      NULL, 'https://donhang.ghn.vn/?order_code={code}', '1900636677', 0),
  ('ghtk',        N'Giao Hàng Tiết Kiệm (GHTK)', NULL, 'https://i.ghtk.vn/{code}', '19006092', 0),
  ('viettelpost', N'Viettel Post',               NULL, 'https://viettelpost.com.vn/tra-cuu-hanh-trinh-don/?code={code}', '19008095', 0);

GO
IF COL_LENGTH('dbo.product_variants', 'weight_grams') IS NULL
    ALTER TABLE dbo.product_variants ADD weight_grams INT NOT NULL CONSTRAINT DF_catalog_variant_weight DEFAULT (0) WITH VALUES;
IF COL_LENGTH('dbo.product_variants', 'created_at') IS NULL
    ALTER TABLE dbo.product_variants ADD created_at DATETIME2(0) NOT NULL CONSTRAINT DF_catalog_variant_created DEFAULT (SYSDATETIME()) WITH VALUES;
IF COL_LENGTH('dbo.product_variants', 'updated_at') IS NULL
    ALTER TABLE dbo.product_variants ADD updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_catalog_variant_updated DEFAULT (SYSDATETIME()) WITH VALUES;
GO
SET XACT_ABORT ON;
GO
IF COL_LENGTH('dbo.payments', 'checkout_url') IS NULL
    ALTER TABLE dbo.payments ADD checkout_url VARCHAR(1000) NULL;
GO
IF COL_LENGTH('dbo.support_tickets', 'cause') IS NULL
    ALTER TABLE dbo.support_tickets ADD cause VARCHAR(25) NULL;
IF COL_LENGTH('dbo.support_tickets', 'reject_reason') IS NULL
    ALTER TABLE dbo.support_tickets ADD reject_reason NVARCHAR(500) NULL;
IF COL_LENGTH('dbo.support_tickets', 'refund_amount') IS NULL
    ALTER TABLE dbo.support_tickets ADD refund_amount DECIMAL(12,0) NULL;
IF COL_LENGTH('dbo.support_tickets', 'refund_qr_url') IS NULL
    ALTER TABLE dbo.support_tickets ADD refund_qr_url NVARCHAR(MAX) NULL;
IF COL_LENGTH('dbo.support_tickets', 'reviewed_by') IS NULL
    ALTER TABLE dbo.support_tickets ADD reviewed_by BIGINT NULL REFERENCES dbo.users(id);
IF COL_LENGTH('dbo.support_tickets', 'reviewed_at') IS NULL
    ALTER TABLE dbo.support_tickets ADD reviewed_at DATETIME2(0) NULL;
IF COL_LENGTH('dbo.support_tickets', 'approved_by') IS NULL
    ALTER TABLE dbo.support_tickets ADD approved_by BIGINT NULL REFERENCES dbo.users(id);
IF COL_LENGTH('dbo.support_tickets', 'approved_at') IS NULL
    ALTER TABLE dbo.support_tickets ADD approved_at DATETIME2(0) NULL;
GO
-- Drop exchange/replacement variant columns from ticket_items if present (exchange feature eliminated)
IF COL_LENGTH('dbo.ticket_items', 'replacement_variant_id') IS NOT NULL
BEGIN
    DECLARE @fkName NVARCHAR(MAX) = (
        SELECT f.name
        FROM sys.foreign_keys f
        JOIN sys.foreign_key_columns fc ON f.object_id = fc.constraint_object_id
        WHERE f.parent_object_id = OBJECT_ID('dbo.ticket_items')
          AND COL_NAME(fc.parent_object_id, fc.parent_column_id) = 'replacement_variant_id'
    );
    IF @fkName IS NOT NULL
        EXEC('ALTER TABLE dbo.ticket_items DROP CONSTRAINT ' + @fkName);
    ALTER TABLE dbo.ticket_items DROP COLUMN replacement_variant_id;
END;
GO
IF COL_LENGTH('dbo.ticket_items', 'exchange_variant_id') IS NOT NULL
BEGIN
    DECLARE @fkNameEx NVARCHAR(MAX) = (
        SELECT f.name
        FROM sys.foreign_keys f
        JOIN sys.foreign_key_columns fc ON f.object_id = fc.constraint_object_id
        WHERE f.parent_object_id = OBJECT_ID('dbo.ticket_items')
          AND COL_NAME(fc.parent_object_id, fc.parent_column_id) = 'exchange_variant_id'
    );
    IF @fkNameEx IS NOT NULL
        EXEC('ALTER TABLE dbo.ticket_items DROP CONSTRAINT ' + @fkNameEx);
    ALTER TABLE dbo.ticket_items DROP COLUMN exchange_variant_id;
END;
GO
-- Strictly Return & Refund (UC15). Convert any legacy ticket types to REFUND.
UPDATE dbo.support_tickets SET ticket_type = 'REFUND' WHERE ticket_type != 'REFUND';
GO
DECLARE @ticketChecks NVARCHAR(MAX) = N'';
SELECT @ticketChecks += N'ALTER TABLE dbo.support_tickets DROP CONSTRAINT ' + QUOTENAME(name) + N';'
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID('dbo.support_tickets')
  AND (definition LIKE '%ticket_type%' OR definition LIKE '%status%');
EXEC sp_executesql @ticketChecks;
GO
ALTER TABLE dbo.support_tickets ALTER COLUMN status VARCHAR(30) NOT NULL;
ALTER TABLE dbo.support_tickets ALTER COLUMN refund_qr_url NVARCHAR(MAX) NULL;
ALTER TABLE dbo.support_tickets ADD CONSTRAINT CK_shop_ticket_type_v2 CHECK
    (ticket_type IN ('REFUND'));
ALTER TABLE dbo.support_tickets ADD CONSTRAINT CK_shop_ticket_status_v2 CHECK
    (status IN ('open','under_review','approved','rejected','resolved','cancelled',
        'PENDING','REJECTED','AWAITING_RETURN','INSPECTION_FAILED','PENDING_APPROVAL',
        'PENDING_TRANSFER','REFUNDED','CANCELLED'));
GO
IF OBJECT_ID('dbo.ticket_evidence', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ticket_evidence (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        ticket_id BIGINT NOT NULL REFERENCES dbo.support_tickets(id) ON DELETE CASCADE,
        file_url NVARCHAR(MAX) NOT NULL,
        file_type VARCHAR(20) NOT NULL DEFAULT 'image',
        uploaded_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME()
    );
END;
GO
ALTER TABLE dbo.ticket_evidence ALTER COLUMN file_url NVARCHAR(MAX) NOT NULL;
GO
IF OBJECT_ID('dbo.ticket_status_history', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ticket_status_history (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        ticket_id BIGINT NOT NULL REFERENCES dbo.support_tickets(id) ON DELETE CASCADE,
        from_status VARCHAR(30) NULL,
        to_status VARCHAR(30) NOT NULL,
        actor_id BIGINT NULL REFERENCES dbo.users(id),
        note NVARCHAR(500) NULL,
        created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME()
    );
END;
GO
IF OBJECT_ID('dbo.ticket_inspection', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ticket_inspection (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        ticket_id BIGINT NOT NULL UNIQUE REFERENCES dbo.support_tickets(id) ON DELETE CASCADE,
        staff_id BIGINT NOT NULL REFERENCES dbo.users(id),
        result VARCHAR(20) NOT NULL CHECK (result IN ('PASSED','FAILED')),
        note NVARCHAR(MAX) NULL,
        inspected_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME()
    );
END;
GO
IF OBJECT_ID('dbo.refund_records', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.refund_records (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        ticket_id BIGINT NOT NULL UNIQUE REFERENCES dbo.support_tickets(id),
        payment_id BIGINT NULL REFERENCES dbo.payments(id),
        order_id BIGINT NOT NULL REFERENCES dbo.orders(id),
        amount DECIMAL(12,0) NOT NULL CHECK (amount > 0),
        bank_reference VARCHAR(100) NOT NULL UNIQUE,
        transferred_by BIGINT NOT NULL REFERENCES dbo.users(id),
        note NVARCHAR(300) NULL,
        transferred_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME()
    );
END;
GO
-- Personal voucher wallet: a row grants this voucher to exactly one account.
IF OBJECT_ID(N'dbo.user_vouchers', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.user_vouchers (
        user_id BIGINT NOT NULL,
        voucher_id INT NOT NULL,
        assigned_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT PK_user_vouchers PRIMARY KEY (user_id, voucher_id),
        CONSTRAINT FK_user_vouchers_user FOREIGN KEY (user_id) REFERENCES dbo.users(id) ON DELETE CASCADE,
        CONSTRAINT FK_user_vouchers_voucher FOREIGN KEY (voucher_id) REFERENCES dbo.vouchers(id) ON DELETE CASCADE
    );
END;
GO
-- To grant a voucher, insert the intended account and voucher IDs:
-- INSERT INTO dbo.user_vouchers(user_id, voucher_id) VALUES (<user_id>, <voucher_id>);

-- Public vouchers remain available to everyone; private vouchers require user_vouchers grants.
IF COL_LENGTH('dbo.vouchers', 'audience') IS NULL
    ALTER TABLE dbo.vouchers ADD audience VARCHAR(10) NOT NULL CONSTRAINT DF_vouchers_audience DEFAULT 'public' CONSTRAINT CK_vouchers_audience CHECK (audience IN ('public','private'));
GO
