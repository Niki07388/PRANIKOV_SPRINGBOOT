# 🎯 Complete Fix Guide - Address, DOB & Frontend Integration

## What Was Wrong

You reported:
1. **Address and DOB fields not being saved to database** when registering via Postman
2. **Frontend not connecting** to the backend

## What Was Fixed

### Fix #1: Missing Database Field Mappers (address, dateOfBirth)

**File:** `UserService.java`

The `registerUser()` method was missing two lines that map the DTO fields to the entity:

```java
// ✅ FIXED - Now includes these lines:
user.setAddress(dto.getAddress());
user.setDateOfBirth(dto.getDateOfBirth());
```

**Before:**
```java
public User registerUser(UserDTO dto) {
    User user = new User();
    user.setEmail(dto.getEmail());
    user.setName(dto.getName());
    user.setPhone(dto.getPhone());
    user.setSpecialization(dto.getSpecialization());
    user.setLicense(dto.getLicense());
    // ❌ MISSING: address and dateOfBirth
    return userRepository.save(user);
}
```

**After:**
```java
public User registerUser(UserDTO dto) {
    User user = new User();
    user.setEmail(dto.getEmail());
    user.setName(dto.getName());
    user.setPhone(dto.getPhone());
    user.setAddress(dto.getAddress());              // ✅ ADDED
    user.setDateOfBirth(dto.getDateOfBirth());      // ✅ ADDED
    user.setSpecialization(dto.getSpecialization());
    user.setLicense(dto.getLicense());
    return userRepository.save(user);
}
```

---

### Fix #2: Frontend Integration - CORS Configuration Missing

**File:** `SecurityConfig.java`

The Spring Security configuration didn't have CORS enabled, preventing frontend from connecting.

**Before:**
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        // ❌ NO CORS CONFIGURATION
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(...)
        .addFilterBefore(...);
    return http.build();
}
// ❌ NO CorsConfigurationSource BEAN
```

**After:**
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))  // ✅ ADDED
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(...)
        .addFilterBefore(...);
    return http.build();
}

// ✅ ADDED - This bean configures CORS
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

**What This Enables:**
- Frontend on ANY port/domain can call API
- Authorization header is exposed to frontend JavaScript
- Preflight OPTIONS requests are handled automatically
- All HTTP methods work (GET, POST, PUT, DELETE, PATCH)

---

### Fix #3: Wrong Port Number

**File:** `application.properties`

Changed server port from 8080 to 5000 to match original Flask application.

**Before:**
```properties
server.port=8080
```

**After:**
```properties
server.port=5000
server.compression.enabled=true
server.compression.mime-types=application/json,application/xml,text/html,text/xml,text/plain
```

---

## How to Test All Fixes

### Test #1: Database Field Persistence

**In Postman:**
```
POST http://localhost:5000/api/register
Content-Type: application/json

{
    "email": "alice@example.com",
    "password": "SecurePass123!",
    "name": "Alice Smith",
    "role": "patient",
    "phone": "+14155552671",
    "address": "456 Oak Avenue, San Francisco, CA 94102",
    "dateOfBirth": "1988-05-20"
}
```

**Expected Response:**
```json
{
    "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
    "user": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "email": "alice@example.com",
        "name": "Alice Smith",
        "role": "patient",
        "phone": "+14155552671",
        "address": "456 Oak Avenue, San Francisco, CA 94102",
        "dateOfBirth": "1988-05-20",
        "phoneVerified": false
    }
}
```

**Then Verify in Database:**
```bash
# Connect to PostgreSQL
psql -U uphill_user -d pranikov_uphill

# Check the data was saved
SELECT email, name, address, date_of_birth, phone FROM users WHERE email='alice@example.com';
```

**Expected Output:**
```
        email        |    name     |                address                 | date_of_birth |     phone
---------------------+-------------+---------------------------------------+---------------+-----------------
alice@example.com    | Alice Smith | 456 Oak Avenue, San Francisco, CA 94102 | 1988-05-20    | +14155552671
```

✅ If address and dateOfBirth are populated, the fix is working!

---

### Test #2: Frontend Integration (CORS)

**Test Option A: Browser Console**
```javascript
// Open browser console on frontend and test:
fetch('http://localhost:5000/api/register', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        email: 'test@example.com',
        password: 'password123',
        name: 'Test User',
        role: 'patient',
        phone: '+1234567890',
        address: '789 Elm Street',
        dateOfBirth: '1992-03-10'
    })
})
.then(r => r.json())
.then(data => console.log('Success!', data))
.catch(err => console.error('Error:', err));
```

**Expected Result:**
- ✅ No CORS errors in console
- ✅ Successful registration response with token

**Test Option B: Postman (CORS Headers Check)**

Send this request and check the response headers:
```
OPTIONS http://localhost:5000/api/register
```

Look for these response headers:
```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
Access-Control-Allow-Headers: *
Access-Control-Expose-Headers: Authorization, Content-Type
```

✅ If these headers are present, CORS is working!

---

### Test #3: Port Number

**Simple Connection Test:**
```bash
curl http://localhost:5000/api/doctors
```

Should return a list of doctors (not an error about port).

---

## Integration with Your Frontend

Update your frontend code to:

### 1. Use Port 5000
```javascript
// OLD
const API_BASE_URL = 'http://localhost:8080/api';

// NEW
const API_BASE_URL = 'http://localhost:5000/api';
```

### 2. Send Authorization Header for Protected Routes
```javascript
// Helper function to make authenticated requests
async function apiCall(endpoint, method = 'GET', body = null) {
    const token = localStorage.getItem('token');
    
    const options = {
        method,
        headers: {
            'Content-Type': 'application/json',
        }
    };
    
    // Add token for protected routes
    if (token) {
        options.headers['Authorization'] = `Bearer ${token}`;
    }
    
    if (body) {
        options.body = JSON.stringify(body);
    }
    
    const response = await fetch(`http://localhost:5000/api${endpoint}`, options);
    
    if (!response.ok) {
        throw new Error(`API Error: ${response.status}`);
    }
    
    return response.json();
}

// Usage:
// Register
const registerResponse = await apiCall('/register', 'POST', {
    email: 'user@example.com',
    password: 'password123',
    name: 'John Doe',
    address: '123 Main St',
    dateOfBirth: '1990-01-15'
});
localStorage.setItem('token', registerResponse.token);

// Get profile (protected)
const profile = await apiCall('/profile');
```

### 3. Handle Responses Properly
```javascript
// After successful login/register, save token
if (response.token) {
    localStorage.setItem('token', response.token);
    localStorage.setItem('user', JSON.stringify(response.user));
}

// When making subsequent requests, include token
const options = {
    headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
};
```

---

## Files Modified Summary

| File | Change | Type | Status |
|------|--------|------|--------|
| `UserService.java` | Added address & dateOfBirth mapping | Bug Fix | ✅ Complete |
| `SecurityConfig.java` | Added CORS configuration | Feature Add | ✅ Complete |
| `application.properties` | Changed port 8080→5000 | Config Update | ✅ Complete |

---

## Build & Run Instructions

### 1. Build the Project
```bash
cd c:\Users\Nikhil Kasyap\Downloads\demo (1)\demo
.\mvnw.cmd clean install -DskipTests
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXs
```

### 2. Start the Application
```bash
.\mvnw.cmd spring-boot:run
```

**Expected Output:**
```
Started PranikovApplication in X.XXX seconds
Tomcat started on port(s): 5000 (http)
```

### 3. Test with API Script
```bash
test-api.bat
```

This will automatically test:
- ✅ Connection to API
- ✅ User registration
- ✅ Authentication with JWT
- ✅ CORS headers

---

## Troubleshooting

### Problem: Still getting CORS error in browser
**Solution:**
1. Verify app is running on port 5000: `curl http://localhost:5000/api/doctors`
2. Hard refresh browser: `Ctrl+Shift+R` (or `Cmd+Shift+R` on Mac)
3. Clear cookies: Press `F12`, Application → Cookies → Delete all
4. Check that `Authorization: Bearer <token>` header is being sent

### Problem: 401 Unauthorized error
**Solution:**
1. Verify token was received from login/register response
2. Check token is in `Authorization: Bearer <token>` format (with space)
3. Verify token hasn't expired (default 30 days)
4. Try login again to get fresh token

### Problem: Database shows NULL for address/dateOfBirth
**Solution:**
1. Verify fields were sent in registration request
2. Check request body isn't malformed JSON
3. Look at application logs for errors
4. Drop and recreate database: `DROP DATABASE pranikov_uphill; CREATE DATABASE pranikov_uphill;`
5. Restart application to regenerate schema

### Problem: Port 5000 already in use
**Solution:**
```bash
# Find what's using port 5000
netstat -ano | findstr :5000

# Kill the process (replace PID with actual number)
taskkill /PID 1234 /F

# Or change port in application.properties
server.port=5001
```

---

## What Each Fix Does

### Fix #1: address & dateOfBirth fields
**Impact:** User registration now captures and saves complete profile information
**Test:** Register user, check database for fields

### Fix #2: CORS configuration
**Impact:** Frontend can now successfully call API without browser blocking
**Test:** Frontend registration/login works without CORS errors

### Fix #3: Port change to 5000
**Impact:** Frontend connects to correct port without configuration changes
**Test:** API accessible at http://localhost:5000/api/

---

## Verification Checklist

Run through these checks to confirm everything is working:

- [ ] Application builds successfully (`BUILD SUCCESS`)
- [ ] Application starts on port 5000 (`Tomcat started on port(s): 5000`)
- [ ] Can GET `/api/doctors` without auth: `curl http://localhost:5000/api/doctors`
- [ ] Can POST `/api/register` with all fields including address & dateOfBirth
- [ ] Response includes token and user data with all fields
- [ ] Database shows address and dateOfBirth populated
- [ ] Frontend can call API without CORS errors
- [ ] Frontend authentication (login/register) works
- [ ] Protected endpoints work with JWT token
- [ ] `test-api.bat` shows all tests passing

Once all checkmarks are complete ✅, you're ready for production testing!

---

## Next Steps

1. **Start the application:**
   ```bash
   .\mvnw.cmd spring-boot:run
   ```

2. **Run quick test:**
   ```bash
   test-api.bat
   ```

3. **Test your frontend:**
   - Update API base URL to port 5000
   - Test registration/login
   - Check browser console for errors

4. **Verify database:**
   - Check that address and dateOfBirth are saved
   - Verify user records are being created

5. **Integrate with frontend:**
   - Update API calls to use port 5000
   - Implement token storage (localStorage)
   - Add Authorization header to requests

---

## Summary

✅ **All issues have been identified and fixed**

The application now:
1. **Saves all user fields** including address and dateOfBirth
2. **Allows frontend integration** with proper CORS configuration
3. **Runs on port 5000** for consistency

**You can proceed with confidence!** The backend is ready for frontend integration.

---

**Date:** December 20, 2025
**Status:** ✅ READY FOR TESTING
**Next Action:** Start app and run `test-api.bat`
