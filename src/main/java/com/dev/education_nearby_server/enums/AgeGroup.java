package com.dev.education_nearby_server.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgeGroup {
    TODDLER("0-6"),
    CHILD("7-12"),
    TEEN("13-18"),
    ADULT("18+"),
    SENIOR("SENIOR");

    private final String label;
}
