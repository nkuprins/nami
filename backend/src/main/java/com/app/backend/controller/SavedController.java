package com.app.backend.controller;

import com.app.backend.service.SavedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/saved")
@RequiredArgsConstructor
public class SavedController {

    private final SavedService savedService;

    @GetMapping
    public List<UUID> getSaved(@AuthenticationPrincipal UUID userId) {
        return savedService.getSavedIds(userId);
    }

    @PostMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@AuthenticationPrincipal UUID userId, @PathVariable UUID listingId) {
        savedService.save(userId, listingId);
    }

    @DeleteMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsave(@AuthenticationPrincipal UUID userId, @PathVariable UUID listingId) {
        savedService.unsave(userId, listingId);
    }
}
