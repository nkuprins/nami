package com.app.backend.service;

import com.app.backend.dto.export.SavedPropertyExportDto;
import com.app.backend.dto.export.UserExportDto;
import com.app.backend.dto.property.response.PropertyItemDto;
import com.app.backend.entity.User;
import com.app.backend.exception.AuthException;
import com.app.backend.mapper.PropertyMapper;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.SavedListingRepository;
import com.app.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Assembles the GDPR data-export payload (profile, owned listings, saved listings) for a user. */
@Service
@RequiredArgsConstructor
public class UserDataExportService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final SavedListingRepository savedListingRepository;
    private final PropertyMapper propertyMapper;

    @Transactional(readOnly = true)
    public UserExportDto export(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "User not found"));
        List<PropertyItemDto> ownedProps = listingRepository.findByOwner(user)
                .stream().map(propertyMapper::toDto).toList();
        List<SavedPropertyExportDto> saved = savedListingRepository.findByIdUserId(userId)
                .stream()
                .map(sl -> new SavedPropertyExportDto(sl.getId().listingId(), sl.getSavedAt()))
                .toList();
        return UserExportDto.builder()
                .id(user.getId()).name(user.getName()).email(user.getEmail())
                .emailVerified(user.isEmailVerified()).createdAt(user.getCreatedAt())
                .ownedProperties(ownedProps).savedProperties(saved)
                .build();
    }
}
