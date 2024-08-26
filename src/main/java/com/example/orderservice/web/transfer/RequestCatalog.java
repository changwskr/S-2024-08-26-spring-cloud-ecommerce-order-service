package com.example.orderservice.web.transfer;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class RequestCatalog {
	
    @NotNull(message = "productId cannot be null")
    @Size(min = 2, message = "productId not be less than two characters")
    private String productId;

    private String productName;
    private Integer unitPrice;
    private Integer stock;
  
}
