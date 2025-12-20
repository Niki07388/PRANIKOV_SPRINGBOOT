package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import java.util.regex.Pattern;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TwilioService {
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

    public boolean sendSmsViaTwilio(String toPhone, String body) {
        try {
            if (accountSid == null || accountSid.isEmpty() || authToken == null || authToken.isEmpty()) {
                System.out.println("Twilio credentials not configured");
                return false;
            }

            String toE164 = normalizePhoneE164(toPhone);
            if (toE164 == null) {
                return false;
            }

            com.twilio.Twilio.init(accountSid, authToken);

            if (messagingServiceSid != null && !messagingServiceSid.isEmpty()) {
                Message.creator(
                        new PhoneNumber(toE164),  // Convert to PhoneNumber
                        messagingServiceSid,
                        body
                ).create();
            } else if (twilioPhoneNumber != null && !twilioPhoneNumber.isEmpty()) {
                Message.creator(
                        new PhoneNumber(toE164),  // Convert to PhoneNumber
                        new PhoneNumber(twilioPhoneNumber),
                        body
                ).create();
            } else {
                return false;
            }

            return true;
        } catch (Exception e) {
            System.err.println("Twilio SMS send failed: " + e.getMessage());
            return false;
        }
    }

    public String normalizePhoneE164(String phone) {
        if (phone == null || phone.isEmpty()) {
            return null;
        }

        phone = phone.trim();
        if (phone.startsWith("+")) {
            return phone;
        }

        // Extract only digits
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }

        if (defaultCountryCode != null && !defaultCountryCode.isEmpty()) {
            String cc = defaultCountryCode.trim();
            if (cc.startsWith("+")) {
                return cc + digits;
            } else {
                return "+" + cc + digits;
            }
        }

        return "+" + digits;
    }

    public Optional<com.twilio.rest.api.v2010.account.Message> receiveSms(String fromNumber, String toNumber, String body) {
        // This is typically called from the webhook
        System.out.println("[Twilio SMS] From=" + fromNumber + " To=" + toNumber + " Body=" + body);
        return Optional.empty();
    }
}