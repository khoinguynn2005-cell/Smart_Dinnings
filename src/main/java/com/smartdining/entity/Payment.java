package com.smartdining.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.smartdining.enums.PaymentStatus;


@Entity
@Table(name = "Payments")
public class Payment {

    @Id
    @Column(name = "PaymentID", length = 50)
    private String paymentId;

    @ManyToOne
    @JoinColumn(name = "Order_ID")
    private Order order;

    @Column(name = "PaymentMethod", length = 50)
    private String paymentMethod;

    @Column(name = "Amount")
    private BigDecimal amount;

    @Column(name = "PayTime")
    private LocalDateTime payTime;

    @Column(name = "Status", length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    /** Anh chup bill chuyen khoan (data URL base64), null khi tra tien mat. */
    @Column(name = "Image", columnDefinition = "VARCHAR(MAX)")
    private String image;

    public String getPaymentId() {
        return this.paymentId;
    }

    public Order getOrder() {
        return this.order;
    }

    public String getPaymentMethod() {
        return this.paymentMethod;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public LocalDateTime getPayTime() {
        return this.payTime;
    }

    public PaymentStatus getStatus() {
        return this.status;
    }

    public String getImage() {
        return this.image;
    }

    public void setPaymentId(final String paymentId) {
        this.paymentId = paymentId;
    }

    public void setOrder(final Order order) {
        this.order = order;
    }

    public void setPaymentMethod(final String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    public void setPayTime(final LocalDateTime payTime) {
        this.payTime = payTime;
    }

    public void setStatus(final PaymentStatus status) {
        this.status = status;
    }

    public void setImage(final String image) {
        this.image = image;
    }

    public Payment() {
    }

    public Payment(final String paymentId, final Order order, final String paymentMethod, final BigDecimal amount, final LocalDateTime payTime, final PaymentStatus status, final String image) {
        this.paymentId = paymentId;
        this.order = order;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.payTime = payTime;
        this.status = status;
        this.image = image;
    }
}