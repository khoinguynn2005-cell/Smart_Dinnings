package com.smartdining.service;

import java.util.List;
import com.smartdining.entity.OrderDetail;
import com.smartdining.enums.OrderDetailStatus;

public interface KitchenService {
    
    // Lấy danh sách các món ăn đang chờ (PENDING) hoặc đang nấu (PREPARING)
    List<OrderDetail> getActiveTasks();
    
    // Cập nhật trạng thái của một món ăn
    boolean updateTaskStatus(String orderDetailId, OrderDetailStatus newStatus, String employeeId);
    
    // Bếp báo Hết nguyên liệu / Hủy món (sẽ trừ tiền vào tổng hóa đơn)
    boolean cancelTask(String orderDetailId, String employeeId);
}
