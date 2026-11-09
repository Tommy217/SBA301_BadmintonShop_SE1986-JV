-- Demo data migrated from badmintonshop.sql; intended for the shop project/demo dataset.
-- Existing matching records are preserved. Flyway applies this version only once.
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
GO
INSERT INTO dbo.ticket_evidence(ticket_id,file_url,file_type,uploaded_at)
SELECT a.ticket_id,a.file_url,a.file_type,a.created_at
FROM dbo.ticket_attachments a
WHERE NOT EXISTS (SELECT 1 FROM dbo.ticket_evidence e WHERE e.ticket_id=a.ticket_id AND e.file_url=a.file_url);
GO

-- 12. SEED ORDERS & PAYMENTS (Phục vụ test tạo đơn và Khiếu nại / Đổi trả UC15)
-- =====================================================================
DECLARE @customerId BIGINT = (SELECT id FROM dbo.users WHERE email = 'customer@gmail.com');
DECLARE @staffId BIGINT = (SELECT id FROM dbo.users WHERE email = 'staff@gmail.com');

IF @customerId IS NOT NULL
BEGIN
    -- -------------------------------------------------------------
    -- Đơn hàng 1: ORD-PAID-001 (1.540.000đ - Dưới hạn mức 3.000.000đ)
    -- Thích hợp test luồng Nhân viên kiểm tra hàng ĐẠT & tự duyệt hoàn tiền (bỏ qua Quản lý)
    -- -------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM dbo.orders WHERE order_code = 'ORD-PAID-001')
    BEGIN
        INSERT INTO dbo.orders (
            order_code, user_id, channel, status, payment_status,
            receiver_name, receiver_phone, shipping_address,
            subtotal, discount_amount, shipping_fee, total_amount,
            note, confirmed_by, confirmed_at, created_at, updated_at
        ) VALUES (
            'ORD-PAID-001', @customerId, 'online', 'completed', 'paid',
            N'Khách hàng', '0912345678', N'Số 123 Đường Cầu Giấy, Phường Dịch Vọng, Quận Cầu Giấy, Hà Nội',
            1540000, 0, 0, 1540000,
            N'Đơn test dưới hạn mức 3M (1.540.000đ) để test nhân viên duyệt trực tiếp',
            @staffId, DATEADD(DAY, -1, SYSDATETIME()), DATEADD(DAY, -1, SYSDATETIME()), SYSDATETIME()
        );

        DECLARE @order1Id BIGINT = SCOPE_IDENTITY();

        -- Chi tiết đơn hàng
        -- Item 1: Vợt Yonex Astrox 88D Play (BS-1-4U-G5) - 1.250.000đ
        INSERT INTO dbo.order_items (order_id, variant_id, product_id, product_name, variant_info, unit_price, quantity, line_total)
        SELECT @order1Id, v.id, p.id, p.name, N'Xanh biển / 4U-G5', 1250000, 1, 1250000
        FROM dbo.product_variants v
        JOIN dbo.products p ON p.id = v.product_id
        WHERE v.sku = 'BS-1-4U-G5';

        -- Item 2: Áo cầu lông Training Essential (BS-6-M) - 290.000đ
        INSERT INTO dbo.order_items (order_id, variant_id, product_id, product_name, variant_info, unit_price, quantity, line_total)
        SELECT @order1Id, v.id, p.id, p.name, N'Trắng / M', 290000, 1, 290000
        FROM dbo.product_variants v
        JOIN dbo.products p ON p.id = v.product_id
        WHERE v.sku = 'BS-6-M';

        -- Thanh toán đã hoàn tất qua VietQR PayOS
        INSERT INTO dbo.payments (order_id, method, gateway, status, amount, gateway_txn_ref, paid_at, created_at, updated_at)
        VALUES (@order1Id, 'qr', 'payos', 'paid', 1540000, 'PAYOS_TEST_001', DATEADD(DAY, -1, SYSDATETIME()), DATEADD(DAY, -1, SYSDATETIME()), SYSDATETIME());

        -- Lịch sử trạng thái đơn
        INSERT INTO dbo.order_status_history (order_id, from_status, to_status, changed_by, note, created_at)
        VALUES (@order1Id, 'shipping', 'completed', @staffId, N'Giao hàng và thanh toán thành công', DATEADD(DAY, -1, SYSDATETIME()));
    END;

    -- -------------------------------------------------------------
    -- Đơn hàng 2: ORD-PAID-002 (4.790.000đ - Vượt hạn mức 3.000.000đ)
    -- Thích hợp test luồng Nhân viên check hàng ĐẠT -> Chuyển Quản lý duyệt (PENDING_APPROVAL)
    -- -------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM dbo.orders WHERE order_code = 'ORD-PAID-002')
    BEGIN
        INSERT INTO dbo.orders (
            order_code, user_id, channel, status, payment_status,
            receiver_name, receiver_phone, shipping_address,
            subtotal, discount_amount, shipping_fee, total_amount,
            note, confirmed_by, confirmed_at, created_at, updated_at
        ) VALUES (
            'ORD-PAID-002', @customerId, 'online', 'completed', 'paid',
            N'Khách hàng', '0912345678', N'Số 123 Đường Cầu Giấy, Phường Dịch Vọng, Quận Cầu Giấy, Hà Nội',
            4790000, 0, 0, 4790000,
            N'Đơn test vượt hạn mức 3M (4.790.000đ) để test luồng chuyển Quản lý duyệt',
            @staffId, DATEADD(DAY, -2, SYSDATETIME()), DATEADD(DAY, -2, SYSDATETIME()), SYSDATETIME()
        );

        DECLARE @order2Id BIGINT = SCOPE_IDENTITY();

        -- Chi tiết đơn hàng
        -- Item 1: Vợt Yonex Astrox 88D Play (BS-1-4U-G5) - 1.250.000đ
        INSERT INTO dbo.order_items (order_id, variant_id, product_id, product_name, variant_info, unit_price, quantity, line_total)
        SELECT @order2Id, v.id, p.id, p.name, N'Xanh biển / 4U-G5', 1250000, 1, 1250000
        FROM dbo.product_variants v
        JOIN dbo.products p ON p.id = v.product_id
        WHERE v.sku = 'BS-1-4U-G5';

        -- Item 2: Vợt Li-Ning Windstorm 72 (BS-3-4U-G5) - 1.890.000đ
        INSERT INTO dbo.order_items (order_id, variant_id, product_id, product_name, variant_info, unit_price, quantity, line_total)
        SELECT @order2Id, v.id, p.id, p.name, N'Xanh biển / 4U-G5', 1890000, 1, 1890000
        FROM dbo.product_variants v
        JOIN dbo.products p ON p.id = v.product_id
        WHERE v.sku = 'BS-3-4U-G5';

        -- Item 3: Giày Yonex Power Cushion 39 (BS-4-40) - 1.650.000đ
        INSERT INTO dbo.order_items (order_id, variant_id, product_id, product_name, variant_info, unit_price, quantity, line_total)
        SELECT @order2Id, v.id, p.id, p.name, N'Trắng / 40', 1650000, 1, 1650000
        FROM dbo.product_variants v
        JOIN dbo.products p ON p.id = v.product_id
        WHERE v.sku = 'BS-4-40';

        -- Thanh toán đã hoàn tất qua VietQR PayOS
        INSERT INTO dbo.payments (order_id, method, gateway, status, amount, gateway_txn_ref, paid_at, created_at, updated_at)
        VALUES (@order2Id, 'qr', 'payos', 'paid', 4790000, 'PAYOS_TEST_002', DATEADD(DAY, -2, SYSDATETIME()), DATEADD(DAY, -2, SYSDATETIME()), SYSDATETIME());

        -- Lịch sử trạng thái đơn
        INSERT INTO dbo.order_status_history (order_id, from_status, to_status, changed_by, note, created_at)
        VALUES (@order2Id, 'shipping', 'completed', @staffId, N'Giao hàng và thanh toán thành công', DATEADD(DAY, -2, SYSDATETIME()));
    END;

    -- -------------------------------------------------------------
    -- Đơn hàng 3: ORD-PAID-003 (3.480.000đ - Nhiều sản phẩm số lượng > 1)
    -- Thích hợp test hoàn một phần (chỉ hoàn áo hoặc chỉ hoàn 1 vợt) so với hoàn toàn bộ
    -- -------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM dbo.orders WHERE order_code = 'ORD-PAID-003')
    BEGIN
        INSERT INTO dbo.orders (
            order_code, user_id, channel, status, payment_status,
            receiver_name, receiver_phone, shipping_address,
            subtotal, discount_amount, shipping_fee, total_amount,
            note, confirmed_by, confirmed_at, created_at, updated_at
        ) VALUES (
            'ORD-PAID-003', @customerId, 'online', 'completed', 'paid',
            N'Khách hàng', '0912345678', N'Số 123 Đường Cầu Giấy, Phường Dịch Vọng, Quận Cầu Giấy, Hà Nội',
            3480000, 0, 0, 3480000,
            N'Đơn test nhiều sản phẩm số lượng > 1 để test đổi trả một phần hoặc toàn phần',
            @staffId, DATEADD(DAY, -3, SYSDATETIME()), DATEADD(DAY, -3, SYSDATETIME()), SYSDATETIME()
        );

        DECLARE @order3Id BIGINT = SCOPE_IDENTITY();

        -- Item 1: Vợt Victor Thruster K 220H (BS-2-4U-G5) x 2 - 2.900.000đ
        INSERT INTO dbo.order_items (order_id, variant_id, product_id, product_name, variant_info, unit_price, quantity, line_total)
        SELECT @order3Id, v.id, p.id, p.name, N'Trắng / 4U-G5', 1450000, 2, 2900000
        FROM dbo.product_variants v
        JOIN dbo.products p ON p.id = v.product_id
        WHERE v.sku = 'BS-2-4U-G5';

        -- Item 2: Áo cầu lông Training Essential (BS-6-L) x 2 - 580.000đ
        INSERT INTO dbo.order_items (order_id, variant_id, product_id, product_name, variant_info, unit_price, quantity, line_total)
        SELECT @order3Id, v.id, p.id, p.name, N'Trắng / L', 290000, 2, 580000
        FROM dbo.product_variants v
        JOIN dbo.products p ON p.id = v.product_id
        WHERE v.sku = 'BS-6-L';

        -- Thanh toán đã hoàn tất qua VietQR PayOS
        INSERT INTO dbo.payments (order_id, method, gateway, status, amount, gateway_txn_ref, paid_at, created_at, updated_at)
        VALUES (@order3Id, 'qr', 'payos', 'paid', 3480000, 'PAYOS_TEST_003', DATEADD(DAY, -3, SYSDATETIME()), DATEADD(DAY, -3, SYSDATETIME()), SYSDATETIME());

        -- Lịch sử trạng thái đơn
        INSERT INTO dbo.order_status_history (order_id, from_status, to_status, changed_by, note, created_at)
        VALUES (@order3Id, 'shipping', 'completed', @staffId, N'Giao hàng và thanh toán thành công', DATEADD(DAY, -3, SYSDATETIME()));
    END;
END;
GO
