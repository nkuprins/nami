package com.app.backend.service;

import com.app.backend.IntegrationTestBase;
import com.app.backend.config.CacheConfig;
import com.app.backend.dto.property.request.PropertyFilter;
import com.app.backend.dto.property.response.PropertyCategoryCountsDto;
import com.app.backend.dto.property.response.PropertyItemDto;
import com.app.backend.entity.Listing;
import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyCategory;

import static com.app.backend.testutil.TestData.listing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PropertyServiceCacheTest extends IntegrationTestBase {

    @Autowired private ListingService listingService;
    @Autowired private ListingQueryService listingQueryService;
    @Autowired private CacheManager cacheManager;
    @Autowired private UserRepository userRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoSpyBean private ListingRepository listingRepository;

    private User owner;

    @BeforeEach
    void setUpOwner() {
        User user = new User();
        user.setName("Cache Owner");
        user.setEmail("cache-owner@test.com");
        user.setPasswordHash(passwordEncoder.encode("TestPassword12345"));
        user.setEmailVerified(true);
        owner = userRepository.save(user);
    }

    private Listing saveListing() {
        Listing l = listing(owner);
        l.setId(null);
        l.getProperty().setId(null);
        l.getProperty().setUpdatedAt(null);
        Property savedProperty = propertyRepository.save(l.getProperty());
        l.setProperty(savedProperty);
        l.setPostedAt(null);
        l.setUpdatedAt(null);
        return listingRepository.save(l);
    }

    private PropertyFilter buyFilter() {
        return PropertyFilter.builder().type(ListingType.BUY).build();
    }

    private Listing saveListing(ListingType type, PropertyCategory category) {
        Listing l = listing(owner);
        l.setId(null);
        l.getProperty().setId(null);
        l.getProperty().setUpdatedAt(null);
        l.setListingType(type);
        l.setPropertyCategory(category);
        Property savedProperty = propertyRepository.save(l.getProperty());
        l.setProperty(savedProperty);
        l.setPostedAt(null);
        l.setUpdatedAt(null);
        return listingRepository.save(l);
    }

    @Test
    void getById_secondIdenticalCall_isServedFromCache() {
        Listing saved = saveListing();
        clearInvocations(listingRepository);

        PropertyItemDto first = listingQueryService.getById(saved.getId(), "lv");
        PropertyItemDto second = listingQueryService.getById(saved.getId(), "lv");

        assertThat(first.id()).isEqualTo(saved.getId());
        assertThat(second.id()).isEqualTo(saved.getId());
        verify(listingRepository, times(1)).findById(saved.getId());
        assertThat(cacheManager.getCache(CacheConfig.PROPERTY_DETAIL).get(List.of(saved.getId(), "lv"))).isNotNull();
    }

    @Test
    void list_secondIdenticalCall_isServedFromCache() {
        saveListing();
        clearInvocations(listingRepository);

        listingQueryService.list(buyFilter(), "newest", 1);
        listingQueryService.list(buyFilter(), "newest", 1);

        verify(listingRepository, times(1)).findAllForList(any(), any());
    }

    @Test
    void delete_evictsBothCaches() {
        Listing saved = saveListing();
        listingQueryService.getById(saved.getId(), "lv");
        listingQueryService.list(buyFilter(), "newest", 1);
        assertThat(cacheManager.getCache(CacheConfig.PROPERTY_DETAIL).get(List.of(saved.getId(), "lv"))).isNotNull();
        assertThat(listCacheSize()).isEqualTo(1);

        listingService.deleteListing(saved.getId(), owner.getId());

        // @CacheEvict on delete: detail cache cleared entirely, list cache cleared entirely.
        assertThat(cacheManager.getCache(CacheConfig.PROPERTY_DETAIL).get(List.of(saved.getId(), "lv"))).isNull();
        assertThat(listCacheSize()).isZero();
    }

    @SuppressWarnings("unchecked")
    private int listCacheSize() {
        var nativeCache = (com.github.benmanes.caffeine.cache.Cache<Object, Object>)
                cacheManager.getCache(CacheConfig.PROPERTY_LIST).getNativeCache();
        return nativeCache.asMap().size();
    }

    @Test
    void countsByType_reflectsPropertyCategoryCounts() {
        saveListing(ListingType.BUY, PropertyCategory.APARTMENT);
        saveListing(ListingType.BUY, PropertyCategory.APARTMENT);
        saveListing(ListingType.BUY, PropertyCategory.HOUSE);
        saveListing(ListingType.RENT, PropertyCategory.HOUSE);

        PropertyCategoryCountsDto counts = listingQueryService.countsByType(ListingType.BUY);

        assertThat(counts.apartment()).isEqualTo(2);
        assertThat(counts.house()).isEqualTo(1);
    }

    @Test
    void countsByType_secondIdenticalCall_isServedFromCache() {
        saveListing(ListingType.BUY, PropertyCategory.APARTMENT);
        clearInvocations(listingRepository);

        listingQueryService.countsByType(ListingType.BUY);
        listingQueryService.countsByType(ListingType.BUY);

        verify(listingRepository, times(2))
                .countByListingTypeAndPropertyCategoryAndStatus(any(), any(), any());
        assertThat(cacheManager.getCache(CacheConfig.PROPERTY_KIND_COUNTS).get(ListingType.BUY)).isNotNull();
    }

    @Test
    void deleteListing_evictsKindCountsCache() {
        Listing saved = saveListing(ListingType.BUY, PropertyCategory.APARTMENT);
        listingQueryService.countsByType(ListingType.BUY);
        assertThat(cacheManager.getCache(CacheConfig.PROPERTY_KIND_COUNTS).get(ListingType.BUY)).isNotNull();

        listingService.deleteListing(saved.getId(), owner.getId());

        assertThat(cacheManager.getCache(CacheConfig.PROPERTY_KIND_COUNTS).get(ListingType.BUY)).isNull();
    }

    @Test
    void list_differentFilters_areCachedSeparately() {
        saveListing();
        clearInvocations(listingRepository);

        listingQueryService.list(buyFilter(), "newest", 1);
        listingQueryService.list(buyFilter(), "price-asc", 1);

        // Different sort → different key → both hit the repository.
        verify(listingRepository, times(2)).findAllForList(any(), any());
        clearInvocations(listingRepository);
        listingQueryService.list(buyFilter(), "newest", 1);
        verify(listingRepository, never()).findAllForList(any(), any());
    }
}
