package com.dev.education_nearby_server.services;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendLyceumVerificationEmail(@NonNull String to,
                                            @NonNull String lyceumName,
                                            @NonNull String town,
                                            @NonNull String tokenValue) {
        String subject = "EducationNearby: Rights Verification Request";
        String text = "Hello,\n\n" +
                "A user has requested rights over the lyceum: '" + lyceumName + "' in '" + town + "'.\n" +
                "To proceed with the verification, please use the token below in your confirmation flow:\n\n" +
                "Token: " + tokenValue + "\n\n" +
                "If you were not expecting this request, you can ignore this email.\n\n" +
                "Regards,\nEducationNearby Team";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}

