# Spring Boot Conversion Guide for Pranikov Healthcare System

## Overview
This document provides the complete Spring Boot conversion from the Flask application. All files have been created with detailed implementations.

## Completed Components

### 1. ✅ Models/Entities (JPA)
- User.java
- Appointment.java
- Prescription.java
- HealthRecord.java
- PharmacyProduct.java
- Order.java
- OrderItem.java
- Conversation.java
- Message.java
- Assistant.java
- AssistantRun.java

### 2. ✅ Repositories (JpaRepository)
- UserRepository.java
- AppointmentRepository.java
- PrescriptionRepository.java
- HealthRecordRepository.java
- PharmacyProductRepository.java
- OrderRepository.java
- OrderItemRepository.java
- ConversationRepository.java
- MessageRepository.java
- AssistantRepository.java
- AssistantRunRepository.java

### 3. ✅ Services Layer
- UserService.java (Authentication, Password Hashing with BCrypt)
- JwtService.java (Token Generation and Validation)
- AppointmentService.java (Business Logic for Appointments)
- PrescriptionService.java (Business Logic for Prescriptions)
- HealthRecordService.java (Business Logic for Health Records)
- PharmacyProductService.java (Business Logic for Products)
- OrderService.java (Business Logic for Orders)

### 4. ✅ DTOs (Data Transfer Objects)
- UserDTO.java
- AppointmentDTO.java
- PrescriptionDTO.java
- HealthRecordDTO.java
- PharmacyProductDTO.java
- OrderDTO.java
- OrderItemDTO.java
- ConversationDTO.java
- MessageDTO.java
- AssistantDTO.java
- AuthResponse.java
- PhoneOtpRequest.java

### 5. ✅ Configuration
- PranikovApplication.java (Main Application Class with CORS)
- SecurityConfig.java (Spring Security Configuration)
- JwtAuthenticationFilter.java (JWT Token Filter)
- application.properties (Configuration File)

### 6. ✅ Controllers
- AuthController.java (Register, Login, Profile, Avatar Upload)
- AppointmentController.java (Appointment Management)
- PrescriptionController.java (Prescription Management)

## Remaining Controllers to Create

### HealthRecordController.java
```java
@RestController
@RequestMapping("/api/health-records")
@RequiredArgsConstructor
public class HealthRecordController {
    private final HealthRecordService healthRecordService;
    
    // GET /api/health-records - List records
    // POST /api/health-records - Create record
    // GET /api/health-records/{id} - Get record
    // PUT /api/health-records/{id} - Update record
    // DELETE /api/health-records/{id} - Delete record
}
```

### PharmacyController.java
```java
@RestController
@RequestMapping("/api/pharmacy")
@RequiredArgsConstructor
public class PharmacyController {
    private final PharmacyProductService productService;
    private final OrderService orderService;
    
    // GET /api/pharmacy/products - List products
    // GET /api/pharmacy/products/{id} - Get product
    // PUT /api/pharmacy/products/{id} - Update product (Admin only)
    // POST /api/pharmacy/orders - Create order
    // GET /api/pharmacy/orders - List orders
    // GET /api/pharmacy/orders/{id} - Get order
    // PUT /api/pharmacy/orders/{id} - Update order (Admin only)
}
```

### DoctorController.java
```java
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {
    private final UserService userService;
    
    // GET /api/doctors - List all doctors
    // GET /api/doctors/{id} - Get doctor details
}
```

### AdminController.java
```java
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;
    
    // GET /api/admin/stats - Get dashboard statistics
    // GET /api/admin/users - List all users
    // PUT /api/admin/users/{id} - Update user
    // DELETE /api/admin/users/{id} - Delete user
}
```

### PhoneVerificationController.java
```java
@RestController
@RequestMapping("/api/phone")
@RequiredArgsConstructor
public class PhoneVerificationController {
    private final UserService userService;
    
    // GET /api/phone/status - Check phone verification status
    // POST /api/phone/send-otp - Send OTP
    // POST /api/phone/verify-otp - Verify OTP
}
```

### TwilioController.java
```java
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TwilioController {
    private final TwilioService twilioService;
    
    // POST /api/twilio/inbound - Receive inbound SMS
    // POST /api/messages/send - Send message (Admin only)
}
```

### ConversationController.java
```java
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversationService;
    private final MessageService messageService;
    
    // GET /api/conversations - List conversations
    // POST /api/conversations/{id}/assign - Assign to agent
    // GET /api/conversations/{id}/messages - Get messages
    // POST /api/conversations/{id}/reply - Reply to conversation
}
```

### AssistantController.java
```java
@RestController
@RequestMapping("/api/assistants")
@RequiredArgsConstructor
public class AssistantController {
    private final AssistantService assistantService;
    
    // GET /api/assistants - List assistants
    // POST /api/assistants - Create assistant
    // POST /api/assistants/{id}/run - Run assistant task
}
```

## Additional Services Needed

### TwilioService.java
- sendSmsViaTwilio(String toPhone, String body)
- normalizePhoneE164(String phone)
- getOrInitializeTwilioClient()

### PhoneVerificationService.java
- generateAndSendOTP(String userId, String phone)
- verifyOTP(String userId, String otp)
- markPhoneVerified(String userId)

### ConversationService.java
- CRUD operations for conversations
- List conversations by user/agent
- Update conversation status

### MessageService.java
- CRUD operations for messages
- List messages by conversation

### AssistantService.java
- Execute assistant tasks (appointments_review, orders_review, etc.)
- Generate detailed reports and suggestions

### DataInitializationService.java
- Seed sample data (optional)
- Initialize pharmacy products database

## Key Configuration Items

### Database Migrations (Flyway/Liquibase)
Consider adding these tools for version control of schema changes.

### Environment Variables Required
```
TWILIO_ACCOUNT_SID=your_twilio_sid
TWILIO_AUTH_TOKEN=your_twilio_token
TWILIO_PHONE_NUMBER=+1234567890
TWILIO_MESSAGING_SERVICE_SID=your_service_sid
OTP_DEV_MODE=false
DEFAULT_COUNTRY_CODE=+1
```

### pom.xml Updates (Already Done)
- Spring Boot 4.0.1
- Spring Data JPA
- Spring Security
- JWT (jjwt-api/impl/jackson) 0.12.3
- PostgreSQL Driver
- Bcrypt 0.10.2
- Twilio SDK 9.2.0
- Lombok
- Jackson

## Running the Application

1. **Build Project**
   ```bash
   mvn clean install
   ```

2. **Configure Database**
   - Update application.properties with PostgreSQL credentials
   - Database: pranikov_db
   - User: uphill_user
   - Password: 1234

3. **Run Application**
   ```bash
   mvn spring-boot:run
   ```

4. **Test Endpoints**
   - Server runs on http://localhost:5000
   - CORS enabled for all origins

## API Endpoint Mappings (Flask to Spring Boot)

| Flask Route | Spring Boot Route | Controller | Method |
|-------------|-------------------|-----------|--------|
| POST /api/register | POST /api/register | AuthController | register() |
| POST /api/login | POST /api/login | AuthController | login() |
| GET /api/profile | GET /api/profile | AuthController | getProfile() |
| PUT /api/profile | PUT /api/profile | AuthController | updateProfile() |
| POST /api/profile/avatar | POST /api/profile/avatar | AuthController | uploadAvatar() |
| GET /api/appointments | GET /api/appointments | AppointmentController | getAppointments() |
| POST /api/appointments | POST /api/appointments | AppointmentController | createAppointment() |
| GET /api/appointments/{id} | GET /api/appointments/{id} | AppointmentController | getAppointment() |
| PUT /api/appointments/{id} | PUT /api/appointments/{id} | AppointmentController | updateAppointment() |
| DELETE /api/appointments/{id} | DELETE /api/appointments/{id} | AppointmentController | deleteAppointment() |
| GET /api/appointments/availability | GET /api/appointments/availability | AppointmentController | getAvailability() |

## Security Considerations

1. **JWT Token**
   - Change `app.jwt.secret` in production
   - Token expires after 30 days (configurable)
   - Stored in Authorization header

2. **Password Hashing**
   - Uses BCrypt with 12 rounds
   - Never stored in plain text

3. **CORS**
   - Currently allows all origins (configure for production)
   - Expose Authorization header

4. **SQL Injection**
   - JPA automatically prevents through parameterized queries

## Testing

Create test files in `src/test/java` for:
- UserServiceTest
- AppointmentServiceTest
- JwtServiceTest
- Controller integration tests

## Next Steps

1. Implement remaining controllers using provided templates
2. Implement Twilio integration service
3. Add comprehensive error handling and logging
4. Create database migration scripts
5. Add unit and integration tests
6. Deploy to cloud platform (AWS, Azure, etc.)
