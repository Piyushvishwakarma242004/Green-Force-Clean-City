package com.greenforce.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "issues", indexes = {
        @Index(name = "idx_issue_status", columnList = "status"),
        @Index(name = "idx_issue_category", columnList = "category")
})
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_code", nullable = false, unique = true, length = 20)
    private String trackingCode;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "reporter_name", length = 100)
    private String reporterName;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "image_path", length = 100)
    private String imagePath;

    /** Raw text sent by Report.html, e.g. "Lat: 23.18, Lon: 79.98" */
    @Column(length = 255)
    private String location;

    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssueStatus status = IssueStatus.ISSUED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public IssueStatus getStatus() { return status; }
    public void setStatus(IssueStatus status) { this.status = status; }
    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
