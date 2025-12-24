# Kafka Testing Guide

## Overview

This guide covers unit testing, integration testing, and manual testing of Kafka producers and consumers in the Pranikov application.

## Test Files

- **Integration Tests**: `src/test/java/com/example/demo/KafkaIntegrationTest.java`
- **Producer Unit Tests**: `src/test/java/com/example/demo/kafka/producer/AppointmentProducerTest.java`
- **Consumer Unit Tests**: `src/test/java/com/example/demo/kafka/consumer/AppointmentConsumerTest.java`

## Running Tests

### All Kafka Tests

```bash
mvn test -Dtest="*Kafka*"
```

### Specific Test Class

```bash
# Integration tests
mvn test -Dtest=KafkaIntegrationTest

# Producer tests
mvn test -Dtest=AppointmentProducerTest

# Consumer tests
mvn test -Dtest=AppointmentConsumerTest
```

### With Coverage Report

```bash
mvn test jacoco:report
# View: target/site/jacoco/index.html
```

## Unit Tests

### AppointmentProducerTest

Tests the `AppointmentProducer` using mocks:

```java
@Test
public void testPublishAppointmentEvent_Success() {
    // Arrange: Create event
    AppointmentEvent event = AppointmentEvent.builder()
        .appointmentId(UUID.randomUUID().toString())
        .patientId("patient-123")
        .action("created")
        .build();
    
    // Mock KafkaTemplate
    when(kafkaTemplate.send(any(Message.class))).thenReturn(null);
    
    // Act & Assert
    assertDoesNotThrow(() -> appointmentProducer.publishAppointmentEvent(event));
    verify(kafkaTemplate, times(1)).send(any(Message.class));
}
```

**Tests Covered**:
- ✅ Successful event publishing
- ✅ Exception handling and error cases
- ✅ Message creation with all fields
- ✅ Kafka template invocation

### AppointmentConsumerTest

Tests the `AppointmentConsumer`:

```java
@Test
public void testConsumeAppointmentEvent_Created() {
    // Arrange
    AppointmentEvent event = AppointmentEvent.builder()
        .appointmentId(UUID.randomUUID().toString())
        .action("created")
        .build();
    
    // Act & Assert
    assertDoesNotThrow(() -> appointmentConsumer.consumeAppointmentEvent(event));
}
```

**Tests Covered**:
- ✅ Consuming "created" events
- ✅ Consuming "updated" events
- ✅ Consuming "cancelled" events
- ✅ Handling unknown actions gracefully

## Integration Tests

### KafkaIntegrationTest

Tests real Kafka integration using embedded Kafka:

```java
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9094",
        "port=9094"
    }
)
public class KafkaIntegrationTest {
    
    @Test
    public void testAppointmentEventPublishing() {
        AppointmentEvent event = createTestEvent();
        assertDoesNotThrow(() -> 
            appointmentProducer.publishAppointmentEvent(event));
    }
}
```

**Tests Covered**:
- ✅ Appointment event publishing
- ✅ Prescription event publishing
- ✅ Order event publishing
- ✅ Health record event publishing
- ✅ Multiple event publishing in sequence
- ✅ Event field validation

### Using Embedded Kafka

Embedded Kafka runs in-memory, no external broker needed:

```java
@EmbeddedKafka(
    partitions = 1,              // 1 partition for testing
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9094",
        "port=9094"
    }
)
```

## Manual Testing

### 1. Test with Real Kafka

**Start Kafka**:
```bash
docker run -d --name kafka-broker \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -p 9092:9092 \
  confluentinc/cp-kafka:7.5.0
```

**Start Application**:
```bash
mvn spring-boot:run
```

### 2. Test Event Publishing via API

```bash
# Create appointment (publishes event)
curl -X POST http://localhost:5000/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "patient-123",
    "doctorId": "doctor-456",
    "date": "2025-12-25",
    "time": "10:30",
    "reason": "Checkup",
    "visitType": "in_person"
  }'

# Expected response: Appointment created and event published
```

### 3. Monitor Events

**Console Consumer**:
```bash
docker exec kafka-broker kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic appointments \
  --from-beginning \
  --property print.key=true
```

**Application Logs**:
```bash
tail -f logs/app.log | grep -i "kafka\|appointment"

# Expected output:
# Publishing appointment event: apt-xxxxx
# Received appointment event: appointmentId=apt-xxxxx, action=created
# Successfully processed appointment event: apt-xxxxx
```

### 4. Test Multiple Operations

```bash
# Create
curl -X POST http://localhost:5000/appointments \
  -H "Content-Type: application/json" \
  -d '{"patientId":"pat-1","doctorId":"doc-1","date":"2025-12-25","time":"10:30"}'

# Update
curl -X PUT http://localhost:5000/appointments/{id} \
  -H "Content-Type: application/json" \
  -d '{"status":"rescheduled"}'

# Delete
curl -X DELETE http://localhost:5000/appointments/{id}

# Monitor all events in console
```

## Test Event Examples

### Appointment Event

```java
AppointmentEvent event = AppointmentEvent.builder()
    .appointmentId(UUID.randomUUID().toString())
    .patientId("patient-123")
    .doctorId("doctor-456")
    .date(LocalDate.of(2025, 12, 25))
    .time(LocalTime.of(10, 30))
    .reason("Annual checkup")
    .status("scheduled")
    .visitType("in_person")
    .action("created")
    .build();
```

### Prescription Event

```java
PrescriptionEvent event = PrescriptionEvent.builder()
    .prescriptionId(UUID.randomUUID().toString())
    .patientId("patient-123")
    .doctorId("doctor-456")
    .medication("Aspirin")
    .dosage("500mg")
    .frequency("Twice daily")
    .startDate(LocalDate.now())
    .endDate(LocalDate.now().plusDays(30))
    .status("active")
    .action("created")
    .build();
```

### Order Event

```java
OrderEvent.OrderItemEvent item = OrderEvent.OrderItemEvent.builder()
    .productId("prod-123")
    .quantity(2)
    .price(25.99)
    .build();

OrderEvent event = OrderEvent.builder()
    .orderId(UUID.randomUUID().toString())
    .patientId("patient-123")
    .pharmacyId("pharmacy-456")
    .totalPrice(51.98)
    .status("pending")
    .orderDate(LocalDateTime.now())
    .items(Arrays.asList(item))
    .action("created")
    .build();
```

## Troubleshooting Tests

### Issue: "Connection refused" during integration tests

**Cause**: Embedded Kafka port conflict

**Solution**:
```bash
# Change port in @EmbeddedKafka
@EmbeddedKafka(brokerProperties = {
    "listeners=PLAINTEXT://localhost:9095",  // Use different port
    "port=9095"
})
```

### Issue: Tests hanging or timing out

**Cause**: Consumer not receiving messages

**Solution**:
```java
// Add explicit wait
Thread.sleep(1000);  // Wait for processing

// Or use CountDownLatch
CountDownLatch latch = new CountDownLatch(1);
// Use in listener
latch.await(5, TimeUnit.SECONDS);
```

### Issue: "Topic does not exist"

**Cause**: Topics not auto-created in test environment

**Solution**:
```java
// Ensure topics exist before test
@Before
public void setup() {
    kafkaAdmin.createTopic(appointmentTopic());
}
```

## Test Coverage Goals

```
Target Coverage:
- Producers: 85%+
- Consumers: 80%+
- Events: 90%+
- Overall: 75%+
```

Check current coverage:
```bash
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Kafka Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Run Kafka Tests
        run: mvn test -Dtest="*Kafka*" -Pcoverage
      - name: Upload Coverage
        uses: codecov/codecov-action@v2
        with:
          files: ./target/site/jacoco/jacoco.xml
```

## Performance Testing

### Load Test Script

```bash
#!/bin/bash

echo "Testing Kafka under load..."

for i in {1..100}; do
    curl -X POST http://localhost:5000/appointments \
      -H "Content-Type: application/json" \
      -d '{
        "patientId": "patient-'$i'",
        "doctorId": "doctor-'$i'",
        "date": "2025-12-25",
        "time": "10:30",
        "reason": "Checkup"
      }' \
      > /dev/null 2>&1 &
done

wait
echo "✅ Load test complete"

# Check consumer lag
docker exec kafka-broker kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group appointment-consumer-group \
  --describe
```

## Best Practices

1. **Always use @DirtiesContext** for integration tests to avoid interference
2. **Mock external dependencies** in unit tests
3. **Use embedded Kafka** for integration tests
4. **Test error paths** and exception handling
5. **Verify message headers** and serialization
6. **Check consumer group offsets** after tests
7. **Clean up resources** after tests complete

## Next Steps

- Implement more consumer integration tests
- Add performance benchmarks
- Set up CI/CD pipeline
- Monitor test coverage over time
- Create custom test fixtures for common events

---

For more information, see [KAFKA_GUIDE.md](KAFKA_GUIDE.md)
