package com.dev.education_nearby_server.enums;

import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum FeedbackReadFilter {
    ALL,
    READ,
    UNREAD;

    public static FeedbackReadFilter from(String value) {
        if (!StringUtils.hasText(value)) {
            return ALL;
        }

        try {
            return FeedbackReadFilter.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            String supportedValues = Arrays.stream(values())
                    .map(filter -> filter.name().toLowerCase(Locale.ROOT))
                    .collect(Collectors.joining(", "));
            throw new BadRequestException("Feedback filter must be one of: " + supportedValues + ".");
        }
    }
}
