package com.smartdining.dao.impl;

import com.smartdining.dao.PaymentDAO;
import com.smartdining.entity.Payment;
import com.smartdining.utils.GenericDAOImpl;

public class PaymentDAOImpl extends GenericDAOImpl<Payment> implements PaymentDAO {

    public PaymentDAOImpl() {
        super(Payment.class);
    }

}