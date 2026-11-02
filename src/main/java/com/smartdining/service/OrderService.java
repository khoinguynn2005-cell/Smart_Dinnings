package com.smartdining.service;

import java.util.List;
import com.smartdining.entity.Order;
import com.smartdining.entity.OrderDetail;

public interface OrderService {
	public boolean create(Order order);
	
	public Order getActiveOrderByTable(Integer tableId);
	
	public boolean addItemToCart(Integer tableId, String productId, int quantity, String note);

	public boolean removeCartItem(Integer tableId, String orderDetailId);

	/** Doi so luong mot mon chua gui bep (1..100). */
	public boolean updateCartItemQuantity(Integer tableId, String orderDetailId, int quantity);
	
	public boolean confirmOrder(Integer tableId);
	
	public List<OrderDetail> getOrderDetailsByTable(Integer tableId);
	
	public boolean requestPayment(Integer tableId);

	public boolean requestService(Integer tableId, String serviceType, String note);
}
