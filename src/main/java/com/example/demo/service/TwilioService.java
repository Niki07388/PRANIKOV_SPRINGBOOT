package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.InitializingBean;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import java.util.regex.Pattern;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TwilioService implements InitializingBean {
    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.phone-number:}")
    private String twilioPhoneNumber;

    @Value("${twilio.messaging-service-sid:}")
    private String messagingServiceSid;

    @Value("${default.country.code:+1}")
    private String defaultCountryCode;

    private boolean isInitialized = false;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (accountSid != null && !accountSid.isEmpty() && authToken != null && !authToken.isEmpty()) {
            com.twilio.Twilio.init(accountSid, authToken);
            isInitialized = true;
            System.out.println("[Twilio] Initialized successfully with Account SID: " + accountSid.substring(0, Math.min(4, accountSid.length())) + "...");
        } else {
            System.err.println("[Twilio] WARNING: Credentials not configured. SMS sending will fail.");
        }
    }

    public boolean sendSmsViaTwilio(String toPhone, String body) {
        try {
            if (!isInitialized) {
                System.err.println("[Twilio] Not initialized. Check TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN.");
                return false;
            }

            String toE164 = normalizePhoneE164(toPhone);
            if (toE164 == null) {
                System.err.println("[Twilio] Failed to normalize phone: " + toPhone);
                return false;
            }

            System.out.println("[Twilio] Sending SMS to: " + toE164 + " | Body: " + body.substring(0, Math.min(50, body.length())));

            if (messagingServiceSid != null && !messagingServiceSid.isEmpty()) {
                Message message = Message.creator(
                        new PhoneNumber(toE164),
                        messagingServiceSid,
                        body
                ).create();
                System.out.println("[Twilio] SMS sent successfully via Messaging Service. SID: " + message.getSid());
            } else if (twilioPhoneNumber != null && !twilioPhoneNumber.isEmpty()) {
                Message message = Message.creator(
                        new PhoneNumber(toE164),
                        new PhoneNumber(twilioPhoneNumber),
                        body
                ).create();
                System.out.println("[Twilio] SMS sent successfully from: " + twilioPhoneNumber + " to " + toE164 + " | SID: " + message.getSid());
            } else {
                System.err.println("[Twilio] Neither Messaging Service SID nor Phone Number configured");
                return false;
            }

            return true;
        } catch (Exception e) {
            System.err.println("[Twilio] SMS send failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public String normalizePhoneE164(String phone) {
        if (phone == null || phone.isEmpty()) {
            System.err.println("[Twilio] Phone number is null or empty");
            return null;
        }

        phone = phone.trim();
        if (phone.startsWith("+")) {
            return phone;
        }

        // Extract only digits
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            System.err.println("[Twilio] No digits found in phone: " + phone);
            return null;
        }

        if (defaultCountryCode != null && !defaultCountryCode.isEmpty()) {
            String cc = defaultCountryCode.trim();
            if (cc.startsWith("+")) {
                String result = cc + digits;
                System.out.println("[Twilio] Normalized phone (with country code): " + result);
                return result;
            } else {
                String result = "+" + cc + digits;
                System.out.println("[Twilio] Normalized phone (with country code): " + result);
                return result;
            }
        }

        String result = "+" + digits;
        System.out.println("[Twilio] Normalized phone (default): " + result);
        return result;
    }

    public Optional<com.twilio.rest.api.v2010.account.Message> receiveSms(String fromNumber, String toNumber, String body) {
        // This is typically called from the webhook
        System.out.println("[Twilio SMS] From=" + fromNumber + " To=" + toNumber + " Body=" + body);
        return Optional.empty();
    }
}