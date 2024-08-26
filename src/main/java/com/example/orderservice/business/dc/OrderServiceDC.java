package com.example.orderservice.business.dc;

import com.example.orderservice.business.as.OrderServiceAS;
import com.example.orderservice.business.dc.dao.IOrderRepositoryDAO;
import com.example.orderservice.business.dc.dao.model.OrderEntity;
import com.example.orderservice.transfer.dto.OrderDto;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Component
public class OrderServiceDC {
    IOrderRepositoryDAO orderRepository;

    public OrderServiceDC(IOrderRepositoryDAO orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderDto createOrder(OrderDto orderDto) {

        orderDto.setOrderId(UUID.randomUUID().toString());
        orderDto.setTotalPrice(orderDto.getQty() * orderDto.getUnitPrice());

        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        OrderEntity orderEntity = mapper.map(orderDto, OrderEntity.class);

        /*********************************************************************************
        MariaDB [mydb]> desc orders
        +-------------+-------------+------+-----+---------------------+----------------+
        | Field       | Type        | Null | Key | Default             | Extra          |
        +-------------+-------------+------+-----+---------------------+----------------+
        | id          | int(11)     | NO   | PRI | NULL                | auto_increment |
        | product_id  | varchar(20) | NO   |     | NULL                |                |
        | qty         | int(11)     | YES  |     | 0                   |                |
        | unit_price  | int(11)     | YES  |     | 0                   |                |
        | total_price | int(11)     | YES  |     | 0                   |                |
        | user_id     | varchar(50) | NO   |     | NULL                |                |
        | order_id    | varchar(50) | NO   |     | NULL                |                |
        | created_at  | datetime    | YES  |     | current_timestamp() |                |
        +-------------+-------------+------+-----+---------------------+----------------+
         상품id, 수량, 단가, 토탈금액, 유저id, 생성일자일시
        *********************************************************************************/

        orderRepository.save(orderEntity);

        OrderDto returnValue = mapper.map(orderEntity, OrderDto.class);

        return returnValue;
    }

    public OrderDto getOrderByOrderId(String orderId) {
        OrderEntity orderEntity = orderRepository.findByOrderId(orderId);
        OrderDto orderDto = new ModelMapper().map(orderEntity, OrderDto.class);

        return orderDto;
    }

    public Iterable<OrderEntity> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }
}
