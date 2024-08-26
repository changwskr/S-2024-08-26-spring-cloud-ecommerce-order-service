package com.example.orderservice.business.as;

import com.example.orderservice.transfer.dto.OrderDto;
import com.example.orderservice.business.dc.dao.model.OrderEntity;

public interface OrderServiceAS {
    OrderDto createOrder(OrderDto orderDetails);
    OrderDto getOrderByOrderId(String orderId);
    Iterable<OrderEntity> getOrdersByUserId(String userId);
    OrderDto createOrder2(OrderDto orderDetails);
    OrderDto getOrderByOrderId2(String orderId);
    Iterable<OrderEntity> getOrdersByUserId2(String userId);

}
