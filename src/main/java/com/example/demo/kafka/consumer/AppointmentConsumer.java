package com.example.demo.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.demo.event.AppointmentEvent;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AppointmentConsumer {

    /**
     * Listen to appointment events from the appointments topic
     * Process appointment events and perform any downstream actions
     */
    @KafkaListener(
            topics = "${kafka.topic.appointment:appointments}",
            groupId = "appointment-consumer-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAppointmentEvent(AppointmentEvent event) {
        try {
            log.info("Received appointment event: appointmentId={}, action={}", 
                    event.getAppointmentId(), event.getAction());

            switch (event.getAction().toLowerCase()) {
                case "created":
                    handleAppointmentCreated(event);
                    break;
                case "updated":
                    handleAppointmentUpdated(event);
                    break;
                case "cancelled":
                    handleAppointmentCancelled(event);
                    break;
                default:
                    log.warn("Unknown appointment action: {}", event.getAction());
            }

            log.info("Successfully processed appointment event: {}", event.getAppointmentId());
        } catch (Exception e) {
            log.error("Error processing appointment event: {}", event.getAppointmentId(), e);
            // Implement DLQ (Dead Letter Queue) handling here if needed
        }
    }

    private void handleAppointmentCreated(AppointmentEvent event) {
        log.info("Handling appointment creation: appointmentId={}, patientId={}, doctorId={}", 
                event.getAppointmentId(), event.getPatientId(), event.getDoctorId());
        
        // Example: Send notification to patient and doctor
        // Example: Update metrics/analytics
        // Example: Create calendar entries
    }

    private void handleAppointmentUpdated(AppointmentEvent event) {
        log.info("Handling appointment update: appointmentId={}, newStatus={}", 
                event.getAppointmentId(), event.getStatus());
        
        // Example: Update related records
        // Example: Send notification about the change
    }

    private void handleAppointmentCancelled(AppointmentEvent event) {
        log.info("Handling appointment cancellation: appointmentId={}", 
                event.getAppointmentId());
        
        // Example: Notify patient and doctor
        // Example: Free up doctor's time slot
        // Example: Process refunds if applicable
    }
}
