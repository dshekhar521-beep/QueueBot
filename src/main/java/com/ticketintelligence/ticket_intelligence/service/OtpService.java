package com.ticketintelligence.ticket_intelligence.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OtpService {

    private final Map<String, OtpEntry> otpStore =
            new ConcurrentHashMap<>();


    private static final Duration OTP_VALIDITY =
            Duration.ofMinutes(5);


    public String generateOtp(
            String identifier
    ) {

        String key =
                normalize(
                        identifier
                );


        String otp =
                String.format(
                        "%06d",
                        ThreadLocalRandom.current()
                                .nextInt(
                                        0,
                                        1_000_000
                                )
                );


        otpStore.put(
                key,
                new OtpEntry(
                        otp,
                        Instant.now()
                )
        );


        return otp;
    }


    public boolean verifyOtp(
            String identifier,
            String otp
    ) {

        if (
                identifier == null
                        || otp == null
        ) {

            return false;
        }


        String key =
                normalize(
                        identifier
                );


        OtpEntry entry =
                otpStore.get(
                        key
                );


        if (entry == null) {
            return false;
        }


        if (
                Instant.now()
                        .minus(
                                OTP_VALIDITY
                        )
                        .isAfter(
                                entry.createdAt()
                        )
        ) {

            otpStore.remove(
                    key
            );

            return false;
        }


        boolean valid =
                entry.otp()
                        .equals(
                                otp.trim()
                        );


        if (valid) {

            otpStore.remove(
                    key
            );
        }


        return valid;
    }


    private String normalize(
            String identifier
    ) {

        String value =
                identifier.trim();


        if (
                value.contains("@")
        ) {

            return value.toLowerCase();
        }


        String phone =
                value.replaceAll(
                        "[^0-9+]",
                        ""
                );


        if (
                phone.startsWith("+91")
        ) {

            phone =
                    phone.substring(3);
        }


        if (
                phone.startsWith("91")
                        && phone.length() == 12
        ) {

            phone =
                    phone.substring(2);
        }


        return phone;
    }


    private record OtpEntry(
            String otp,
            Instant createdAt
    ) {}
}