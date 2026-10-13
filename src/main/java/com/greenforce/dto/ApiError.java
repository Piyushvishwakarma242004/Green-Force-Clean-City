package com.greenforce.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(Instant timestamp, int status, String message, Map<String, String> errors) {

    public static ApiError of(int status, String message) {
        return new ApiError(Instant.now(), status, message, null);
    }
}
