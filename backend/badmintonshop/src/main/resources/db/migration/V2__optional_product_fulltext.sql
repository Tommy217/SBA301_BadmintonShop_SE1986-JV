-- SQL Server full-text DDL cannot run inside a user transaction.
-- Keep this separate so the initial schema remains transactional.
IF FULLTEXTSERVICEPROPERTY('IsFullTextInstalled') = 1
BEGIN
    IF NOT EXISTS (SELECT 1 FROM sys.fulltext_catalogs WHERE name = 'ft_badminton')
        EXEC('CREATE FULLTEXT CATALOG ft_badminton AS DEFAULT');

    IF NOT EXISTS (SELECT 1 FROM sys.fulltext_indexes WHERE object_id = OBJECT_ID(N'dbo.products'))
        EXEC('CREATE FULLTEXT INDEX ON dbo.products(name) KEY INDEX PK_products ON ft_badminton');
END
ELSE
    PRINT N'Full-Text Search is not installed; product search can use LIKE.';
GO
