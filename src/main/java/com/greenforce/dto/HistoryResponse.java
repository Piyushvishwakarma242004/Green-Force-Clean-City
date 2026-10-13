package com.greenforce.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HistoryResponse(String from, String to, String note, Instant at) {
}
