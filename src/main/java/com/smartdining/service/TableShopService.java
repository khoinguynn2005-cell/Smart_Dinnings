package com.smartdining.service;

import com.smartdining.entity.TableShop;
import com.smartdining.enums.TableStatus;

public interface TableShopService {
    TableShop findById(Integer id);
    TableShop findByQrCodeUrl(String qrCodeUrl);
    TableShop updateTableStatus(Integer tableId, TableStatus newStatus);
    boolean isTableAvailable(Integer tableId);
}
