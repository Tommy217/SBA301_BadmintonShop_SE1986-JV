-- Script duy nhất của DB shop: pull về chạy lại toàn bộ file này là có DB mới nhất.
-- CẢNH BÁO: chạy lại sẽ XOÁ database badminton_shop cũ cùng toàn bộ dữ liệu trong đó.
USE master;
GO
IF DB_ID('badminton_shop') IS NOT NULL
BEGIN
    ALTER DATABASE badminton_shop SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE badminton_shop;
END
GO
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

-- ===== Customer & Membership Report =====
-- Khách hàng mới theo ngày và lũy kế (chỉ tài khoản role customer)
CREATE VIEW v_customer_growth AS
SELECT d.[day],
       d.new_customers,
       SUM(d.new_customers) OVER (ORDER BY d.[day]) AS cumulative_customers
FROM (SELECT CAST(u.created_at AS DATE) AS [day], COUNT(*) AS new_customers
      FROM users u
      JOIN user_roles ur ON ur.user_id = u.id
      JOIN roles r ON r.id = ur.role_id AND r.code = 'customer'
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
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id AND r.code = 'customer'
LEFT JOIN orders o ON o.user_id = u.id
GROUP BY u.id, u.full_name, u.email, u.phone, u.status, u.created_at;
GO

-- ===== Return & Refund Report =====
-- Số phiếu đổi/trả theo tháng và loại. LOWER() để chạy được với cả giá trị thường lẫn HOA của module support ticket.
CREATE VIEW v_return_ticket_monthly AS
SELECT CONVERT(CHAR(7), t.created_at, 126) + '|' + LOWER(t.ticket_type) AS id,
       CONVERT(CHAR(7), t.created_at, 126) AS [month],
       LOWER(t.ticket_type) AS ticket_type,
       COUNT(*) AS total_tickets,
       SUM(CASE WHEN LOWER(t.status) IN ('resolved','completed','refunded','exchanged') THEN 1 ELSE 0 END) AS resolved_tickets,
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

-- ===== TÀI KHOẢN (mật khẩu chung: 123) =====
INSERT INTO users (email, password_hash, full_name, status, email_verified_at) VALUES
  ('admin@gmail.com',    '$2a$10$NJDzXjN2O69vD1OjD1/MU.KsvrAlJHEBK8PF5VdOYWQPn4hDeUUNS', N'Quản trị viên', 'active', SYSDATETIME()),
  ('manager@gmail.com',  '$2a$10$NJDzXjN2O69vD1OjD1/MU.KsvrAlJHEBK8PF5VdOYWQPn4hDeUUNS', N'Quản lý',       'active', SYSDATETIME()),
  ('staff@gmail.com',    '$2a$10$NJDzXjN2O69vD1OjD1/MU.KsvrAlJHEBK8PF5VdOYWQPn4hDeUUNS', N'Nhân viên',     'active', SYSDATETIME()),
  ('customer@gmail.com', '$2a$10$NJDzXjN2O69vD1OjD1/MU.KsvrAlJHEBK8PF5VdOYWQPn4hDeUUNS', N'Khách hàng',    'active', SYSDATETIME());

-- ===== GÁN ROLE (email đầu = role cùng tên) =====
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = LEFT(u.email, CHARINDEX('@', u.email) - 1);

-- ===== PHÂN QUYỀN =====
-- admin: tất cả quyền
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'admin';

-- manager
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN ('catalog.manage','order.confirm','payment.refund',
                                 'inventory.manage','invoice.manage','ticket.handle','report.view')
WHERE r.code = 'manager';

-- staff
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN ('order.confirm','inventory.manage','invoice.manage','ticket.handle')
WHERE r.code = 'staff';

-- ===== ĐƠN VỊ VẬN CHUYỂN (FastShip là đối tác giả lập, chạy local) =====
INSERT INTO carriers (code, name, api_base_url, tracking_url_template, hotline, is_active) VALUES
  ('fastship',    N'FastShip Express (Đối Tác)', 'http://localhost:8081/api/v1', 'http://localhost:8082/track?code={code}', '19006868', 1),
  ('ghn',         N'Giao Hàng Nhanh (GHN)',      NULL, 'https://donhang.ghn.vn/?order_code={code}', '1900636677', 0),
  ('ghtk',        N'Giao Hàng Tiết Kiệm (GHTK)', NULL, 'https://i.ghtk.vn/{code}', '19006092', 0),
  ('viettelpost', N'Viettel Post',               NULL, 'https://viettelpost.com.vn/tra-cuu-hanh-trinh-don/?code={code}', '19008095', 0);

GO

-- CATALOG SNAPSHOT: mock/db.json, exported 2026-11-03.
-- Only Catalog rows. Identity IDs are assigned by SQL Server; references use slug/SKU/code.
-- Existing rows are preserved; rerunning adds only missing records.
-- product_price_history and voucher_usages are empty in the source, so no INSERTs.
USE badminton_shop;
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
-- Catalog compatibility with the current Java ProductVariant entity.
-- Mock has no weights: 0 is the Java default, not a measured shipping weight.
IF COL_LENGTH('dbo.product_variants', 'weight_grams') IS NULL
    ALTER TABLE dbo.product_variants ADD weight_grams INT NOT NULL CONSTRAINT DF_catalog_variant_weight DEFAULT (0) WITH VALUES;
IF COL_LENGTH('dbo.product_variants', 'created_at') IS NULL
    ALTER TABLE dbo.product_variants ADD created_at DATETIME2(0) NOT NULL CONSTRAINT DF_catalog_variant_created DEFAULT (SYSDATETIME()) WITH VALUES;
IF COL_LENGTH('dbo.product_variants', 'updated_at') IS NULL
    ALTER TABLE dbo.product_variants ADD updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_catalog_variant_updated DEFAULT (SYSDATETIME()) WITH VALUES;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    -- brands: 4 rows
    IF NOT EXISTS (SELECT 1 FROM dbo.brands WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'yonex')
        INSERT INTO dbo.brands (name, slug, logo_url, is_active)
        VALUES (N'Yonex', N'yonex', NULL, 1);
    IF NOT EXISTS (SELECT 1 FROM dbo.brands WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'victor')
        INSERT INTO dbo.brands (name, slug, logo_url, is_active)
        VALUES (N'Victor', N'victor', NULL, 1);
    IF NOT EXISTS (SELECT 1 FROM dbo.brands WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'li-ning')
        INSERT INTO dbo.brands (name, slug, logo_url, is_active)
        VALUES (N'Li-Ning', N'li-ning', NULL, 1);
    IF NOT EXISTS (SELECT 1 FROM dbo.brands WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'badminton-shop')
        INSERT INTO dbo.brands (name, slug, logo_url, is_active)
        VALUES (N'Badminton Shop', N'badminton-shop', NULL, 1);

    -- categories: 3 rows
    IF NOT EXISTS (SELECT 1 FROM dbo.categories WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'racket')
        INSERT INTO dbo.categories (parent_id, name, slug, is_active)
        VALUES (NULL, N'Vợt cầu lông', N'racket', 1);
    IF NOT EXISTS (SELECT 1 FROM dbo.categories WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'shoes')
        INSERT INTO dbo.categories (parent_id, name, slug, is_active)
        VALUES (NULL, N'Giày cầu lông', N'shoes', 1);
    IF NOT EXISTS (SELECT 1 FROM dbo.categories WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'apparel')
        INSERT INTO dbo.categories (parent_id, name, slug, is_active)
        VALUES (NULL, N'Trang phục', N'apparel', 1);

    -- products: 9 rows
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'yonex-astrox-88d-play-1')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'racket'), (SELECT id FROM dbo.brands WHERE slug = N'yonex'), N'racket', N'Yonex Astrox 88D Play', N'yonex-astrox-88d-play-1', N'Cân bằng sức mạnh và khả năng kiểm soát.', 1250000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'victor-thruster-k-220h-2')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'racket'), (SELECT id FROM dbo.brands WHERE slug = N'victor'), N'racket', N'Victor Thruster K 220H', N'victor-thruster-k-220h-2', N'Linh hoạt cho lối chơi tấn công.', 1450000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'li-ning-windstorm-72-3')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'racket'), (SELECT id FROM dbo.brands WHERE slug = N'li-ning'), N'racket', N'Li-Ning Windstorm 72', N'li-ning-windstorm-72-3', N'Trọng lượng nhẹ, xoay chuyển nhanh.', 1890000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'yonex-power-cushion-39-4')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'shoes'), (SELECT id FROM dbo.brands WHERE slug = N'yonex'), N'shoes', N'Yonex Power Cushion 39', N'yonex-power-cushion-39-4', N'Êm ái trong từng bước di chuyển.', 1650000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'victor-a170-ii-5')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'shoes'), (SELECT id FROM dbo.brands WHERE slug = N'victor'), N'shoes', N'Victor A170 II', N'victor-a170-ii-5', N'Đế bám sân, thoải mái khi luyện tập.', 1150000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'ao-cau-long-training-essential-6')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'apparel'), (SELECT id FROM dbo.brands WHERE slug = N'badminton-shop'), N'apparel', N'Áo cầu lông Training Essential', N'ao-cau-long-training-essential-6', N'Chất liệu thoáng nhẹ cho buổi tập mỗi ngày.', 290000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'demo-product-racket')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'racket'), (SELECT id FROM dbo.brands WHERE slug = N'yonex'), N'racket', N'Vợt luyện tập Club 2026 (Mẫu)', N'demo-product-racket', N'Sản phẩm mẫu để minh họa tồn kho và báo cáo doanh thu.', 1650000, 1, NULL, CONVERT(datetime2(0), '2026-05-01T08:00:00', 126), CONVERT(datetime2(0), '2026-05-01T08:00:00', 126));
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'demo-product-shoes')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'shoes'), (SELECT id FROM dbo.brands WHERE slug = N'yonex'), N'shoes', N'Giày thi đấu Court Pro (Mẫu)', N'demo-product-shoes', N'Sản phẩm mẫu để minh họa tồn kho và báo cáo doanh thu.', 1250000, 1, NULL, CONVERT(datetime2(0), '2026-05-01T08:00:00', 126), CONVERT(datetime2(0), '2026-05-01T08:00:00', 126));
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'demo-product-apparel')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'apparel'), (SELECT id FROM dbo.brands WHERE slug = N'yonex'), N'apparel', N'Áo thể thao Air Fit (Mẫu)', N'demo-product-apparel', N'Sản phẩm mẫu để minh họa tồn kho và báo cáo doanh thu.', 350000, 1, NULL, CONVERT(datetime2(0), '2026-05-01T08:00:00', 126), CONVERT(datetime2(0), '2026-05-01T08:00:00', 126));

    -- racket_specs: 4 rows
    IF NOT EXISTS (SELECT 1 FROM dbo.racket_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'yonex-astrox-88d-play-1'))
        INSERT INTO dbo.racket_specs (product_id, balance_type, flex, play_style, max_tension_lbs, material, shaft_diameter)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'yonex-astrox-88d-play-1'), N'even', N'medium', N'all_round', 28, N'Thông số mẫu — cập nhật theo nhà sản xuất', 7);
    IF NOT EXISTS (SELECT 1 FROM dbo.racket_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'victor-thruster-k-220h-2'))
        INSERT INTO dbo.racket_specs (product_id, balance_type, flex, play_style, max_tension_lbs, material, shaft_diameter)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'victor-thruster-k-220h-2'), N'even', N'medium', N'all_round', 28, N'Thông số mẫu — cập nhật theo nhà sản xuất', 7);
    IF NOT EXISTS (SELECT 1 FROM dbo.racket_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'li-ning-windstorm-72-3'))
        INSERT INTO dbo.racket_specs (product_id, balance_type, flex, play_style, max_tension_lbs, material, shaft_diameter)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'li-ning-windstorm-72-3'), N'even', N'medium', N'all_round', 28, N'Thông số mẫu — cập nhật theo nhà sản xuất', 7);
    IF NOT EXISTS (SELECT 1 FROM dbo.racket_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'demo-product-racket'))
        INSERT INTO dbo.racket_specs (product_id, balance_type, flex, play_style, max_tension_lbs, material, shaft_diameter)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'demo-product-racket'), N'even', N'medium', N'all_round', 28, N'Thông số mẫu — cập nhật theo nhà sản xuất', 7);

    -- shoe_specs: 3 rows
    IF NOT EXISTS (SELECT 1 FROM dbo.shoe_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'yonex-power-cushion-39-4'))
        INSERT INTO dbo.shoe_specs (product_id, gender, sole_type, technology, foot_width)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'yonex-power-cushion-39-4'), N'unisex', N'Đế cao su (mẫu)', N'Thông tin mẫu — cập nhật theo nhà sản xuất', N'regular');
    IF NOT EXISTS (SELECT 1 FROM dbo.shoe_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'victor-a170-ii-5'))
        INSERT INTO dbo.shoe_specs (product_id, gender, sole_type, technology, foot_width)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'victor-a170-ii-5'), N'unisex', N'Đế cao su (mẫu)', N'Thông tin mẫu — cập nhật theo nhà sản xuất', N'regular');
    IF NOT EXISTS (SELECT 1 FROM dbo.shoe_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'demo-product-shoes'))
        INSERT INTO dbo.shoe_specs (product_id, gender, sole_type, technology, foot_width)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'demo-product-shoes'), N'unisex', N'Đế cao su (mẫu)', N'Thông tin mẫu — cập nhật theo nhà sản xuất', N'regular');

    -- apparel_specs: 2 rows
    IF NOT EXISTS (SELECT 1 FROM dbo.apparel_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'ao-cau-long-training-essential-6'))
        INSERT INTO dbo.apparel_specs (product_id, gender, apparel_kind, material)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'ao-cau-long-training-essential-6'), N'unisex', N'shirt', N'Chất liệu mẫu — cập nhật theo nhà sản xuất');
    IF NOT EXISTS (SELECT 1 FROM dbo.apparel_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'demo-product-apparel'))
        INSERT INTO dbo.apparel_specs (product_id, gender, apparel_kind, material)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'demo-product-apparel'), N'unisex', N'shirt', N'Chất liệu mẫu — cập nhật theo nhà sản xuất');

    -- product_variants: 27 rows
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'BS-1-4U-G5')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'yonex-astrox-88d-play-1'), N'BS-1-4U-G5', NULL, N'Xanh biển', N'4U-G5', 1250000, 0, 4, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'BS-1-3U-G5')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'yonex-astrox-88d-play-1'), N'BS-1-3U-G5', NULL, N'Xanh biển', N'3U-G5', 1250000, 0, 13, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'BS-2-4U-G5')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'victor-thruster-k-220h-2'), N'BS-2-4U-G5', NULL, N'Trắng', N'4U-G5', 1450000, 0, 9, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'BS-2-3U-G5')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'victor-thruster-k-220h-2'), N'BS-2-3U-G5', NULL, N'Trắng', N'3U-G5', 1450000, 0, 13, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'BS-3-4U-G5')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'li-ning-windstorm-72-3'), N'BS-3-4U-G5', NULL, N'Xanh biển', N'4U-G5', 1890000, 0, 12, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'BS-3-3U-G5')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'li-ning-windstorm-72-3'), N'BS-3-3U-G5', NULL, N'Xanh biển', N'3U-G5', 1890000, 0, 13, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'BS-4-40')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'yonex-power-cushion-39-4'), N'BS-4-40', NULL, N'Trắng', N'40', 1650000, 0, 12, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'BS-4-41')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'yonex-power-cushion-39-4'), N'BS-4-41', NULL, N'Trắng', N'41', 1650000, 0, 13, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'BS-4-42')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'yonex-power-cushion-39-4'), N'BS-4-42', NULL, N'Trắng', N'42', 1650000, 0, 14, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'BS-4-43')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'yonex-power-cushion-39-4'), N'BS-4-43', NULL, N'Trắng', N'43', 1650000, 0, 15, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'BS-5-40')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'victor-a170-ii-5'), N'BS-5-40', NULL, N'Xanh biển', N'40', 1150000, 0, 12, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'BS-5-41')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'victor-a170-ii-5'), N'BS-5-41', NULL, N'Xanh biển', N'41', 1150000, 0, 13, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'BS-5-42')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'victor-a170-ii-5'), N'BS-5-42', NULL, N'Xanh biển', N'42', 1150000, 0, 14, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'BS-5-43')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'victor-a170-ii-5'), N'BS-5-43', NULL, N'Xanh biển', N'43', 1150000, 0, 15, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'BS-6-S')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'ao-cau-long-training-essential-6'), N'BS-6-S', NULL, N'Trắng', N'S', 290000, 0, 12, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'BS-6-M')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'ao-cau-long-training-essential-6'), N'BS-6-M', NULL, N'Trắng', N'M', 290000, 0, 13, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'BS-6-L')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'ao-cau-long-training-essential-6'), N'BS-6-L', NULL, N'Trắng', N'L', 290000, 0, 14, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'BS-6-XL')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'ao-cau-long-training-essential-6'), N'BS-6-XL', NULL, N'Trắng', N'XL', 290000, 0, 15, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'DEMO-RACKET-3U-G5')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'demo-product-racket'), N'DEMO-RACKET-3U-G5', NULL, N'Xanh', N'3U-G5', 1650000, 1100000, 0, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'DEMO-RACKET-4U-G5')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'demo-product-racket'), N'DEMO-RACKET-4U-G5', NULL, N'Trắng', N'4U-G5', 1650000, 1100000, 3, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'DEMO-RACKET-5U-G5')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'demo-product-racket'), N'DEMO-RACKET-5U-G5', NULL, N'Đen', N'5U-G5', 1650000, 1100000, 24, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'DEMO-SHOES-40')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'demo-product-shoes'), N'DEMO-SHOES-40', NULL, N'Xanh', N'40', 1250000, 780000, 4, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'DEMO-SHOES-41')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'demo-product-shoes'), N'DEMO-SHOES-41', NULL, N'Trắng', N'41', 1250000, 780000, 32, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'DEMO-SHOES-42')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'demo-product-shoes'), N'DEMO-SHOES-42', NULL, N'Đen', N'42', 1250000, 780000, 18, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'DEMO-APPAREL-S')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'demo-product-apparel'), N'DEMO-APPAREL-S', NULL, N'Xanh', N'S', 350000, 180000, 0, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'DEMO-APPAREL-M')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'demo-product-apparel'), N'DEMO-APPAREL-M', NULL, N'Trắng', N'M', 350000, 180000, 48, 5, 1, 0, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'DEMO-APPAREL-L')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'demo-product-apparel'), N'DEMO-APPAREL-L', NULL, N'Đen', N'L', 350000, 180000, 65, 5, 1, 0, SYSDATETIME(), SYSDATETIME());

    -- product_images: 9 rows
    IF NOT EXISTS (SELECT 1 FROM dbo.product_images WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'yonex-astrox-88d-play-1') AND variant_id IS NULL AND url = N'/products/racket.svg' AND sort_order = 0)
        INSERT INTO dbo.product_images (product_id, variant_id, url, sort_order)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'yonex-astrox-88d-play-1'), NULL, N'/products/racket.svg', 0);
    IF NOT EXISTS (SELECT 1 FROM dbo.product_images WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'victor-thruster-k-220h-2') AND variant_id IS NULL AND url = N'/products/racket.svg' AND sort_order = 0)
        INSERT INTO dbo.product_images (product_id, variant_id, url, sort_order)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'victor-thruster-k-220h-2'), NULL, N'/products/racket.svg', 0);
    IF NOT EXISTS (SELECT 1 FROM dbo.product_images WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'li-ning-windstorm-72-3') AND variant_id IS NULL AND url = N'/products/racket.svg' AND sort_order = 0)
        INSERT INTO dbo.product_images (product_id, variant_id, url, sort_order)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'li-ning-windstorm-72-3'), NULL, N'/products/racket.svg', 0);
    IF NOT EXISTS (SELECT 1 FROM dbo.product_images WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'yonex-power-cushion-39-4') AND variant_id IS NULL AND url = N'/products/shoes.svg' AND sort_order = 0)
        INSERT INTO dbo.product_images (product_id, variant_id, url, sort_order)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'yonex-power-cushion-39-4'), NULL, N'/products/shoes.svg', 0);
    IF NOT EXISTS (SELECT 1 FROM dbo.product_images WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'victor-a170-ii-5') AND variant_id IS NULL AND url = N'/products/shoes.svg' AND sort_order = 0)
        INSERT INTO dbo.product_images (product_id, variant_id, url, sort_order)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'victor-a170-ii-5'), NULL, N'/products/shoes.svg', 0);
    IF NOT EXISTS (SELECT 1 FROM dbo.product_images WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'ao-cau-long-training-essential-6') AND variant_id IS NULL AND url = N'/products/apparel.svg' AND sort_order = 0)
        INSERT INTO dbo.product_images (product_id, variant_id, url, sort_order)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'ao-cau-long-training-essential-6'), NULL, N'/products/apparel.svg', 0);
    IF NOT EXISTS (SELECT 1 FROM dbo.product_images WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'demo-product-racket') AND variant_id IS NULL AND url = N'/products/racket.svg' AND sort_order = 0)
        INSERT INTO dbo.product_images (product_id, variant_id, url, sort_order)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'demo-product-racket'), NULL, N'/products/racket.svg', 0);
    IF NOT EXISTS (SELECT 1 FROM dbo.product_images WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'demo-product-shoes') AND variant_id IS NULL AND url = N'/products/shoes.svg' AND sort_order = 0)
        INSERT INTO dbo.product_images (product_id, variant_id, url, sort_order)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'demo-product-shoes'), NULL, N'/products/shoes.svg', 0);
    IF NOT EXISTS (SELECT 1 FROM dbo.product_images WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'demo-product-apparel') AND variant_id IS NULL AND url = N'/products/apparel.svg' AND sort_order = 0)
        INSERT INTO dbo.product_images (product_id, variant_id, url, sort_order)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'demo-product-apparel'), NULL, N'/products/apparel.svg', 0);

    -- vouchers: 2 rows
    IF NOT EXISTS (SELECT 1 FROM dbo.vouchers WITH (UPDLOCK, HOLDLOCK) WHERE code = N'DEMO10')
        INSERT INTO dbo.vouchers (code, name, description, discount_type, discount_value, min_order, max_discount, usage_limit, per_user_limit, used_count, applies_to, starts_at, ends_at, is_active, created_by)
        VALUES (N'DEMO10', N'Ưu đãi mẫu 10%', N'Voucher mẫu để thử chức năng đặt hàng và báo cáo. Có thể sửa hoặc tạm ẩn.', N'percent', 10, 500000, 200000, 100, 5, 0, N'all', CONVERT(datetime2(0), '2026-01-01T00:00:00', 126), CONVERT(datetime2(0), '2027-12-31T23:59:59', 126), 1, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.vouchers WITH (UPDLOCK, HOLDLOCK) WHERE code = N'BRAND50')
        INSERT INTO dbo.vouchers (code, name, description, discount_type, discount_value, min_order, max_discount, usage_limit, per_user_limit, used_count, applies_to, starts_at, ends_at, is_active, created_by)
        VALUES (N'BRAND50', N'Giảm mẫu 50.000đ theo thương hiệu', N'Voucher mẫu để thử chức năng đặt hàng và báo cáo. Có thể sửa hoặc tạm ẩn.', N'fixed', 50000, 500000, 200000, 100, 5, 0, N'brand', CONVERT(datetime2(0), '2026-01-01T00:00:00', 126), CONVERT(datetime2(0), '2027-12-31T23:59:59', 126), 1, NULL);

    -- voucher_scopes: 1 rows
    IF NOT EXISTS (SELECT 1 FROM dbo.voucher_scopes WITH (UPDLOCK, HOLDLOCK) WHERE voucher_id = (SELECT id FROM dbo.vouchers WHERE code = N'BRAND50') AND scope_type = N'brand' AND scope_id = (SELECT id FROM dbo.brands WHERE slug = N'yonex'))
        INSERT INTO dbo.voucher_scopes (voucher_id, scope_type, scope_id)
        VALUES ((SELECT id FROM dbo.vouchers WHERE code = N'BRAND50'), N'brand', (SELECT id FROM dbo.brands WHERE slug = N'yonex'));

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

-- =====================================================================
-- INVENTORY SNAPSHOT: mock/db.json + mock/actions/inventory.mjs, 2026-11-04
-- 67 source rows. SQL Server assigns numeric identity IDs.
-- Keep source quantities, costs (including 0), statuses and nullable actors.
-- Timestamps use Vietnam local time; DATETIME2(0) rounds fractional seconds.
-- diff_qty is computed by SQL Server, never inserted.
-- This is historical data: DO NOT apply the movements to stock_quantity again.
-- Requires the Catalog seed above. Re-run only this section on an existing DB.
-- Mock Catalog arrays are now empty: legacy variant IDs map to SKUs from
-- the previously exported Catalog snapshot above; Inventory values are unchanged.
-- Existing matching rows are preserved; no UPDATE/DELETE of business data.
-- =====================================================================
USE badminton_shop;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO
BEGIN TRY
    BEGIN TRANSACTION;

    -- Validate referenced SKUs before writing any Inventory data.
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'BS-1-4U-G5')
        THROW 51001, N'Missing Catalog SKU: BS-1-4U-G5', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'BS-1-3U-G5')
        THROW 51001, N'Missing Catalog SKU: BS-1-3U-G5', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'BS-2-4U-G5')
        THROW 51001, N'Missing Catalog SKU: BS-2-4U-G5', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'BS-2-3U-G5')
        THROW 51001, N'Missing Catalog SKU: BS-2-3U-G5', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'BS-3-4U-G5')
        THROW 51001, N'Missing Catalog SKU: BS-3-4U-G5', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'BS-3-3U-G5')
        THROW 51001, N'Missing Catalog SKU: BS-3-3U-G5', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'BS-4-40')
        THROW 51001, N'Missing Catalog SKU: BS-4-40', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'BS-4-41')
        THROW 51001, N'Missing Catalog SKU: BS-4-41', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'BS-4-42')
        THROW 51001, N'Missing Catalog SKU: BS-4-42', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'BS-4-43')
        THROW 51001, N'Missing Catalog SKU: BS-4-43', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'BS-5-40')
        THROW 51001, N'Missing Catalog SKU: BS-5-40', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'BS-5-41')
        THROW 51001, N'Missing Catalog SKU: BS-5-41', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'BS-5-42')
        THROW 51001, N'Missing Catalog SKU: BS-5-42', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'BS-5-43')
        THROW 51001, N'Missing Catalog SKU: BS-5-43', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'BS-6-S')
        THROW 51001, N'Missing Catalog SKU: BS-6-S', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'BS-6-M')
        THROW 51001, N'Missing Catalog SKU: BS-6-M', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'BS-6-L')
        THROW 51001, N'Missing Catalog SKU: BS-6-L', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'BS-6-XL')
        THROW 51001, N'Missing Catalog SKU: BS-6-XL', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-3U-G5')
        THROW 51001, N'Missing Catalog SKU: DEMO-RACKET-3U-G5', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-4U-G5')
        THROW 51001, N'Missing Catalog SKU: DEMO-RACKET-4U-G5', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-5U-G5')
        THROW 51001, N'Missing Catalog SKU: DEMO-RACKET-5U-G5', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'DEMO-SHOES-40')
        THROW 51001, N'Missing Catalog SKU: DEMO-SHOES-40', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'DEMO-SHOES-41')
        THROW 51001, N'Missing Catalog SKU: DEMO-SHOES-41', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'DEMO-SHOES-42')
        THROW 51001, N'Missing Catalog SKU: DEMO-SHOES-42', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'DEMO-APPAREL-S')
        THROW 51001, N'Missing Catalog SKU: DEMO-APPAREL-S', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'DEMO-APPAREL-M')
        THROW 51001, N'Missing Catalog SKU: DEMO-APPAREL-M', 1;
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WHERE sku = N'DEMO-APPAREL-L')
        THROW 51001, N'Missing Catalog SKU: DEMO-APPAREL-L', 1;
    IF (SELECT COUNT(*) FROM dbo.suppliers WHERE name = N'Công ty Cầu lông Việt' AND email = N'sales@caulongviet.vn') > 1
        THROW 51002, N'Ambiguous supplier mapping; resolve duplicate supplier names/emails first.', 1;
    IF (SELECT COUNT(*) FROM dbo.suppliers WHERE name = N'Nhà phân phối Yonex Việt Nam' AND email = N'orders@yonex-vn.vn') > 1
        THROW 51002, N'Ambiguous supplier mapping; resolve duplicate supplier names/emails first.', 1;
    IF (SELECT COUNT(*) FROM dbo.suppliers WHERE name = N'Nhà phân phối thể thao tổng hợp' AND email = N'supply@sport-vn.vn') > 1
        THROW 51002, N'Ambiguous supplier mapping; resolve duplicate supplier names/emails first.', 1;

    -- suppliers: 3 rows
    IF NOT EXISTS (SELECT 1 FROM dbo.suppliers WITH (UPDLOCK, HOLDLOCK) WHERE name = N'Công ty Cầu lông Việt' AND email = N'sales@caulongviet.vn')
        INSERT INTO dbo.suppliers (name, phone, email, address)
        VALUES (N'Công ty Cầu lông Việt', N'02838220001', N'sales@caulongviet.vn', N'Quận 5, TP. Hồ Chí Minh');
    IF NOT EXISTS (SELECT 1 FROM dbo.suppliers WITH (UPDLOCK, HOLDLOCK) WHERE name = N'Nhà phân phối Yonex Việt Nam' AND email = N'orders@yonex-vn.vn')
        INSERT INTO dbo.suppliers (name, phone, email, address)
        VALUES (N'Nhà phân phối Yonex Việt Nam', N'02838220002', N'orders@yonex-vn.vn', N'Quận 1, TP. Hồ Chí Minh');
    IF NOT EXISTS (SELECT 1 FROM dbo.suppliers WITH (UPDLOCK, HOLDLOCK) WHERE name = N'Nhà phân phối thể thao tổng hợp' AND email = N'supply@sport-vn.vn')
        INSERT INTO dbo.suppliers (name, phone, email, address)
        VALUES (N'Nhà phân phối thể thao tổng hợp', N'02438220003', N'supply@sport-vn.vn', N'Cầu Giấy, Hà Nội');

    -- goods_receipts: 1 rows
    IF NOT EXISTS (SELECT 1 FROM dbo.goods_receipts WITH (UPDLOCK, HOLDLOCK) WHERE receipt_code = N'GR-1792246254460-6B16FD')
        INSERT INTO dbo.goods_receipts (receipt_code, supplier_id, received_by, total_cost, note, received_at)
        VALUES (N'GR-1792246254460-6B16FD', (SELECT id FROM dbo.suppliers WHERE name = N'Công ty Cầu lông Việt' AND email = N'sales@caulongviet.vn'), NULL, 0, NULL, CONVERT(datetime2(0), '2026-10-17T21:10:54.460', 126));

    -- goods_receipt_items: 1 rows
    IF NOT EXISTS (SELECT 1 FROM dbo.goods_receipt_items WITH (UPDLOCK, HOLDLOCK) WHERE receipt_id = (SELECT id FROM dbo.goods_receipts WHERE receipt_code = N'GR-1792246254460-6B16FD') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-1-4U-G5'))
        INSERT INTO dbo.goods_receipt_items (receipt_id, variant_id, quantity, unit_cost)
        VALUES ((SELECT id FROM dbo.goods_receipts WHERE receipt_code = N'GR-1792246254460-6B16FD'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-1-4U-G5'), 2, 0);

    -- stock_audits: 2 rows
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audits WITH (UPDLOCK, HOLDLOCK) WHERE audit_code = N'AUD-1792246550932-BBF689')
        INSERT INTO dbo.stock_audits (audit_code, status, conducted_by, note, started_at, completed_at)
        VALUES (N'AUD-1792246550932-BBF689', N'cancelled', NULL, NULL, CONVERT(datetime2(0), '2026-10-17T21:15:50.932', 126), NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audits WITH (UPDLOCK, HOLDLOCK) WHERE audit_code = N'AUD-1792246578025-96613B')
        INSERT INTO dbo.stock_audits (audit_code, status, conducted_by, note, started_at, completed_at)
        VALUES (N'AUD-1792246578025-96613B', N'draft', NULL, NULL, CONVERT(datetime2(0), '2026-10-17T21:16:18.025', 126), NULL);

    -- stock_audit_items: 54 rows
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-1-4U-G5'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-1-4U-G5'), 4, 4, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-1-3U-G5'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-1-3U-G5'), 13, 13, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-2-4U-G5'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-2-4U-G5'), 9, 9, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-2-3U-G5'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-2-3U-G5'), 13, 13, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-3-4U-G5'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-3-4U-G5'), 12, 12, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-3-3U-G5'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-3-3U-G5'), 13, 13, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-4-40'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-4-40'), 12, 12, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-4-41'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-4-41'), 13, 13, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-4-42'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-4-42'), 14, 14, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-4-43'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-4-43'), 15, 15, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-5-40'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-5-40'), 12, 12, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-5-41'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-5-41'), 13, 13, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-5-42'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-5-42'), 14, 14, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-5-43'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-5-43'), 15, 15, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-6-S'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-6-S'), 12, 12, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-6-M'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-6-M'), 13, 13, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-6-L'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-6-L'), 14, 14, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-6-XL'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-6-XL'), 15, 15, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-3U-G5'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-3U-G5'), 0, 0, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-4U-G5'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-4U-G5'), 3, 3, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-5U-G5'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-5U-G5'), 24, 24, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-SHOES-40'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-SHOES-40'), 4, 4, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-SHOES-41'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-SHOES-41'), 32, 32, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-SHOES-42'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-SHOES-42'), 18, 18, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-APPAREL-S'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-APPAREL-S'), 0, 0, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-APPAREL-M'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-APPAREL-M'), 48, 48, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-APPAREL-L'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246550932-BBF689'), (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-APPAREL-L'), 65, 65, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-1-4U-G5'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-1-4U-G5'), 4, 4, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-1-3U-G5'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-1-3U-G5'), 13, 13, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-2-4U-G5'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-2-4U-G5'), 9, 9, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-2-3U-G5'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-2-3U-G5'), 13, 13, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-3-4U-G5'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-3-4U-G5'), 12, 12, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-3-3U-G5'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-3-3U-G5'), 13, 13, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-4-40'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-4-40'), 12, 12, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-4-41'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-4-41'), 13, 13, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-4-42'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-4-42'), 14, 14, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-4-43'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-4-43'), 15, 15, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-5-40'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-5-40'), 12, 12, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-5-41'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-5-41'), 13, 13, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-5-42'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-5-42'), 14, 14, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-5-43'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-5-43'), 15, 15, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-6-S'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-6-S'), 12, 12, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-6-M'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-6-M'), 13, 13, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-6-L'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-6-L'), 14, 14, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-6-XL'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'BS-6-XL'), 15, 15, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-3U-G5'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-3U-G5'), 0, 0, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-4U-G5'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-4U-G5'), 3, 3, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-5U-G5'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-5U-G5'), 24, 24, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-SHOES-40'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-SHOES-40'), 4, 4, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-SHOES-41'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-SHOES-41'), 32, 32, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-SHOES-42'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-SHOES-42'), 18, 18, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-APPAREL-S'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-APPAREL-S'), 0, 0, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-APPAREL-M'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-APPAREL-M'), 48, 48, NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_audit_items WITH (UPDLOCK, HOLDLOCK) WHERE audit_id = (SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B') AND variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-APPAREL-L'))
        INSERT INTO dbo.stock_audit_items (audit_id, variant_id, system_qty, counted_qty, note)
        VALUES ((SELECT id FROM dbo.stock_audits WHERE audit_code = N'AUD-1792246578025-96613B'), (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-APPAREL-L'), 65, 65, NULL);

    -- inventory_movements: 1 rows
    IF NOT EXISTS (SELECT 1 FROM dbo.inventory_movements WITH (UPDLOCK, HOLDLOCK) WHERE variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-1-4U-G5') AND movement_type = N'stock_in' AND quantity = 2 AND balance_after = 4 AND ref_type = N'goods_receipt' AND ref_id = (SELECT id FROM dbo.goods_receipts WHERE receipt_code = N'GR-1792246254460-6B16FD') AND created_at = CONVERT(datetime2(0), '2026-10-17T21:10:54.460', 126))
        INSERT INTO dbo.inventory_movements (variant_id, movement_type, quantity, balance_after, ref_type, ref_id, created_by, note, created_at)
        VALUES ((SELECT id FROM dbo.product_variants WHERE sku = N'BS-1-4U-G5'), N'stock_in', 2, 4, N'goods_receipt', (SELECT id FROM dbo.goods_receipts WHERE receipt_code = N'GR-1792246254460-6B16FD'), NULL, NULL, CONVERT(datetime2(0), '2026-10-17T21:10:54.460', 126));

    -- stock_alerts: 5 rows
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_alerts WITH (UPDLOCK, HOLDLOCK) WHERE variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'BS-1-4U-G5') AND created_at = CONVERT(datetime2(0), '2026-10-17T21:01:34.951', 126))
        INSERT INTO dbo.stock_alerts (variant_id, stock_at_alert, threshold, status, created_at, resolved_at)
        VALUES ((SELECT id FROM dbo.product_variants WHERE sku = N'BS-1-4U-G5'), 4, 5, N'open', CONVERT(datetime2(0), '2026-10-17T21:01:34.951', 126), NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_alerts WITH (UPDLOCK, HOLDLOCK) WHERE variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-3U-G5') AND created_at = CONVERT(datetime2(0), '2026-10-17T21:01:34.951', 126))
        INSERT INTO dbo.stock_alerts (variant_id, stock_at_alert, threshold, status, created_at, resolved_at)
        VALUES ((SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-3U-G5'), 0, 5, N'open', CONVERT(datetime2(0), '2026-10-17T21:01:34.951', 126), NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_alerts WITH (UPDLOCK, HOLDLOCK) WHERE variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-4U-G5') AND created_at = CONVERT(datetime2(0), '2026-10-17T21:01:34.951', 126))
        INSERT INTO dbo.stock_alerts (variant_id, stock_at_alert, threshold, status, created_at, resolved_at)
        VALUES ((SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-RACKET-4U-G5'), 3, 5, N'open', CONVERT(datetime2(0), '2026-10-17T21:01:34.951', 126), NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_alerts WITH (UPDLOCK, HOLDLOCK) WHERE variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-SHOES-40') AND created_at = CONVERT(datetime2(0), '2026-10-17T21:01:34.951', 126))
        INSERT INTO dbo.stock_alerts (variant_id, stock_at_alert, threshold, status, created_at, resolved_at)
        VALUES ((SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-SHOES-40'), 4, 5, N'open', CONVERT(datetime2(0), '2026-10-17T21:01:34.951', 126), NULL);
    IF NOT EXISTS (SELECT 1 FROM dbo.stock_alerts WITH (UPDLOCK, HOLDLOCK) WHERE variant_id = (SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-APPAREL-S') AND created_at = CONVERT(datetime2(0), '2026-10-17T21:01:34.951', 126))
        INSERT INTO dbo.stock_alerts (variant_id, stock_at_alert, threshold, status, created_at, resolved_at)
        VALUES ((SELECT id FROM dbo.product_variants WHERE sku = N'DEMO-APPAREL-S'), 0, 5, N'open', CONVERT(datetime2(0), '2026-10-17T21:01:34.951', 126), NULL);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

-- =====================================================================
-- INVOICE SNAPSHOT: mock/db.json, 2026-11-05
-- 52 invoices, 100 invoice items, 1 delivery; 51 required order headers.
-- Source invoice amounts/statuses are preserved, including replaced invoices.
-- UUIDs map by invoice_no/order_code; SQL Server assigns identity IDs.
-- All source order user_id values are 'demo-customer', absent from users.
-- Their nullable user_id is NULL: no real customer identity is invented.
-- Buyer and receiver snapshots are preserved. No users/payments/order items added.
-- Vietnam local time (+07:00), fractional seconds rounded by DATETIME2(0).
-- Run this section alone for existing DBs; it preserves matching records.
-- =====================================================================
USE badminton_shop;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO
BEGIN TRY
    BEGIN TRANSACTION;

    -- orders: 51 rows
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'BS-1792148654606-01a0') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'BS-1792148654606-01a0', NULL, N'online', N'pending', N'unpaid', N'Phạm Thành', N'0326538343', N'Hạ Long, Quảng Ninh', NULL, NULL, 2500000, 0, 0, 2500000, N'dewde', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-16T18:04:14.606', 126), CONVERT(datetime2(0), '2026-10-16T18:04:14.606', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'BS-1792149064570-a8b5') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'BS-1792149064570-a8b5', NULL, N'online', N'completed', N'paid', N'Phạm Thành', N'0326538343', N'Hạ Longfcdswvsd', NULL, NULL, 1250000, 0, 30000, 1280000, N'xsac', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-16T18:11:04.570', 126), CONVERT(datetime2(0), '2026-10-16T18:11:04.570', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'BS-1792149369962-0309') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'BS-1792149369962-0309', NULL, N'online', N'pending', N'unpaid', N'Phạm Thành', N'0326538343', N'Hạ Longfcdswvsd', NULL, NULL, 1250000, 0, 30000, 1280000, N'', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-16T18:16:09.962', 126), CONVERT(datetime2(0), '2026-10-16T18:16:09.962', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202605-001') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202605-001', NULL, N'in_store', N'completed', N'paid', N'Nguyễn Minh Anh', N'0900000001', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 4550000, 100000, 0, 4450000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-05-01T10:00:00.000', 126), CONVERT(datetime2(0), '2026-05-01T10:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202605-002') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202605-002', NULL, N'online', N'completed', N'paid', N'Trần Hoàng Nam', N'0900000002', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 4000000, 0, 0, 4000000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-05-03T11:00:00.000', 126), CONVERT(datetime2(0), '2026-05-03T11:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202605-003') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202605-003', NULL, N'online', N'completed', N'paid', N'Lê Thảo Vy', N'0900000003', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 4350000, 0, 0, 4350000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-05-05T12:00:00.000', 126), CONVERT(datetime2(0), '2026-05-05T12:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202605-004') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202605-004', NULL, N'in_store', N'completed', N'paid', N'Phạm Quốc Huy', N'0900000004', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 2850000, 0, 0, 2850000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-05-08T13:00:00.000', 126), CONVERT(datetime2(0), '2026-05-08T13:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202605-005') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202605-005', NULL, N'online', N'completed', N'paid', N'Võ Ngọc Linh', N'0900000005', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 5800000, 100000, 0, 5700000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-05-10T14:00:00.000', 126), CONVERT(datetime2(0), '2026-05-10T14:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202605-006') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202605-006', NULL, N'online', N'completed', N'paid', N'Đặng Tuấn Kiệt', N'0900000006', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 7450000, 0, 0, 7450000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-05-13T15:00:00.000', 126), CONVERT(datetime2(0), '2026-05-13T15:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202605-007') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202605-007', NULL, N'in_store', N'completed', N'paid', N'Nguyễn Minh Anh', N'0900000007', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 2350000, 0, 0, 2350000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-05-15T16:00:00.000', 126), CONVERT(datetime2(0), '2026-05-15T16:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202605-008') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202605-008', NULL, N'online', N'completed', N'paid', N'Trần Hoàng Nam', N'0900000008', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 3200000, 0, 0, 3200000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-05-18T17:00:00.000', 126), CONVERT(datetime2(0), '2026-05-18T17:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202606-001') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202606-001', NULL, N'in_store', N'completed', N'paid', N'Nguyễn Minh Anh', N'0900000001', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 1600000, 80000, 30000, 1550000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-06-01T10:00:00.000', 126), CONVERT(datetime2(0), '2026-06-01T10:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202606-002') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202606-002', NULL, N'online', N'completed', N'paid', N'Trần Hoàng Nam', N'0900000002', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 4150000, 0, 0, 4150000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-06-03T11:00:00.000', 126), CONVERT(datetime2(0), '2026-06-03T11:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202606-003') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202606-003', NULL, N'online', N'completed', N'paid', N'Lê Thảo Vy', N'0900000003', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 2700000, 0, 0, 2700000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-06-05T12:00:00.000', 126), CONVERT(datetime2(0), '2026-06-05T12:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202606-004') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202606-004', NULL, N'in_store', N'completed', N'paid', N'Phạm Quốc Huy', N'0900000004', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 2000000, 0, 0, 2000000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-06-08T13:00:00.000', 126), CONVERT(datetime2(0), '2026-06-08T13:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202606-005') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202606-005', NULL, N'online', N'completed', N'paid', N'Võ Ngọc Linh', N'0900000005', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 1950000, 97500, 30000, 1882500, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-06-10T14:00:00.000', 126), CONVERT(datetime2(0), '2026-06-10T14:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202606-006') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202606-006', NULL, N'online', N'completed', N'paid', N'Đặng Tuấn Kiệt', N'0900000006', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 6200000, 0, 0, 6200000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-06-13T15:00:00.000', 126), CONVERT(datetime2(0), '2026-06-13T15:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202606-007') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202606-007', NULL, N'in_store', N'completed', N'paid', N'Nguyễn Minh Anh', N'0900000007', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 2900000, 0, 0, 2900000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-06-15T16:00:00.000', 126), CONVERT(datetime2(0), '2026-06-15T16:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202606-008') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202606-008', NULL, N'online', N'completed', N'paid', N'Trần Hoàng Nam', N'0900000008', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 3650000, 0, 0, 3650000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-06-18T17:00:00.000', 126), CONVERT(datetime2(0), '2026-06-18T17:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202607-001') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202607-001', NULL, N'in_store', N'completed', N'paid', N'Nguyễn Minh Anh', N'0900000001', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 1950000, 97500, 30000, 1882500, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-07-01T10:00:00.000', 126), CONVERT(datetime2(0), '2026-07-01T10:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202607-002') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202607-002', NULL, N'online', N'completed', N'paid', N'Trần Hoàng Nam', N'0900000002', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 3200000, 0, 0, 3200000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-07-03T11:00:00.000', 126), CONVERT(datetime2(0), '2026-07-03T11:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202607-003') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202607-003', NULL, N'online', N'completed', N'paid', N'Lê Thảo Vy', N'0900000003', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 7050000, 0, 0, 7050000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-07-05T12:00:00.000', 126), CONVERT(datetime2(0), '2026-07-05T12:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202607-004') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202607-004', NULL, N'in_store', N'completed', N'paid', N'Phạm Quốc Huy', N'0900000004', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 3650000, 0, 0, 3650000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-07-08T13:00:00.000', 126), CONVERT(datetime2(0), '2026-07-08T13:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202607-005') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202607-005', NULL, N'online', N'completed', N'paid', N'Võ Ngọc Linh', N'0900000005', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 4000000, 100000, 0, 3900000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-07-10T14:00:00.000', 126), CONVERT(datetime2(0), '2026-07-10T14:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202607-006') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202607-006', NULL, N'online', N'completed', N'paid', N'Đặng Tuấn Kiệt', N'0900000006', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 3550000, 0, 0, 3550000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-07-13T15:00:00.000', 126), CONVERT(datetime2(0), '2026-07-13T15:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202607-007') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202607-007', NULL, N'in_store', N'completed', N'paid', N'Nguyễn Minh Anh', N'0900000007', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 4150000, 0, 0, 4150000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-07-15T16:00:00.000', 126), CONVERT(datetime2(0), '2026-07-15T16:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202607-008') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202607-008', NULL, N'online', N'completed', N'paid', N'Trần Hoàng Nam', N'0900000008', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 5800000, 0, 0, 5800000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-07-18T17:00:00.000', 126), CONVERT(datetime2(0), '2026-07-18T17:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202608-001') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202608-001', NULL, N'in_store', N'completed', N'paid', N'Nguyễn Minh Anh', N'0900000001', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 2000000, 100000, 0, 1900000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-08-01T10:00:00.000', 126), CONVERT(datetime2(0), '2026-08-01T10:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202608-002') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202608-002', NULL, N'online', N'completed', N'paid', N'Trần Hoàng Nam', N'0900000002', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 2850000, 0, 0, 2850000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-08-03T11:00:00.000', 126), CONVERT(datetime2(0), '2026-08-03T11:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202608-003') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202608-003', NULL, N'online', N'completed', N'paid', N'Lê Thảo Vy', N'0900000003', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 4100000, 0, 0, 4100000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-08-05T12:00:00.000', 126), CONVERT(datetime2(0), '2026-08-05T12:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202608-004') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202608-004', NULL, N'in_store', N'completed', N'paid', N'Phạm Quốc Huy', N'0900000004', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 2900000, 0, 0, 2900000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-08-08T13:00:00.000', 126), CONVERT(datetime2(0), '2026-08-08T13:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202608-005') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202608-005', NULL, N'online', N'completed', N'paid', N'Võ Ngọc Linh', N'0900000005', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 2350000, 100000, 0, 2250000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-08-10T14:00:00.000', 126), CONVERT(datetime2(0), '2026-08-10T14:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202608-006') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202608-006', NULL, N'online', N'completed', N'paid', N'Đặng Tuấn Kiệt', N'0900000006', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 2700000, 0, 0, 2700000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-08-13T15:00:00.000', 126), CONVERT(datetime2(0), '2026-08-13T15:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202608-007') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202608-007', NULL, N'in_store', N'completed', N'paid', N'Nguyễn Minh Anh', N'0900000007', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 1600000, 0, 30000, 1630000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-08-15T16:00:00.000', 126), CONVERT(datetime2(0), '2026-08-15T16:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202608-008') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202608-008', NULL, N'online', N'completed', N'paid', N'Trần Hoàng Nam', N'0900000008', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 4550000, 0, 0, 4550000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-08-18T17:00:00.000', 126), CONVERT(datetime2(0), '2026-08-18T17:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202609-001') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202609-001', NULL, N'in_store', N'completed', N'paid', N'Nguyễn Minh Anh', N'0900000001', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 4150000, 100000, 0, 4050000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-09-01T10:00:00.000', 126), CONVERT(datetime2(0), '2026-09-01T10:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202609-002') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202609-002', NULL, N'online', N'completed', N'paid', N'Trần Hoàng Nam', N'0900000002', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 4000000, 0, 0, 4000000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-09-03T11:00:00.000', 126), CONVERT(datetime2(0), '2026-09-03T11:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202609-003') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202609-003', NULL, N'online', N'completed', N'paid', N'Lê Thảo Vy', N'0900000003', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 4450000, 0, 0, 4450000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-09-05T12:00:00.000', 126), CONVERT(datetime2(0), '2026-09-05T12:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202609-004') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202609-004', NULL, N'in_store', N'completed', N'paid', N'Phạm Quốc Huy', N'0900000004', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 1950000, 0, 30000, 1980000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-09-08T13:00:00.000', 126), CONVERT(datetime2(0), '2026-09-08T13:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202609-005') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202609-005', NULL, N'online', N'completed', N'paid', N'Võ Ngọc Linh', N'0900000005', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 5800000, 100000, 0, 5700000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-09-10T14:00:00.000', 126), CONVERT(datetime2(0), '2026-09-10T14:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202609-006') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202609-006', NULL, N'online', N'completed', N'paid', N'Đặng Tuấn Kiệt', N'0900000006', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 4350000, 0, 0, 4350000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-09-13T15:00:00.000', 126), CONVERT(datetime2(0), '2026-09-13T15:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202609-007') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202609-007', NULL, N'in_store', N'completed', N'paid', N'Nguyễn Minh Anh', N'0900000007', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 3650000, 0, 0, 3650000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-09-15T16:00:00.000', 126), CONVERT(datetime2(0), '2026-09-15T16:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202609-008') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202609-008', NULL, N'online', N'completed', N'paid', N'Trần Hoàng Nam', N'0900000008', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 3200000, 0, 0, 3200000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-09-18T17:00:00.000', 126), CONVERT(datetime2(0), '2026-09-18T17:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202610-001') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202610-001', NULL, N'in_store', N'completed', N'paid', N'Nguyễn Minh Anh', N'0900000001', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 2900000, 100000, 0, 2800000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-01T10:00:00.000', 126), CONVERT(datetime2(0), '2026-10-01T10:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202610-002') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202610-002', NULL, N'online', N'completed', N'paid', N'Trần Hoàng Nam', N'0900000002', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 4550000, 0, 0, 4550000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-02T11:00:00.000', 126), CONVERT(datetime2(0), '2026-10-02T11:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202610-003') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202610-003', NULL, N'online', N'completed', N'paid', N'Lê Thảo Vy', N'0900000003', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 5300000, 0, 0, 5300000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-03T12:00:00.000', 126), CONVERT(datetime2(0), '2026-10-03T12:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202610-004') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202610-004', NULL, N'in_store', N'completed', N'paid', N'Phạm Quốc Huy', N'0900000004', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 1600000, 0, 30000, 1630000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-05T13:00:00.000', 126), CONVERT(datetime2(0), '2026-10-05T13:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202610-005') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202610-005', NULL, N'online', N'completed', N'paid', N'Võ Ngọc Linh', N'0900000005', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 2850000, 100000, 0, 2750000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-06T14:00:00.000', 126), CONVERT(datetime2(0), '2026-10-06T14:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202610-006') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202610-006', NULL, N'online', N'completed', N'paid', N'Đặng Tuấn Kiệt', N'0900000006', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 5400000, 0, 0, 5400000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-08T15:00:00.000', 126), CONVERT(datetime2(0), '2026-10-08T15:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202610-007') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202610-007', NULL, N'in_store', N'completed', N'paid', N'Nguyễn Minh Anh', N'0900000007', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 2000000, 0, 0, 2000000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-09T16:00:00.000', 126), CONVERT(datetime2(0), '2026-10-09T16:00:00.000', 126));
    IF (SELECT COUNT(*) FROM dbo.orders WITH (UPDLOCK, HOLDLOCK) WHERE order_code = N'DEMO-202610-008') < 1
        INSERT INTO dbo.orders (order_code, user_id, channel, status, payment_status, receiver_name, receiver_phone, shipping_address, voucher_id, voucher_code, subtotal, discount_amount, shipping_fee, total_amount, note, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, created_at, updated_at)
        VALUES (N'DEMO-202610-008', NULL, N'online', N'completed', N'paid', N'Trần Hoàng Nam', N'0900000008', N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, NULL, 2350000, 0, 0, 2350000, N'Giao dịch mẫu cho dashboard. Không phát sinh giao hàng thực tế; tồn kho mẫu là số dư hiện tại.', NULL, NULL, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-11T17:00:00.000', 126), CONVERT(datetime2(0), '2026-10-11T17:00:00.000', 126));

    -- invoices: 52 rows
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-6CFAD2') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-6CFAD2', (SELECT id FROM dbo.orders WHERE order_code = N'BS-1792149064570-a8b5'), N'sale', NULL, N'replaced', N'Phạm Thành', NULL, N'Hạ Longfcdswvsd', NULL, 1250000, 0, 30000, 1280000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-58B126') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-58B126', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202605-001'), N'sale', NULL, N'issued', N'Nguyễn Minh Anh', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 4550000, 100000, 0, 4450000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-8A57B6') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-8A57B6', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202605-002'), N'sale', NULL, N'issued', N'Trần Hoàng Nam', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 4000000, 0, 0, 4000000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-C9647E') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-C9647E', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202605-003'), N'sale', NULL, N'issued', N'Lê Thảo Vy', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 4350000, 0, 0, 4350000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-FE4DE1') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-FE4DE1', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202605-004'), N'sale', NULL, N'issued', N'Phạm Quốc Huy', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 2850000, 0, 0, 2850000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-9A8F95') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-9A8F95', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202605-005'), N'sale', NULL, N'issued', N'Võ Ngọc Linh', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 5800000, 100000, 0, 5700000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-73CDB6') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-73CDB6', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202605-006'), N'sale', NULL, N'issued', N'Đặng Tuấn Kiệt', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 7450000, 0, 0, 7450000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-823ACA') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-823ACA', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202605-007'), N'sale', NULL, N'issued', N'Nguyễn Minh Anh', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 2350000, 0, 0, 2350000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-8E0FF0') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-8E0FF0', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202605-008'), N'sale', NULL, N'issued', N'Trần Hoàng Nam', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 3200000, 0, 0, 3200000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-350004') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-350004', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202606-001'), N'sale', NULL, N'issued', N'Nguyễn Minh Anh', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 1600000, 80000, 30000, 1550000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-E34361') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-E34361', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202606-002'), N'sale', NULL, N'issued', N'Trần Hoàng Nam', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 4150000, 0, 0, 4150000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-1FA287') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-1FA287', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202606-003'), N'sale', NULL, N'issued', N'Lê Thảo Vy', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 2700000, 0, 0, 2700000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-67D826') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-67D826', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202606-004'), N'sale', NULL, N'issued', N'Phạm Quốc Huy', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 2000000, 0, 0, 2000000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-6476A1') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-6476A1', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202606-005'), N'sale', NULL, N'issued', N'Võ Ngọc Linh', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 1950000, 97500, 30000, 1882500, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-B18DC5') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-B18DC5', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202606-006'), N'sale', NULL, N'issued', N'Đặng Tuấn Kiệt', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 6200000, 0, 0, 6200000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-A488A3') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-A488A3', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202606-007'), N'sale', NULL, N'issued', N'Nguyễn Minh Anh', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 2900000, 0, 0, 2900000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-779BA5') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-779BA5', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202606-008'), N'sale', NULL, N'issued', N'Trần Hoàng Nam', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 3650000, 0, 0, 3650000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-AEFBD1') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-AEFBD1', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202607-001'), N'sale', NULL, N'issued', N'Nguyễn Minh Anh', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 1950000, 97500, 30000, 1882500, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-A27516') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-A27516', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202607-002'), N'sale', NULL, N'issued', N'Trần Hoàng Nam', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 3200000, 0, 0, 3200000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-C293ED') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-C293ED', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202607-003'), N'sale', NULL, N'issued', N'Lê Thảo Vy', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 7050000, 0, 0, 7050000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-18358F') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-18358F', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202607-004'), N'sale', NULL, N'issued', N'Phạm Quốc Huy', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 3650000, 0, 0, 3650000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-6BDE16') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-6BDE16', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202607-005'), N'sale', NULL, N'issued', N'Võ Ngọc Linh', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 4000000, 100000, 0, 3900000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-75AD52') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-75AD52', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202607-006'), N'sale', NULL, N'issued', N'Đặng Tuấn Kiệt', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 3550000, 0, 0, 3550000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-889410') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-889410', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202607-007'), N'sale', NULL, N'issued', N'Nguyễn Minh Anh', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 4150000, 0, 0, 4150000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-C32474') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-C32474', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202607-008'), N'sale', NULL, N'issued', N'Trần Hoàng Nam', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 5800000, 0, 0, 5800000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-05C9C5') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-05C9C5', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202608-001'), N'sale', NULL, N'issued', N'Nguyễn Minh Anh', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 2000000, 100000, 0, 1900000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-4F4B4C') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-4F4B4C', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202608-002'), N'sale', NULL, N'issued', N'Trần Hoàng Nam', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 2850000, 0, 0, 2850000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-AC301C') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-AC301C', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202608-003'), N'sale', NULL, N'issued', N'Lê Thảo Vy', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 4100000, 0, 0, 4100000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-370778') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-370778', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202608-004'), N'sale', NULL, N'issued', N'Phạm Quốc Huy', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 2900000, 0, 0, 2900000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-AA5554') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-AA5554', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202608-005'), N'sale', NULL, N'issued', N'Võ Ngọc Linh', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 2350000, 100000, 0, 2250000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-789194') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-789194', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202608-006'), N'sale', NULL, N'issued', N'Đặng Tuấn Kiệt', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 2700000, 0, 0, 2700000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-43EDF7') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-43EDF7', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202608-007'), N'sale', NULL, N'issued', N'Nguyễn Minh Anh', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 1600000, 0, 30000, 1630000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-070959') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-070959', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202608-008'), N'sale', NULL, N'issued', N'Trần Hoàng Nam', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 4550000, 0, 0, 4550000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-47D751') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-47D751', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202609-001'), N'sale', NULL, N'issued', N'Nguyễn Minh Anh', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 4150000, 100000, 0, 4050000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-94D037') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-94D037', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202609-002'), N'sale', NULL, N'issued', N'Trần Hoàng Nam', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 4000000, 0, 0, 4000000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-3F92B9') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-3F92B9', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202609-003'), N'sale', NULL, N'issued', N'Lê Thảo Vy', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 4450000, 0, 0, 4450000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-134373') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-134373', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202609-004'), N'sale', NULL, N'issued', N'Phạm Quốc Huy', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 1950000, 0, 30000, 1980000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-795C21') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-795C21', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202609-005'), N'sale', NULL, N'issued', N'Võ Ngọc Linh', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 5800000, 100000, 0, 5700000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-41BBAE') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-41BBAE', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202609-006'), N'sale', NULL, N'issued', N'Đặng Tuấn Kiệt', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 4350000, 0, 0, 4350000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-147A8C') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-147A8C', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202609-007'), N'sale', NULL, N'issued', N'Nguyễn Minh Anh', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 3650000, 0, 0, 3650000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-0FB663') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-0FB663', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202609-008'), N'sale', NULL, N'issued', N'Trần Hoàng Nam', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 3200000, 0, 0, 3200000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-C88D26') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-C88D26', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202610-001'), N'sale', NULL, N'issued', N'Nguyễn Minh Anh', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 2900000, 100000, 0, 2800000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-4B4FAC') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-4B4FAC', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202610-002'), N'sale', NULL, N'issued', N'Trần Hoàng Nam', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 4550000, 0, 0, 4550000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-2B92E0') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-2B92E0', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202610-003'), N'sale', NULL, N'issued', N'Lê Thảo Vy', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 5300000, 0, 0, 5300000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-9FF51F') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-9FF51F', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202610-004'), N'sale', NULL, N'issued', N'Phạm Quốc Huy', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 1600000, 0, 30000, 1630000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-5F5851') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-5F5851', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202610-005'), N'sale', NULL, N'issued', N'Võ Ngọc Linh', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 2850000, 100000, 0, 2750000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-1DD44F') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-1DD44F', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202610-006'), N'sale', NULL, N'issued', N'Đặng Tuấn Kiệt', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 5400000, 0, 0, 5400000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-1F9EAF') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-1F9EAF', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202610-007'), N'sale', NULL, N'issued', N'Nguyễn Minh Anh', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 2000000, 0, 0, 2000000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-CB6F37') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-CB6F37', (SELECT id FROM dbo.orders WHERE order_code = N'DEMO-202610-008'), N'sale', NULL, N'issued', N'Trần Hoàng Nam', NULL, N'Địa chỉ mẫu, Thành phố Hồ Chí Minh', NULL, 2350000, 0, 0, 2350000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T19:57:39.913', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-869613') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-869613', (SELECT id FROM dbo.orders WHERE order_code = N'BS-1792148654606-01a0'), N'sale', NULL, N'issued', N'Phạm Thành', NULL, N'Hạ Long, Quảng Ninh', NULL, 2500000, 0, 0, 2500000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-18T01:18:04.851', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-4B5786') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-4B5786', (SELECT id FROM dbo.orders WHERE order_code = N'BS-1792149369962-0309'), N'sale', NULL, N'issued', N'Phạm Thành', NULL, N'Hạ Longfcdswvsd', NULL, 1250000, 0, 30000, 1280000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-18T01:18:04.851', 126));
    IF (SELECT COUNT(*) FROM dbo.invoices WITH (UPDLOCK, HOLDLOCK) WHERE invoice_no = N'INV-20261017-A5C459') < 1
        INSERT INTO dbo.invoices (invoice_no, order_id, invoice_type, parent_invoice_id, status, buyer_name, buyer_email, buyer_address, buyer_tax_code, subtotal, discount_amount, shipping_fee, total_amount, reason, pdf_url, issued_by, issued_at)
        VALUES (N'INV-20261017-A5C459', (SELECT id FROM dbo.orders WHERE order_code = N'BS-1792149064570-a8b5'), N'sale', (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-6CFAD2'), N'issued', N'Phạm Thành', NULL, N'Hạ Longfcdswvsd', NULL, 1250000, 0, 30000, 1280000, NULL, NULL, NULL, CONVERT(datetime2(0), '2026-10-17T20:00:11.234', 126));

    -- invoice_items: 100 rows
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-6CFAD2') AND description = N'Yonex Astrox 88D Play - Xanh biển / 4U-G5' AND unit_price = 1250000 AND quantity = 1 AND line_total = 1250000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-6CFAD2'), N'Yonex Astrox 88D Play - Xanh biển / 4U-G5', 1250000, 1, 1250000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-58B126') AND description = N'Giày thi đấu Court Pro (Mẫu) - Đen / 42' AND unit_price = 1250000 AND quantity = 1 AND line_total = 1250000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-58B126'), N'Giày thi đấu Court Pro (Mẫu) - Đen / 42', 1250000, 1, 1250000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-58B126') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5' AND unit_price = 1650000 AND quantity = 2 AND line_total = 3300000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-58B126'), N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5', 1650000, 2, 3300000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-8A57B6') AND description = N'Áo thể thao Air Fit (Mẫu) - Xanh / S' AND unit_price = 350000 AND quantity = 2 AND line_total = 700000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-8A57B6'), N'Áo thể thao Air Fit (Mẫu) - Xanh / S', 350000, 2, 700000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-8A57B6') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5' AND unit_price = 1650000 AND quantity = 2 AND line_total = 3300000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-8A57B6'), N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5', 1650000, 2, 3300000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-C9647E') AND description = N'Áo thể thao Air Fit (Mẫu) - Trắng / M' AND unit_price = 350000 AND quantity = 3 AND line_total = 1050000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-C9647E'), N'Áo thể thao Air Fit (Mẫu) - Trắng / M', 350000, 3, 1050000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-C9647E') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5' AND unit_price = 1650000 AND quantity = 2 AND line_total = 3300000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-C9647E'), N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5', 1650000, 2, 3300000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-FE4DE1') AND description = N'Áo thể thao Air Fit (Mẫu) - Đen / L' AND unit_price = 350000 AND quantity = 1 AND line_total = 350000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-FE4DE1'), N'Áo thể thao Air Fit (Mẫu) - Đen / L', 350000, 1, 350000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-FE4DE1') AND description = N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40' AND unit_price = 1250000 AND quantity = 2 AND line_total = 2500000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-FE4DE1'), N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40', 1250000, 2, 2500000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-9A8F95') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5' AND unit_price = 1650000 AND quantity = 2 AND line_total = 3300000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-9A8F95'), N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5', 1650000, 2, 3300000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-9A8F95') AND description = N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41' AND unit_price = 1250000 AND quantity = 2 AND line_total = 2500000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-9A8F95'), N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41', 1250000, 2, 2500000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-73CDB6') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5' AND unit_price = 1650000 AND quantity = 3 AND line_total = 4950000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-73CDB6'), N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5', 1650000, 3, 4950000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-73CDB6') AND description = N'Giày thi đấu Court Pro (Mẫu) - Đen / 42' AND unit_price = 1250000 AND quantity = 2 AND line_total = 2500000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-73CDB6'), N'Giày thi đấu Court Pro (Mẫu) - Đen / 42', 1250000, 2, 2500000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-823ACA') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5' AND unit_price = 1650000 AND quantity = 1 AND line_total = 1650000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-823ACA'), N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5', 1650000, 1, 1650000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-823ACA') AND description = N'Áo thể thao Air Fit (Mẫu) - Xanh / S' AND unit_price = 350000 AND quantity = 2 AND line_total = 700000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-823ACA'), N'Áo thể thao Air Fit (Mẫu) - Xanh / S', 350000, 2, 700000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-8E0FF0') AND description = N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40' AND unit_price = 1250000 AND quantity = 2 AND line_total = 2500000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-8E0FF0'), N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40', 1250000, 2, 2500000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-8E0FF0') AND description = N'Áo thể thao Air Fit (Mẫu) - Trắng / M' AND unit_price = 350000 AND quantity = 2 AND line_total = 700000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-8E0FF0'), N'Áo thể thao Air Fit (Mẫu) - Trắng / M', 350000, 2, 700000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-350004') AND description = N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41' AND unit_price = 1250000 AND quantity = 1 AND line_total = 1250000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-350004'), N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41', 1250000, 1, 1250000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-350004') AND description = N'Áo thể thao Air Fit (Mẫu) - Đen / L' AND unit_price = 350000 AND quantity = 1 AND line_total = 350000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-350004'), N'Áo thể thao Air Fit (Mẫu) - Đen / L', 350000, 1, 350000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-E34361') AND description = N'Giày thi đấu Court Pro (Mẫu) - Đen / 42' AND unit_price = 1250000 AND quantity = 2 AND line_total = 2500000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-E34361'), N'Giày thi đấu Court Pro (Mẫu) - Đen / 42', 1250000, 2, 2500000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-E34361') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5' AND unit_price = 1650000 AND quantity = 1 AND line_total = 1650000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-E34361'), N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5', 1650000, 1, 1650000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-1FA287') AND description = N'Áo thể thao Air Fit (Mẫu) - Xanh / S' AND unit_price = 350000 AND quantity = 3 AND line_total = 1050000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-1FA287'), N'Áo thể thao Air Fit (Mẫu) - Xanh / S', 350000, 3, 1050000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-1FA287') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5' AND unit_price = 1650000 AND quantity = 1 AND line_total = 1650000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-1FA287'), N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5', 1650000, 1, 1650000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-67D826') AND description = N'Áo thể thao Air Fit (Mẫu) - Trắng / M' AND unit_price = 350000 AND quantity = 1 AND line_total = 350000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-67D826'), N'Áo thể thao Air Fit (Mẫu) - Trắng / M', 350000, 1, 350000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-67D826') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5' AND unit_price = 1650000 AND quantity = 1 AND line_total = 1650000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-67D826'), N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5', 1650000, 1, 1650000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-6476A1') AND description = N'Áo thể thao Air Fit (Mẫu) - Đen / L' AND unit_price = 350000 AND quantity = 2 AND line_total = 700000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-6476A1'), N'Áo thể thao Air Fit (Mẫu) - Đen / L', 350000, 2, 700000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-6476A1') AND description = N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40' AND unit_price = 1250000 AND quantity = 1 AND line_total = 1250000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-6476A1'), N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40', 1250000, 1, 1250000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-B18DC5') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5' AND unit_price = 1650000 AND quantity = 3 AND line_total = 4950000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-B18DC5'), N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5', 1650000, 3, 4950000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-B18DC5') AND description = N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41' AND unit_price = 1250000 AND quantity = 1 AND line_total = 1250000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-B18DC5'), N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41', 1250000, 1, 1250000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-A488A3') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5' AND unit_price = 1650000 AND quantity = 1 AND line_total = 1650000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-A488A3'), N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5', 1650000, 1, 1650000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-A488A3') AND description = N'Giày thi đấu Court Pro (Mẫu) - Đen / 42' AND unit_price = 1250000 AND quantity = 1 AND line_total = 1250000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-A488A3'), N'Giày thi đấu Court Pro (Mẫu) - Đen / 42', 1250000, 1, 1250000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-779BA5') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5' AND unit_price = 1650000 AND quantity = 2 AND line_total = 3300000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-779BA5'), N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5', 1650000, 2, 3300000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-779BA5') AND description = N'Áo thể thao Air Fit (Mẫu) - Xanh / S' AND unit_price = 350000 AND quantity = 1 AND line_total = 350000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-779BA5'), N'Áo thể thao Air Fit (Mẫu) - Xanh / S', 350000, 1, 350000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-AEFBD1') AND description = N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40' AND unit_price = 1250000 AND quantity = 1 AND line_total = 1250000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-AEFBD1'), N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40', 1250000, 1, 1250000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-AEFBD1') AND description = N'Áo thể thao Air Fit (Mẫu) - Trắng / M' AND unit_price = 350000 AND quantity = 2 AND line_total = 700000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-AEFBD1'), N'Áo thể thao Air Fit (Mẫu) - Trắng / M', 350000, 2, 700000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-A27516') AND description = N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41' AND unit_price = 1250000 AND quantity = 2 AND line_total = 2500000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-A27516'), N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41', 1250000, 2, 2500000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-A27516') AND description = N'Áo thể thao Air Fit (Mẫu) - Đen / L' AND unit_price = 350000 AND quantity = 2 AND line_total = 700000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-A27516'), N'Áo thể thao Air Fit (Mẫu) - Đen / L', 350000, 2, 700000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-C293ED') AND description = N'Giày thi đấu Court Pro (Mẫu) - Đen / 42' AND unit_price = 1250000 AND quantity = 3 AND line_total = 3750000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-C293ED'), N'Giày thi đấu Court Pro (Mẫu) - Đen / 42', 1250000, 3, 3750000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-C293ED') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5' AND unit_price = 1650000 AND quantity = 2 AND line_total = 3300000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-C293ED'), N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5', 1650000, 2, 3300000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-18358F') AND description = N'Áo thể thao Air Fit (Mẫu) - Xanh / S' AND unit_price = 350000 AND quantity = 1 AND line_total = 350000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-18358F'), N'Áo thể thao Air Fit (Mẫu) - Xanh / S', 350000, 1, 350000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-18358F') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5' AND unit_price = 1650000 AND quantity = 2 AND line_total = 3300000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-18358F'), N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5', 1650000, 2, 3300000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-6BDE16') AND description = N'Áo thể thao Air Fit (Mẫu) - Trắng / M' AND unit_price = 350000 AND quantity = 2 AND line_total = 700000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-6BDE16'), N'Áo thể thao Air Fit (Mẫu) - Trắng / M', 350000, 2, 700000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-6BDE16') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5' AND unit_price = 1650000 AND quantity = 2 AND line_total = 3300000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-6BDE16'), N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5', 1650000, 2, 3300000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-75AD52') AND description = N'Áo thể thao Air Fit (Mẫu) - Đen / L' AND unit_price = 350000 AND quantity = 3 AND line_total = 1050000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-75AD52'), N'Áo thể thao Air Fit (Mẫu) - Đen / L', 350000, 3, 1050000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-75AD52') AND description = N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40' AND unit_price = 1250000 AND quantity = 2 AND line_total = 2500000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-75AD52'), N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40', 1250000, 2, 2500000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-889410') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5' AND unit_price = 1650000 AND quantity = 1 AND line_total = 1650000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-889410'), N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5', 1650000, 1, 1650000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-889410') AND description = N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41' AND unit_price = 1250000 AND quantity = 2 AND line_total = 2500000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-889410'), N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41', 1250000, 2, 2500000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-C32474') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5' AND unit_price = 1650000 AND quantity = 2 AND line_total = 3300000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-C32474'), N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5', 1650000, 2, 3300000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-C32474') AND description = N'Giày thi đấu Court Pro (Mẫu) - Đen / 42' AND unit_price = 1250000 AND quantity = 2 AND line_total = 2500000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-C32474'), N'Giày thi đấu Court Pro (Mẫu) - Đen / 42', 1250000, 2, 2500000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-05C9C5') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5' AND unit_price = 1650000 AND quantity = 1 AND line_total = 1650000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-05C9C5'), N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5', 1650000, 1, 1650000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-05C9C5') AND description = N'Áo thể thao Air Fit (Mẫu) - Xanh / S' AND unit_price = 350000 AND quantity = 1 AND line_total = 350000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-05C9C5'), N'Áo thể thao Air Fit (Mẫu) - Xanh / S', 350000, 1, 350000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-4F4B4C') AND description = N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40' AND unit_price = 1250000 AND quantity = 2 AND line_total = 2500000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-4F4B4C'), N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40', 1250000, 2, 2500000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-4F4B4C') AND description = N'Áo thể thao Air Fit (Mẫu) - Trắng / M' AND unit_price = 350000 AND quantity = 1 AND line_total = 350000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-4F4B4C'), N'Áo thể thao Air Fit (Mẫu) - Trắng / M', 350000, 1, 350000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-AC301C') AND description = N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41' AND unit_price = 1250000 AND quantity = 3 AND line_total = 3750000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-AC301C'), N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41', 1250000, 3, 3750000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-AC301C') AND description = N'Áo thể thao Air Fit (Mẫu) - Đen / L' AND unit_price = 350000 AND quantity = 1 AND line_total = 350000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-AC301C'), N'Áo thể thao Air Fit (Mẫu) - Đen / L', 350000, 1, 350000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-370778') AND description = N'Giày thi đấu Court Pro (Mẫu) - Đen / 42' AND unit_price = 1250000 AND quantity = 1 AND line_total = 1250000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-370778'), N'Giày thi đấu Court Pro (Mẫu) - Đen / 42', 1250000, 1, 1250000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-370778') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5' AND unit_price = 1650000 AND quantity = 1 AND line_total = 1650000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-370778'), N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5', 1650000, 1, 1650000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-AA5554') AND description = N'Áo thể thao Air Fit (Mẫu) - Xanh / S' AND unit_price = 350000 AND quantity = 2 AND line_total = 700000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-AA5554'), N'Áo thể thao Air Fit (Mẫu) - Xanh / S', 350000, 2, 700000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-AA5554') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5' AND unit_price = 1650000 AND quantity = 1 AND line_total = 1650000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-AA5554'), N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5', 1650000, 1, 1650000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-789194') AND description = N'Áo thể thao Air Fit (Mẫu) - Trắng / M' AND unit_price = 350000 AND quantity = 3 AND line_total = 1050000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-789194'), N'Áo thể thao Air Fit (Mẫu) - Trắng / M', 350000, 3, 1050000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-789194') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5' AND unit_price = 1650000 AND quantity = 1 AND line_total = 1650000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-789194'), N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5', 1650000, 1, 1650000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-43EDF7') AND description = N'Áo thể thao Air Fit (Mẫu) - Đen / L' AND unit_price = 350000 AND quantity = 1 AND line_total = 350000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-43EDF7'), N'Áo thể thao Air Fit (Mẫu) - Đen / L', 350000, 1, 350000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-43EDF7') AND description = N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40' AND unit_price = 1250000 AND quantity = 1 AND line_total = 1250000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-43EDF7'), N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40', 1250000, 1, 1250000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-070959') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5' AND unit_price = 1650000 AND quantity = 2 AND line_total = 3300000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-070959'), N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5', 1650000, 2, 3300000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-070959') AND description = N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41' AND unit_price = 1250000 AND quantity = 1 AND line_total = 1250000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-070959'), N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41', 1250000, 1, 1250000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-47D751') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5' AND unit_price = 1650000 AND quantity = 1 AND line_total = 1650000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-47D751'), N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5', 1650000, 1, 1650000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-47D751') AND description = N'Giày thi đấu Court Pro (Mẫu) - Đen / 42' AND unit_price = 1250000 AND quantity = 2 AND line_total = 2500000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-47D751'), N'Giày thi đấu Court Pro (Mẫu) - Đen / 42', 1250000, 2, 2500000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-94D037') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5' AND unit_price = 1650000 AND quantity = 2 AND line_total = 3300000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-94D037'), N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5', 1650000, 2, 3300000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-94D037') AND description = N'Áo thể thao Air Fit (Mẫu) - Xanh / S' AND unit_price = 350000 AND quantity = 2 AND line_total = 700000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-94D037'), N'Áo thể thao Air Fit (Mẫu) - Xanh / S', 350000, 2, 700000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-3F92B9') AND description = N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40' AND unit_price = 1250000 AND quantity = 3 AND line_total = 3750000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-3F92B9'), N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40', 1250000, 3, 3750000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-3F92B9') AND description = N'Áo thể thao Air Fit (Mẫu) - Trắng / M' AND unit_price = 350000 AND quantity = 2 AND line_total = 700000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-3F92B9'), N'Áo thể thao Air Fit (Mẫu) - Trắng / M', 350000, 2, 700000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-134373') AND description = N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41' AND unit_price = 1250000 AND quantity = 1 AND line_total = 1250000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-134373'), N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41', 1250000, 1, 1250000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-134373') AND description = N'Áo thể thao Air Fit (Mẫu) - Đen / L' AND unit_price = 350000 AND quantity = 2 AND line_total = 700000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-134373'), N'Áo thể thao Air Fit (Mẫu) - Đen / L', 350000, 2, 700000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-795C21') AND description = N'Giày thi đấu Court Pro (Mẫu) - Đen / 42' AND unit_price = 1250000 AND quantity = 2 AND line_total = 2500000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-795C21'), N'Giày thi đấu Court Pro (Mẫu) - Đen / 42', 1250000, 2, 2500000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-795C21') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5' AND unit_price = 1650000 AND quantity = 2 AND line_total = 3300000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-795C21'), N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5', 1650000, 2, 3300000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-41BBAE') AND description = N'Áo thể thao Air Fit (Mẫu) - Xanh / S' AND unit_price = 350000 AND quantity = 3 AND line_total = 1050000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-41BBAE'), N'Áo thể thao Air Fit (Mẫu) - Xanh / S', 350000, 3, 1050000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-41BBAE') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5' AND unit_price = 1650000 AND quantity = 2 AND line_total = 3300000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-41BBAE'), N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5', 1650000, 2, 3300000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-147A8C') AND description = N'Áo thể thao Air Fit (Mẫu) - Trắng / M' AND unit_price = 350000 AND quantity = 1 AND line_total = 350000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-147A8C'), N'Áo thể thao Air Fit (Mẫu) - Trắng / M', 350000, 1, 350000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-147A8C') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5' AND unit_price = 1650000 AND quantity = 2 AND line_total = 3300000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-147A8C'), N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5', 1650000, 2, 3300000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-0FB663') AND description = N'Áo thể thao Air Fit (Mẫu) - Đen / L' AND unit_price = 350000 AND quantity = 2 AND line_total = 700000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-0FB663'), N'Áo thể thao Air Fit (Mẫu) - Đen / L', 350000, 2, 700000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-0FB663') AND description = N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40' AND unit_price = 1250000 AND quantity = 2 AND line_total = 2500000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-0FB663'), N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40', 1250000, 2, 2500000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-C88D26') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5' AND unit_price = 1650000 AND quantity = 1 AND line_total = 1650000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-C88D26'), N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5', 1650000, 1, 1650000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-C88D26') AND description = N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41' AND unit_price = 1250000 AND quantity = 1 AND line_total = 1250000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-C88D26'), N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41', 1250000, 1, 1250000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-4B4FAC') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5' AND unit_price = 1650000 AND quantity = 2 AND line_total = 3300000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-4B4FAC'), N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5', 1650000, 2, 3300000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-4B4FAC') AND description = N'Giày thi đấu Court Pro (Mẫu) - Đen / 42' AND unit_price = 1250000 AND quantity = 1 AND line_total = 1250000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-4B4FAC'), N'Giày thi đấu Court Pro (Mẫu) - Đen / 42', 1250000, 1, 1250000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-2B92E0') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5' AND unit_price = 1650000 AND quantity = 3 AND line_total = 4950000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-2B92E0'), N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5', 1650000, 3, 4950000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-2B92E0') AND description = N'Áo thể thao Air Fit (Mẫu) - Xanh / S' AND unit_price = 350000 AND quantity = 1 AND line_total = 350000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-2B92E0'), N'Áo thể thao Air Fit (Mẫu) - Xanh / S', 350000, 1, 350000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-9FF51F') AND description = N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40' AND unit_price = 1250000 AND quantity = 1 AND line_total = 1250000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-9FF51F'), N'Giày thi đấu Court Pro (Mẫu) - Xanh / 40', 1250000, 1, 1250000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-9FF51F') AND description = N'Áo thể thao Air Fit (Mẫu) - Trắng / M' AND unit_price = 350000 AND quantity = 1 AND line_total = 350000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-9FF51F'), N'Áo thể thao Air Fit (Mẫu) - Trắng / M', 350000, 1, 350000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-5F5851') AND description = N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41' AND unit_price = 1250000 AND quantity = 2 AND line_total = 2500000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-5F5851'), N'Giày thi đấu Court Pro (Mẫu) - Trắng / 41', 1250000, 2, 2500000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-5F5851') AND description = N'Áo thể thao Air Fit (Mẫu) - Đen / L' AND unit_price = 350000 AND quantity = 1 AND line_total = 350000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-5F5851'), N'Áo thể thao Air Fit (Mẫu) - Đen / L', 350000, 1, 350000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-1DD44F') AND description = N'Giày thi đấu Court Pro (Mẫu) - Đen / 42' AND unit_price = 1250000 AND quantity = 3 AND line_total = 3750000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-1DD44F'), N'Giày thi đấu Court Pro (Mẫu) - Đen / 42', 1250000, 3, 3750000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-1DD44F') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5' AND unit_price = 1650000 AND quantity = 1 AND line_total = 1650000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-1DD44F'), N'Vợt luyện tập Club 2026 (Mẫu) - Xanh / 3U-G5', 1650000, 1, 1650000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-1F9EAF') AND description = N'Áo thể thao Air Fit (Mẫu) - Xanh / S' AND unit_price = 350000 AND quantity = 1 AND line_total = 350000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-1F9EAF'), N'Áo thể thao Air Fit (Mẫu) - Xanh / S', 350000, 1, 350000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-1F9EAF') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5' AND unit_price = 1650000 AND quantity = 1 AND line_total = 1650000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-1F9EAF'), N'Vợt luyện tập Club 2026 (Mẫu) - Trắng / 4U-G5', 1650000, 1, 1650000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-CB6F37') AND description = N'Áo thể thao Air Fit (Mẫu) - Trắng / M' AND unit_price = 350000 AND quantity = 2 AND line_total = 700000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-CB6F37'), N'Áo thể thao Air Fit (Mẫu) - Trắng / M', 350000, 2, 700000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-CB6F37') AND description = N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5' AND unit_price = 1650000 AND quantity = 1 AND line_total = 1650000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-CB6F37'), N'Vợt luyện tập Club 2026 (Mẫu) - Đen / 5U-G5', 1650000, 1, 1650000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-A5C459') AND description = N'Yonex Astrox 88D Play - Xanh biển / 4U-G5' AND unit_price = 1250000 AND quantity = 1 AND line_total = 1250000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-A5C459'), N'Yonex Astrox 88D Play - Xanh biển / 4U-G5', 1250000, 1, 1250000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-869613') AND description = N'Yonex Astrox 88D Play - Xanh biển / 4U-G5' AND unit_price = 1250000 AND quantity = 2 AND line_total = 2500000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-869613'), N'Yonex Astrox 88D Play - Xanh biển / 4U-G5', 1250000, 2, 2500000);
    IF (SELECT COUNT(*) FROM dbo.invoice_items WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-4B5786') AND description = N'Yonex Astrox 88D Play - Xanh biển / 4U-G5' AND unit_price = 1250000 AND quantity = 1 AND line_total = 1250000) < 1
        INSERT INTO dbo.invoice_items (invoice_id, description, unit_price, quantity, line_total)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-4B5786'), N'Yonex Astrox 88D Play - Xanh biển / 4U-G5', 1250000, 1, 1250000);

    -- invoice_deliveries: 1 rows
    IF (SELECT COUNT(*) FROM dbo.invoice_deliveries WITH (UPDLOCK, HOLDLOCK) WHERE invoice_id = (SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-6CFAD2') AND channel = N'download' AND recipient IS NULL AND status = N'sent' AND sent_at = CONVERT(datetime2(0), '2026-10-17T19:58:25.079', 126)) < 1
        INSERT INTO dbo.invoice_deliveries (invoice_id, channel, recipient, status, sent_at)
        VALUES ((SELECT id FROM dbo.invoices WHERE invoice_no = N'INV-20261017-6CFAD2'), N'download', NULL, N'sent', CONVERT(datetime2(0), '2026-10-17T19:58:25.079', 126));

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
-- Add them du lieu
USE badminton_shop;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
BEGIN TRY
 BEGIN TRANSACTION;

 -- ============================================================
 -- INSERT TRUC TIEP VAO CAC BANG GOC dbo.*
 -- Khong tao bang tam, khong tao lai cac bang goc.
 -- ============================================================

 -- Them 4 brands / 3 categories neu chua co (khong tao trung).
 INSERT INTO dbo.brands(name,slug,logo_url,is_active)
 SELECT b.name,b.slug,NULL,1 FROM (VALUES
 (N'Yonex','yonex'),(N'Victor','victor'),(N'Li-Ning','li-ning'),(N'Badminton Shop','badminton-shop')
 ) b(name,slug) WHERE NOT EXISTS (SELECT 1 FROM dbo.brands x WHERE x.slug=b.slug);
 INSERT INTO dbo.categories(parent_id,name,slug,is_active)
 SELECT NULL,c.name,c.slug,1 FROM (VALUES
 (N'Vợt cầu lông','racket'),(N'Giày cầu lông','shoes'),(N'Trang phục','apparel')
 ) c(name,slug) WHERE NOT EXISTS (SELECT 1 FROM dbo.categories x WHERE x.slug=c.slug);


 INSERT INTO dbo.products(category_id,brand_id,product_type,name,slug,description,base_price,is_active,created_by,created_at,updated_at)
 SELECT c.id,b.id,s.product_type,s.name,s.slug,s.description,s.base_price,1,NULL,SYSDATETIME(),SYSDATETIME()
 FROM (VALUES
 (N'yonex-astrox-100zz', N'Vợt cầu lông Yonex Astrox 100 ZZ', N'racket', N'yonex', 3890000, N'Vợt thiên công, dành cho người chơi trình độ cao.'),
 (N'yonex-arcsaber-11-pro', N'Vợt cầu lông Yonex Arcsaber 11 Pro', N'racket', N'yonex', 4190000, N'Vợt cân bằng thiên về kiểm soát và điều cầu.'),
 (N'yonex-nanoflare-1000z', N'Vợt cầu lông Yonex Nanoflare 1000 Z', N'racket', N'yonex', 4590000, N'Vợt nhẹ đầu hỗ trợ tốc độ vung vợt.'),
 (N'victor-thruster-ryuga-ii', N'Vợt cầu lông Victor Thruster Ryuga II', N'racket', N'victor', 3690000, N'Vợt thiên công với khung trợ lực.'),
 (N'victor-auraspeed-90k-ii', N'Vợt cầu lông Victor Auraspeed 90K II', N'racket', N'victor', 3490000, N'Vợt tốc độ dành cho lối chơi phản tạt.'),
 (N'li-ning-axforce-90-dragon', N'Vợt cầu lông Li-Ning Axforce 90 Dragon', N'racket', N'li-ning', 3990000, N'Vợt thiên công phục vụ đập cầu mạnh.'),
 (N'yonex-power-cushion-65z3', N'Giày Yonex Power Cushion 65 Z3', N'shoes', N'yonex', 2890000, N'Giày cầu lông có đệm hỗ trợ di chuyển.'),
 (N'yonex-aerus-z2', N'Giày Yonex Aerus Z2', N'shoes', N'yonex', 3190000, N'Giày nhẹ ưu tiên tính linh hoạt.'),
 (N'victor-p9200iii', N'Giày Victor P9200 III', N'shoes', N'victor', 2590000, N'Giày có độ ổn định và hỗ trợ bàn chân.'),
 (N'li-ning-saga-lite', N'Giày Li-Ning Saga Lite', N'shoes', N'li-ning', 1790000, N'Giày luyện tập trọng lượng nhẹ.'),
 (N'yonex-shirt-crew-10650', N'Áo Yonex Crew Neck 10650', N'apparel', N'yonex', 590000, N'Áo thi đấu cầu lông thoáng khí.'),
 (N'yonex-shorts-15110', N'Quần Yonex Shorts 15110', N'apparel', N'yonex', 490000, N'Quần short thể thao nhẹ, co giãn.'),
 (N'victor-shirt-t25000', N'Áo Victor T-25000', N'apparel', N'victor', 450000, N'Áo cầu lông dành cho luyện tập.'),
 (N'li-ning-shirt-aayu', N'Áo Li-Ning Training AAYU', N'apparel', N'li-ning', 390000, N'Áo luyện tập nhanh khô.'),
 (N'badminton-shop-training-socks', N'Tất cầu lông Training Socks', N'apparel', N'badminton-shop', 99000, N'Tất thể thao cổ trung, thoáng khí.')
 ) s(slug,name,product_type,brand_slug,base_price,description) JOIN dbo.categories c ON c.slug=s.product_type
 JOIN dbo.brands b ON b.slug=s.brand_slug
 WHERE NOT EXISTS (SELECT 1 FROM dbo.products p WHERE p.slug=s.slug);

 -- Thong so chi tiet theo 3 loai san pham.

 INSERT INTO dbo.racket_specs(product_id,balance_type,flex,play_style,max_tension_lbs,material,shaft_diameter)
 SELECT p.id,s.balance_type,s.flex,s.play_style,s.max_tension_lbs,s.material,s.shaft_diameter
 FROM (VALUES
 (N'yonex-astrox-100zz', N'head_heavy', N'stiff', N'attack', 28, N'High Modulus Graphite', 6.2),
 (N'yonex-arcsaber-11-pro', N'even', N'stiff', N'all_round', 27, N'HM Graphite', 6.8),
 (N'yonex-nanoflare-1000z', N'head_light', N'stiff', N'defense', 28, N'Graphite', 6.4),
 (N'victor-thruster-ryuga-ii', N'head_heavy', N'stiff', N'attack', 29, N'High Resilience Graphite', 6.6),
 (N'victor-auraspeed-90k-ii', N'even', N'stiff', N'all_round', 28, N'Graphite', 6.8),
 (N'li-ning-axforce-90-dragon', N'head_heavy', N'stiff', N'attack', 30, N'Carbon Fiber', 6.6)
 ) s(slug,balance_type,flex,play_style,max_tension_lbs,material,shaft_diameter) JOIN dbo.products p ON p.slug=s.slug
 WHERE NOT EXISTS (SELECT 1 FROM dbo.racket_specs x WHERE x.product_id=p.id);

 INSERT INTO dbo.shoe_specs(product_id,gender,sole_type,technology,foot_width)
 SELECT p.id,s.gender,s.sole_type,s.technology,s.foot_width FROM (VALUES
 (N'yonex-power-cushion-65z3', N'unisex', N'Cao su chống trượt', N'Power Cushion+', N'regular'),
 (N'yonex-aerus-z2', N'unisex', N'Cao su chống trượt', N'Power Cushion', N'regular'),
 (N'victor-p9200iii', N'unisex', N'Cao su chịu mài mòn', N'Energymax', N'wide'),
 (N'li-ning-saga-lite', N'unisex', N'Cao su chống trượt', N'Đệm EVA', N'regular')
 ) s(slug,gender,sole_type,technology,foot_width) JOIN dbo.products p ON p.slug=s.slug
 WHERE NOT EXISTS (SELECT 1 FROM dbo.shoe_specs x WHERE x.product_id=p.id);

 INSERT INTO dbo.apparel_specs(product_id,gender,apparel_kind,material)
 SELECT p.id,s.gender,s.apparel_kind,s.material FROM (VALUES
 (N'yonex-shirt-crew-10650', N'unisex', N'shirt', N'Polyester'),
 (N'yonex-shorts-15110', N'unisex', N'shorts', N'Polyester'),
 (N'victor-shirt-t25000', N'unisex', N'shirt', N'Polyester'),
 (N'li-ning-shirt-aayu', N'unisex', N'shirt', N'Polyester'),
 (N'badminton-shop-training-socks', N'unisex', N'socks', N'Cotton pha Polyester')
 ) s(slug,gender,apparel_kind,material) JOIN dbo.products p ON p.slug=s.slug
 WHERE NOT EXISTS (SELECT 1 FROM dbo.apparel_specs x WHERE x.product_id=p.id);

 -- Moi san pham 2 bien the, co gia von, ton kho va barcode de trong.

 INSERT INTO dbo.product_variants(product_id,sku,barcode,color,size,price,cost_price,stock_quantity,low_stock_threshold,is_active,weight_grams,created_at,updated_at)
 SELECT p.id,s.sku,NULL,s.color,s.size,s.price,s.cost_price,s.stock_quantity,s.low_stock_threshold,1,0,SYSDATETIME(),SYSDATETIME()
 FROM (VALUES
 (N'yonex-astrox-100zz', N'NEW-01-4U-G5', N'Đen', N'4U-G5', 3890000, 2528500, 14, 5),
 (N'yonex-astrox-100zz', N'NEW-01-3U-G5', N'Xanh', N'3U-G5', 3890000, 2528500, 23, 5),
 (N'yonex-arcsaber-11-pro', N'NEW-02-4U-G5', N'Đen', N'4U-G5', 4190000, 2723500, 14, 5),
 (N'yonex-arcsaber-11-pro', N'NEW-02-3U-G5', N'Xanh', N'3U-G5', 4190000, 2723500, 23, 5),
 (N'yonex-nanoflare-1000z', N'NEW-03-4U-G5', N'Đen', N'4U-G5', 4590000, 2983500, 14, 5),
 (N'yonex-nanoflare-1000z', N'NEW-03-3U-G5', N'Xanh', N'3U-G5', 4590000, 2983500, 23, 5),
 (N'victor-thruster-ryuga-ii', N'NEW-04-4U-G5', N'Đen', N'4U-G5', 3690000, 2398500, 14, 5),
 (N'victor-thruster-ryuga-ii', N'NEW-04-3U-G5', N'Xanh', N'3U-G5', 3690000, 2398500, 23, 5),
 (N'victor-auraspeed-90k-ii', N'NEW-05-4U-G5', N'Đen', N'4U-G5', 3490000, 2268500, 14, 5),
 (N'victor-auraspeed-90k-ii', N'NEW-05-3U-G5', N'Xanh', N'3U-G5', 3490000, 2268500, 23, 5),
 (N'li-ning-axforce-90-dragon', N'NEW-06-4U-G5', N'Đen', N'4U-G5', 3990000, 2593500, 14, 5),
 (N'li-ning-axforce-90-dragon', N'NEW-06-3U-G5', N'Xanh', N'3U-G5', 3990000, 2593500, 23, 5),
 (N'yonex-power-cushion-65z3', N'NEW-07-40', N'Đen', N'40', 2890000, 1878500, 14, 5),
 (N'yonex-power-cushion-65z3', N'NEW-07-42', N'Trắng', N'42', 2890000, 1878500, 23, 5),
 (N'yonex-aerus-z2', N'NEW-08-40', N'Đen', N'40', 3190000, 2073500, 14, 5),
 (N'yonex-aerus-z2', N'NEW-08-42', N'Trắng', N'42', 3190000, 2073500, 23, 5),
 (N'victor-p9200iii', N'NEW-09-40', N'Đen', N'40', 2590000, 1683500, 14, 5),
 (N'victor-p9200iii', N'NEW-09-42', N'Trắng', N'42', 2590000, 1683500, 23, 5),
 (N'li-ning-saga-lite', N'NEW-10-40', N'Đen', N'40', 1790000, 1163500, 14, 5),
 (N'li-ning-saga-lite', N'NEW-10-42', N'Trắng', N'42', 1790000, 1163500, 23, 5),
 (N'yonex-shirt-crew-10650', N'NEW-11-M', N'Đen', N'M', 590000, 383500, 14, 5),
 (N'yonex-shirt-crew-10650', N'NEW-11-L', N'Trắng', N'L', 590000, 383500, 23, 5),
 (N'yonex-shorts-15110', N'NEW-12-M', N'Đen', N'M', 490000, 318500, 14, 5),
 (N'yonex-shorts-15110', N'NEW-12-L', N'Trắng', N'L', 490000, 318500, 23, 5),
 (N'victor-shirt-t25000', N'NEW-13-M', N'Đen', N'M', 450000, 292500, 14, 5),
 (N'victor-shirt-t25000', N'NEW-13-L', N'Trắng', N'L', 450000, 292500, 23, 5),
 (N'li-ning-shirt-aayu', N'NEW-14-M', N'Đen', N'M', 390000, 253500, 14, 5),
 (N'li-ning-shirt-aayu', N'NEW-14-L', N'Trắng', N'L', 390000, 253500, 23, 5),
 (N'badminton-shop-training-socks', N'NEW-15-M', N'Đen', N'M', 99000, 64350, 14, 5),
 (N'badminton-shop-training-socks', N'NEW-15-L', N'Trắng', N'L', 99000, 64350, 23, 5)
 ) s(slug,sku,color,size,price,cost_price,stock_quantity,low_stock_threshold) JOIN dbo.products p ON p.slug=s.slug
 WHERE NOT EXISTS (SELECT 1 FROM dbo.product_variants v WHERE v.sku=s.sku)
   AND NOT EXISTS (SELECT 1 FROM dbo.product_variants v WHERE v.product_id=p.id AND v.color=s.color AND v.size=s.size);

 -- ANH SAN PHAM: sua cot url = '' thanh URL https://...png neu co.
 -- Moi dong mot anh dai dien product (variant_id = NULL).

 INSERT INTO dbo.product_images(product_id,variant_id,url,sort_order)
 SELECT p.id,NULL,s.url,s.sort_order FROM (VALUES
 (N'yonex-astrox-100zz', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/tải_xuống', 0), -- PNG: Yonex Astrox 100 ZZ
 (N'yonex-arcsaber-11-pro', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images', 0), -- PNG: Yonex Arcsaber 11 Pro
 (N'yonex-nanoflare-1000z', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_1', 0), -- PNG: Yonex Nanoflare 1000 Z
 (N'victor-thruster-ryuga-ii', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_2', 0), -- PNG: Victor Thruster Ryuga II
 (N'victor-auraspeed-90k-ii', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_3', 0), -- PNG: Victor Auraspeed 90K II
 (N'li-ning-axforce-90-dragon', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_4', 0), -- PNG: Li-Ning Axforce 90 Dragon
 (N'yonex-power-cushion-65z3', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_5', 0), -- PNG: Yonex Power Cushion 65 Z3
 (N'yonex-aerus-z2', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_6', 0), -- PNG: Yonex Aerus Z2
 (N'victor-p9200iii', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_7', 0), -- PNG: Victor P9200 III
 (N'li-ning-saga-lite', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_8', 0), -- PNG: Li-Ning Saga Lite
 (N'yonex-shirt-crew-10650', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_9', 0), -- PNG: Áo Yonex Crew Neck 10650
 (N'yonex-shorts-15110', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_10', 0), -- PNG: Quần Yonex Shorts 15110
 (N'victor-shirt-t25000', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_11', 0), -- PNG: Áo Victor T-25000
 (N'li-ning-shirt-aayu', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_12', 0), -- PNG: Áo Li-Ning Training AAYU
 (N'badminton-shop-training-socks', '', 0)
 ) s(slug,url,sort_order)
 JOIN dbo.products p ON p.slug=s.slug
 WHERE NOT EXISTS (SELECT 1 FROM dbo.product_images x WHERE x.product_id=p.id AND x.variant_id IS NULL AND x.sort_order=s.sort_order);

 -- Gia lich su: mot ban ghi cho moi bien the moi, neu chua co lich su.
 -- Gia cu tham khao = gia hien tai + 100000.
 INSERT INTO dbo.product_price_history(variant_id,old_price,new_price,changed_by,changed_at)
 SELECT v.id,s.price+100000,s.price,NULL,SYSDATETIME()
 FROM (VALUES
 (N'yonex-astrox-100zz', N'NEW-01-4U-G5', N'Đen', N'4U-G5', 3890000, 2528500, 14, 5),
 (N'yonex-astrox-100zz', N'NEW-01-3U-G5', N'Xanh', N'3U-G5', 3890000, 2528500, 23, 5),
 (N'yonex-arcsaber-11-pro', N'NEW-02-4U-G5', N'Đen', N'4U-G5', 4190000, 2723500, 14, 5),
 (N'yonex-arcsaber-11-pro', N'NEW-02-3U-G5', N'Xanh', N'3U-G5', 4190000, 2723500, 23, 5),
 (N'yonex-nanoflare-1000z', N'NEW-03-4U-G5', N'Đen', N'4U-G5', 4590000, 2983500, 14, 5),
 (N'yonex-nanoflare-1000z', N'NEW-03-3U-G5', N'Xanh', N'3U-G5', 4590000, 2983500, 23, 5),
 (N'victor-thruster-ryuga-ii', N'NEW-04-4U-G5', N'Đen', N'4U-G5', 3690000, 2398500, 14, 5),
 (N'victor-thruster-ryuga-ii', N'NEW-04-3U-G5', N'Xanh', N'3U-G5', 3690000, 2398500, 23, 5),
 (N'victor-auraspeed-90k-ii', N'NEW-05-4U-G5', N'Đen', N'4U-G5', 3490000, 2268500, 14, 5),
 (N'victor-auraspeed-90k-ii', N'NEW-05-3U-G5', N'Xanh', N'3U-G5', 3490000, 2268500, 23, 5),
 (N'li-ning-axforce-90-dragon', N'NEW-06-4U-G5', N'Đen', N'4U-G5', 3990000, 2593500, 14, 5),
 (N'li-ning-axforce-90-dragon', N'NEW-06-3U-G5', N'Xanh', N'3U-G5', 3990000, 2593500, 23, 5),
 (N'yonex-power-cushion-65z3', N'NEW-07-40', N'Đen', N'40', 2890000, 1878500, 14, 5),
 (N'yonex-power-cushion-65z3', N'NEW-07-42', N'Trắng', N'42', 2890000, 1878500, 23, 5),
 (N'yonex-aerus-z2', N'NEW-08-40', N'Đen', N'40', 3190000, 2073500, 14, 5),
 (N'yonex-aerus-z2', N'NEW-08-42', N'Trắng', N'42', 3190000, 2073500, 23, 5),
 (N'victor-p9200iii', N'NEW-09-40', N'Đen', N'40', 2590000, 1683500, 14, 5),
 (N'victor-p9200iii', N'NEW-09-42', N'Trắng', N'42', 2590000, 1683500, 23, 5),
 (N'li-ning-saga-lite', N'NEW-10-40', N'Đen', N'40', 1790000, 1163500, 14, 5),
 (N'li-ning-saga-lite', N'NEW-10-42', N'Trắng', N'42', 1790000, 1163500, 23, 5),
 (N'yonex-shirt-crew-10650', N'NEW-11-M', N'Đen', N'M', 590000, 383500, 14, 5),
 (N'yonex-shirt-crew-10650', N'NEW-11-L', N'Trắng', N'L', 590000, 383500, 23, 5),
 (N'yonex-shorts-15110', N'NEW-12-M', N'Đen', N'M', 490000, 318500, 14, 5),
 (N'yonex-shorts-15110', N'NEW-12-L', N'Trắng', N'L', 490000, 318500, 23, 5),
 (N'victor-shirt-t25000', N'NEW-13-M', N'Đen', N'M', 450000, 292500, 14, 5),
 (N'victor-shirt-t25000', N'NEW-13-L', N'Trắng', N'L', 450000, 292500, 23, 5),
 (N'li-ning-shirt-aayu', N'NEW-14-M', N'Đen', N'M', 390000, 253500, 14, 5),
 (N'li-ning-shirt-aayu', N'NEW-14-L', N'Trắng', N'L', 390000, 253500, 23, 5),
 (N'badminton-shop-training-socks', N'NEW-15-M', N'Đen', N'M', 99000, 64350, 14, 5),
 (N'badminton-shop-training-socks', N'NEW-15-L', N'Trắng', N'L', 99000, 64350, 23, 5)
 ) s(slug,sku,color,size,price,cost_price,stock_quantity,low_stock_threshold) JOIN dbo.product_variants v ON v.sku=s.sku
 WHERE NOT EXISTS (SELECT 1 FROM dbo.product_price_history h WHERE h.variant_id=v.id);

 COMMIT TRANSACTION;
 PRINT N'Da them du lieu catalog (chi nhung ban ghi chua co).';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
GO
