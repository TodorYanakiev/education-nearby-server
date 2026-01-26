package com.dev.education_nearby_server.services;

import org.junit.jupiter.api.Test;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class EmailServiceIT {

    @Autowired
    private EmailService emailService;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void sendLyceumVerificationEmailUsesConfiguredBean() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        emailService.sendLyceumVerificationEmail(
                "admin@example.com",
                "Integration Lyceum",
                "Sofia",
                "integration-token");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        MimeMessage message = messageCaptor.getValue();
        message.saveChanges();

        assertThat(((InternetAddress) message.getAllRecipients()[0]).getAddress())
                .isEqualTo("admin@example.com");
        assertThat(message.getSubject()).isEqualTo("Школи: Потвърди права за администратор");

        EmailTestSupport.EmailParts parts = EmailTestSupport.extractParts(message);
        assertThat(parts.plainText())
                .contains("Integration Lyceum")
                .contains("Sofia")
                .contains("integration-token");
        assertThat(parts.htmlText()).contains("cid:educationNearbyLogo");
        assertThat(parts.inlineParts())
                .extracting(Part::getContentType)
                .anySatisfy(contentType -> assertThat(contentType).contains("image/png"));
    }

    @Test
    void sendLyceumVerificationEmailBuildsFormattedBody() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        emailService.sendLyceumVerificationEmail(
                "admin@example.com",
                "Integration Lyceum",
                "Sofia",
                "integration-token");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        MimeMessage message = messageCaptor.getValue();
        message.saveChanges();

        String expectedText = """
                Здравейте,
                
                Вие заявихте администраторски права за читалище 'Integration Lyceum' в 'Sofia'.
                Ползвайте кода за потвърждение долу за да потвърдите читалището:
                
                Код за потвърждение: integration-token
                
                Ако не сте заявявали права, моля обърнете се към нашия екип.
                
                Поздрави,
                екипът на Школи""";

        EmailTestSupport.EmailParts parts = EmailTestSupport.extractParts(message);
        assertThat(parts.plainText()).isEqualTo(expectedText);
    }
}
