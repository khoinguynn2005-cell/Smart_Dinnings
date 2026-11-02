package com.smartdining.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

import com.smartdining.enums.ProductStatus;


@Entity
@Table(name = "Products")
public class Product {

    @Id
    @Column(name = "ID", length = 50)
    private String id;

    @ManyToOne
    @JoinColumn(name = "Category_ID")
    private Category category;

    @Column(name = "Name", length = 150)
    private String name;

    @Lob
    @Column(name = "ImageURL")
    private String imageUrl;

    @Column(name = "Description", length = 500)
    private String description;

    @Column(name = "Price")
    private BigDecimal price;

    @Column(name = "Status", length = 20)
    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    public String getId() {
        return this.id;
    }

    public Category getCategory() {
        return this.category;
    }

    public String getName() {
        return this.name;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public String getDescription() {
        return this.description;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public ProductStatus getStatus() {
        return this.status;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setCategory(final Category category) {
        this.category = category;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setImageUrl(final String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setPrice(final BigDecimal price) {
        this.price = price;
    }

    public void setStatus(final ProductStatus status) {
        this.status = status;
    }

    public Product() {
    }

    public Product(final String id, final Category category, final String name, final String imageUrl, final String description, final BigDecimal price, final ProductStatus status) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.imageUrl = imageUrl;
        this.description = description;
        this.price = price;
        this.status = status;
    }
}