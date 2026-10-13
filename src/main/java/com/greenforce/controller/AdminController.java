package com.greenforce.controller;

import com.greenforce.dto.IssueResponse;
import com.greenforce.dto.PageResponse;
import com.greenforce.dto.StatusUpdateRequest;
import com.greenforce.service.IssueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Everything under /api/admin requires the ADMIN role (enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final IssueService issueService;

    public AdminController(IssueService issueService) {
        this.issueService = issueService;
    }

    /** Same filters as the public list, but includes reporter name / email. */
    @GetMapping("/issues")
    public PageResponse<IssueResponse> list(@RequestParam(required = false) String category,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return issueService.search(category, status, q, page, size, true);
    }

    @GetMapping("/issues/{id}")
    public IssueResponse get(@PathVariable Long id) {
        return issueService.get(id, true);
    }

    /** Body: { "status": "IN_PROGRESS", "note": "Crew assigned" } */
    @PatchMapping("/issues/{id}/status")
    public IssueResponse updateStatus(@PathVariable Long id,
                                      @Valid @RequestBody StatusUpdateRequest request,
                                      Authentication authentication) {
        return issueService.updateStatus(id, request.status(), request.note(), authentication.getName());
    }

    @DeleteMapping("/issues/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        issueService.delete(id);
    }
}
