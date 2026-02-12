package com.dev.education_nearby_server.models.entity;

import com.dev.education_nearby_server.enums.ImageRole;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * Common image metadata fields persisted for course and lyceum images.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class ImageMetadata implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "s3_key", nullable = false, unique = true)
    private String s3Key;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageRole role;

    private String altText;
    private Integer width;
    private Integer height;
    private String mimeType;
    private Integer orderIndex = 0;
}
