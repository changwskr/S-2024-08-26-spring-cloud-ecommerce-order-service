package com.example.orderservice.business.as;

import com.example.orderservice.business.dc.OrderServiceDC;
import com.example.orderservice.transfer.dto.OrderDto;
import com.example.orderservice.business.dc.dao.model.OrderEntity;
import com.example.orderservice.business.dc.dao.IOrderRepositoryDAO;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderServiceASImpl implements OrderServiceAS {
    IOrderRepositoryDAO orderRepository;
    OrderServiceDC orderServiceDC;

    @Autowired
    public OrderServiceASImpl(IOrderRepositoryDAO orderRepository, OrderServiceDC orderServiceDC) {
        this.orderRepository = orderRepository;
        this.orderServiceDC = orderServiceDC;
    }

    @Override
    public OrderDto createOrder(OrderDto orderDto) {
        orderDto.setOrderId(UUID.randomUUID().toString());
        orderDto.setTotalPrice(orderDto.getQty() * orderDto.getUnitPrice());

        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        OrderEntity orderEntity = mapper.map(orderDto, OrderEntity.class);

        orderRepository.save(orderEntity);

        OrderDto returnValue = mapper.map(orderEntity, OrderDto.class);

        return returnValue;
    }

    @Override
    public OrderDto getOrderByOrderId(String orderId) {
        OrderEntity orderEntity = orderRepository.findByOrderId(orderId);
        OrderDto orderDto = new ModelMapper().map(orderEntity, OrderDto.class);

        return orderDto;
    }

    @Override
    public Iterable<OrderEntity> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }


    //----------------------------------------------------------------------------------------------------------
    // 신규 적용되는 Domain Component를 통해서 실시한다.
    //----------------------------------------------------------------------------------------------------------
    @Override
    public OrderDto createOrder2(OrderDto orderDto) {

        // 한건의 주문을 주문 테이블에 저장한다.
        OrderDto returnValue = orderServiceDC.createOrder(orderDto);

        /*-------------------------------------------------------------------------------------------------------
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
         ----------------------------------------------------------------------------------------------------------*/



        return returnValue;
    }

    @Override
    public OrderDto getOrderByOrderId2(String orderId) {
        return orderServiceDC.getOrderByOrderId(orderId);
    }

    @Override
    public Iterable<OrderEntity> getOrdersByUserId2(String userId) {
        return orderServiceDC.getOrdersByUserId(userId);
    }




}
