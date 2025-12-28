package com.example.demo.controller;

import com.example.demo.dto.PharmacyProductDTO;
import com.example.demo.dto.OrderDTO;
import com.example.demo.entity.PharmacyProduct;
import com.example.demo.entity.Order;
import com.example.demo.entity.User;
import com.example.demo.service.PharmacyProductService;
import com.example.demo.service.OrderService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pharmacy")
@RequiredArgsConstructor
public class PharmacyController {

    private final PharmacyProductService productService;
    private final OrderService orderService;
    private final UserService userService;

    // -------------------- PRODUCTS --------------------

    // Get all in-stock products (PUBLIC)
    @GetMapping("/products")
    public ResponseEntity<?> getProducts() {
        try {
            List<PharmacyProduct> products = productService.getAllInStockProducts();
            return ResponseEntity.ok(
                    products.stream()
                            .map(this::convertProductToDTO)
                            .collect(Collectors.toList())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // Get product by ID (PUBLIC)
    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProduct(@PathVariable String id) {
        try {
            PharmacyProduct product = productService.getProductById(id);
            return ResponseEntity.ok(convertProductToDTO(product));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // Create product (ADMIN ONLY)
    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody PharmacyProductDTO dto) {
        try {
            if (!isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Admin access required"));
            }

            PharmacyProduct product = productService.createProduct(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(convertProductToDTO(product));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // Update product (ADMIN ONLY)
    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable String id,
            @RequestBody PharmacyProductDTO dto) {

        try {
            if (!isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Admin access required"));
            }

            PharmacyProduct product = productService.updateProduct(id, dto);
            return ResponseEntity.ok(convertProductToDTO(product));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // -------------------- ORDERS --------------------

    // Create order (USER / PATIENT)
    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody OrderDTO dto) {
        try {
            dto.setUserId(getCurrentUserId());
            Order order = orderService.createOrder(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(convertOrderToDTO(order));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // Get orders (ADMIN sees all, USER sees own)
    @GetMapping("/orders")
    public ResponseEntity<?> getOrders() {
        try {
            String userId = getCurrentUserId();
            String role = getCurrentUserRole();

            List<Order> orders = "admin".equals(role)
                    ? orderService.getAllOrders()
                    : orderService.getOrdersByUser(userId);

            return ResponseEntity.ok(
                    orders.stream()
                            .map(this::convertOrderToDTO)
                            .collect(Collectors.toList())
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // Get order by ID
    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id) {
        try {
            Order order = orderService.getOrderById(id);
            String role = getCurrentUserRole();
            String userId = getCurrentUserId();

            if (!"admin".equals(role) && !order.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Access denied"));
            }

            return ResponseEntity.ok(convertOrderToDTO(order));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // Update order status (ADMIN ONLY)
    @PutMapping("/orders/{id}")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {

        try {
            if (!isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Admin access required"));
            }

            Order order = orderService.updateOrderStatus(id, request.get("status"));
            return ResponseEntity.ok(convertOrderToDTO(order));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // -------------------- SECURITY HELPERS --------------------

    protected String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName(); // SAFE
    }

    protected String getCurrentUserRole() {
        try {
            String currentUserId = getCurrentUserId();
            User currentUser = userService.getUserById(currentUserId);
            return currentUser.getRole();
        } catch (Exception e) {
            return null;
        }
    }

    protected boolean isAdmin() {
        String role = getCurrentUserRole();
        return "admin".equals(role);
    }

    // -------------------- DTO MAPPERS --------------------

    private PharmacyProductDTO convertProductToDTO(PharmacyProduct product) {
        PharmacyProductDTO dto = new PharmacyProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setCategory(product.getCategory());
        dto.setInStock(product.getInStock());
        dto.setImageUrl(product.getImageUrl());
        dto.setPrescriptionRequired(product.getPrescriptionRequired());
        return dto;
    }

    private OrderDTO convertOrderToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setTotal(order.getTotal());
        dto.setStatus(order.getStatus());
        dto.setShippingAddress(order.getShippingAddress());
        return dto;
    }
}
