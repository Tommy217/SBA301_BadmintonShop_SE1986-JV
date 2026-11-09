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

 -- Moi san pham 2 mau x 2 kich co = 4 bien the; SKU va ton kho rieng cho tung to hop.

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
,
 (N'yonex-astrox-100zz', N'NEW-01-3U-G5-C1', N'Đen', N'3U-G5', 3890000, 2528500, 23, 5),
 (N'yonex-astrox-100zz', N'NEW-01-4U-G5-C2', N'Xanh', N'4U-G5', 3890000, 2528500, 14, 5),
 (N'yonex-arcsaber-11-pro', N'NEW-02-3U-G5-C1', N'Đen', N'3U-G5', 4190000, 2723500, 23, 5),
 (N'yonex-arcsaber-11-pro', N'NEW-02-4U-G5-C2', N'Xanh', N'4U-G5', 4190000, 2723500, 14, 5),
 (N'yonex-nanoflare-1000z', N'NEW-03-3U-G5-C1', N'Đen', N'3U-G5', 4590000, 2983500, 23, 5),
 (N'yonex-nanoflare-1000z', N'NEW-03-4U-G5-C2', N'Xanh', N'4U-G5', 4590000, 2983500, 14, 5),
 (N'victor-thruster-ryuga-ii', N'NEW-04-3U-G5-C1', N'Đen', N'3U-G5', 3690000, 2398500, 23, 5),
 (N'victor-thruster-ryuga-ii', N'NEW-04-4U-G5-C2', N'Xanh', N'4U-G5', 3690000, 2398500, 14, 5),
 (N'victor-auraspeed-90k-ii', N'NEW-05-3U-G5-C1', N'Đen', N'3U-G5', 3490000, 2268500, 23, 5),
 (N'victor-auraspeed-90k-ii', N'NEW-05-4U-G5-C2', N'Xanh', N'4U-G5', 3490000, 2268500, 14, 5),
 (N'li-ning-axforce-90-dragon', N'NEW-06-3U-G5-C1', N'Đen', N'3U-G5', 3990000, 2593500, 23, 5),
 (N'li-ning-axforce-90-dragon', N'NEW-06-4U-G5-C2', N'Xanh', N'4U-G5', 3990000, 2593500, 14, 5),
 (N'yonex-power-cushion-65z3', N'NEW-07-42-C1', N'Đen', N'42', 2890000, 1878500, 23, 5),
 (N'yonex-power-cushion-65z3', N'NEW-07-40-C2', N'Trắng', N'40', 2890000, 1878500, 14, 5),
 (N'yonex-aerus-z2', N'NEW-08-42-C1', N'Đen', N'42', 3190000, 2073500, 23, 5),
 (N'yonex-aerus-z2', N'NEW-08-40-C2', N'Trắng', N'40', 3190000, 2073500, 14, 5),
 (N'victor-p9200iii', N'NEW-09-42-C1', N'Đen', N'42', 2590000, 1683500, 23, 5),
 (N'victor-p9200iii', N'NEW-09-40-C2', N'Trắng', N'40', 2590000, 1683500, 14, 5),
 (N'li-ning-saga-lite', N'NEW-10-42-C1', N'Đen', N'42', 1790000, 1163500, 23, 5),
 (N'li-ning-saga-lite', N'NEW-10-40-C2', N'Trắng', N'40', 1790000, 1163500, 14, 5),
 (N'yonex-shirt-crew-10650', N'NEW-11-L-C1', N'Đen', N'L', 590000, 383500, 23, 5),
 (N'yonex-shirt-crew-10650', N'NEW-11-M-C2', N'Trắng', N'M', 590000, 383500, 14, 5),
 (N'yonex-shorts-15110', N'NEW-12-L-C1', N'Đen', N'L', 490000, 318500, 23, 5),
 (N'yonex-shorts-15110', N'NEW-12-M-C2', N'Trắng', N'M', 490000, 318500, 14, 5),
 (N'victor-shirt-t25000', N'NEW-13-L-C1', N'Đen', N'L', 450000, 292500, 23, 5),
 (N'victor-shirt-t25000', N'NEW-13-M-C2', N'Trắng', N'M', 450000, 292500, 14, 5),
 (N'li-ning-shirt-aayu', N'NEW-14-L-C1', N'Đen', N'L', 390000, 253500, 23, 5),
 (N'li-ning-shirt-aayu', N'NEW-14-M-C2', N'Trắng', N'M', 390000, 253500, 14, 5),
 (N'badminton-shop-training-socks', N'NEW-15-L-C1', N'Đen', N'L', 99000, 64350, 23, 5),
 (N'badminton-shop-training-socks', N'NEW-15-M-C2', N'Trắng', N'M', 99000, 64350, 14, 5)
 ) s(slug,sku,color,size,price,cost_price,stock_quantity,low_stock_threshold) JOIN dbo.products p ON p.slug=s.slug
 WHERE NOT EXISTS (SELECT 1 FROM dbo.product_variants v WHERE v.sku=s.sku)
   AND NOT EXISTS (SELECT 1 FROM dbo.product_variants v WHERE v.product_id=p.id AND v.color=s.color AND v.size=s.size);

 -- ANH SAN PHAM: sua cot url = '' thanh URL https://...png neu co.
 -- Moi dong mot anh dai dien product (variant_id = NULL).

 INSERT INTO dbo.product_images(product_id,variant_id,url,sort_order)
 SELECT p.id,NULL,s.url,s.sort_order FROM (VALUES
 (N'yonex-astrox-100zz', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_14', 0), -- PNG: Yonex Astrox 100 ZZ
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

-- Cap nhat lai anh thuc te cho cac san pham [TEST] neu DB da ton tai tu truoc
UPDATE img
SET img.url = m.target_url
FROM dbo.product_images img
JOIN dbo.products p ON p.id = img.product_id
JOIN (VALUES
    (N'test-quan-can-yonex-ac102ex-10k', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_9'),
    (N'test-bang-co-tay-victor-sp128-12k', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_11'),
    (N'test-day-cuoc-lining-no1-15k', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_4'),
    (N'test-tat-the-thao-yonex-pro-20k', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_10'),
    (N'test-moc-khoa-yonex-11k', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_1'),
    (N'test-bang-dan-dau-vot-victor-14k', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_3'),
    (N'test-lot-giay-lining-cushion-16k', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_8'),
    (N'test-qua-cau-long-yonex-mavis-18k', 'https://res.cloudinary.com/kzizr98t/image/upload/f_auto,q_auto/images_14')
) m(slug, target_url) ON m.slug = p.slug
WHERE img.url LIKE '/products/%' OR img.url IS NULL OR img.url = '';
GO
