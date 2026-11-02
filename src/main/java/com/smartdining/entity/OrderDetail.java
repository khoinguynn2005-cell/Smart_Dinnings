package com.smartdining.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.smartdining.enums.OrderDetailStatus;


@Entity
@Table(name = "Order_Details")
public class OrderDetail {

    @Id
    @Column(name = "OrderDetailID", length = 50)
    private String orderDetailId; 

    @ManyToOne
    @JoinColumn(name = "Order_ID")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "Product_ID")
    private Product product;

    @Column(name = "Quantity")
    private Integer quantity;

    @Column(name = "UnitPrice")
    private BigDecimal unitPrice;

    @Column(name = "CreatedTime")
    private LocalDateTime createdTime;

    @Column(name = "Note", length = 255)
    private String note;

    @Column(name = "Status", length = 20)
    @Enumerated(EnumType.STRING)
    private OrderDetailStatus status;

    // Theo doi hoat dong nhan vien (bao cao): ai nau, nau luc nao; ai mang ra, luc nao
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PreparedBy")
    private Employee preparedBy;

    @Column(name = "StartedTime")
    private LocalDateTime startedTime;

    @Column(name = "ReadyTime")
    private LocalDateTime readyTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ServedBy")
    private Employee servedBy;

    @Column(name = "ServedTime")
    private LocalDateTime servedTime;

    public String getOrderDetailId() {
        return this.orderDetailId;
    }

    public Order getOrder() {
        return this.order;
    }

    public Product getProduct() {
        return this.product;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public BigDecimal getUnitPrice() {
        return this.unitPrice;
    }

    public LocalDateTime getCreatedTime() {
        return this.createdTime;
    }

    public String getNote() {
        return this.note;
    }

    public OrderDetailStatus getStatus() {
        return this.status;
    }

    public Employee getPreparedBy() {
        return this.preparedBy;
    }

    public LocalDateTime getStartedTime() {
        return this.startedTime;
    }

    public LocalDateTime getReadyTime() {
        return this.readyTime;
    }

    public Employee getServedBy() {
        return this.servedBy;
    }

    public LocalDateTime getServedTime() {
        return this.servedTime;
    }

    public void setOrderDetailId(final String orderDetailId) {
        this.orderDetailId = orderDetailId;
    }

    public void setOrder(final Order order) {
        this.order = order;
    }

    public void setProduct(final Product product) {
        this.product = product;
    }

    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    public void setUnitPrice(final BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setCreatedTime(final LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    public void setStatus(final OrderDetailStatus status) {
        this.status = status;
    }

    public void setPreparedBy(final Employee preparedBy) {
        this.preparedBy = preparedBy;
    }

    public void setStartedTime(final LocalDateTime startedTime) {
        this.startedTime = startedTime;
    }

    public void setReadyTime(final LocalDateTime readyTime) {
        this.readyTime = readyTime;
    }

    public void setServedBy(final Employee servedBy) {
        this.servedBy = servedBy;
    }

    public void setServedTime(final LocalDateTime servedTime) {
        this.servedTime = servedTime;
    }

    public OrderDetail() {
    }

    public OrderDetail(final String orderDetailId, final Order order, final Product product, final Integer quantity, final BigDecimal unitPrice, final LocalDateTime createdTime, final String note, final OrderDetailStatus status, final Employee preparedBy, final LocalDateTime startedTime, final LocalDateTime readyTime, final Employee servedBy, final LocalDateTime servedTime) {
        this.orderDetailId = orderDetailId;
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.createdTime = createdTime;
        this.note = note;
        this.status = status;
        this.preparedBy = preparedBy;
        this.startedTime = startedTime;
        this.readyTime = readyTime;
        this.servedBy = servedBy;
        this.servedTime = servedTime;
    }
}