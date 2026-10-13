package com.greenforce.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * "id", "title", "description" and "status" are named exactly like the objects hard-coded in
 * Admin.html, so that page can later read this API without changing its table code.
 * Reporter details are only filled in for admins.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IssueResponse(
        Long id,
        String trackingCode,
        String title,
        String category,
        String description,
        String status,
        String statusCode,
        String imageUrl,
        String location,
        Double latitude,
        Double longitude,
        String reporterName,
        String reporterEmail,
        Instant createdAt,
        Instant updatedAt) {
}
