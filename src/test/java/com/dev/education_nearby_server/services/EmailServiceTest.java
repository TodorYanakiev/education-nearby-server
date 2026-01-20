package com.dev.education_nearby_server.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    @Test
    void sendLyceumVerificationEmailBuildsExpectedMessage() throws Exception {
        emailService.sendLyceumVerificationEmail(
                "admin@example.com",
                "Test Lyceum",
                "Sofia",
                "token-123");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        MimeMessage message = messageCaptor.getValue();
        message.saveChanges();

        assertThat(((InternetAddress) message.getAllRecipients()[0]).getAddress())
                .isEqualTo("admin@example.com");
        assertThat(message.getSubject()).isEqualTo("EducationNearby: Verify Lyceum Administration Rights");

        EmailTestSupport.EmailParts parts = EmailTestSupport.extractParts(message);
        assertThat(parts.plainText())
                .contains("Test Lyceum")
                .contains("Sofia")
                .contains("token-123");
        assertThat(parts.htmlText()).contains("cid:educationNearbyLogo");
        assertThat(parts.inlineParts())
                .extracting(Part::getContentType)
                .anySatisfy(contentType -> assertThat(contentType).contains("image/png"));
    }

    @Test
    void sendLyceumVerificationEmailProducesTemplatedBody() throws Exception {
        emailService.sendLyceumVerificationEmail(
                "admin@example.com",
                "Test Lyceum",
                "Sofia",
                "token-456");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        MimeMessage message = messageCaptor.getValue();
        message.saveChanges();

        String expectedText = """
                Hello,
                
                You requested administrator rights for the lyceum 'Test Lyceum' in 'Sofia'.
                Use the verification code below to complete your request:
                
                Verification code: token-456
                
                If you did not initiate this request, please contact our support team immediately.
                
                Regards,
                EducationNearby Team""";

        EmailTestSupport.EmailParts parts = EmailTestSupport.extractParts(message);
        assertThat(parts.plainText()).isEqualTo(expectedText);
    }
}
