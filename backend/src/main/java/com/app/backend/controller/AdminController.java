package com.app.backend.controller;

import com.app.backend.dto.property.response.PropertyListItemDto;
import com.app.backend.service.AdminAccess;
import com.app.backend.service.AdminReviewService;
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

    @GetMapping("/listings/pending")
    public List<PropertyListItemDto> pending(@AuthenticationPrincipal UUID userId) {
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
}
