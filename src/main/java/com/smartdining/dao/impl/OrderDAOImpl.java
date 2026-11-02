package com.smartdining.dao.impl;

import com.smartdining.dao.OrderDAO;
import com.smartdining.entity.Order;
import com.smartdining.utils.GenericDAOImpl;

public class OrderDAOImpl extends GenericDAOImpl<Order> implements OrderDAO {

    public OrderDAOImpl() {
        super(Order.class);
    }

}