package com.app.backend.repository;

import com.app.backend.IntegrationTestBase;
import com.app.backend.entity.Listing;
import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import com.app.backend.enums.PropertyStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;

import static com.app.backend.testutil.TestData.listingWithPhotos;
import static org.assertj.core.api.Assertions.assertThat;

class ListingRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // Reproduces the MultipleBagFetchException from PropertyExpiryJob.purgeExpiredInactiveListings:
    // the query must execute and expose the property's JSON media columns.
    @Test
    void findInactiveExpiredBefore_returnsListingWithMedia() {
        User owner = new User();
        owner.setName("Purge Owner");
        owner.setEmail("purge-owner@test.com");
        owner.setPasswordHash(passwordEncoder.encode("TestPassword12345"));
        owner.setEmailVerified(true);
        owner = userRepository.save(owner);

        Listing l = listingWithPhotos(owner);
        l.setId(null);
        l.getProperty().setId(null);
        l.getProperty().setUpdatedAt(null);
        l.setStatus(PropertyStatus.INACTIVE);
        l.setExpiresAt(OffsetDateTime.now().minusDays(120));
        Property savedProperty = propertyRepository.save(l.getProperty());
        l.setProperty(savedProperty);
        l.setPostedAt(null);
        l.setUpdatedAt(null);
        listingRepository.save(l);

        List<Listing> result = listingRepository.findInactiveExpiredBefore(
                PropertyStatus.INACTIVE, OffsetDateTime.now().minusDays(90));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().allMediaUrls())
                .containsExactly(
                        "https://cdn.test.local/uploads/photo1.jpg",
                        "https://cdn.test.local/uploads/photo2.jpg");
    }
}
