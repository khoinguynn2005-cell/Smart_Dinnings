package com.smartdining.service.impl;

import com.smartdining.dao.TableShopDAO;
import com.smartdining.dao.impl.TableShopDAOImpl;
import com.smartdining.entity.TableShop;
import com.smartdining.enums.TableStatus;
import com.smartdining.service.TableShopService;

public class TableShopServiceImpl implements TableShopService {
    private final TableShopDAO tableShopDAO = new TableShopDAOImpl();

    @Override
    public TableShop findById(Integer id) {
        return tableShopDAO.findById(id);
    }

    @Override
    public TableShop findByQrCodeUrl(String qrCodeUrl) {
        if (qrCodeUrl == null || qrCodeUrl.isBlank()) {
            return null;
        }
        return tableShopDAO.findByQrCodeUrl(qrCodeUrl);
    }

    @Override
    public TableShop updateTableStatus(Integer tableId, TableStatus newStatus) {
        TableShop table = tableShopDAO.findById(tableId);
        if (table == null) {
            return null;
        }
        table.setStatus(newStatus);
        return tableShopDAO.update(table);
    }

    @Override
    public boolean isTableAvailable(Integer tableId) {
        TableShop table = tableShopDAO.findById(tableId);
        return table != null && table.getStatus() == TableStatus.AVAILABLE;
    }
}
