package com.dev.education_nearby_server.services;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Helper service for sending outbound emails related to platform workflows.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Sends a verification email containing a token for lyceum administration claims.
     *
     * @param to recipient email address
     * @param lyceumName target lyceum name
     * @param town lyceum town for context
     * @param tokenValue verification code to be used during confirmation
     */
    public void sendLyceumVerificationEmail(@NonNull String to,
                                            @NonNull String lyceumName,
                                            @NonNull String town,
                                            @NonNull String tokenValue) {
        String subject = "EducationNearby: Verify Lyceum Administration Rights";
        String text = "Hello,\n\n" +
                "You requested administrator rights for the lyceum '" + lyceumName + "' in '" + town + "'.\n" +
                "Use the verification code below to complete your request:\n\n" +
                "Verification code: " + tokenValue + "\n\n" +
                "If you did not initiate this request, please contact our support team immediately.\n\n" +
                "Regards,\nEducationNearby Team";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
