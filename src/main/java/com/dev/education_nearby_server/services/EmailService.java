package com.dev.education_nearby_server.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.NonNull;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Helper service for sending outbound emails related to platform workflows.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private static final String LOGO_CONTENT_ID = "educationNearbyLogo";
    private static final String LOGO_RESOURCE = "static/logo.svg";
    private static final String REGISTER_URL = "https://shkoli.bg/auth/register";
    private static final String LOGIN_URL = "https://shkoli.bg/auth/login";
    private static final String REGISTER_LABEL = "Регистрация";
    private static final String LOGIN_LABEL = "Вход";
    private static final float LOGO_WIDTH_PX = 120f;
    private volatile byte[] cachedLogoPng;

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

        sendEmailWithLogo(to, subject, text);
    }

    /**
     * Sends an invitation email to join a lyceum as a lecturer.
     *
     * @param to recipient email address
     * @param lyceumName target lyceum name
     * @param town lyceum town for context
     */
    public void sendLyceumLecturerInvitationEmail(@NonNull String to,
                                                  @NonNull String lyceumName,
                                                  @NonNull String town) {
        String subject = "Школи.бг: покана за преподавател";
        String text = "Здравейте,\n\n" +
                "Вие сте поканени да се присъедините към Школи като преподавател към читалище '" + lyceumName +
                "' в " + town + ".\n\n" +
                "Ако все още нямате акаунт, Ви молим да се регистрирате с тази е-поща: " + REGISTER_URL + ". " +
                "След като се регистрирате, Вие автоматично ще бъдете добавен като преподавател към читалището.\n\n" +
                "Ако вече имате акаунт, може да влезете и да започнете да обучавате: " + LOGIN_URL + ".\n\n" +
                "Поздрави,\nекипът на Школи";

        sendEmailWithLogo(to, subject, text);
    }

    private void sendEmailWithLogo(String to, String subject, String text) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_RELATED,
                    StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, buildHtmlBody(text));
            addInlineLogo(helper);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to build email message.", e);
        }
        mailSender.send(message);
    }

    private String buildHtmlBody(String text) {
        String escapedText = HtmlUtils.htmlEscape(text)
                .replace(REGISTER_URL, "<a href=\"" + REGISTER_URL + "\">" + REGISTER_LABEL + "</a>")
                .replace(LOGIN_URL, "<a href=\"" + LOGIN_URL + "\">" + LOGIN_LABEL + "</a>")
                .replace("\n", "<br>");
        return """
                <html>
                  <body style="font-family: Arial, sans-serif; color: #1f2937;">
                    <div>
                      <img src="cid:%s" alt="Shkoli" style="width: 120px; height: auto; margin-bottom: 16px;">
                    </div>
                    <div>%s</div>
                  </body>
                </html>
                """.formatted(LOGO_CONTENT_ID, escapedText);
    }

    private void addInlineLogo(MimeMessageHelper helper) throws MessagingException {
        byte[] logoPng = loadLogoPng();
        if (logoPng != null && logoPng.length > 0) {
            helper.addInline(LOGO_CONTENT_ID, new ByteArrayResource(logoPng), "image/png");
            return;
        }
        helper.addInline(LOGO_CONTENT_ID, new ClassPathResource(LOGO_RESOURCE), "image/svg+xml");
    }

    private byte[] loadLogoPng() {
        byte[] cached = cachedLogoPng;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (cachedLogoPng != null) {
                return cachedLogoPng;
            }
            cachedLogoPng = transcodeSvgToPng();
            return cachedLogoPng;
        }
    }

    private byte[] transcodeSvgToPng() {
        ClassPathResource resource = new ClassPathResource(LOGO_RESOURCE);
        try (InputStream inputStream = resource.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PNGTranscoder transcoder = new PNGTranscoder();
            transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, LOGO_WIDTH_PX);
            transcoder.transcode(new TranscoderInput(inputStream), new TranscoderOutput(outputStream));
            return outputStream.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}
