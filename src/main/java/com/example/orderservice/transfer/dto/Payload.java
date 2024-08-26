package com.example.orderservice.transfer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Payload {
    private String orderId;
    private String userId;
    private String productId;
    private int qty;
    private int unitPrice;
    private int totalPrice;
}
