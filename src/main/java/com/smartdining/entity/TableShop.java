package com.smartdining.entity;

import com.smartdining.enums.TableStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "[Tables]")
public class TableShop {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "TableName", length = 50)
    private String tableName;

    @Column(name = "QRCodeURL", length = 255, unique = true)
    private String qrCodeUrl;

    @Column(name = "Status", length = 20)
    @Enumerated(EnumType.STRING)
    private TableStatus status;

	public Integer getId() {
		return this.id;
	}

	public String getTableName() {
		return this.tableName;
	}

	public String getQrCodeUrl() {
		return this.qrCodeUrl;
	}

	public TableStatus getStatus() {
		return this.status;
	}

	public void setId(final Integer id) {
		this.id = id;
	}

	public void setTableName(final String tableName) {
		this.tableName = tableName;
	}

	public void setQrCodeUrl(final String qrCodeUrl) {
		this.qrCodeUrl = qrCodeUrl;
	}

	public void setStatus(final TableStatus status) {
		this.status = status;
	}

	public TableShop() {
	}

	public TableShop(final Integer id, final String tableName, final String qrCodeUrl, final TableStatus status) {
		this.id = id;
		this.tableName = tableName;
		this.qrCodeUrl = qrCodeUrl;
		this.status = status;
	}
}