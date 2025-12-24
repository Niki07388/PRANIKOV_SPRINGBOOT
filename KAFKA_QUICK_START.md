# Kafka Quick Start Guide

## 5-Minute Setup

### 1. Start Kafka with Docker

```bash
docker run -d --name kafka-broker \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -p 9092:9092 \
  confluentinc/cp-kafka:7.5.0
```

### 2. Verify Installation

```bash
# Check Kafka is running
docker ps | grep kafka-broker

# List topics
docker exec kafka-broker kafka-topics --list --bootstrap-server localhost:9092
```

### 3. Build & Run Application

```bash
# Build the project
mvn clean install -DskipTests

# Run the application
mvn spring-boot:run
```

### 4. Test Event Publishing

```bash
# Create an appointment (triggers event publishing)
curl -X POST http://localhost:5000/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "patient-123",
    "doctorId": "doctor-456",
    "date": "2025-12-25",
    "time": "10:30",
    "reason": "Annual checkup",
    "visitType": "in_person"
  }'
```

### 5. Monitor Events

```bash
# Watch events in real-time
docker exec kafka-broker kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic appointments \
  --from-beginning \
  --property print.key=true \
  --property print.value=true
```

## Key Kafka Topics

| Topic | Triggered By | Example |
|-------|--------------|---------|
| `appointments` | Appointment CRUD | POST/PUT/DELETE /appointments |
| `prescriptions` | Prescription CRUD | POST/PUT/DELETE /prescriptions |
| `orders` | Order CRUD | POST /orders |
| `health-records` | Health Record CRUD | POST /health-records |

## Event Publishing Flow

```
API Request → Service → Producer → Kafka Topic → Consumer → Action
```

Example:
```
POST /appointments → AppointmentService → AppointmentProducer 
  → appointments topic → AppointmentConsumer → Send notifications
```

## Common Commands

### List Topics
```bash
docker exec kafka-broker kafka-topics --list --bootstrap-server localhost:9092
```

### Create Topic Manually
```bash
docker exec kafka-broker kafka-topics \
  --create \
  --bootstrap-server localhost:9092 \
  --topic my-topic \
  --partitions 3 \
  --replication-factor 1
```

### Describe Topic
```bash
docker exec kafka-broker kafka-topics \
  --describe \
  --bootstrap-server localhost:9092 \
  --topic appointments
```

### Consumer Group Status
```bash
docker exec kafka-broker kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group appointment-consumer-group \
  --describe
```

### Delete Topic
```bash
docker exec kafka-broker kafka-topics \
  --delete \
  --bootstrap-server localhost:9092 \
  --topic my-topic
```

## Troubleshooting

### Kafka Not Running?
```bash
# Start Kafka
docker start kafka-broker

# Check logs
docker logs -f kafka-broker
```

### Events Not Being Published?
1. Check application logs: `tail -f logs/app.log | grep Kafka`
2. Verify Kafka is running: `docker ps | grep kafka`
3. Verify topic exists: `docker exec kafka-broker kafka-topics --list --bootstrap-server localhost:9092`

### Consumer Lag High?
```bash
# Check lag
docker exec kafka-broker kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group appointment-consumer-group \
  --describe
```

## Running Tests

```bash
# All Kafka tests
mvn test -Dtest="*Kafka*"

# Specific test
mvn test -Dtest=KafkaIntegrationTest

# With coverage
mvn test -Pcoverage
```

## Example Event JSON

### Appointment Event
```json
{
  "appointmentId": "apt-12345",
  "patientId": "pat-67890",
  "doctorId": "doc-11111",
  "date": "2025-12-25",
  "time": "10:30:00",
  "reason": "Annual checkup",
  "status": "scheduled",
  "visitType": "in_person",
  "action": "created"
}
```

### Prescription Event
```json
{
  "prescriptionId": "presc-12345",
  "patientId": "pat-67890",
  "doctorId": "doc-11111",
  "medication": "Aspirin",
  "dosage": "500mg",
  "frequency": "Twice daily",
  "startDate": "2025-12-20",
  "endDate": "2025-01-20",
  "status": "active",
  "action": "created"
}
```

### Order Event
```json
{
  "orderId": "ord-12345",
  "patientId": "pat-67890",
  "pharmacyId": "pharm-111",
  "totalPrice": 99.99,
  "status": "pending",
  "orderDate": "2025-12-20T10:30:00",
  "items": [
    {
      "productId": "prod-123",
      "quantity": 2,
      "price": 50.00
    }
  ],
  "action": "created"
}
```

## Next Steps

- Read the full [Kafka Guide](KAFKA_GUIDE.md)
- Implement custom consumers in your microservices
- Monitor Kafka metrics with Prometheus
- Set up Dead Letter Queue (DLQ) for failed events
- Configure TLS/SSL for production

---

For detailed documentation, see [KAFKA_GUIDE.md](KAFKA_GUIDE.md)
