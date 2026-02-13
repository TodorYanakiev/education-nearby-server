package com.dev.education_nearby_server.models.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Image metadata stored for a lyceum, either as an S3 key or public URL.
 */
@Entity
@Table(name = "lyceum_images")
@Getter
@Setter
@NoArgsConstructor
public class LyceumImage extends ImageMetadata {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lyceum_id")
    private Lyceum lyceum;
}
