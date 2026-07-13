package com.app.backend.service;

import com.app.backend.entity.User;
import com.app.backend.enums.UserRole;
import com.app.backend.exception.ApiException;
import com.app.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Asserts the caller has the {@code admin} role, throwing {@code FORBIDDEN}
 * otherwise. Mirrors {@link PropertyAccess}'s ownership-check pattern — a
 * manual guard called at the top of each admin service method, rather than
 * Spring Security roles (the JWT filter assigns no authorities today).
 */
@Service
@RequiredArgsConstructor
public class AdminAccess {

    private final UserRepository userRepository;

    public void requireAdmin(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN));
        if (user.getRole() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN);
        }
    }
}
