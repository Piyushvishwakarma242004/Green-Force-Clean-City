package com.greenforce.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Labels match the ones already used by Admin.html: "Issued", "In Progress", "Solved" (plus "Verified").
 */
public enum IssueStatus {
    ISSUED("Issued"),
    VERIFIED("Verified"),
    IN_PROGRESS("In Progress"),
    SOLVED("Solved");

    private final String label;

    IssueStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Accepts "IN_PROGRESS", "in progress", "In-Progress", ... */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static IssueStatus parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        try {
            return IssueStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown status '" + value + "'. Use one of: ISSUED, VERIFIED, IN_PROGRESS, SOLVED");
        }
    }
}
