# Kafka Integration Guide for Pranikov Application

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Setup & Installation](#setup--installation)
4. [Configuration](#configuration)
5. [Producers](#producers)
6. [Consumers](#consumers)
7. [Events](#events)
8. [Testing](#testing)
9. [Monitoring & Troubleshooting](#monitoring--troubleshooting)
10. [Best Practices](#best-practices)

## Overview

This document provides comprehensive guidance for using Apache Kafka with the Pranikov healthcare application. Kafka enables event-driven architecture, allowing services to communicate asynchronously through event streams.

### Why Kafka?

- **Decoupling**: Services don't need to know about each other
- **Scalability**: Handle high-volume event streams
- **Durability**: Events are persisted and can be replayed
- **Real-time Processing**: Immediate processing of events
- **Fault Tolerance**: Built-in replication and recovery

### Use Cases in Pranikov

1. **Appointment Events**: Notify multiple services when appointments are created, updated, or cancelled
2. **Prescription Events**: Alert pharmacy systems and patient notifications
3. **Order Events**: Manage order lifecycle and fulfillment
4. **Health Records**: Archive and index medical records
5. **Notifications**: Decouple notification system from core services

## Architecture

### Event Flow

```
Service (e.g., AppointmentService)
    ↓
Producer (e.g., AppointmentProducer)
    ↓
Kafka Topic (e.g., appointments)
    ↓
Consumer (e.g., AppointmentConsumer)
    ↓
Downstream Processing (Notifications, Analytics, etc.)
```

### Topics

| Topic | Partitions | Description |
|-------|-----------|-------------|
| `appointments` | 3 | Appointment CRUD events |
| `prescriptions` | 3 | Prescription CRUD events |
| `orders` | 3 | Order lifecycle events |
| `health-records` | 2 | Health record creation/updates |
| `notifications` | 1 | System notification events |

### Consumer Groups

| Consumer Group | Topics | Purpose |
|---|---|---|
| `appointment-consumer-group` | appointments | Process appointment events |
| `prescription-consumer-group` | prescriptions | Process prescription events |
| `order-consumer-group` | orders | Process order events |
| `health-record-consumer-group` | health-records | Process health record events |

## Setup & Installation

### Prerequisites

- Java 17+
- Kafka 3.x
- Spring Boot 4.0.1+
- Maven 3.6+

### 1. Install Kafka

**Option A: Using Docker**

```bash
# Start Zookeeper and Kafka
docker run -d --name zookeeper \
  -p 2181:2181 \
  confluentinc/cp-zookeeper:7.5.0 \
  -e ZOOKEEPER_CLIENT_PORT=2181

docker run -d --name kafka \
  -p 9092:9092 \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  confluentinc/cp-kafka:7.5.0
```

**Option B: Using Docker Compose**

Create a `docker-compose.yml`:

```yaml
version: '3'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'false'
```

Then run:
```bash
docker-compose up -d
```

### 2. Verify Kafka Installation

```bash
# Check if Kafka is running
docker ps | grep kafka

# Check Kafka logs
docker logs kafka

# List topics
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

### 3. Dependencies

The following dependencies are already added to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

## Configuration

### application.properties

Kafka configuration in `src/main/resources/application.properties`:

```properties
# Kafka Bootstrap Servers
spring.kafka.bootstrap-servers=localhost:9092

# Producer Configuration
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.producer.acks=all
spring.kafka.producer.retries=3
spring.kafka.producer.linger-ms=10
spring.kafka.producer.compression-type=snappy

# Consumer Configuration
spring.kafka.consumer.bootstrap-servers=localhost:9092
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.group-id=pranikov-service-group
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.max-poll-records=500
spring.kafka.consumer.session-timeout-ms=30000

# Topic Names
kafka.topic.appointment=appointments
kafka.topic.prescription=prescriptions
kafka.topic.order=orders
kafka.topic.health-record=health-records
kafka.topic.notifications=notifications
```

### KafkaConfig.java

The configuration class `src/main/java/com/example/demo/config/KafkaConfig.java` handles:

- Topic creation with appropriate partitions and replication
- Producer factory setup with JSON serialization
- Consumer factory setup with JSON deserialization
- Concurrency and performance tuning
- Admin client configuration

## Producers

### AppointmentProducer

**Location**: `src/main/java/com/example/demo/kafka/producer/AppointmentProducer.java`

```java
@Service
@RequiredArgsConstructor
public class AppointmentProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishAppointmentEvent(AppointmentEvent event) {
        // Publishes appointment events to 'appointments' topic
    }
}
```

**Usage in Service**:

```java
// In AppointmentService.createAppointment()
AppointmentEvent event = AppointmentEvent.builder()
    .appointmentId(saved.getId())
    .patientId(saved.getPatientId())
    .doctorId(saved.getDoctorId())
    .date(saved.getDate())
    .time(saved.getTime())
    .status(saved.getStatus())
    .action("created")
    .build();
appointmentProducer.publishAppointmentEvent(event);
```

### PrescriptionProducer

**Location**: `src/main/java/com/example/demo/kafka/producer/PrescriptionProducer.java`

Publishes prescription events to the `prescriptions` topic.

### OrderProducer

**Location**: `src/main/java/com/example/demo/kafka/producer/OrderProducer.java`

Publishes order events to the `orders` topic, including order items.

### HealthRecordProducer

**Location**: `src/main/java/com/example/demo/kafka/producer/HealthRecordProducer.java`

Publishes health record events to the `health-records` topic.

## Consumers

### AppointmentConsumer

**Location**: `src/main/java/com/example/demo/kafka/consumer/AppointmentConsumer.java`

```java
@Service
public class AppointmentConsumer {
    @KafkaListener(
        topics = "${kafka.topic.appointment:appointments}",
        groupId = "appointment-consumer-group"
    )
    public void consumeAppointmentEvent(AppointmentEvent event) {
        // Process appointment events
        // Handle created, updated, cancelled actions
    }
}
```

**Event Handling**:
- **created**: Initialize calendar entries, send notifications, update availability
- **updated**: Update related records, notify stakeholders
- **cancelled**: Free up time slots, process refunds, cancel related orders

### PrescriptionConsumer

Listens to the `prescriptions` topic and handles:
- **created**: Check drug interactions, notify pharmacy, create fulfillment requests
- **updated**: Update pharmacy about changes
- **cancelled**: Cancel pharmacy orders, notify patient

### OrderConsumer

Listens to the `orders` topic and handles:
- **created**: Process payment, reserve inventory
- **updated**: Update patient and logistics
- **shipped**: Generate tracking, send notifications
- **delivered**: Complete fulfillment, request feedback

### HealthRecordConsumer

Listens to the `health-records` topic and handles:
- **created**: Archive to storage, index for search
- **updated**: Update indices and notify professionals

## Events

### Event Classes

All events are located in `src/main/java/com/example/demo/event/`

#### AppointmentEvent

```java
@Data
@Builder
public class AppointmentEvent {
    private String appointmentId;
    private String patientId;
    private String doctorId;
    private LocalDate date;
    private LocalTime time;
    private String reason;
    private String status;          // scheduled, confirmed, completed, cancelled
    private String visitType;       // in_person, virtual
    private String action;          // created, updated, cancelled
}
```

#### PrescriptionEvent

```java
@Data
@Builder
public class PrescriptionEvent {
    private String prescriptionId;
    private String patientId;
    private String doctorId;
    private String medication;
    private String dosage;
    private String frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;          // active, inactive, completed
    private String action;          // created, updated, cancelled
}
```

#### OrderEvent

```java
@Data
@Builder
public class OrderEvent {
    private String orderId;
    private String patientId;
    private String pharmacyId;
    private Double totalPrice;
    private String status;          // pending, processing, shipped, delivered
    private LocalDateTime orderDate;
    private LocalDateTime deliveryDate;
    private List<OrderItemEvent> items;
    private String action;          // created, updated, shipped, delivered
}
```

#### HealthRecordEvent

```java
@Data
@Builder
public class HealthRecordEvent {
    private String recordId;
    private String patientId;
    private String doctorId;
    private String recordType;      // lab_result, diagnosis, treatment, etc.
    private String description;
    private LocalDate recordDate;
    private String action;          // created, updated
}
```

## Testing

### Integration Tests

**Location**: `src/test/java/com/example/demo/KafkaIntegrationTest.java`

Tests use embedded Kafka for testing without a real instance:

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, brokerProperties = {
    "listeners=PLAINTEXT://localhost:9094",
    "port=9094"
})
public class KafkaIntegrationTest {
    
    @Test
    public void testAppointmentEventPublishing() {
        AppointmentEvent event = AppointmentEvent.builder()
            .appointmentId(UUID.randomUUID().toString())
            .patientId("patient-123")
            .doctorId("doctor-456")
            .action("created")
            .build();
        
        assertDoesNotThrow(() -> appointmentProducer.publishAppointmentEvent(event));
    }
}
```

### Unit Tests

**Producer Test** (`src/test/java/com/example/demo/kafka/producer/AppointmentProducerTest.java`):
- Tests successful event publishing
- Tests error handling
- Mocks KafkaTemplate

**Consumer Test** (`src/test/java/com/example/demo/kafka/consumer/AppointmentConsumerTest.java`):
- Tests event consumption for different actions
- Tests error handling
- Validates event processing logic

### Running Tests

```bash
# Run all Kafka tests
mvn test -Dtest="*KafkaIntegrationTest,*AppointmentProducerTest,*AppointmentConsumerTest"

# Run specific test class
mvn test -Dtest=KafkaIntegrationTest

# Run with coverage
mvn test -Pcoverage
```

## Monitoring & Troubleshooting

### Kafka Console Tools

#### List all topics
```bash
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

#### Describe a topic
```bash
docker exec kafka kafka-topics --describe --topic appointments --bootstrap-server localhost:9092
```

#### Consume messages from a topic
```bash
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic appointments \
  --from-beginning
```

#### Check consumer group lag
```bash
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group appointment-consumer-group \
  --describe
```

### Logs

Check application logs for Kafka issues:

```bash
# View logs
tail -f logs/pranikov.log | grep -i kafka

# Set log levels in application.properties
logging.level.org.springframework.kafka=DEBUG
logging.level.org.apache.kafka=DEBUG
```

### Common Issues

#### Issue: "Connection refused" on localhost:9092

**Solution**: Verify Kafka is running
```bash
docker ps | grep kafka
docker logs kafka
```

#### Issue: Topics not being created

**Solution**: Check KafkaAdmin configuration and ensure topics are defined as `@Bean`

#### Issue: Consumer lag increasing

**Solution**: 
- Check consumer throughput: `mvn clean install -DskipTests -q`
- Increase partition count for topic
- Add more consumer instances
- Check application logs for processing errors

#### Issue: Events not being consumed

**Solution**:
1. Verify consumer group: `kafka-consumer-groups --list --bootstrap-server localhost:9092`
2. Check topic exists: `kafka-topics --list --bootstrap-server localhost:9092`
3. Verify @KafkaListener annotations are present
4. Check Spring Boot logs for listener registration

## Best Practices

### 1. Event Design

✅ **DO**:
- Use specific event types for different domains
- Include action/verb in event (created, updated, deleted)
- Include timestamp and actor information
- Make events immutable/final

❌ **DON'T**:
- Use generic "Event" class for all events
- Include too much data; reference IDs instead
- Send sensitive information in events
- Assume event ordering across partitions

### 2. Producer Best Practices

```java
// Good: Explicit error handling
try {
    appointmentProducer.publishAppointmentEvent(event);
} catch (Exception e) {
    log.error("Failed to publish event", e);
    // Fallback: store in database for retry
}

// Good: Non-blocking publication
CompletableFuture<SendResult<String, AppointmentEvent>> future = 
    kafkaTemplate.send(topic, event);
```

### 3. Consumer Best Practices

```java
// Good: Handle different event types
@KafkaListener(topics = "appointments")
public void consume(AppointmentEvent event) {
    try {
        switch(event.getAction()) {
            case "created": handleCreated(event); break;
            case "updated": handleUpdated(event); break;
            // ...
        }
    } catch (Exception e) {
        log.error("Error processing event", e);
        // Send to DLQ (Dead Letter Queue)
    }
}

// Good: Idempotent processing
private Map<String, Boolean> processedEvents = new ConcurrentHashMap<>();

public void consume(AppointmentEvent event) {
    if (!processedEvents.putIfAbsent(event.getAppointmentId(), true)) {
        return; // Already processed
    }
    // Process event
}
```

### 4. Topic Configuration

```java
@Bean
public NewTopic appointmentTopic() {
    return TopicBuilder.name("appointments")
        .partitions(3)              // For parallelism
        .replicas(1)                // Increase in production
        .config("retention.ms", "604800000") // 7 days
        .build();
}
```

### 5. Monitoring

Add metrics and monitoring:

```java
@Component
public class KafkaMetrics {
    private final MeterRegistry meterRegistry;
    
    public void recordEventPublished(String topic) {
        meterRegistry.counter("kafka.events.published", "topic", topic).increment();
    }
}
```

### 6. Error Handling

Implement Dead Letter Queue (DLQ):

```java
@Configuration
public class DLQConfiguration {
    
    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name("appointments-dlq")
            .partitions(1)
            .replicas(1)
            .build();
    }
}
```

### 7. Security (Production)

```properties
# Enable SSL/TLS
spring.kafka.security.protocol=SSL
spring.kafka.ssl.key-store-location=classpath:kafka.client.keystore.jks
spring.kafka.ssl.key-store-password=password

# Enable SASL authentication
spring.kafka.security.protocol=SASL_SSL
spring.kafka.sasl.mechanism=PLAIN
spring.kafka.sasl.jaas-config=org.apache.kafka.common.security.plain.PlainLoginModule required username="user" password="pass";
```

## API Integration Example

### Publishing an Event via API

```java
@PostMapping("/appointments")
public ResponseEntity<AppointmentDTO> createAppointment(
        @RequestBody AppointmentDTO dto) {
    
    // Service automatically publishes event
    Appointment appointment = appointmentService.createAppointment(dto);
    
    return ResponseEntity.ok(new AppointmentDTO(appointment));
}
```

The appointment is automatically published to Kafka by `AppointmentService.createAppointment()`.

### Verifying Event Flow

1. **API Call**: Create appointment via `POST /appointments`
2. **Service**: `AppointmentService.createAppointment()` publishes event
3. **Topic**: Event appears in `appointments` topic
4. **Consumer**: `AppointmentConsumer.consumeAppointmentEvent()` processes it
5. **Action**: Notifications sent, calendars updated, etc.

## Performance Tuning

### Producer Tuning

```properties
# Batch messages for better throughput
spring.kafka.producer.batch-size=32768
spring.kafka.producer.linger-ms=10

# Compression reduces network traffic
spring.kafka.producer.compression-type=snappy

# Buffer for in-flight requests
spring.kafka.producer.buffer-memory=67108864
```

### Consumer Tuning

```properties
# Process multiple records per poll
spring.kafka.consumer.max-poll-records=500
spring.kafka.consumer.fetch-min-bytes=1024
spring.kafka.consumer.fetch-max-wait-ms=500

# Parallel processing
spring.kafka.listener.concurrency=3
```

## Migration Guide

### Adding Kafka to Existing Services

1. **Add Producer Dependency**: Inject producer into service
2. **Publish Events**: Call producer after CRUD operations
3. **Add Consumer**: Create listener for downstream processing
4. **Test**: Write integration tests
5. **Deploy**: Deploy with Kafka running

Example:
```java
// Before
public Appointment createAppointment(AppointmentDTO dto) {
    return appointmentRepository.save(appointment);
}

// After
public Appointment createAppointment(AppointmentDTO dto) {
    Appointment saved = appointmentRepository.save(appointment);
    appointmentProducer.publishAppointmentEvent(event);
    return saved;
}
```

## Support & Resources

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Confluent Kafka Tutorial](https://docs.confluent.io/kafka/introduction.html)
- [Kafka Best Practices](https://kafka.apache.org/documentation/#bestpractices)

## Glossary

- **Topic**: Stream of events grouped by category
- **Partition**: Sub-division of a topic for parallelism
- **Consumer Group**: Group of consumers reading from same topic
- **Offset**: Position in a topic/partition
- **Replication**: Copies of partitions for fault tolerance
- **Producer**: Application publishing events
- **Consumer**: Application reading events
- **DLQ**: Dead Letter Queue for failed messages

---

**Last Updated**: December 2024
**Version**: 1.0
**Author**: Pranikov Development Team
