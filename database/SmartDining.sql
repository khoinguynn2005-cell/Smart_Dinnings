/*
USE [master];
GO 

IF EXISTS (SELECT name FROM sys.databases WHERE name = 'Smart_Dinings')
BEGIN
    ALTER DATABASE Smart_Dinings SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE [Smart_Dinings];
END
GO

CREATE DATABASE [Smart_Dinings];
GO
*/

USE [Smart_Dinings];
GO

select name from sys.tables;

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

CREATE TABLE [Employees] (
    [ID]		VARCHAR(50) PRIMARY KEY,
    [Username]	VARCHAR(50) UNIQUE,
    [Password]  VARCHAR(255),
    [FullName]  NVARCHAR(100),
    [Phone]		VARCHAR(15),
    [Email]		VARCHAR(100),
    [Role]		VARCHAR(20) CHECK ([Role] IN ('ADMIN', 'STAFF', 'KITCHEN')),
    [Status]    VARCHAR(20)
);
GO

CREATE UNIQUE INDEX [UX_Employees_OneAdmin] ON [Employees] ([Role]) WHERE [Role] = 'ADMIN';
GO

CREATE TABLE [Tables] (
    [ID]			INT IDENTITY(1,1) PRIMARY KEY,
    [TableName]     NVARCHAR(50),
    [QRCodeURL]		VARCHAR(255) UNIQUE,
    [Status]        VARCHAR(20) CHECK ([Status] IN ('AVAILABLE', 'FULL', 'WAITING_PAYMENT', 'INACTIVE'))
);
GO

CREATE TABLE [Categories] (
    [ID]	INT IDENTITY(1,1) PRIMARY KEY,
    [Name]	NVARCHAR(100)
);
GO

CREATE TABLE [Products] (
    [ID]			VARCHAR(50) PRIMARY KEY,
    [Category_ID]	INT,
    [Name]			NVARCHAR(150),
    [ImageURL]		VARCHAR(MAX),
    [Description]	NVARCHAR(500),
    [Price]			DECIMAL(13,2),
    [Status]		VARCHAR(20) 
);
GO

CREATE TABLE [Orders] (
    [ID]			VARCHAR(50) PRIMARY KEY,
    [Table_ID]		INT,
    [Employee_ID]	VARCHAR(50), 
    [StartTime]		DATETIME,
    [TotalAmount]	DECIMAL(13,2),
    [Note]			NVARCHAR(255),
    [Status]		VARCHAR(20),
    [OrderType]		VARCHAR(20) NOT NULL DEFAULT 'DINE_IN' CHECK ([OrderType] IN ('DINE_IN', 'TAKEAWAY')),
    [CustomerName]	NVARCHAR(100),	-- don mang ve: Table_ID = NULL, thong tin khach khong bat buoc
    [CustomerPhone]	VARCHAR(15),
    [PickupCode]		INT,			-- ma lay do #1, #2... dem lai moi ngay
    [PickupTime]		DATETIME
);
GO

CREATE TABLE [Order_Details] (
    [OrderDetailID]		VARCHAR(50) PRIMARY KEY,
    [Order_ID]			VARCHAR(50),
    [Product_ID]		VARCHAR(50),
    [Quantity]			INT CHECK ([Quantity] > 0),
    [UnitPrice]			DECIMAL(13,2) CHECK ([UnitPrice] >= 0),
    [CreatedTime]		DATETIME,
    [Note]				NVARCHAR(255),
    [Status]			VARCHAR(20),
    [PreparedBy]		VARCHAR(50),	-- nhan vien bep nhan lam / bao het mon
    [StartedTime]		DATETIME,
    [ReadyTime]			DATETIME,
    [ServedBy]			VARCHAR(50),	-- nhan vien mang mon ra
    [ServedTime]		DATETIME
);
GO

CREATE TABLE [Payments] (
    [PaymentID]			VARCHAR(50) PRIMARY KEY,
    [Order_ID]			VARCHAR(50),
    [PaymentMethod]		VARCHAR(50),
    [Amount]			DECIMAL(13,2),
    [PayTime]			DATETIME,
    [Status]			VARCHAR(20),
    [Image]			VARCHAR(MAX)	-- anh chup bill chuyen khoan (data URL), NULL khi tra tien mat
);
GO

CREATE TABLE [Table_Requests] (
    [ID]            VARCHAR(50) PRIMARY KEY,
    [Table_ID]      INT NOT NULL,
    [RequestType]   VARCHAR(20) NOT NULL CHECK ([RequestType] IN ('PAYMENT', 'SERVICE')),
    [ServiceType]   NVARCHAR(80) NULL,
    [Note]          NVARCHAR(255) NULL,
    [TotalAmount]   DECIMAL(13,2) NULL CHECK ([TotalAmount] >= 0),
    [RequestedTime] DATETIME NOT NULL,
    [Status]        VARCHAR(20) NOT NULL CHECK ([Status] IN ('PENDING', 'COMPLETED')),
    [CompletedTime] DATETIME NULL,
    [CompletedBy]   VARCHAR(50) NULL
);
GO

ALTER TABLE [Products] 
ADD CONSTRAINT [FK_PRODUCTS_CATEGORY] FOREIGN KEY([Category_ID]) REFERENCES [Categories]([ID]);
GO

ALTER TABLE [Orders]
ADD CONSTRAINT [FK_Order_Table] FOREIGN KEY([Table_ID]) REFERENCES [Tables]([ID]);
GO

ALTER TABLE [Orders]
ADD CONSTRAINT [FK_Order_Employee] FOREIGN KEY([Employee_ID]) REFERENCES [Employees]([ID]);
GO

ALTER TABLE [Order_Details]
ADD CONSTRAINT [FK_ORDE_Order] FOREIGN KEY([Order_ID]) REFERENCES [Orders]([ID]);
GO

ALTER TABLE [Order_Details]
ADD CONSTRAINT [FK_ORDE_Product] FOREIGN KEY([Product_ID]) REFERENCES [Products]([ID]);
GO 

ALTER TABLE [Order_Details]
ADD CONSTRAINT [FK_OD_PreparedBy] FOREIGN KEY([PreparedBy]) REFERENCES [Employees]([ID]);
GO

ALTER TABLE [Order_Details]
ADD CONSTRAINT [FK_OD_ServedBy] FOREIGN KEY([ServedBy]) REFERENCES [Employees]([ID]);
GO

ALTER TABLE [Payments]
ADD CONSTRAINT [FK_PM_Order] FOREIGN KEY([Order_ID]) REFERENCES [Orders]([ID]);

ALTER TABLE [Table_Requests]
ADD CONSTRAINT [FK_TableRequests_Table] FOREIGN KEY([Table_ID]) REFERENCES [Tables]([ID]);

ALTER TABLE [Table_Requests]
ADD CONSTRAINT [FK_TableRequests_Employee] FOREIGN KEY([CompletedBy]) REFERENCES [Employees]([ID]);

CREATE INDEX [IX_TableRequests_Status_Time] ON [Table_Requests] ([Status], [RequestedTime]);

CREATE UNIQUE INDEX [UX_TableRequests_PendingPayment] ON [Table_Requests] ([Table_ID])
WHERE [RequestType] = 'PAYMENT' AND [Status] = 'PENDING';

USE [Smart_Dinings];
GO

-- Tài khoản ADMIN: admin           MK: Admin@123
-- Tài khoản nhân viên: staff1      MK: Staff@123
-- Tài khoản bếp: kitchen1         MK: Kitchen@123
INSERT INTO [Employees] ([ID], [Username], [Password], [FullName], [Phone], [Email], [Role], [Status]) VALUES
('EMP01', 'admin', 'Admin@123', N'Nguyễn Văn Quản Lý', '0901234567', 'admin@smartdining.com', 'ADMIN', 'ACTIVE'),
('EMP02', 'staff1', 'Staff@123', N'Nhân viên 01', '0900000001', 'staff1@smartdining.local', 'STAFF', 'ACTIVE'),
('EMP03', 'kitchen1', 'Kitchen@123', N'Nhân viên bếp 01', '0900000002', 'kitchen1@smartdining.local', 'KITCHEN', 'ACTIVE');
GO

INSERT INTO [Employees] ([ID], [Username], [Password], [FullName], [Phone], [Email], [Role], [Status]) VALUES
('EMP04', 'staff2', '1234', N'Trấn Đức Quang', '0901234567', 'tranquang@gmail.com', 'STAFF', 'INACTIVE');
GO

select * from [Employees]

INSERT INTO [Tables] ([TableName], [QRCodeURL], [Status]) VALUES
(N'Bàn 01', '6556bbebe9ef44db870a5ba2ba571584', 'AVAILABLE'),
(N'Bàn 02', '07077465940b430686d4789888aa4af9', 'FULL'),
(N'Bàn 03', '9fce0415be4542eea39efd5c1ff2d32c', 'AVAILABLE'),
(N'Bàn 04', 'ca8f16046221459793d09cdec624f20f', 'AVAILABLE'),
(N'Bàn 05', 'fcc23b6a33af47eaa1987f6b1191ed75', 'FULL'),
(N'Bàn VIP 1', 'b227c44da3ce4856b22136f5e4560ca3', 'AVAILABLE');
GO

INSERT INTO [Categories] ([Name]) VALUES
(N'Món Khai Vị'),    -- ID: 1
(N'Món Chính'),      -- ID: 2
(N'Đồ Uống'),        -- ID: 3
(N'Tráng Miệng');    -- ID: 4
GO

INSERT INTO [Products] ([ID], [Category_ID], [Name], [ImageURL], [Description], [Price], [Status]) VALUES
-- Khai vị 
('P021', 1, N'Đậu Phộng Rang Tỏi Ớt', 'https://tse3.mm.bing.net/th/id/OIP.PGruOSDDTtEIbj0Q2s7o0QHaEK?r=0&rs=1&pid=ImgDetMain&o=7&rm=3', N'Đậu phộng rang giòn rụm cháy tỏi ớt cay nồng', 30000, 'AVAILABLE'),
('P022', 1, N'Chân Gà Ngâm Sả Tắc', 'https://tse1.mm.bing.net/th/id/OIP.oJfUTxsbI-BVDR1Z_YwHlAHaGR?r=0&rs=1&pid=ImgDetMain&o=7&rm=3', N'Chân gà giòn sần sật ngâm chua ngọt cùng sả, tắc, ớt', 65000, 'AVAILABLE'),
('P023', 1, N'Gỏi Xoài Tôm Khô', 'https://tse2.mm.bing.net/th/id/OIP.CJERM6LbpHzmuI8LL0HnuAHaE7?r=0&rs=1&pid=ImgDetMain&o=7&rm=3', N'Gỏi xoài xanh bào sợi trộn tôm khô chua cay', 75000, 'AVAILABLE'),
('P024', 1, N'Khô Mực Nướng Cồn', 'https://tse2.mm.bing.net/th/id/OIP.GJwi-MkEpvQ6eWqedK_KkQHaHi?r=0&rs=1&pid=ImgDetMain&o=7&rm=3', N'Khô mực câu nướng thơm lừng, xé tơi chấm tương ớt', 120000, 'AVAILABLE'),

-- Món Chính
('P025', 2, N'Sụn Gà Chiên Nước Mắm', 'https://tse4.mm.bing.net/th/id/OIP.oQnIGBKLTL_4fZi9knwq1wHaFK?r=0&rs=1&pid=ImgDetMain&o=7&rm=3', N'Sụn gà tẩm bột chiên giòn sốt nước mắm mặn ngọt', 95000, 'AVAILABLE'),
('P026', 2, N'Bạch Tuộc Nướng Sa Tế', 'https://cdn2.fptshop.com.vn/unsafe/1920x0/filters:format(webp):quality(75)/2024_5_30_638526799624396210_cach-uop-bach-tuoc-nuong-sa-te-9.jpg', N'Bạch tuộc tươi ướp sa tế cay nướng than hoa', 130000, 'AVAILABLE'),
('P027', 2, N'Ếch Núp Lùm', 'https://beptruong.edu.vn/wp-content/uploads/2024/05/cach-lam-mon-ech-nup-lum.jpg', N'Ếch chiên bơ tỏi phủ sả chiên giòn rụm', 110000, 'AVAILABLE'),
('P028', 2, N'Bò Lúc Lắc Khoai Tây', 'https://th.bing.com/th/id/R.5610ed6ab5f56baeee55ea286356a43c?rik=g5OtsYFDBPRJAw&pid=ImgRaw&r=0', N'Thịt bò thăn mềm xào lúc lắc kèm khoai tây chiên', 135000, 'AVAILABLE'),
('P029', 2, N'Tôm Sú Nướng Muối Ớt', 'https://tse4.mm.bing.net/th/id/OIP.0TMi_3N2T961qWjiq6JDhAHaHa?r=0&rs=1&pid=ImgDetMain&o=7&rm=3', N'Tôm sú tươi nướng muối ớt cay xé lưỡi', 150000, 'AVAILABLE'),
('P030', 2, N'Lòng Non Xào Dưa Cải', 'https://tse1.explicit.bing.net/th/id/OIP.HyNxTHc3UmFl4e9tyKGmRwHaFP?r=0&rs=1&pid=ImgDetMain&o=7&rm=3', N'Lòng heo làm sạch xào cùng dưa cải chua giòn', 85000, 'AVAILABLE'),
('P031', 2, N'Dê Tái Chanh', 'https://tse2.mm.bing.net/th/id/OIP.BidOwbSBAFj4Bgglfsk3lAHaHa?r=0&rs=1&pid=ImgDetMain&o=7&rm=3', N'Thịt dê bóp chanh tái mềm, ăn kèm lá sung và tương bần', 145000, 'AVAILABLE'),
('P032', 2, N'Mực Trứng Hấp Gừng', 'https://yummyday.vn/uploads/images/muc-trung-hap-gung.jpg', N'Mực trứng hấp hành gừng thơm nức mũi', 140000, 'AVAILABLE'),
('P033', 2, N'Lẩu Gà Lá É', 'https://tse2.mm.bing.net/th/id/OIP.Bm9XodJwUNiJWM6we_lQXwHaHa?r=0&rs=1&pid=ImgDetMain&o=7&rm=3', N'Lẩu gà ta nấu cùng măng chua và lá é đặc sản', 250000, 'AVAILABLE'),
('P034', 2, N'Lẩu Bò Nhúng Dấm', 'https://tse4.mm.bing.net/th/id/OIP.wiCBeSr8InX6QVC-6kLNsQHaFn?r=0&rs=1&pid=ImgDetMain&o=7&rm=3', N'Bò ba chỉ nhúng nước lẩu chua thanh, cuốn bánh tráng', 280000, 'AVAILABLE'),

-- Đồ Uống 
('P035', 3, N'Bia Tiger Bạc', 'https://winecity.vn/wp-content/uploads/2023/09/bia-tiger-bac-uong-lanh-rat-ngon.jpg', N'Bia Tiger Crystal chai 330ml ướp lạnh sâu', 22000, 'AVAILABLE'),
('P036', 3, N'Bia Sài Gòn Special', 'https://tse1.mm.bing.net/th/id/OIP.5cjzX4aajxpZb3GNo1oNpAHaFy?r=0&rs=1&pid=ImgDetMain&o=7&rm=3', N'Bia Sài Gòn lùn chai xanh 330ml', 20000, 'AVAILABLE'),
('P037', 3, N'Rượu Táo Mèo', 'https://tse4.mm.bing.net/th/id/OIP.Yh9ISbYZbgmdL6E8obVrBwHaJ4?r=0&rs=1&pid=ImgDetMain&o=7&rm=3', N'Rượu ngâm táo mèo Tây Bắc (1 xị)', 45000, 'AVAILABLE'),
('P038', 3, N'Nước Suối Aquafina', 'https://file.hstatic.net/1000394081/file/inbound6927470786154414157_e5cd453ed0404eee98c0034e387822c7.jpg', N'Nước tinh khiết chai 500ml', 15000, 'AVAILABLE'),

-- Tráng Miệng 
('P039', 4, N'Sữa Chua Nếp Cẩm', 'https://sgmilk.com/wp-content/uploads/2025/09/so-sanh-sua-chua-nep-cam-cung-cac-san-pham-an-vat-khac.png', N'Sữa chua nhà làm kết hợp nếp cẩm dẻo ngọt, êm bụng sau khi nhậu', 35000, 'AVAILABLE'),
('P040', 4, N'Rau Câu Trái Dừa', 'https://tse1.explicit.bing.net/th/id/OIP.xntSkcm8t20IneBIYVaYTwHaHa?r=0&rs=1&pid=ImgDetMain&o=7&rm=3', N'Rau câu thanh mát đổ nguyên trái dừa tươi, giải nhiệt cực đã', 45000, 'AVAILABLE');
GO

INSERT INTO [Orders] ([ID], [Table_ID], [Employee_ID], [StartTime], [TotalAmount], [Note], [Status]) VALUES
('ORD001', 2, 'EMP02', '2023-10-25 18:30:00', 480000, N'Khách nước ngoài', 'PAID'),
('ORD002', 5, 'EMP02', '2023-10-25 19:15:00', 120000, N'Xin thêm ớt', 'UNPAID'),
('ORD003', 1, 'EMP02', '2023-10-25 20:00:00', 350000, NULL, 'PAID');
GO

INSERT INTO [Order_Details] ([OrderDetailID], [Order_ID], [Product_ID], [Quantity], [UnitPrice], [CreatedTime], [Note], [Status]) VALUES
-- Chi tiết hóa đơn ORD001 (Tổng 480,000)
('OD001', 'ORD001', 'P034', 1, 280000, '2023-10-25 18:32:00', N'Bò nhúng dấm', 'SERVED'),
('OD002', 'ORD001', 'P025', 1, 95000, '2023-10-25 18:32:00', NULL, 'SERVED'),
('OD003', 'ORD001', 'P023', 1, 75000, '2023-10-25 18:35:00', N'Ít cay', 'SERVED'),
('OD007', 'ORD001', 'P021', 1, 30000, '2023-10-25 18:35:00', NULL, 'SERVED'),

-- Chi tiết hóa đơn ORD002 (Tổng 120,000)
('OD004', 'ORD002', 'P024', 1, 120000, '2023-10-25 19:16:00', N'Thêm hành chẻ', 'PREPARING'),

-- Chi tiết hóa đơn ORD003 (Tổng 350,000)
('OD005', 'ORD003', 'P025', 1, 95000, '2023-10-25 20:05:00', NULL, 'SERVED'),
('OD006', 'ORD003', 'P026', 1, 130000, '2023-10-25 20:05:00', N'Ít cay', 'SERVED'),
('OD008', 'ORD003', 'P027', 1, 110000, '2023-10-25 20:05:00', NULL, 'SERVED'),
('OD009', 'ORD003', 'P038', 1, 15000, '2023-10-25 20:05:00', NULL, 'SERVED');
GO

INSERT INTO [Payments] ([PaymentID], [Order_ID], [PaymentMethod], [Amount], [PayTime], [Status]) VALUES
('PAY001', 'ORD001', 'Credit Card', 480000, '2023-10-25 19:30:00', 'COMPLETED'),
('PAY002', 'ORD003', 'Cash', 350000, '2023-10-25 21:00:00', 'COMPLETED');
GO


update [Employees]
set [FullName] = N'Nguyễn Quốc Việt'
where [id] = 'EMP01'