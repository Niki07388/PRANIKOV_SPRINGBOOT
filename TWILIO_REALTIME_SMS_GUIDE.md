# Twilio Real-time SMS Configuration Guide

## ✅ Quick Checklist

- [ ] Twilio Account created and credentials obtained
- [ ] Environment variables set in system
- [ ] Phone number normalized correctly
- [ ] SMS delivery verified in Twilio logs
- [ ] CORS configured for webhook (if applicable)

---

## 1. Setup Twilio Account

1. Go to [Twilio Console](https://www.twilio.com/console)
2. Get your credentials:
   - **Account SID**: Located in Dashboard
   - **Auth Token**: Located in Dashboard (keep secret!)
   - **Phone Number**: Twilio phone number assigned to your account (e.g., +1234567890)
   - **Messaging Service SID** (optional): For better SMS reliability

---

## 2. Set Environment Variables

### Windows (PowerShell - RECOMMENDED)

```powershell
# Run PowerShell as Administrator
[Environment]::SetEnvironmentVariable("TWILIO_ACCOUNT_SID", "ACxxxxxxxxxxxxxxxx", "User")
[Environment]::SetEnvironmentVariable("TWILIO_AUTH_TOKEN", "your_auth_token_here", "User")
[Environment]::SetEnvironmentVariable("TWILIO_PHONE_NUMBER", "+1234567890", "User")
[Environment]::SetEnvironmentVariable("DEFAULT_COUNTRY_CODE", "+1", "User")

# Verify
Get-ChildItem env:TWILIO*
```

### Windows (Command Prompt)

```cmd
setx TWILIO_ACCOUNT_SID "ACxxxxxxxxxxxxxxxx"
setx TWILIO_AUTH_TOKEN "your_auth_token_here"
setx TWILIO_PHONE_NUMBER "+1234567890"
setx DEFAULT_COUNTRY_CODE "+1"
```

### Restart Application
After setting environment variables, **restart your Spring Boot application** for changes to take effect.

---

## 3. Verify Configuration in application.properties

Check `src/main/resources/application.properties`:

```properties
# Twilio Configuration (should read from environment variables)
twilio.account-sid=${TWILIO_ACCOUNT_SID:}
twilio.auth-token=${TWILIO_AUTH_TOKEN:}
twilio.phone-number=${TWILIO_PHONE_NUMBER:}
twilio.messaging-service-sid=${TWILIO_MESSAGING_SERVICE_SID:}

# Country code for phone normalization (default: +1 for US)
default.country.code=${DEFAULT_COUNTRY_CODE:+1}
```

---

## 4. SMS Send Workflow

### Via Phone Verification OTP
```
1. User requests OTP: POST /api/phone/send-otp
2. PhoneVerificationService generates 6-digit OTP
3. TwilioService normalizes phone number to E.164 format
4. SMS sent via Twilio API
5. Logs show: "[Twilio] SMS sent successfully | SID: SMxxxxxxxx"
```

### Via Admin Message Send
```
1. Admin sends message: POST /api/messages/send
   {
     "to": "5551234567",
     "body": "Your appointment is confirmed"
   }
2. Phone normalized to E.164: +15551234567
3. SMS sent via Twilio
4. Response returns success/failure
```

---

## 5. Phone Number Normalization

### E.164 Format (Required by Twilio)
- Format: `+[country code][phone number]`
- Example: `+1 (555) 123-4567` → `+15551234567`

### Configuration
```properties
default.country.code=${DEFAULT_COUNTRY_CODE:+1}
```

**Adjust country code based on your region:**
- USA/Canada: `+1`
- UK: `+44`
- India: `+91`
- Germany: `+49`
- etc.

---

## 6. Testing Real-time SMS

### Test 1: Via Phone Verification
```bash
# Endpoint: POST /api/phone/send-otp
curl -X POST http://localhost:5000/api/phone/send-otp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer [your_jwt_token]" \
  -d '{"phone": "5551234567"}'
```

Expected Console Output:
```
[Twilio] Normalized phone (with country code): +15551234567
[Twilio] Sending SMS to: +15551234567 | Body: Your verification code is 123456
[Twilio] SMS sent successfully from: +1234567890 to +15551234567 | SID: SMxxxxxxxx
```

### Test 2: Via Admin Message
```bash
# Endpoint: POST /api/messages/send
curl -X POST http://localhost:5000/api/messages/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer [your_jwt_token]" \
  -d '{
    "to": "5551234567",
    "body": "Test message from Pranikov"
  }'
```

---

## 7. Common Issues & Solutions

### ❌ "Twilio credentials not configured"
**Solution**: 
- Set environment variables (see Section 2)
- Restart the application
- Check logs: Look for `[Twilio] Initialized successfully`

### ❌ "Failed to normalize phone"
**Solution**:
- Verify phone is in format: `5551234567` or `(555) 123-4567`
- Check `DEFAULT_COUNTRY_CODE` is set correctly
- Ensure phone has 10-15 digits

### ❌ SMS not received by recipient
**Solution**:
- Verify recipient phone is correct in Twilio logs
- Check SMS balance in Twilio account
- Verify Twilio phone number is active
- Check phone is not in Twilio block list
- Ensure recipient region is supported by Twilio

### ❌ "Invalid phone number" error
**Solution**:
- Phone must have at least 10 digits
- Remove country code from input (app adds it automatically)
- Example: Send `5551234567` NOT `+15551234567`

---

## 8. Monitoring SMS Delivery

### In Application Logs
Look for these success indicators:
```
[Twilio] Initialized successfully with Account SID: ACxx...
[Twilio] Normalized phone: +15551234567
[Twilio] Sending SMS to: +15551234567
[Twilio] SMS sent successfully | SID: SMxxxxxxxx
```

### In Twilio Console
1. Go to [Twilio Console Logs](https://www.twilio.com/console/sms/logs)
2. View SMS delivery status
3. Check for errors or delivery failures

---

## 9. Real-time Features Enabled

✅ **Instant OTP Delivery**: SMS sent within 1-2 seconds  
✅ **Webhook Support**: Inbound SMS at `/api/twilio/inbound`  
✅ **Phone Normalization**: Automatic E.164 conversion  
✅ **Error Logging**: Detailed error messages in console  
✅ **Retry Logic**: Kafka can retry failed SMS sends  

---

## 10. Production Checklist

Before going live:

- [ ] Use Messaging Service SID for better reliability
- [ ] Set up Twilio retry policy
- [ ] Configure backup SMS provider
- [ ] Monitor Twilio costs
- [ ] Set up alerts for SMS failures
- [ ] Test with real phone numbers in target region
- [ ] Document supported countries
- [ ] Store JWT tokens securely
- [ ] Use HTTPS for webhook (not HTTP)
- [ ] Rate limit SMS endpoint to prevent abuse

---

## Support

For Twilio issues:
- [Twilio SMS Troubleshooting](https://support.twilio.com/hc/en-us/articles/223183968)
- [Twilio API Docs](https://www.twilio.com/docs/sms/api)

For application issues:
- Check console logs for `[Twilio]` prefix messages
- Verify environment variables are set
- Restart the application after configuration changes
