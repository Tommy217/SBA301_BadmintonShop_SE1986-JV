-- Keep existing Cloudinary URLs as text. Only new uploads use byte columns.
-- Also restores URL text from the previous binary-only migration without losing uploads.
SET XACT_ABORT ON;
BEGIN TRY
  BEGIN TRANSACTION;
  IF COL_LENGTH('dbo.brands', 'logo_data') IS NULL
    ALTER TABLE dbo.brands ADD logo_data VARBINARY(MAX) NULL;
  IF COL_LENGTH('dbo.product_images', 'image_data') IS NULL
    ALTER TABLE dbo.product_images ADD image_data VARBINARY(MAX) NULL;

  IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.brands') AND name = 'logo_url' AND TYPE_NAME(system_type_id) = 'varbinary')
  BEGIN
    ALTER TABLE dbo.brands ADD logo_url_text VARCHAR(MAX) NULL;
    EXEC(N'UPDATE dbo.brands SET
      logo_url_text = CASE WHEN CONVERT(VARCHAR(MAX), logo_url) LIKE ''http%'' OR CONVERT(VARCHAR(MAX), logo_url) LIKE ''/%'' THEN CONVERT(VARCHAR(MAX), logo_url) END,
      logo_data = CASE WHEN CONVERT(VARCHAR(MAX), logo_url) LIKE ''http%'' OR CONVERT(VARCHAR(MAX), logo_url) LIKE ''/%'' THEN NULL ELSE logo_url END;
      IF EXISTS (SELECT 1 FROM dbo.brands WHERE logo_url IS NOT NULL AND (logo_url_text IS NULL AND logo_data IS NULL))
        THROW 50001, ''Brand image migration failed'', 1;');
    ALTER TABLE dbo.brands DROP COLUMN logo_url;
    EXEC sys.sp_rename 'dbo.brands.logo_url_text', 'logo_url', 'COLUMN';
  END
  ELSE
    ALTER TABLE dbo.brands ALTER COLUMN logo_url VARCHAR(MAX) NULL;

  IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.product_images') AND name = 'url' AND TYPE_NAME(system_type_id) = 'varbinary')
  BEGIN
    ALTER TABLE dbo.product_images ADD url_text VARCHAR(MAX) NULL;
    EXEC(N'UPDATE dbo.product_images SET
      url_text = CASE WHEN CONVERT(VARCHAR(MAX), url) LIKE ''http%'' OR CONVERT(VARCHAR(MAX), url) LIKE ''/%'' THEN CONVERT(VARCHAR(MAX), url) END,
      image_data = CASE WHEN CONVERT(VARCHAR(MAX), url) LIKE ''http%'' OR CONVERT(VARCHAR(MAX), url) LIKE ''/%'' THEN NULL ELSE url END;
      IF EXISTS (SELECT 1 FROM dbo.product_images WHERE url IS NOT NULL AND (url_text IS NULL AND image_data IS NULL))
        THROW 50002, ''Product image migration failed'', 1;');
    ALTER TABLE dbo.product_images DROP COLUMN url;
    EXEC sys.sp_rename 'dbo.product_images.url_text', 'url', 'COLUMN';
  END
  ELSE
    ALTER TABLE dbo.product_images ALTER COLUMN url VARCHAR(MAX) NULL;
  EXEC(N'UPDATE dbo.brands SET logo_url = '''', logo_data = NULL WHERE DATALENGTH(logo_data) = 0;
    UPDATE dbo.product_images SET url = '''', image_data = NULL WHERE DATALENGTH(image_data) = 0;');
  COMMIT TRANSACTION;
END TRY
BEGIN CATCH
  IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
  THROW;
END CATCH;
