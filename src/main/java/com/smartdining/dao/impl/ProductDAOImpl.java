package com.smartdining.dao.impl;

import com.smartdining.dao.ProductDAO;
import com.smartdining.entity.Product;
import com.smartdining.utils.GenericDAOImpl;

public class ProductDAOImpl extends GenericDAOImpl<Product> implements ProductDAO {

    public ProductDAOImpl() {
        super(Product.class);
    }

}