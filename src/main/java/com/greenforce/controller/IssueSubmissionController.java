package com.greenforce.controller;

import com.greenforce.dto.IssueResponse;
import com.greenforce.dto.SubmitResponse;
import com.greenforce.service.IssueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Report.html (unchanged) does:  $.ajax({ url: 'submit_issue.php', type: 'POST', data: FormData })
 * with fields category, name, description, image, location. That legacy URL is mapped here,
 * together with the cleaner /api/issues path for any new client.
 *
 * Anonymous reports are allowed; if a valid Bearer token is sent the issue is linked to that user.
 */
@RestController
public class IssueSubmissionController {

    private final IssueService issueService;

    public IssueSubmissionController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping(value = {"/submit_issue.php", "/api/issues"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmitResponse> submit(@RequestParam("category") String category,
                                                 @RequestParam(value = "name", required = false) String name,
                                                 @RequestParam("description") String description,
                                                 @RequestParam(value = "location", required = false) String location,
                                                 @RequestParam("image") MultipartFile image,
                                                 Authentication authentication) {
        String email = isLoggedIn(authentication) ? authentication.getName() : null;
        IssueResponse issue = issueService.create(category, name, description, location, image, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SubmitResponse(
                issue.id(), issue.trackingCode(), issue.status(),
                "Issue reported successfully. Use tracking code " + issue.trackingCode() + " to follow it."));
    }

    private static boolean isLoggedIn(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
}
