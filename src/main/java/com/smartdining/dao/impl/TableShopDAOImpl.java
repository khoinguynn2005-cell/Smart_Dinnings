package com.smartdining.dao.impl;

import java.util.List;

import com.smartdining.dao.TableShopDAO;
import com.smartdining.entity.TableShop;
import com.smartdining.utils.GenericDAOImpl;
import com.smartdining.utils.JpaUtil;

import jakarta.persistence.EntityManager;

public class TableShopDAOImpl extends GenericDAOImpl<TableShop> implements TableShopDAO {
    public TableShopDAOImpl() {
        super(TableShop.class);
    }

    @Override
    public TableShop findByQrCodeUrl(String qrCodeUrl) {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            List<TableShop> tables = em.createQuery(
                    "SELECT t FROM TableShop t WHERE t.qrCodeUrl = :qrCodeUrl", TableShop.class)
                    .setParameter("qrCodeUrl", qrCodeUrl)
                    .setMaxResults(1)
                    .getResultList();
            return tables.isEmpty() ? null : tables.get(0);
        } finally {
            em.close();
        }
    }
}
