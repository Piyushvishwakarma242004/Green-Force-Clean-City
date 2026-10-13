package com.greenforce.dto;

import com.greenforce.entity.IssueStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusUpdateRequest(@NotNull IssueStatus status, @Size(max = 500) String note) {
}
