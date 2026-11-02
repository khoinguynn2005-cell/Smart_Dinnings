-- Xoa du lieu do bo test tu dong (docs/testing/api-test-runner.mjs) tao ra.
-- Moi ban ghi test deu co ten bat dau bang "QA ": danh muc, mon, ban, nhan vien, ten khach cua don mang ve.
-- Chay trong SSMS (database Smart_Dinings) hoac:
--   sqlcmd -S localhost,1433 -U sa -C -I -d Smart_Dinings -i docs/testing/cleanup-test-data.sql
USE [Smart_Dinings];
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;

BEGIN TRANSACTION;

DECLARE @t TABLE (ID int);
INSERT @t SELECT ID FROM [Tables] WHERE TableName LIKE 'QA %';

DECLARE @e TABLE (ID varchar(50));
INSERT @e SELECT ID FROM [Employees] WHERE FullName LIKE 'QA %';

-- Hoa don tren ban test + don mang ve cua khach test (khong co ban)
DECLARE @o TABLE (ID varchar(50));
INSERT @o SELECT ID FROM [Orders]
    WHERE Table_ID IN (SELECT ID FROM @t)
       OR (OrderType = 'TAKEAWAY' AND CustomerName LIKE 'QA %');

DELETE FROM [Payments] WHERE Order_ID IN (SELECT ID FROM @o);
PRINT 'Payments: ' + CAST(@@ROWCOUNT AS varchar);
DELETE FROM [Order_Details] WHERE Order_ID IN (SELECT ID FROM @o)
    OR Product_ID IN (SELECT ID FROM [Products] WHERE Name LIKE 'QA %');
PRINT 'Order_Details: ' + CAST(@@ROWCOUNT AS varchar);
DELETE FROM [Table_Requests] WHERE Table_ID IN (SELECT ID FROM @t);
PRINT 'Table_Requests: ' + CAST(@@ROWCOUNT AS varchar);
DELETE FROM [Orders] WHERE ID IN (SELECT ID FROM @o);
PRINT 'Orders: ' + CAST(@@ROWCOUNT AS varchar);
DELETE FROM [Tables] WHERE ID IN (SELECT ID FROM @t);
PRINT 'Tables: ' + CAST(@@ROWCOUNT AS varchar);
DELETE FROM [Products] WHERE Name LIKE 'QA %';
PRINT 'Products: ' + CAST(@@ROWCOUNT AS varchar);
DELETE FROM [Categories] WHERE Name LIKE 'QA %';
PRINT 'Categories: ' + CAST(@@ROWCOUNT AS varchar);

-- Nhan vien test: go tham chieu (nguoi nau / nguoi mang ra / nguoi thu tien) truoc khi xoa
UPDATE [Order_Details] SET PreparedBy = NULL WHERE PreparedBy IN (SELECT ID FROM @e);
UPDATE [Order_Details] SET ServedBy = NULL WHERE ServedBy IN (SELECT ID FROM @e);
UPDATE [Orders] SET Employee_ID = NULL WHERE Employee_ID IN (SELECT ID FROM @e);
UPDATE [Table_Requests] SET CompletedBy = NULL WHERE CompletedBy IN (SELECT ID FROM @e);
DELETE FROM [Employees] WHERE ID IN (SELECT ID FROM @e);
PRINT 'Employees: ' + CAST(@@ROWCOUNT AS varchar);

COMMIT;
