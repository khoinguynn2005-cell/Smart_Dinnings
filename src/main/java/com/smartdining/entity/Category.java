package com.smartdining.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Categories")
public class Category {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", length = 50)
    private Integer id;

    @Column(name = "Name", length = 100)
    private String name;

	public Integer getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public void setId(final Integer id) {
		this.id = id;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Category() {
	}

	public Category(final Integer id, final String name) {
		this.id = id;
		this.name = name;
	}
}