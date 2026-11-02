USE [Smart_Dinings];
GO
-- Don mang ve + theo doi hoat dong nhan vien. Chay mot lan tren DB dang dung (an toan khi chay lai).
SET QUOTED_IDENTIFIER ON;

-- Orders: don mang ve khong gan voi ban nao (Table_ID = NULL), phan biet bang OrderType
IF COL_LENGTH('Orders', 'OrderType') IS NULL
    ALTER TABLE [Orders] ADD [OrderType] VARCHAR(20) NOT NULL
        CONSTRAINT [DF_Orders_OrderType] DEFAULT 'DINE_IN'
        CONSTRAINT [CK_Orders_OrderType] CHECK ([OrderType] IN ('DINE_IN', 'TAKEAWAY'));
IF COL_LENGTH('Orders', 'CustomerName') IS NULL ALTER TABLE [Orders] ADD [CustomerName] NVARCHAR(100) NULL;
IF COL_LENGTH('Orders', 'CustomerPhone') IS NULL ALTER TABLE [Orders] ADD [CustomerPhone] VARCHAR(15) NULL;
IF COL_LENGTH('Orders', 'PickupCode') IS NULL ALTER TABLE [Orders] ADD [PickupCode] INT NULL;
IF COL_LENGTH('Orders', 'PickupTime') IS NULL ALTER TABLE [Orders] ADD [PickupTime] DATETIME NULL;

-- Order_Details: ai nau / ai mang mon ra va luc nao (cho bao cao hoat dong nhan vien)
IF COL_LENGTH('Order_Details', 'PreparedBy') IS NULL ALTER TABLE [Order_Details] ADD [PreparedBy] VARCHAR(50) NULL;
IF COL_LENGTH('Order_Details', 'StartedTime') IS NULL ALTER TABLE [Order_Details] ADD [StartedTime] DATETIME NULL;
IF COL_LENGTH('Order_Details', 'ReadyTime') IS NULL ALTER TABLE [Order_Details] ADD [ReadyTime] DATETIME NULL;
IF COL_LENGTH('Order_Details', 'ServedBy') IS NULL ALTER TABLE [Order_Details] ADD [ServedBy] VARCHAR(50) NULL;
IF COL_LENGTH('Order_Details', 'ServedTime') IS NULL ALTER TABLE [Order_Details] ADD [ServedTime] DATETIME NULL;
IF OBJECT_ID('FK_OD_PreparedBy') IS NULL
    ALTER TABLE [Order_Details] ADD CONSTRAINT [FK_OD_PreparedBy] FOREIGN KEY ([PreparedBy]) REFERENCES [Employees]([ID]);
IF OBJECT_ID('FK_OD_ServedBy') IS NULL
    ALTER TABLE [Order_Details] ADD CONSTRAINT [FK_OD_ServedBy] FOREIGN KEY ([ServedBy]) REFERENCES [Employees]([ID]);
