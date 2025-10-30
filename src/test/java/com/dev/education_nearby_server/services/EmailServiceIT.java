package com.dev.education_nearby_server.services;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest
class EmailServiceIT {

    @Autowired
    private EmailService emailService;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void sendLyceumVerificationEmailUsesConfiguredBean() {
        emailService.sendLyceumVerificationEmail(
                "admin@example.com",
                "Integration Lyceum",
                "Sofia",
                "integration-token");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        assertThat(message.getTo()).containsExactly("admin@example.com");
        assertThat(message.getSubject()).isEqualTo("EducationNearby: Verify Lyceum Administration Rights");
        assertThat(message.getText())
                .contains("Integration Lyceum")
                .contains("Sofia")
                .contains("integration-token");
    }

    @Test
    void sendLyceumVerificationEmailBuildsFormattedBody() {
        emailService.sendLyceumVerificationEmail(
                "admin@example.com",
                "Integration Lyceum",
                "Sofia",
                "integration-token");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        String expectedText = """
                Hello,
                
                You requested administrator rights for the lyceum 'Integration Lyceum' in 'Sofia'.
                Use the verification code below to complete your request:
                
                Verification code: integration-token
                
                If you did not initiate this request, please contact our support team immediately.
                
                Regards,
                EducationNearby Team""";

        assertThat(message.getText()).isEqualTo(expectedText);
    }
}
