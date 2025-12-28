package com.example.demo.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private String id;
    private String userId;
    private Float total;
    private String status;
    private String shippingAddress;
    private List<OrderItemDTO> items;
}
