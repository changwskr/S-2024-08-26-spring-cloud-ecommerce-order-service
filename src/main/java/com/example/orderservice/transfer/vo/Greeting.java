package com.example.orderservice.transfer.vo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
//@AllArgsConstructor
//@NoArgsConstructor
public class Greeting {
    @Value("${greeting.message}")
    private String message;
}
