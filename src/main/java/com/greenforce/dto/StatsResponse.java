package com.greenforce.dto;

import java.util.Map;

public record StatsResponse(long total, long issued, long verified, long inProgress, long solved, Map<String, Long> byCategory) {
}
