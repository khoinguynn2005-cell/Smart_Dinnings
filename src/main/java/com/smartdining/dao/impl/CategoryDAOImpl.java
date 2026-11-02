package com.smartdining.dao.impl;

import com.smartdining.dao.CategoryDAO;
import com.smartdining.entity.Category;
import com.smartdining.utils.GenericDAOImpl;

public class CategoryDAOImpl extends GenericDAOImpl<Category> implements CategoryDAO {

    public CategoryDAOImpl() {
        super(Category.class);
    }

}