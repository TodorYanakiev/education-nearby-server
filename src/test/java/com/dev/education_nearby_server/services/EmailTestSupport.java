package com.dev.education_nearby_server.services;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;

import java.util.ArrayList;
import java.util.List;

final class EmailTestSupport {

    private EmailTestSupport() {
    }

    static EmailParts extractParts(MimeMessage message) throws Exception {
        EmailParts parts = new EmailParts();
        collectParts(message, parts);
        return parts;
    }

    private static void collectParts(Part part, EmailParts parts) throws Exception {
        if (part.isMimeType("text/plain")) {
            parts.plainText = (String) part.getContent();
            return;
        }
        if (part.isMimeType("text/html")) {
            parts.htmlText = (String) part.getContent();
            return;
        }
        if (part.isMimeType("image/*")) {
            parts.inlineParts.add(part);
            return;
        }
        if (Part.INLINE.equalsIgnoreCase(part.getDisposition())) {
            parts.inlineParts.add(part);
        }

        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                collectParts(multipart.getBodyPart(i), parts);
            }
        }
    }

    static final class EmailParts {
        private String plainText;
        private String htmlText;
        private final List<Part> inlineParts = new ArrayList<>();

        String plainText() {
            return plainText;
        }

        String htmlText() {
            return htmlText;
        }

        List<Part> inlineParts() {
            return inlineParts;
        }
    }
}
