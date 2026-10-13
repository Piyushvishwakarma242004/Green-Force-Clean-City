package com.greenforce.controller;

import com.greenforce.dto.HistoryResponse;
import com.greenforce.dto.IssueResponse;
import com.greenforce.dto.PageResponse;
import com.greenforce.dto.StatsResponse;
import com.greenforce.service.IssueService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public, read-only endpoints for tracking issues (reporter details are never exposed here). */
@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    /** e.g. GET /api/issues?category=water&status=IN_PROGRESS&q=pipe&page=0&size=10 */
    @GetMapping
    public PageResponse<IssueResponse> list(@RequestParam(required = false) String category,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return issueService.search(category, status, q, page, size, false);
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return issueService.stats();
    }

    /** Issues reported while logged in. Requires a Bearer token. */
    @GetMapping("/mine")
    public PageResponse<IssueResponse> mine(Authentication authentication,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return issueService.mine(authentication.getName(), page, size);
    }

    @GetMapping("/track/{trackingCode}")
    public IssueResponse byTrackingCode(@PathVariable String trackingCode) {
        return issueService.getByTrackingCode(trackingCode);
    }

    @GetMapping("/{id}")
    public IssueResponse get(@PathVariable Long id) {
        return issueService.get(id, false);
    }

    @GetMapping("/{id}/history")
    public List<HistoryResponse> history(@PathVariable Long id) {
        return issueService.history(id);
    }
}
