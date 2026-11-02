USE [Smart_Dinings];
GO
-- BUG-5: ma QR cua ban truoc day la /home/index?tableId=<so ban>, ai cung doan duoc de goi mon cho ban khac.
-- Doi sang ma ngau nhien. Chi cap nhat cac ban con dang dung dang cu, nen chay lai an toan.
-- SAU KHI CHAY: in lai ma QR cho cac ban (ma cu khong con hop le).
SET QUOTED_IDENTIFIER ON;

UPDATE [Tables]
SET [QRCodeURL] = REPLACE(CAST(NEWID() AS VARCHAR(36)) + CAST(NEWID() AS VARCHAR(36)), '-', '')
WHERE [QRCodeURL] IS NULL OR [QRCodeURL] LIKE '/home/index?tableId=%';
GO
