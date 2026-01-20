package com.dev.education_nearby_server.services;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.MimeMessage;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
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
        Object content = part.getContent();
        String baseType = getBaseType(part);
        if ("text/plain".equalsIgnoreCase(baseType)) {
            String text = readTextContent(content, part.getContentType());
            if (text != null && parts.plainText == null) {
                parts.plainText = text;
                return;
            }
        }
        if ("text/html".equalsIgnoreCase(baseType)) {
            String html = readTextContent(content, part.getContentType());
            if (html != null && parts.htmlText == null) {
                parts.htmlText = html;
                return;
            }
        }
        if (content instanceof String text && parts.htmlText == null) {
            if (looksLikeHtml(text) && !"text/plain".equalsIgnoreCase(baseType)) {
                parts.htmlText = text;
                return;
            }
        }
        if (part.isMimeType("image/*")) {
            parts.inlineParts.add(part);
            return;
        }
        if (Part.INLINE.equalsIgnoreCase(part.getDisposition())) {
            parts.inlineParts.add(part);
        }

        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                collectParts(multipart.getBodyPart(i), parts);
            }
        }
    }

    private static String getBaseType(Part part) {
        try {
            return new ContentType(part.getContentType()).getBaseType();
        } catch (Exception e) {
            return null;
        }
    }

    private static String readTextContent(Object content, String contentType) throws Exception {
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof Reader reader) {
            return readAll(reader);
        }
        if (content instanceof InputStream inputStream) {
            return new String(inputStream.readAllBytes(), resolveCharset(contentType));
        }
        if (content instanceof byte[] bytes) {
            return new String(bytes, resolveCharset(contentType));
        }
        return null;
    }

    private static String readAll(Reader reader) throws Exception {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[1024];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            builder.append(buffer, 0, read);
        }
        return builder.toString();
    }

    private static Charset resolveCharset(String contentType) {
        try {
            ContentType parsed = new ContentType(Objects.requireNonNullElse(contentType, ""));
            String charset = parsed.getParameter("charset");
            if (charset != null && !charset.isBlank()) {
                return Charset.forName(charset);
            }
        } catch (Exception ignored) {
        }
        return StandardCharsets.UTF_8;
    }

    private static boolean looksLikeHtml(String text) {
        String lowered = text.toLowerCase();
        return lowered.contains("<html") || lowered.contains("<body") || lowered.contains("<br");
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
