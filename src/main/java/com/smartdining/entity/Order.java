package com.smartdining.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.smartdining.enums.OrderStatus;
import com.smartdining.enums.OrderType;


@Entity
@Table(name = "Orders")
public class Order {

    @Id
    @Column(name = "ID", length = 50)
    private String id; 

    @ManyToOne
    @JoinColumn(name = "Table_ID")
    private TableShop table;

    @ManyToOne
    @JoinColumn(name = "Employee_ID", nullable = true)
    private Employee employee;

    @Column(name = "StartTime")
    private LocalDateTime startTime;

    @Column(name = "TotalAmount")
    private BigDecimal totalAmount;

    @Column(name = "Note", length = 255)
    private String note;

    @Column(name = "Status", length = 20)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    /** DINE_IN: an tai ban. TAKEAWAY: mang ve, khong gan voi ban (table = null). */
    @Column(name = "OrderType", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderType orderType = OrderType.DINE_IN;

    @Column(name = "CustomerName", length = 100)
    private String customerName;

    @Column(name = "CustomerPhone", length = 15)
    private String customerPhone;

    /** Ma lay do cua don mang ve (#1, #2... dem lai moi ngay). */
    @Column(name = "PickupCode")
    private Integer pickupCode;

    @Column(name = "PickupTime")
    private LocalDateTime pickupTime;

    /** Ten hien thi: ten ban, hoac "Mang ve #12" voi don mang ve. */
    public String getDisplayName() {
        if (table != null) {
            return table.getTableName();
        }
        return "Mang về #" + (pickupCode == null ? "?" : pickupCode);
    }

    public boolean isTakeaway() {
        return orderType == OrderType.TAKEAWAY;
    }

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails;

    public String getId() {
        return this.id;
    }

    public TableShop getTable() {
        return this.table;
    }

    public Employee getEmployee() {
        return this.employee;
    }

    public LocalDateTime getStartTime() {
        return this.startTime;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public String getNote() {
        return this.note;
    }

    public OrderStatus getStatus() {
        return this.status;
    }

    public OrderType getOrderType() {
        return this.orderType;
    }

    public String getCustomerName() {
        return this.customerName;
    }

    public String getCustomerPhone() {
        return this.customerPhone;
    }

    public Integer getPickupCode() {
        return this.pickupCode;
    }

    public LocalDateTime getPickupTime() {
        return this.pickupTime;
    }

    public List<OrderDetail> getOrderDetails() {
        return this.orderDetails;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setTable(final TableShop table) {
        this.table = table;
    }

    public void setEmployee(final Employee employee) {
        this.employee = employee;
    }

    public void setStartTime(final LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    public void setStatus(final OrderStatus status) {
        this.status = status;
    }

    public void setOrderType(final OrderType orderType) {
        this.orderType = orderType;
    }

    public void setCustomerName(final String customerName) {
        this.customerName = customerName;
    }

    public void setCustomerPhone(final String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public void setPickupCode(final Integer pickupCode) {
        this.pickupCode = pickupCode;
    }

    public void setPickupTime(final LocalDateTime pickupTime) {
        this.pickupTime = pickupTime;
    }

    public void setOrderDetails(final List<OrderDetail> orderDetails) {
        this.orderDetails = orderDetails;
    }

    public Order() {
    }

    public Order(final String id, final TableShop table, final Employee employee, final LocalDateTime startTime, final BigDecimal totalAmount, final String note, final OrderStatus status, final OrderType orderType, final String customerName, final String customerPhone, final Integer pickupCode, final LocalDateTime pickupTime, final List<OrderDetail> orderDetails) {
        this.id = id;
        this.table = table;
        this.employee = employee;
        this.startTime = startTime;
        this.totalAmount = totalAmount;
        this.note = note;
        this.status = status;
        this.orderType = orderType;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.pickupCode = pickupCode;
        this.pickupTime = pickupTime;
        this.orderDetails = orderDetails;
    }
}