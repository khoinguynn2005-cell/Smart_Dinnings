package com.smartdining.dao;

import java.util.List;
import com.smartdining.entity.OrderDetail;
import com.smartdining.utils.GenericDAO;

public interface OrderDetailDAO extends GenericDAO<OrderDetail> {
    List<OrderDetail> getActiveKitchenTasks();
}