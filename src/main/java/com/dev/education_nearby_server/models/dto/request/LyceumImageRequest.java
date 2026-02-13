package com.dev.education_nearby_server.models.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Payload for registering or updating a lyceum image reference (S3 key or URL) with metadata.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class LyceumImageRequest extends ImageMetadataRequest {
}
