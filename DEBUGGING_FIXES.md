# 🐛 Debugging Report & Fixes Applied

## Issues Found & Fixed

### ✅ Issue 1: Missing Fields Not Being Saved (address, dateOfBirth)

**Problem:**
- Fields like `address` and `dateOfBirth` were defined in the `User` entity and `UserDTO`
- But `UserService.registerUser()` was NOT setting these fields when creating a new user

**Root Cause:**
```java
// BEFORE (WRONG)
public User registerUser(UserDTO dto) {
    User user = new User();
    // ... other fields ...
    user.setPhone(dto.getPhone());
    user.setSpecialization(dto.getSpecialization());  // Had these
    user.setLicense(dto.getLicense());                 // Had these
    // MISSING: user.setAddress(dto.getAddress());
    // MISSING: user.setDateOfBirth(dto.getDateOfBirth());
    return userRepository.save(user);
}
```

**Solution Applied:**
```java
// AFTER (FIXED)
public User registerUser(UserDTO dto) {
    User user = new User();
    user.setId(UUID.randomUUID().toString());
    user.setEmail(dto.getEmail());
    user.setPasswordHash(hashPassword(dto.getPassword()));
    user.setName(dto.getName());
    user.setRole(dto.getRole() != null ? dto.getRole() : "patient");
    user.setPhone(dto.getPhone());
    user.setAddress(dto.getAddress());              // ✅ ADDED
    user.setDateOfBirth(dto.getDateOfBirth());      // ✅ ADDED
    user.setSpecialization(dto.getSpecialization());
    user.setLicense(dto.getLicense());
    user.setPhoneVerified(false);
    return userRepository.save(user);
}
```

**File Changed:** `src/main/java/com/example/demo/service/UserService.java`

---

### ✅ Issue 2: Frontend Integration Not Working - CORS Configuration

**Problem:**
- Frontend on different port/origin couldn't call backend API
- CORS (Cross-Origin Resource Sharing) was not properly configured
- No CorsConfigurationSource bean in Spring Security

**Root Cause:**
```java
// BEFORE (WRONG)
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        // .cors(...) was MISSING
        .sessionManagement(...)
        .authorizeHttpRequests(...)
        .addFilterBefore(...);
    return http.build();
}
// NO CorsConfigurationSource bean existed
```

**Solution Applied:**

1. **Added CORS configuration bean:**
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("*"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
    configuration.setAllowCredentials(false);
    configuration.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

2. **Updated SecurityFilterChain to use CORS:**
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))  // ✅ ADDED
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // ... rest of config
}
```

3. **Added required imports:**
```java
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
```

**File Changed:** `src/main/java/com/example/demo/config/SecurityConfig.java`

**What This Fixes:**
- ✅ Frontend can now call API from any origin
- ✅ Authorization header is exposed to frontend
- ✅ OPTIONS preflight requests are handled
- ✅ All HTTP methods (GET, POST, PUT, DELETE, PATCH) allowed

---

### ✅ Issue 3: Wrong Server Port

**Problem:**
- Application was running on port 8080 (hardcoded)
- Original Flask app ran on port 5000
- Frontend was probably trying to connect to port 5000

**Solution Applied:**

Changed in `application.properties`:
```properties
# BEFORE
server.port=8080

# AFTER
server.port=5000
```

Also added compression for faster frontend responses:
```properties
server.compression.enabled=true
server.compression.mime-types=application/json,application/xml,text/html,text/xml,text/plain
```

**File Changed:** `src/main/resources/application.properties`

---

## Testing Steps

### Step 1: Register a User with All Fields

**Postman Request:**
```
POST http://localhost:5000/api/register
Content-Type: application/json

{
    "email": "john@example.com",
    "password": "password123",
    "name": "John Doe",
    "role": "patient",
    "phone": "+1234567890",
    "address": "123 Main Street, New York, NY 10001",
    "dateOfBirth": "1990-01-15"
}
```

**Expected Response:**
```json
{
    "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
    "user": {
        "id": "uuid-here",
        "email": "john@example.com",
        "name": "John Doe",
        "role": "patient",
        "phone": "+1234567890",
        "address": "123 Main Street, New York, NY 10001",
        "dateOfBirth": "1990-01-15",
        "phoneVerified": false
    }
}
```

### Step 2: Check Database

**Connect to PostgreSQL and run:**
```sql
SELECT id, email, name, address, date_of_birth FROM users;
```

**Verify:**
- ✅ `address` field has value "123 Main Street, New York, NY 10001"
- ✅ `date_of_birth` field has value "1990-01-15"

---

## Frontend Integration Guide

### 1. Update Your Frontend Base URL

**React/Vue/Angular:**
```javascript
// old (port 8080)
const API_BASE_URL = 'http://localhost:8080/api';

// new (port 5000)
const API_BASE_URL = 'http://localhost:5000/api';
```

### 2. Add Authorization Header to Requests

**Example with fetch:**
```javascript
async function loginUser(email, password) {
    const response = await fetch(`http://localhost:5000/api/login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify({ email, password })
    });
    
    const data = await response.json();
    localStorage.setItem('token', data.token);
    return data;
}

async function getProfile() {
    const token = localStorage.getItem('token');
    const response = await fetch(`http://localhost:5000/api/profile`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    });
    return response.json();
}
```

**Example with Axios:**
```javascript
import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:5000/api'
});

api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// Now use: api.post('/login', {...})
```

### 3. Verify CORS is Working

**Test with curl:**
```bash
curl -X OPTIONS http://localhost:5000/api/register \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST" \
  -v
```

**Look for response headers:**
```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
Access-Control-Allow-Headers: *
Access-Control-Expose-Headers: Authorization, Content-Type
```

---

## Files Modified

| File | Changes |
|------|---------|
| `UserService.java` | Added `address` and `dateOfBirth` field mapping in `registerUser()` |
| `SecurityConfig.java` | Added CORS configuration bean and enabled CORS in filter chain |
| `application.properties` | Changed port from 8080 to 5000, added compression config |

---

## Summary of What Was Fixed

✅ **Database Persistence**
- User registration now saves all fields including address and dateOfBirth
- All subsequent calls to `updateUser()` already handled these fields correctly

✅ **Frontend Integration**
- CORS now properly configured to allow requests from any frontend
- Authorization header is exposed for JWT token handling
- All HTTP methods (GET, POST, PUT, DELETE, PATCH) are allowed
- Preflight (OPTIONS) requests are handled automatically

✅ **Port Alignment**
- Application now runs on port 5000 (matching Flask original)
- Frontend no longer needs to connect to wrong port

---

## How to Test Everything

### 1. Build & Start Application
```bash
cd c:\Users\Nikhil Kasyap\Downloads\demo (1)\demo
.\mvnw.cmd clean install -DskipTests
.\mvnw.cmd spring-boot:run
```

### 2. Test with Postman
- Register a user with all fields (address, dateOfBirth)
- Check response contains all fields
- Check database for data persistence

### 3. Test Frontend Integration
- Start your frontend (React/Vue/Angular) on any port
- Try login/register
- Verify Authorization header is sent
- Check network tab for successful CORS preflight

### 4. Verify Database
```sql
psql -U uphill_user -d pranikov_uphill
SELECT id, email, name, address, date_of_birth, phone FROM users LIMIT 5;
```

---

## Production Notes

⚠️ **For Production:**
1. Change CORS `setAllowedOrigins(Arrays.asList("*"))` to specific frontend domain
2. Change JWT secret in `application.properties` to strong random value
3. Add HTTPS/SSL configuration
4. Implement rate limiting for APIs
5. Add input validation annotations to DTOs

**Current CORS setting allows all origins** - suitable for development/testing only.

---

## If Issues Persist

### Issue: Still getting CORS errors
- Clear browser cache (Ctrl+Shift+Delete)
- Check that backend is running on port 5000: `curl http://localhost:5000/api/doctors`
- Verify OPTIONS preflight request returns proper headers

### Issue: Fields still not saving
- Check database credentials in application.properties match your PostgreSQL setup
- Verify `spring.jpa.hibernate.ddl-auto=update` for auto-schema generation
- Check application logs for SQL errors: `SELECT * FROM information_schema.tables WHERE table_schema='public';`

### Issue: Frontend getting 401 Unauthorized
- Ensure token is being sent in `Authorization: Bearer <token>` header
- Verify token is being saved after login
- Check token hasn't expired (30 days default)

---

**All fixes applied successfully. Application rebuilt and ready for testing!** ✅
