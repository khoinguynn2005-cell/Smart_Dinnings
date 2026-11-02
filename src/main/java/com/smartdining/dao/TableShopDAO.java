package com.smartdining.dao;

import com.smartdining.entity.TableShop;
import com.smartdining.utils.GenericDAO;

public interface TableShopDAO extends GenericDAO<TableShop> {
    TableShop findByQrCodeUrl(String qrCodeUrl);

}
