package com.smartdining.entity;

import com.smartdining.enums.EmployeeRole;
import com.smartdining.enums.EmployeeStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Employees")
public class Employee {

    @Id
    @Column(name = "ID", length = 50)
    private String id;

    @Column(name = "Username", length = 50, unique = true)
    private String username;

    @Column(name = "Password", length = 255)
    private String password;

    @Column(name = "FullName", length = 100)
    private String fullName;

    @Column(name = "Phone", length = 15)
    private String phone;

    @Column(name = "Email", length = 100)
    private String email;

    @Column(name = "Role", length = 20)
    @Enumerated(EnumType.STRING)
    private EmployeeRole role;

    @Column(name = "Status", length = 20)
    @Enumerated(EnumType.STRING)
    private EmployeeStatus status;

    public String getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getFullName() {
        return this.fullName;
    }

    public String getPhone() {
        return this.phone;
    }

    public String getEmail() {
        return this.email;
    }

    public EmployeeRole getRole() {
        return this.role;
    }

    public EmployeeStatus getStatus() {
        return this.status;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setFullName(final String fullName) {
        this.fullName = fullName;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public void setRole(final EmployeeRole role) {
        this.role = role;
    }

    public void setStatus(final EmployeeStatus status) {
        this.status = status;
    }

    public Employee() {
    }

    public Employee(final String id, final String username, final String password, final String fullName, final String phone, final String email, final EmployeeRole role, final EmployeeStatus status) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.status = status;
    }
}