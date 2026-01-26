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

import java.lang.reflect.Field;

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
        assertThat(message.getSubject()).isEqualTo("Школи: Потвърди права за администратор");

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
                Здравейте,
                
                Вие заявихте администраторски права за читалище 'Test Lyceum' в 'Sofia'.
                Ползвайте кода за потвърждение долу за да потвърдите читалището:
                
                Код за потвърждение: token-456
                
                Ако не сте заявявали права, моля обърнете се към нашия екип.
                
                Поздрави,
                екипът на Школи""";

        EmailTestSupport.EmailParts parts = EmailTestSupport.extractParts(message);
        assertThat(parts.plainText()).isEqualTo(expectedText);
    }

    @Test
    void sendLyceumLecturerInvitationEmailMasksLinksInHtml() throws Exception {
        emailService.sendLyceumLecturerInvitationEmail(
                "teacher@example.com",
                "Art Lyceum",
                "Sofia");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        MimeMessage message = messageCaptor.getValue();
        message.saveChanges();

        EmailTestSupport.EmailParts parts = EmailTestSupport.extractParts(message);
        assertThat(parts.plainText())
                .contains("https://shkoli.bg/auth/register")
                .contains("https://shkoli.bg/auth/login");
        assertThat(parts.htmlText())
                .contains(">Регистрация<")
                .contains(">Вход<")
                .doesNotContain(">https://shkoli.bg/auth/register<")
                .doesNotContain(">https://shkoli.bg/auth/login<");
        assertThat(parts.inlineParts())
                .extracting(Part::getContentType)
                .anySatisfy(contentType -> assertThat(contentType).contains("image/png"));
    }

    @Test
    void sendLyceumVerificationEmailFallsBackToSvgWhenPngMissing() throws Exception {
        setCachedLogoPng(new byte[0]);

        emailService.sendLyceumVerificationEmail(
                "admin@example.com",
                "Test Lyceum",
                "Sofia",
                "token-789");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        MimeMessage message = messageCaptor.getValue();
        message.saveChanges();

        EmailTestSupport.EmailParts parts = EmailTestSupport.extractParts(message);
        assertThat(parts.inlineParts())
                .extracting(Part::getContentType)
                .anySatisfy(contentType -> assertThat(contentType).contains("image/svg+xml"));
    }

    private void setCachedLogoPng(byte[] value) throws Exception {
        Field cachedLogoField = EmailService.class.getDeclaredField("cachedLogoPng");
        cachedLogoField.setAccessible(true);
        cachedLogoField.set(emailService, value);
    }
}
