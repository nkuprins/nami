package com.app.backend.repository;

import com.app.backend.IntegrationTestBase;
import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import com.app.backend.enums.PropertyStatus;
import com.app.backend.testutil.TestData;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends IntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User persistUser(String email) {
        User u = new User();
        u.setName("Test");
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode("TestPassword12345"));
        u.setEmailVerified(true);
        return userRepository.save(u);
    }

    private void persistProperty(User owner, PropertyStatus status) {
        Property p = TestData.property(owner);
        p.setId(null);
        p.setPostedAt(null);
        p.setUpdatedAt(null);
        p.setStatus(status);
        propertyRepository.save(p);
    }

    @Test
    void findByEmailIgnoreCase_returnsUser_caseInsensitive() {
        persistUser("ALICE@test.com");

        Optional<User> found = userRepository.findByEmailIgnoreCase("alice@test.com");
        assertThat(found).isPresent().get().extracting(User::getEmail).isEqualTo("ALICE@test.com");
    }

    @Test
    void findByEmailIgnoreCase_returnsEmpty_whenNotFound() {
        Optional<User> found = userRepository.findByEmailIgnoreCase("nobody@test.com");
        assertThat(found).isEmpty();
    }

    @Test
    void findInactive_includesUsers_withNoActiveListings() {
        User inactive = persistUser("inactive@test.com");

        List<User> results = userRepository.findInactiveWithoutActiveListings(
                OffsetDateTime.now().plusDays(1), PropertyStatus.ACTIVE);

        assertThat(results).extracting(User::getId).contains(inactive.getId());
    }

    @Test
    void findInactive_excludesUsers_withActiveListings() {
        User active = persistUser("active@test.com");
        persistProperty(active, PropertyStatus.ACTIVE);

        List<User> results = userRepository.findInactiveWithoutActiveListings(
                OffsetDateTime.now().plusDays(1), PropertyStatus.ACTIVE);

        assertThat(results).extracting(User::getId).doesNotContain(active.getId());
    }

    @Test
    void findInactive_includesUsers_withInactiveListingsOnly() {
        User withInactive = persistUser("hasinactive@test.com");
        persistProperty(withInactive, PropertyStatus.INACTIVE);

        List<User> results = userRepository.findInactiveWithoutActiveListings(
                OffsetDateTime.now().plusDays(1), PropertyStatus.ACTIVE);

        assertThat(results).extracting(User::getId).contains(withInactive.getId());
    }

    @Test
    void findInactive_excludesUsers_updatedAfterCutoff() {
        persistUser("recent@test.com");

        // cutoff in the past: user saved just now is NOT older than cutoff
        List<User> results = userRepository.findInactiveWithoutActiveListings(
                OffsetDateTime.now().minusDays(1), PropertyStatus.ACTIVE);

        assertThat(results).isEmpty();
    }
}
