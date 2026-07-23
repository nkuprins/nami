package com.app.backend.controller;

import com.app.backend.dto.property.response.PendingReviewDto;
import com.app.backend.dto.property.response.PropertyItemDto;
import com.app.backend.service.AdminAccess;
import com.app.backend.service.AdminReviewService;
import com.app.backend.service.ListingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminAccess adminAccess;
    private final AdminReviewService adminReviewService;
    private final ListingQueryService listingQueryService;

    @GetMapping("/listings/pending")
    public List<PendingReviewDto> pending(@AuthenticationPrincipal UUID userId) {
        adminAccess.requireAdmin(userId);
        return adminReviewService.listPending();
    }

    @PostMapping("/listings/{id}/approve")
    public void approve(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        adminAccess.requireAdmin(userId);
        adminReviewService.approve(id);
    }

    @PostMapping("/listings/{id}/reject")
    public void reject(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        adminAccess.requireAdmin(userId);
        adminReviewService.reject(id);
    }

    @GetMapping("/listings/{id}")
    public PropertyItemDto getById(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @RequestParam(required = false) String locale
    ) {
        adminAccess.requireAdmin(userId);
        return listingQueryService.getByIdForAdmin(id, locale);
    }

    @PostMapping("/listings/{id}/suspend")
    public void suspend(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        adminAccess.requireAdmin(userId);
        adminReviewService.suspend(id);
    }

    @PostMapping("/listings/{id}/reactivate")
    public void reactivate(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        adminAccess.requireAdmin(userId);
        adminReviewService.reactivate(id);
    }
}
