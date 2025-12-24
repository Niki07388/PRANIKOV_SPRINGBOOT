package com.example.demo.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.demo.event.OrderEvent;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderConsumer {

    /**
     * Listen to order events from the orders topic
     * Process order events and perform any downstream actions
     */
    @KafkaListener(
            topics = "${kafka.topic.order:orders}",
            groupId = "order-consumer-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderEvent(OrderEvent event) {
        try {
            log.info("Received order event: orderId={}, action={}", 
                    event.getOrderId(), event.getAction());

            switch (event.getAction().toLowerCase()) {
                case "created":
                    handleOrderCreated(event);
                    break;
                case "updated":
                    handleOrderUpdated(event);
                    break;
                case "shipped":
                    handleOrderShipped(event);
                    break;
                case "delivered":
                    handleOrderDelivered(event);
                    break;
                default:
                    log.warn("Unknown order action: {}", event.getAction());
            }

            log.info("Successfully processed order event: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error processing order event: {}", event.getOrderId(), e);
            // Implement DLQ (Dead Letter Queue) handling here if needed
        }
    }

    private void handleOrderCreated(OrderEvent event) {
        log.info("Handling order creation: orderId={}, patientId={}, totalPrice={}", 
                event.getOrderId(), event.getPatientId(), event.getTotalPrice());
        
        // Example: Initiate payment processing
        // Example: Create inventory reservations
        // Example: Send confirmation to patient
        // Example: Notify pharmacy/supplier
    }

    private void handleOrderUpdated(OrderEvent event) {
        log.info("Handling order update: orderId={}, newStatus={}", 
                event.getOrderId(), event.getStatus());
        
        // Example: Update patient about changes
        // Example: Adjust inventory if items changed
    }

    private void handleOrderShipped(OrderEvent event) {
        log.info("Handling order shipment: orderId={}, deliveryDate={}", 
                event.getOrderId(), event.getDeliveryDate());
        
        // Example: Send shipping notification to patient
        // Example: Generate tracking number
        // Example: Update logistics system
    }

    private void handleOrderDelivered(OrderEvent event) {
        log.info("Handling order delivery: orderId={}", event.getOrderId());
        
        // Example: Complete order fulfillment
        // Example: Send delivery confirmation
        // Example: Trigger customer feedback request
        // Example: Update analytics
    }
}
