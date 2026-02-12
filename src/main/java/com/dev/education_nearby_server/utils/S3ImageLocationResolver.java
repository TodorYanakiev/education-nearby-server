package com.dev.education_nearby_server.utils;

import com.dev.education_nearby_server.config.S3Properties;
import com.dev.education_nearby_server.exceptions.common.ValidationException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Shared helper for resolving and validating S3 image keys and URLs.
 */
public class S3ImageLocationResolver {

    private S3ImageLocationResolver() {
    }

    public static ResolvedImageLocation resolveAndValidate(
            String s3Key,
            String url,
            String requiredPrefix,
            S3Properties s3Properties
    ) {
        String resolvedKey = resolveS3Key(s3Key, url, s3Properties);
        String resolvedUrl = resolveUrl(url, resolvedKey, s3Properties);
        validateS3Location(resolvedUrl, resolvedKey, requiredPrefix, s3Properties);
        return new ResolvedImageLocation(resolvedKey, resolvedUrl);
    }

    private static String resolveS3Key(String s3Key, String url, S3Properties s3Properties) {
        String explicitKey = trimToNull(s3Key);
        String trimmedUrl = trimToNull(url);

        if (StringUtils.hasText(explicitKey)) {
            return explicitKey;
        }
        if (StringUtils.hasText(trimmedUrl)) {
            return extractKeyFromUrl(trimmedUrl, s3Properties);
        }
        throw new ValidationException("Either url or s3Key must be provided.");
    }

    private static String resolveUrl(String url, String resolvedKey, S3Properties s3Properties) {
        String trimmedUrl = trimToNull(url);
        if (StringUtils.hasText(trimmedUrl)) {
            parseUri(trimmedUrl);
            return trimmedUrl;
        }
        if (!StringUtils.hasText(resolvedKey)) {
            throw new ValidationException("Could not resolve an image URL without a valid S3 key.");
        }
        return buildUrlFromKey(resolvedKey, s3Properties);
    }

    private static void validateS3Location(String url, String key, String requiredPrefix, S3Properties s3Properties) {
        if (!StringUtils.hasText(key)) {
            throw new ValidationException("S3 key cannot be empty.");
        }

        String prefix = trimToNull(requiredPrefix);
        if (StringUtils.hasText(prefix) && !key.startsWith(prefix)) {
            throw new ValidationException("S3 key must start with the configured prefix: " + prefix);
        }

        URI uri = parseUri(url);
        String bucketName = trimToNull(s3Properties.getBucketName());
        String normalizedPath = normalizePath(uri.getPath());

        if (!StringUtils.hasText(bucketName)) {
            if (StringUtils.hasText(normalizedPath) && !normalizedPath.equals(key)) {
                throw new ValidationException("Resolved S3 key does not match the key extracted from the URL.");
            }
            return;
        }

        String host = uri.getHost();
        String allowedHost = extractHost(trimToNull(s3Properties.getPublicBaseUrl()));
        boolean bucketInHost = host != null && host.contains(bucketName);
        boolean bucketInPath = pathStartsWithBucket(uri.getPath(), bucketName);
        boolean matchesAllowedHost = host != null && allowedHost != null && host.equalsIgnoreCase(allowedHost);

        if (!bucketInHost && !bucketInPath && !matchesAllowedHost) {
            throw new ValidationException("Image URL must point to bucket " + bucketName + ".");
        }

        String derivedKey = deriveKeyFromPath(uri.getPath(), bucketName);
        if (StringUtils.hasText(derivedKey) && !derivedKey.equals(key)) {
            throw new ValidationException("Resolved S3 key does not match the key extracted from the URL.");
        }
    }

    private static String buildUrlFromKey(String key, S3Properties s3Properties) {
        String baseUrl = trimToNull(s3Properties.getPublicBaseUrl());
        if (!StringUtils.hasText(baseUrl)) {
            String bucketName = trimToNull(s3Properties.getBucketName());
            if (!StringUtils.hasText(bucketName)) {
                throw new ValidationException("Provide a full image URL or configure app.s3.public-base-url.");
            }
            baseUrl = "https://" + bucketName + ".s3.amazonaws.com/";
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        return baseUrl + key;
    }

    private static String extractKeyFromUrl(String url, S3Properties s3Properties) {
        URI uri = parseUri(url);
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            throw new ValidationException("Image URL must contain an object path.");
        }
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        String bucketName = trimToNull(s3Properties.getBucketName());
        if (StringUtils.hasText(bucketName) && normalizedPath.startsWith(bucketName + "/")) {
            normalizedPath = normalizedPath.substring(bucketName.length() + 1);
        }
        if (!StringUtils.hasText(normalizedPath)) {
            throw new ValidationException("Could not extract an S3 key from the provided URL.");
        }
        return normalizedPath;
    }

    private static URI parseUri(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new ValidationException("Provided URL is not valid.");
        }
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private static boolean pathStartsWithBucket(String path, String bucketName) {
        return StringUtils.hasText(path) && StringUtils.hasText(bucketName) && path.startsWith("/" + bucketName + "/");
    }

    private static String deriveKeyFromPath(String path, String bucketName) {
        String normalized = normalizePath(path);
        if (!StringUtils.hasText(normalized)) {
            return normalized;
        }
        if (StringUtils.hasText(bucketName) && normalized.startsWith(bucketName + "/")) {
            normalized = normalized.substring(bucketName.length() + 1);
        }
        return normalized;
    }

    private static String extractHost(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (URISyntaxException ignored) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        String trimmed = value == null ? null : value.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    @Getter
    @RequiredArgsConstructor
    public static class ResolvedImageLocation {
        private final String s3Key;
        private final String url;
    }
}
