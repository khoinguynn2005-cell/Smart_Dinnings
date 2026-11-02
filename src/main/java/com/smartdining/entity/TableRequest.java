package com.smartdining.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.smartdining.enums.TableRequestStatus;
import com.smartdining.enums.TableRequestType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "Table_Requests")
public class TableRequest {
    @Id
    @Column(name = "ID", length = 50)
    private String id;

    @ManyToOne
    @JoinColumn(name = "Table_ID", nullable = false)
    private TableShop table;

    @Column(name = "RequestType", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private TableRequestType requestType;

    @Column(name = "ServiceType", length = 80)
    private String serviceType;

    @Column(name = "Note", length = 255)
    private String note;

    @Column(name = "TotalAmount", precision = 13, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "RequestedTime", nullable = false)
    private LocalDateTime requestedTime;

    @Column(name = "Status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private TableRequestStatus status;

    @Column(name = "CompletedTime")
    private LocalDateTime completedTime;

    @ManyToOne
    @JoinColumn(name = "CompletedBy")
    private Employee completedBy;

    public String getId() {
        return this.id;
    }

    public TableShop getTable() {
        return this.table;
    }

    public TableRequestType getRequestType() {
        return this.requestType;
    }

    public String getServiceType() {
        return this.serviceType;
    }

    public String getNote() {
        return this.note;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public LocalDateTime getRequestedTime() {
        return this.requestedTime;
    }

    public TableRequestStatus getStatus() {
        return this.status;
    }

    public LocalDateTime getCompletedTime() {
        return this.completedTime;
    }

    public Employee getCompletedBy() {
        return this.completedBy;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setTable(final TableShop table) {
        this.table = table;
    }

    public void setRequestType(final TableRequestType requestType) {
        this.requestType = requestType;
    }

    public void setServiceType(final String serviceType) {
        this.serviceType = serviceType;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setRequestedTime(final LocalDateTime requestedTime) {
        this.requestedTime = requestedTime;
    }

    public void setStatus(final TableRequestStatus status) {
        this.status = status;
    }

    public void setCompletedTime(final LocalDateTime completedTime) {
        this.completedTime = completedTime;
    }

    public void setCompletedBy(final Employee completedBy) {
        this.completedBy = completedBy;
    }

    public TableRequest() {
    }

    public TableRequest(final String id, final TableShop table, final TableRequestType requestType, final String serviceType, final String note, final BigDecimal totalAmount, final LocalDateTime requestedTime, final TableRequestStatus status, final LocalDateTime completedTime, final Employee completedBy) {
        this.id = id;
        this.table = table;
        this.requestType = requestType;
        this.serviceType = serviceType;
        this.note = note;
        this.totalAmount = totalAmount;
        this.requestedTime = requestedTime;
        this.status = status;
        this.completedTime = completedTime;
        this.completedBy = completedBy;
    }
}