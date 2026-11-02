package com.smartdining.dao.impl;

import java.util.Collections;
import java.util.List;
import jakarta.persistence.EntityManager;
import com.smartdining.dao.OrderDetailDAO;
import com.smartdining.entity.OrderDetail;
import com.smartdining.enums.OrderDetailStatus;
import com.smartdining.enums.OrderStatus;
import com.smartdining.enums.OrderType;
import com.smartdining.utils.GenericDAOImpl;
import com.smartdining.utils.JpaUtil;

public class OrderDetailDAOImpl extends GenericDAOImpl<OrderDetail> implements OrderDetailDAO {

    public OrderDetailDAOImpl() {
        super(OrderDetail.class);
    }

    @Override
    public List<OrderDetail> getActiveKitchenTasks() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            String jpql = "SELECT od FROM OrderDetail od JOIN od.order o "
                    + "WHERE od.status IN (:pending, :preparing) AND (o.status = :orderStatus OR o.orderType = :takeaway) "
                    + "ORDER BY od.createdTime ASC";
            return em.createQuery(jpql, OrderDetail.class)
                     .setParameter("pending", OrderDetailStatus.PENDING)
                     .setParameter("preparing", OrderDetailStatus.PREPARING)
                     .setParameter("orderStatus", OrderStatus.UNPAID)
                     .setParameter("takeaway", OrderType.TAKEAWAY)
                     .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }
}
