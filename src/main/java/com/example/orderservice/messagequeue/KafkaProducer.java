package com.example.orderservice.messagequeue;

import com.example.orderservice.transfer.dto.OrderDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * user-service 주문한다. http://127.0.0.1:8000/order-service/(user-id)/orders
 *      상품id, 수량, 단가
 * ---------------------------------------------------------------------------------
 *  * order-service 주문을 수신한다.
 *      createOrder.controller -----------------------------------------------------------------
 *            KafkaProducer는 주문이 들어오면
 * 	          (POST) http://127.0.0.1:8000/order-service/(user-id)/orders
 *            @PostMapping("/{userId}/orders")
 *            public ResponseEntity<ResponseOrder> createOrder(@PathVariable("userId") String userId, @RequestBody RequestOrder orderDetails)
 *            {
 *                주문한다.
 *                OrderDto createdOrder = orderService.createOrder2(orderDto);
 *                주문이 성공하면 example-catalog-topic에 메시지를 publisher 한다.
 *                □□□□□□□□□□□ example-catalog-topic publisher □□□□□□□□□
 *                KafkaProducer kafkaProducer; 상품id, 수량, 단가
 *                kafkaProducer.send("example-catalog-topic", orderDto);
 *            }
 * ---------------------------------------------------------------------------------
 * catalog-service
 *      example-catalog-topic에 메시지를 subsciber 한다.
 *      public class KafkaConsumer {
 * 	        // 여기서 생산자에서 생산한 카탈로그에 대한 갯수를 수정해야 되어서 repository를 선언한다.
 *          ICatalogRepositoryDAO repository;
 *
 *          □□□□□□□□□□□ example-catalog-topic subscribe □□□□□□□□□□□□□□□
 *          ★★★★★ 여기서 리스너로 example-catalog-topic 수신대기하고 있다. 상품id, 수량, 단가
 *          @KafkaListener(topics = "example-catalog-topic")
 *          public void updateQty(String kafkaMessage) throws Exception {
 *              상품id, 수량, 단가
 *              CatalogEntity entity = repository.findByProductId((String)map.get("productId"));
 *              주문된 수량을 원 catalog 테이블에 있는 수량에서 차감하고 저장한다.
 *              entity.setStock(entity.getStock() - (Integer)map.get("qty"));
 *              repository.save(entity);
 *          }
 *      }
 */
@Service
@Slf4j
public class KafkaProducer {
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // 실제 메시지를 보내는 역할수행
    // topic ==
    public OrderDto send(String topic, OrderDto orderDto) {
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = "";

        log.info("★ KafkaProducer.send()----------------------start-topic:" + topic);
        
        try {
        	// 오브젝트를 JSON으로 변환한다.
            jsonInString = mapper.writeValueAsString(orderDto);
        } catch(JsonProcessingException ex) {
            ex.printStackTrace();
        }
        
        // JSON 스트링으로 보낸다
        kafkaTemplate.send(topic, jsonInString);

        log.info("★ KafkaProducer.send()      --DTO: " + orderDto);
        log.info("★ KafkaProducer.send()----------------------end-topic:" + topic);

        return orderDto;
    }
}
