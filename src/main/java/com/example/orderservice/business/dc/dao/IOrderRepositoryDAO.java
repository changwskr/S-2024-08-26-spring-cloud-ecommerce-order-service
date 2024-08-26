package com.example.orderservice.business.dc.dao;

import com.example.orderservice.business.dc.dao.model.OrderEntity;
import org.springframework.data.repository.CrudRepository;

public interface IOrderRepositoryDAO extends CrudRepository<OrderEntity, Long> {
    OrderEntity findByOrderId(String orderId);
    Iterable<OrderEntity> findByUserId(String userId);
}
