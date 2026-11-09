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
