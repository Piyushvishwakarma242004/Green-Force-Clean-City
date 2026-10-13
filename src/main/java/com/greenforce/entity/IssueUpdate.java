package com.greenforce.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** One row per status change: this is the "track progress" timeline of an issue. */
@Entity
@Table(name = "issue_updates")
public class IssueUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private IssueStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private IssueStatus newStatus;

    @Column(length = 500)
    private String note;

    @Column(name = "changed_by", length = 150)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt = Instant.now();

    protected IssueUpdate() {
    }

    public IssueUpdate(Issue issue, IssueStatus oldStatus, IssueStatus newStatus, String note, String changedBy) {
        this.issue = issue;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.note = note;
        this.changedBy = changedBy;
    }

    public Long getId() { return id; }
    public Issue getIssue() { return issue; }
    public IssueStatus getOldStatus() { return oldStatus; }
    public IssueStatus getNewStatus() { return newStatus; }
    public String getNote() { return note; }
    public String getChangedBy() { return changedBy; }
    public Instant getChangedAt() { return changedAt; }
}
