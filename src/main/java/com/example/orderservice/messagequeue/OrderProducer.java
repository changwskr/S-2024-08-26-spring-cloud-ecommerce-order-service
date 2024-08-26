package com.example.orderservice.messagequeue;

import com.example.orderservice.transfer.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class OrderProducer {
    private KafkaTemplate<String, String> kafkaTemplate;

    List<Field> fields = Arrays.asList(new Field("string", true, "orderId"),
            new Field("string", true, "userId"),
            new Field("string", true, "productId"),
            new Field("int32", true, "qty"),
            new Field("int32", true, "unitPrice"),
            new Field("int32", true, "totalPrice"));
    
    Schema schema = Schema.builder()
            .type("struct")
            .fields(fields)
            .optional(false)
            .name("orders")
            .build();

    @Autowired
    public OrderProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public OrderDto send(String topic, OrderDto orderDto) {
        Payload payload = Payload.builder()
                .orderId(orderDto.getOrderId())
                .userId(orderDto.getUserId())
                .productId(orderDto.getProductId())
                .qty(orderDto.getQty())
                .unitPrice(orderDto.getUnitPrice())
                .totalPrice(orderDto.getTotalPrice())
                .build();

        log.info("★ OrderProducer.send()-------------------------------start-topic:" + topic);

        KafkaOrderDto kafkaOrderDto = new KafkaOrderDto(schema, payload);

        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = "";
        try {
            jsonInString = mapper.writeValueAsString(kafkaOrderDto);
        } catch(JsonProcessingException ex) {
            ex.printStackTrace();
        }

        log.info("  OrderProducer.send  >topic> " + topic + " > jsonInString >: " + jsonInString);
        kafkaTemplate.send(topic, jsonInString);

        log.info("★ OrderProducer.send()--           DTO :" + kafkaOrderDto);

        log.info("★ OrderProducer.send()------------------------------end-topic:" + topic);

        return orderDto;
    }
}
