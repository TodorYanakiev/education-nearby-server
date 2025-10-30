package com.dev.education_nearby_server.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
    }

    @Test
    void sendLyceumVerificationEmailBuildsExpectedMessage() {
        emailService.sendLyceumVerificationEmail(
                "admin@example.com",
                "Test Lyceum",
                "Sofia",
                "token-123");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        assertThat(message.getTo()).containsExactly("admin@example.com");
        assertThat(message.getSubject()).isEqualTo("EducationNearby: Verify Lyceum Administration Rights");
        assertThat(message.getText())
                .contains("Test Lyceum")
                .contains("Sofia")
                .contains("token-123");
    }

    @Test
    void sendLyceumVerificationEmailProducesTemplatedBody() {
        emailService.sendLyceumVerificationEmail(
                "admin@example.com",
                "Test Lyceum",
                "Sofia",
                "token-456");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        String expectedText = """
                Hello,
                
                You requested administrator rights for the lyceum 'Test Lyceum' in 'Sofia'.
                Use the verification code below to complete your request:
                
                Verification code: token-456
                
                If you did not initiate this request, please contact our support team immediately.
                
                Regards,
                EducationNearby Team""";

        assertThat(message.getText()).isEqualTo(expectedText);
    }
}
