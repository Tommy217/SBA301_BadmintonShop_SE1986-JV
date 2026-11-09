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
    -- Insert separately so identity IDs follow this file order on a fresh database.
    -- Storefront default order is product ID ascending: rackets, shoes, apparel, then test products.
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'yonex-astrox-100zz')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'racket'), (SELECT id FROM dbo.brands WHERE slug = N'yonex'), N'racket', N'Vợt cầu lông Yonex Astrox 100 ZZ', N'yonex-astrox-100zz', N'Vợt thiên công, dành cho người chơi trình độ cao.', 3890000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'yonex-arcsaber-11-pro')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'racket'), (SELECT id FROM dbo.brands WHERE slug = N'yonex'), N'racket', N'Vợt cầu lông Yonex Arcsaber 11 Pro', N'yonex-arcsaber-11-pro', N'Vợt cân bằng thiên về kiểm soát và điều cầu.', 4190000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'yonex-nanoflare-1000z')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'racket'), (SELECT id FROM dbo.brands WHERE slug = N'yonex'), N'racket', N'Vợt cầu lông Yonex Nanoflare 1000 Z', N'yonex-nanoflare-1000z', N'Vợt nhẹ đầu hỗ trợ tốc độ vung vợt.', 4590000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'victor-thruster-ryuga-ii')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'racket'), (SELECT id FROM dbo.brands WHERE slug = N'victor'), N'racket', N'Vợt cầu lông Victor Thruster Ryuga II', N'victor-thruster-ryuga-ii', N'Vợt thiên công với khung trợ lực.', 3690000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'victor-auraspeed-90k-ii')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'racket'), (SELECT id FROM dbo.brands WHERE slug = N'victor'), N'racket', N'Vợt cầu lông Victor Auraspeed 90K II', N'victor-auraspeed-90k-ii', N'Vợt tốc độ dành cho lối chơi phản tạt.', 3490000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'li-ning-axforce-90-dragon')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'racket'), (SELECT id FROM dbo.brands WHERE slug = N'li-ning'), N'racket', N'Vợt cầu lông Li-Ning Axforce 90 Dragon', N'li-ning-axforce-90-dragon', N'Vợt thiên công phục vụ đập cầu mạnh.', 3990000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'yonex-power-cushion-65z3')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'shoes'), (SELECT id FROM dbo.brands WHERE slug = N'yonex'), N'shoes', N'Giày Yonex Power Cushion 65 Z3', N'yonex-power-cushion-65z3', N'Giày cầu lông có đệm hỗ trợ di chuyển.', 2890000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'yonex-aerus-z2')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'shoes'), (SELECT id FROM dbo.brands WHERE slug = N'yonex'), N'shoes', N'Giày Yonex Aerus Z2', N'yonex-aerus-z2', N'Giày nhẹ ưu tiên tính linh hoạt.', 3190000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'victor-p9200iii')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'shoes'), (SELECT id FROM dbo.brands WHERE slug = N'victor'), N'shoes', N'Giày Victor P9200 III', N'victor-p9200iii', N'Giày có độ ổn định và hỗ trợ bàn chân.', 2590000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'li-ning-saga-lite')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'shoes'), (SELECT id FROM dbo.brands WHERE slug = N'li-ning'), N'shoes', N'Giày Li-Ning Saga Lite', N'li-ning-saga-lite', N'Giày luyện tập trọng lượng nhẹ.', 1790000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'yonex-shirt-crew-10650')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'apparel'), (SELECT id FROM dbo.brands WHERE slug = N'yonex'), N'apparel', N'Áo Yonex Crew Neck 10650', N'yonex-shirt-crew-10650', N'Áo thi đấu cầu lông thoáng khí.', 590000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'yonex-shorts-15110')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'apparel'), (SELECT id FROM dbo.brands WHERE slug = N'yonex'), N'apparel', N'Quần Yonex Shorts 15110', N'yonex-shorts-15110', N'Quần short thể thao nhẹ, co giãn.', 490000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'victor-shirt-t25000')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'apparel'), (SELECT id FROM dbo.brands WHERE slug = N'victor'), N'apparel', N'Áo Victor T-25000', N'victor-shirt-t25000', N'Áo cầu lông dành cho luyện tập.', 450000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'li-ning-shirt-aayu')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'apparel'), (SELECT id FROM dbo.brands WHERE slug = N'li-ning'), N'apparel', N'Áo Li-Ning Training AAYU', N'li-ning-shirt-aayu', N'Áo luyện tập nhanh khô.', 390000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'badminton-shop-training-socks')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'apparel'), (SELECT id FROM dbo.brands WHERE slug = N'badminton-shop'), N'apparel', N'Tất cầu lông Training Socks', N'badminton-shop-training-socks', N'Tất thể thao cổ trung, thoáng khí.', 99000, 1, NULL, SYSDATETIME(), SYSDATETIME());

    -- Keep the 8 test products with mismatched images at the end.
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'test-quan-can-yonex-ac102ex-10k')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'apparel'), (SELECT id FROM dbo.brands WHERE slug = N'yonex'), N'apparel', N'[TEST] Quấn cán vợt Yonex AC102EX', N'test-quan-can-yonex-ac102ex-10k', N'Quấn cán chống trơn trượt Yonex AC102EX - Sản phẩm test luồng Order, Payment và Return (10.000 VNĐ).', 10000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'test-bang-co-tay-victor-sp128-12k')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'apparel'), (SELECT id FROM dbo.brands WHERE slug = N'victor'), N'apparel', N'[TEST] Băng cổ tay Victor SP128', N'test-bang-co-tay-victor-sp128-12k', N'Băng cổ tay thấm hút mồ hôi Victor SP128 - Sản phẩm test luồng Order, Payment và Return (12.000 VNĐ).', 12000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'test-day-cuoc-lining-no1-15k')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'racket'), (SELECT id FROM dbo.brands WHERE slug = N'li-ning'), N'racket', N'[TEST] Dây cước căng vợt Li-Ning No.1', N'test-day-cuoc-lining-no1-15k', N'Dây cước trợ lực nảy cầu Li-Ning No.1 0.65mm - Sản phẩm test luồng Order, Payment và Return (15.000 VNĐ).', 15000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'test-tat-the-thao-yonex-pro-20k')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'apparel'), (SELECT id FROM dbo.brands WHERE slug = N'yonex'), N'apparel', N'[TEST] Tất thể thao Yonex Pro Cushion', N'test-tat-the-thao-yonex-pro-20k', N'Vớ cổ ngắn thể thao đệm gót êm ái Yonex Pro - Sản phẩm test luồng Order, Payment và Return (20.000 VNĐ).', 20000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'test-moc-khoa-yonex-11k')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'apparel'), (SELECT id FROM dbo.brands WHERE slug = N'yonex'), N'apparel', N'[TEST] Móc khóa cầu lông Yonex Mini', N'test-moc-khoa-yonex-11k', N'Móc khóa hình vợt cầu lông mini Yonex - Sản phẩm test luồng Order, Payment và Return (11.000 VNĐ).', 11000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'test-bang-dan-dau-vot-victor-14k')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'racket'), (SELECT id FROM dbo.brands WHERE slug = N'victor'), N'racket', N'[TEST] Băng dán bảo vệ đầu vợt Victor Guard', N'test-bang-dan-dau-vot-victor-14k', N'Băng dán chống trầy xước khung vợt Victor - Sản phẩm test luồng Order, Payment và Return (14.000 VNĐ).', 14000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'test-lot-giay-lining-cushion-16k')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'shoes'), (SELECT id FROM dbo.brands WHERE slug = N'li-ning'), N'shoes', N'[TEST] Lót giày giảm chấn Li-Ning Cushion', N'test-lot-giay-lining-cushion-16k', N'Miếng lót giày thể thao trợ lực êm chân Li-Ning - Sản phẩm test luồng Order, Payment và Return (16.000 VNĐ).', 16000, 1, NULL, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.products WITH (UPDLOCK, HOLDLOCK) WHERE slug = N'test-qua-cau-long-yonex-mavis-18k')
        INSERT INTO dbo.products (category_id, brand_id, product_type, name, slug, description, base_price, is_active, created_by, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.categories WHERE slug = N'apparel'), (SELECT id FROM dbo.brands WHERE slug = N'yonex'), N'apparel', N'[TEST] Quả cầu lông nhựa Yonex Mavis 350', N'test-qua-cau-long-yonex-mavis-18k', N'Quả cầu lông nhựa siêu bền Yonex Mavis 350 - Sản phẩm test luồng Order, Payment và Return (18.000 VNĐ).', 18000, 1, NULL, SYSDATETIME(), SYSDATETIME());

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
    IF NOT EXISTS (SELECT 1 FROM dbo.racket_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-day-cuoc-lining-no1-15k'))
        INSERT INTO dbo.racket_specs (product_id, balance_type, flex, play_style, max_tension_lbs, material, shaft_diameter)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-day-cuoc-lining-no1-15k'), N'even', N'medium', N'all_round', 30.0, N'Nylon đàn hồi cao cấp', 0.7);
    IF NOT EXISTS (SELECT 1 FROM dbo.racket_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-bang-dan-dau-vot-victor-14k'))
        INSERT INTO dbo.racket_specs (product_id, balance_type, flex, play_style, max_tension_lbs, material, shaft_diameter)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-bang-dan-dau-vot-victor-14k'), N'even', N'medium', N'all_round', 30.0, N'Polyurethane dẻo chống mài mòn', 0.7);

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
    IF NOT EXISTS (SELECT 1 FROM dbo.shoe_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-lot-giay-lining-cushion-16k'))
        INSERT INTO dbo.shoe_specs (product_id, gender, sole_type, technology, foot_width)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-lot-giay-lining-cushion-16k'), N'unisex', N'Đệm EVA trợ lực', N'Li-Ning Cloud Cushion', N'regular');

    -- apparel_specs: 2 rows
    IF NOT EXISTS (SELECT 1 FROM dbo.apparel_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'ao-cau-long-training-essential-6'))
        INSERT INTO dbo.apparel_specs (product_id, gender, apparel_kind, material)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'ao-cau-long-training-essential-6'), N'unisex', N'shirt', N'Chất liệu mẫu — cập nhật theo nhà sản xuất');
    IF NOT EXISTS (SELECT 1 FROM dbo.apparel_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'demo-product-apparel'))
        INSERT INTO dbo.apparel_specs (product_id, gender, apparel_kind, material)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'demo-product-apparel'), N'unisex', N'shirt', N'Chất liệu mẫu — cập nhật theo nhà sản xuất');
    IF NOT EXISTS (SELECT 1 FROM dbo.apparel_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-quan-can-yonex-ac102ex-10k'))
        INSERT INTO dbo.apparel_specs (product_id, gender, apparel_kind, material)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-quan-can-yonex-ac102ex-10k'), N'unisex', N'other', N'Polyurethane cao cấp');
    IF NOT EXISTS (SELECT 1 FROM dbo.apparel_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-bang-co-tay-victor-sp128-12k'))
        INSERT INTO dbo.apparel_specs (product_id, gender, apparel_kind, material)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-bang-co-tay-victor-sp128-12k'), N'unisex', N'other', N'Cotton dệt kim đàn hồi');
    IF NOT EXISTS (SELECT 1 FROM dbo.apparel_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-tat-the-thao-yonex-pro-20k'))
        INSERT INTO dbo.apparel_specs (product_id, gender, apparel_kind, material)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-tat-the-thao-yonex-pro-20k'), N'unisex', N'socks', N'Cotton dệt kim thoáng khí');
    IF NOT EXISTS (SELECT 1 FROM dbo.apparel_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-moc-khoa-yonex-11k'))
        INSERT INTO dbo.apparel_specs (product_id, gender, apparel_kind, material)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-moc-khoa-yonex-11k'), N'unisex', N'other', N'Nhựa dẻo & hợp kim');
    IF NOT EXISTS (SELECT 1 FROM dbo.apparel_specs WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-qua-cau-long-yonex-mavis-18k'))
        INSERT INTO dbo.apparel_specs (product_id, gender, apparel_kind, material)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-qua-cau-long-yonex-mavis-18k'), N'unisex', N'other', N'Nylon siêu bền & đế bần tự nhiên');

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
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'TEST-QC-10K')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-quan-can-yonex-ac102ex-10k'), N'TEST-QC-10K', N'TESTQC10K01', N'Trắng', N'Tiêu chuẩn', 10000, 5000, 1000, 5, 1, 20, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'TEST-BCT-12K')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-bang-co-tay-victor-sp128-12k'), N'TEST-BCT-12K', N'TESTBCT12K01', N'Đen', N'FreeSize', 12000, 6000, 1000, 5, 1, 30, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'TEST-DC-15K')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-day-cuoc-lining-no1-15k'), N'TEST-DC-15K', N'TESTDC15K01', N'Vàng chanh', N'0.65mm', 15000, 8000, 1000, 5, 1, 15, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'TEST-TAT-20K')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-tat-the-thao-yonex-pro-20k'), N'TEST-TAT-20K', N'TESTTAT20K01', N'Trắng xám', N'L (25-28cm)', 20000, 10000, 1000, 5, 1, 50, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'TEST-MK-11K')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-moc-khoa-yonex-11k'), N'TEST-MK-11K', N'TESTMK11K01', N'Xanh dương', N'Mini', 11000, 5000, 1000, 5, 1, 15, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'TEST-BDV-14K')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-bang-dan-dau-vot-victor-14k'), N'TEST-BDV-14K', N'TESTBDV14K01', N'Đen mờ', N'Tiêu chuẩn', 14000, 7000, 1000, 5, 1, 10, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'TEST-LG-16K')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-lot-giay-lining-cushion-16k'), N'TEST-LG-16K', N'TESTLG16K01', N'Vàng đen', N'40-44 (FreeSize)', 16000, 8000, 1000, 5, 1, 40, SYSDATETIME(), SYSDATETIME());
    IF NOT EXISTS (SELECT 1 FROM dbo.product_variants WITH (UPDLOCK, HOLDLOCK) WHERE sku = N'TEST-CL-18K')
        INSERT INTO dbo.product_variants (product_id, sku, barcode, color, size, price, cost_price, stock_quantity, low_stock_threshold, is_active, weight_grams, created_at, updated_at)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-qua-cau-long-yonex-mavis-18k'), N'TEST-CL-18K', N'TESTCL18K01', N'Vàng neon', N'Ống 1 quả', 18000, 9000, 1000, 5, 1, 25, SYSDATETIME(), SYSDATETIME());

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
    -- product_images: Tái sử dụng ảnh thực tế (Cloudinary) từ các sản phẩm khác cho các bản [TEST]
    IF NOT EXISTS (SELECT 1 FROM dbo.product_images WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-quan-can-yonex-ac102ex-10k'))
        INSERT INTO dbo.product_images (product_id, variant_id, url, sort_order)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-quan-can-yonex-ac102ex-10k'), (SELECT id FROM dbo.product_variants WHERE sku = N'TEST-QC-10K'), 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_9', 0); -- Áo Yonex Crew Neck 10650
    ELSE
        UPDATE dbo.product_images
        SET url = 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_9'
        WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-quan-can-yonex-ac102ex-10k') AND (url LIKE '/products/%' OR url IS NULL OR url = '');

    IF NOT EXISTS (SELECT 1 FROM dbo.product_images WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-bang-co-tay-victor-sp128-12k'))
        INSERT INTO dbo.product_images (product_id, variant_id, url, sort_order)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-bang-co-tay-victor-sp128-12k'), (SELECT id FROM dbo.product_variants WHERE sku = N'TEST-BCT-12K'), 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_11', 0); -- Áo Victor T-25000
    ELSE
        UPDATE dbo.product_images
        SET url = 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_11'
        WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-bang-co-tay-victor-sp128-12k') AND (url LIKE '/products/%' OR url IS NULL OR url = '');

    IF NOT EXISTS (SELECT 1 FROM dbo.product_images WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-day-cuoc-lining-no1-15k'))
        INSERT INTO dbo.product_images (product_id, variant_id, url, sort_order)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-day-cuoc-lining-no1-15k'), (SELECT id FROM dbo.product_variants WHERE sku = N'TEST-DC-15K'), 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_4', 0); -- Vợt Li-Ning Axforce 90 Dragon
    ELSE
        UPDATE dbo.product_images
        SET url = 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_4'
        WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-day-cuoc-lining-no1-15k') AND (url LIKE '/products/%' OR url IS NULL OR url = '');

    IF NOT EXISTS (SELECT 1 FROM dbo.product_images WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-tat-the-thao-yonex-pro-20k'))
        INSERT INTO dbo.product_images (product_id, variant_id, url, sort_order)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-tat-the-thao-yonex-pro-20k'), (SELECT id FROM dbo.product_variants WHERE sku = N'TEST-TAT-20K'), 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_10', 0); -- Quần Yonex Shorts 15110
    ELSE
        UPDATE dbo.product_images
        SET url = 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_10'
        WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-tat-the-thao-yonex-pro-20k') AND (url LIKE '/products/%' OR url IS NULL OR url = '');

    IF NOT EXISTS (SELECT 1 FROM dbo.product_images WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-moc-khoa-yonex-11k'))
        INSERT INTO dbo.product_images (product_id, variant_id, url, sort_order)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-moc-khoa-yonex-11k'), (SELECT id FROM dbo.product_variants WHERE sku = N'TEST-MK-11K'), 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_1', 0); -- Vợt Yonex Nanoflare 1000 Z
    ELSE
        UPDATE dbo.product_images
        SET url = 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_1'
        WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-moc-khoa-yonex-11k') AND (url LIKE '/products/%' OR url IS NULL OR url = '');

    IF NOT EXISTS (SELECT 1 FROM dbo.product_images WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-bang-dan-dau-vot-victor-14k'))
        INSERT INTO dbo.product_images (product_id, variant_id, url, sort_order)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-bang-dan-dau-vot-victor-14k'), (SELECT id FROM dbo.product_variants WHERE sku = N'TEST-BDV-14K'), 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_3', 0); -- Vợt Victor Auraspeed 90K II
    ELSE
        UPDATE dbo.product_images
        SET url = 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_3'
        WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-bang-dan-dau-vot-victor-14k') AND (url LIKE '/products/%' OR url IS NULL OR url = '');

    IF NOT EXISTS (SELECT 1 FROM dbo.product_images WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-lot-giay-lining-cushion-16k'))
        INSERT INTO dbo.product_images (product_id, variant_id, url, sort_order)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-lot-giay-lining-cushion-16k'), (SELECT id FROM dbo.product_variants WHERE sku = N'TEST-LG-16K'), 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_8', 0); -- Giày Li-Ning Saga Lite
    ELSE
        UPDATE dbo.product_images
        SET url = 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_8'
        WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-lot-giay-lining-cushion-16k') AND (url LIKE '/products/%' OR url IS NULL OR url = '');

    IF NOT EXISTS (SELECT 1 FROM dbo.product_images WITH (UPDLOCK, HOLDLOCK) WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-qua-cau-long-yonex-mavis-18k'))
        INSERT INTO dbo.product_images (product_id, variant_id, url, sort_order)
        VALUES ((SELECT id FROM dbo.products WHERE slug = N'test-qua-cau-long-yonex-mavis-18k'), (SELECT id FROM dbo.product_variants WHERE sku = N'TEST-CL-18K'), 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_14', 0); -- Vợt Yonex Astrox 100 ZZ
    ELSE
        UPDATE dbo.product_images
        SET url = 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_14'
        WHERE product_id = (SELECT id FROM dbo.products WHERE slug = N'test-qua-cau-long-yonex-mavis-18k') AND (url LIKE '/products/%' OR url IS NULL OR url = '');

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
