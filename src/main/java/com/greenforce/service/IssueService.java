package com.greenforce.service;

import com.greenforce.dto.HistoryResponse;
import com.greenforce.dto.IssueResponse;
import com.greenforce.dto.PageResponse;
import com.greenforce.dto.StatsResponse;
import com.greenforce.entity.Issue;
import com.greenforce.entity.IssueStatus;
import com.greenforce.entity.IssueUpdate;
import com.greenforce.entity.User;
import com.greenforce.exception.BadRequestException;
import com.greenforce.exception.NotFoundException;
import com.greenforce.repository.IssueRepository;
import com.greenforce.repository.IssueUpdateRepository;
import com.greenforce.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IssueService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I
    private static final Pattern LAT_LON =
            Pattern.compile("Lat:\\s*(-?\\d+(?:\\.\\d+)?)\\s*,\\s*Lon:\\s*(-?\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);
    private static final int MAX_PAGE_SIZE = 50;

    private final SecureRandom random = new SecureRandom();

    private final IssueRepository issues;
    private final IssueUpdateRepository updates;
    private final UserRepository users;
    private final FileStorageService storage;

    public IssueService(IssueRepository issues, IssueUpdateRepository updates,
                        UserRepository users, FileStorageService storage) {
        this.issues = issues;
        this.updates = updates;
        this.users = users;
        this.storage = storage;
    }

    // ------------------------------------------------------------------ create

    /** Used by Report.html (via /submit_issue.php) and by the new /api/issues endpoint. */
    @Transactional
    public IssueResponse create(String category, String reporterName, String description,
                                String location, MultipartFile image, String reporterEmail) {
        category = requireText(category, "Category", 100);
        description = requireText(description, "Description", 2000);
        reporterName = optionalText(reporterName, "Name", 100);
        location = optionalText(location, "Location", 255);
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("Image is required");
        }

        String storedImage = storage.store(image);
        try {
            Issue issue = new Issue();
            issue.setTrackingCode(newTrackingCode());
            issue.setCategory(category);
            issue.setReporterName(reporterName);
            issue.setDescription(description);
            issue.setImagePath(storedImage);
            issue.setLocation(location);
            applyCoordinates(issue, location);
            issue.setStatus(IssueStatus.ISSUED);
            if (reporterEmail != null) {
                users.findByEmail(reporterEmail).ifPresent(issue::setReporter);
            }
            issue = issues.save(issue);
            updates.save(new IssueUpdate(issue, null, IssueStatus.ISSUED, "Issue reported", reporterEmail));
            return toResponse(issue, false);
        } catch (RuntimeException e) {
            storage.deleteQuietly(storedImage); // don't leave an orphan file behind
            throw e;
        }
    }

    // ------------------------------------------------------------------ read

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> search(String category, String status, String q,
                                              int page, int size, boolean admin) {
        IssueStatus statusFilter = parseStatus(status);
        Page<Issue> result = issues.findAll(filter(category, statusFilter, q), pageable(page, size));
        return PageResponse.of(result, i -> toResponse(i, admin));
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> mine(String email, int page, int size) {
        Page<Issue> result = issues.findByReporterEmail(email, pageable(page, size));
        return PageResponse.of(result, i -> toResponse(i, false));
    }

    @Transactional(readOnly = true)
    public IssueResponse get(Long id, boolean admin) {
        return toResponse(find(id), admin);
    }

    @Transactional(readOnly = true)
    public IssueResponse getByTrackingCode(String code) {
        Issue issue = issues.findByTrackingCode(code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("No issue found with tracking code " + code));
        return toResponse(issue, false);
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> history(Long id) {
        find(id); // 404 if missing
        return updates.findByIssueIdOrderByChangedAtAscIdAsc(id).stream()
                .map(u -> new HistoryResponse(
                        u.getOldStatus() == null ? null : u.getOldStatus().getLabel(),
                        u.getNewStatus().getLabel(),
                        u.getNote(),
                        u.getChangedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        long issued = issues.countByStatus(IssueStatus.ISSUED);
        long verified = issues.countByStatus(IssueStatus.VERIFIED);
        long inProgress = issues.countByStatus(IssueStatus.IN_PROGRESS);
        long solved = issues.countByStatus(IssueStatus.SOLVED);

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Object[] row : issues.countGroupedByCategory()) {
            byCategory.put((String) row[0], (Long) row[1]);
        }
        return new StatsResponse(issued + verified + inProgress + solved, issued, verified, inProgress, solved, byCategory);
    }

    // ------------------------------------------------------------------ admin actions

    @Transactional
    public IssueResponse updateStatus(Long id, IssueStatus newStatus, String note, String adminEmail) {
        Issue issue = find(id);
        IssueStatus old = issue.getStatus();
        if (old == newStatus) {
            throw new BadRequestException("Issue is already " + newStatus.getLabel());
        }
        issue.setStatus(newStatus);
        issue = issues.save(issue);
        String cleanNote = (note == null || note.isBlank()) ? null : note.trim();
        updates.save(new IssueUpdate(issue, old, newStatus, cleanNote, adminEmail));
        return toResponse(issue, true);
    }

    @Transactional
    public void delete(Long id) {
        Issue issue = find(id);
        String image = issue.getImagePath();
        updates.deleteByIssueId(id);
        issues.delete(issue);
        storage.deleteQuietly(image);
    }

    // ------------------------------------------------------------------ helpers

    private Issue find(Long id) {
        return issues.findById(id).orElseThrow(() -> new NotFoundException("Issue " + id + " not found"));
    }

    private IssueResponse toResponse(Issue i, boolean admin) {
        User reporter = admin ? i.getReporter() : null;
        return new IssueResponse(
                i.getId(),
                i.getTrackingCode(),
                i.getCategory(),                       // "title" (same shape as Admin.html)
                i.getCategory(),
                i.getDescription(),
                i.getStatus().getLabel(),              // "Issued" / "In Progress" / "Solved"
                i.getStatus().name(),
                i.getImagePath() == null ? null : "/uploads/" + i.getImagePath(),
                i.getLocation(),
                i.getLatitude(),
                i.getLongitude(),
                admin ? i.getReporterName() : null,
                reporter == null ? null : reporter.getEmail(),
                i.getCreatedAt(),
                i.getUpdatedAt());
    }

    private static Specification<Issue> filter(String category, IssueStatus status, String q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (category != null && !category.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("category")), like(category)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.isBlank()) {
                String pattern = like(q);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("category")), pattern),
                        cb.like(cb.lower(root.get("location")), pattern),
                        cb.like(cb.lower(root.get("trackingCode")), pattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String like(String text) {
        return "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private static Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    }

    private static IssueStatus parseStatus(String status) {
        try {
            return IssueStatus.parse(status);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private static void applyCoordinates(Issue issue, String location) {
        if (location == null) {
            return;
        }
        Matcher m = LAT_LON.matcher(location);
        if (m.find()) {
            double lat = Double.parseDouble(m.group(1));
            double lon = Double.parseDouble(m.group(2));
            if (lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
                issue.setLatitude(lat);
                issue.setLongitude(lon);
            }
        }
    }

    private String newTrackingCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder("GF-");
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            String code = sb.toString();
            if (!issues.existsByTrackingCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique tracking code");
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(field + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BadRequestException(field + " must be at most " + maxLength + " characters");
        }
        return trimmed;
    }

    private static String optionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireText(value, field, maxLength);
    }
}
