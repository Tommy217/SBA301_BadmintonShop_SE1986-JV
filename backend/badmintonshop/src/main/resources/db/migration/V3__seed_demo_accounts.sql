-- Demo accounts from badmintonshop.sql. All four demo passwords are 123.
-- Preserve existing accounts and their passwords on databases baselined at V1.
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.users WITH (UPDLOCK, HOLDLOCK) WHERE email = 'admin@gmail.com')
    INSERT INTO dbo.users (role_id, email, password_hash, full_name, status, email_verified_at) VALUES
    ((SELECT id FROM roles WHERE code = 'admin'),    'admin@gmail.com',    '$2a$10$NJDzXjN2O69vD1OjD1/MU.KsvrAlJHEBK8PF5VdOYWQPn4hDeUUNS', N'Quản trị viên', 'active', SYSDATETIME());

IF NOT EXISTS (SELECT 1 FROM dbo.users WITH (UPDLOCK, HOLDLOCK) WHERE email = 'manager@gmail.com')
    INSERT INTO dbo.users (role_id, email, password_hash, full_name, status, email_verified_at) VALUES
    ((SELECT id FROM roles WHERE code = 'manager'),  'manager@gmail.com',  '$2a$10$NJDzXjN2O69vD1OjD1/MU.KsvrAlJHEBK8PF5VdOYWQPn4hDeUUNS', N'Quản lý',       'active', SYSDATETIME());

IF NOT EXISTS (SELECT 1 FROM dbo.users WITH (UPDLOCK, HOLDLOCK) WHERE email = 'staff@gmail.com')
    INSERT INTO dbo.users (role_id, email, password_hash, full_name, status, email_verified_at) VALUES
    ((SELECT id FROM roles WHERE code = 'staff'),    'staff@gmail.com',    '$2a$10$NJDzXjN2O69vD1OjD1/MU.KsvrAlJHEBK8PF5VdOYWQPn4hDeUUNS', N'Nhân viên',     'active', SYSDATETIME());

IF NOT EXISTS (SELECT 1 FROM dbo.users WITH (UPDLOCK, HOLDLOCK) WHERE email = 'customer@gmail.com')
    INSERT INTO dbo.users (role_id, email, password_hash, full_name, status, email_verified_at) VALUES
    ((SELECT id FROM roles WHERE code = 'customer'), 'customer@gmail.com', '$2a$10$NJDzXjN2O69vD1OjD1/MU.KsvrAlJHEBK8PF5VdOYWQPn4hDeUUNS', N'Khách hàng',    'active', SYSDATETIME());
GO
