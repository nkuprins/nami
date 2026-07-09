package com.app.backend.service;

import com.app.backend.config.AppProperties;
import com.app.backend.dto.property.model.*;
import com.app.backend.dto.property.request.*;
import com.app.backend.dto.property.response.*;
import com.app.backend.entity.Listing;
import com.app.backend.entity.Listing_;
import com.app.backend.entity.Property;
import com.app.backend.entity.User;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyFeature;
import com.app.backend.enums.PropertyStatus;
import com.app.backend.mapper.PropertyMapper;
import com.app.backend.messaging.ImageProcessingPublisher;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Sort;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import com.app.backend.exception.ApiException;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.app.backend.testutil.TestData.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock private ListingRepository listingRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private UserRepository userRepository;
    @Mock private PropertyMapper propertyMapper;
    @Mock private MediaCleanupService mediaCleanupService;
    @Mock private ImageProcessingPublisher imageProcessingPublisher;

    private PropertyAccess propertyAccess;
    private PropertyService propertyService;
    private ListingService listingService;
    private PropertyQueryService propertyQueryService;
    private ListingQueryService listingQueryService;

    @BeforeEach
    void initTransactionSync() {
        propertyAccess = new PropertyAccess(propertyRepository, listingRepository);
        AppProperties props = new AppProperties(null,
                new AppProperties.S3Properties("test-bucket", "us-east-1", 5, "https://cdn.test.local"),
                null, null, null, null, null);
        MediaUrlValidator mediaUrlValidator = new MediaUrlValidator(props);
        propertyService = new PropertyService(propertyRepository, listingRepository, userRepository,
                propertyMapper, mediaCleanupService, mediaUrlValidator, imageProcessingPublisher, propertyAccess);
        listingService = new ListingService(listingRepository, propertyRepository, propertyMapper, propertyAccess,
                mediaCleanupService, mediaUrlValidator, imageProcessingPublisher);
        propertyQueryService = new PropertyQueryService(propertyMapper, propertyAccess);
        listingQueryService = new ListingQueryService(listingRepository, userRepository, propertyMapper);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransactionSync() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private PropertyFilter defaultFilter() {
        return PropertyFilter.builder().type(ListingType.BUY).build();
    }

    @Nested
    class ListProperties {

        @Test
        void appliesFilter_andPagination_withDefaultSort() {
            Listing l = listing(user());
            PropertyListItemDto dto = listItemDto(l);
            when(listingRepository.findAllForList(ArgumentMatchers.<Specification<Listing>>any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(dto)));

            PropertyPageResponse result = listingQueryService.list(defaultFilter(), "newest", 1);

            assertThat(result.items()).hasSize(1);
            assertThat(result.total()).isEqualTo(1);
        }

        @ParameterizedTest
        @MethodSource("com.app.backend.service.PropertyServiceTest#sortOptions")
        void list_passesCorrectSortToRepository(String sortParam, String expectedField, Sort.Direction expectedDir) {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(listingRepository.findAllForList(ArgumentMatchers.<Specification<Listing>>any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            listingQueryService.list(defaultFilter(), sortParam, 1);

            verify(listingRepository).findAllForList(ArgumentMatchers.<Specification<Listing>>any(), captor.capture());
            Sort.Order order = captor.getValue().getSort().iterator().next();
            assertThat(order.getProperty()).isEqualTo(expectedField);
            assertThat(order.getDirection()).isEqualTo(expectedDir);
        }

        @Test
        void returnsEmptyPage_whenPageExceedsTotalResults() {
            when(listingRepository.findAllForList(ArgumentMatchers.<Specification<Listing>>any(), any(Pageable.class)))
                    .thenAnswer(inv -> new PageImpl<>(List.<PropertyListItemDto>of(), inv.<Pageable>getArgument(1), 1));

            PropertyPageResponse result = listingQueryService.list(defaultFilter(), "price-per-m2-asc", 100);

            assertThat(result.items()).isEmpty();
            assertThat(result.total()).isEqualTo(1);
        }

        @Test
        void mapsFeatureFilter_whenFeaturesPresent() {
            PropertyFilter filter = PropertyFilter.builder()
                    .type(ListingType.BUY)
                    .features(List.of(PropertyFeature.BALCONY))
                    .build();
            when(listingRepository.findAllForList(ArgumentMatchers.<Specification<Listing>>any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            assertThatCode(() -> listingQueryService.list(filter, "newest", 1))
                    .doesNotThrowAnyException();
        }

        @Test
        void mapsCompletionFilter_whenCompletionPresent() {
            PropertyFilter filter = PropertyFilter.builder()
                    .type(ListingType.BUY)
                    .completion(PropertyCompletion.READY)
                    .build();
            when(listingRepository.findAllForList(ArgumentMatchers.<Specification<Listing>>any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            assertThatCode(() -> listingQueryService.list(filter, "newest", 1))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class ListByOwner {
        @Test
        void returnsOwnerProperties() {
            User owner = user();
            Listing l = listing(owner);
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            when(listingRepository.findByOwner(owner)).thenReturn(List.of(l));
            when(propertyMapper.toListDto(l)).thenReturn(listItemDto(l));

            List<PropertyListItemDto> result = listingQueryService.listByOwner(owner.getId());

            assertThat(result).hasSize(1);
        }

        @Test
        void throwsNotFound_whenOwnerNotFound() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> listingQueryService.listByOwner(id))
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(404));
        }
    }

    @Nested
    class GetById {
        @Test
        void returnsProperty_whenActiveAndExists() {
            Listing l = listing(user());
            PropertyItemDto dto = itemDto(l);
            when(listingRepository.findById(l.getId())).thenReturn(Optional.of(l));
            when(propertyMapper.toDto(l)).thenReturn(dto);

            PropertyItemDto result = listingQueryService.getById(l.getId());

            assertThat(result.id()).isEqualTo(l.getId());
        }

        @Test
        void throwsNotFound_whenPropertyNotFound() {
            UUID id = UUID.randomUUID();
            when(listingRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> listingQueryService.getById(id))
                    .isInstanceOf(ApiException.class);
        }

        @Test
        void throwsNotFound_whenPropertyInactive() {
            Listing l = listing(user());
            l.setStatus(PropertyStatus.INACTIVE);
            when(listingRepository.findById(l.getId())).thenReturn(Optional.of(l));

            assertThatThrownBy(() -> listingQueryService.getById(l.getId()))
                    .isInstanceOf(ApiException.class);
        }
    }

    @Nested
    class Create {
        @Test
        void savesProperty_withAllRelations() {
            User owner = user();
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            when(propertyRepository.save(any())).thenAnswer(inv -> {
                Property p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
            when(listingRepository.save(any())).thenAnswer(inv -> {
                Listing l = inv.getArgument(0);
                l.setId(UUID.randomUUID());
                return l;
            });
            when(propertyMapper.toDto(any(Listing.class))).thenAnswer(inv -> itemDto(inv.getArgument(0)));

            PropertyItemDto result = propertyService.create(createPropertyRequest(), owner.getId());

            assertThat(result).isNotNull();
            verify(propertyRepository).save(any(Property.class));
            verify(listingRepository).save(any(Listing.class));
        }

        @Test
        void publishesPhotosAndPlansForProcessing() {
            User owner = user();
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            when(propertyRepository.save(any())).thenAnswer(inv -> {
                Property p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
            when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(propertyMapper.toDto(any(Listing.class))).thenAnswer(inv -> itemDto(inv.getArgument(0)));
            doAnswer(inv -> {
                Listing l = inv.getArgument(0);
                l.setPhotos(new ArrayList<>(List.of("https://cdn.test.local/uploads/a.jpg")));
                l.setPlans(new ArrayList<>(List.of("https://cdn.test.local/uploads/plan.jpg")));
                return null;
            }).when(propertyMapper).applyListingContent(any(), any());

            propertyService.create(createPropertyRequest(), owner.getId());

            verify(imageProcessingPublisher).enqueue(any(), eq(List.of(
                    "https://cdn.test.local/uploads/a.jpg",
                    "https://cdn.test.local/uploads/plan.jpg")));
        }

        @Test
        void throwsBadRequest_whenPhotoUrlIsNotOnOurCdn() {
            User owner = user();
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            doAnswer(inv -> {
                Listing l = inv.getArgument(0);
                l.setPhotos(new ArrayList<>(List.of("https://evil.example.com/../secret.jpg")));
                return null;
            }).when(propertyMapper).applyListingContent(any(), any());

            assertThatThrownBy(() -> propertyService.create(createPropertyRequest(), owner.getId()))
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(400));
            verify(listingRepository, never()).save(any());
            verifyNoInteractions(imageProcessingPublisher);
        }

        @Test
        void throwsNotFound_whenOwnerNotFound() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> propertyService.create(createPropertyRequest(), id))
                    .isInstanceOf(ApiException.class);
        }

        @Test
        void throwsConflict_whenOwnerAlreadyHasSameAddress() {
            User owner = user();
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            Property existing = listing(owner).getProperty();
            existing.setAddress("Main Street 10"); // same as createPropertyRequest() location
            when(propertyRepository.findByOwner(owner)).thenReturn(List.of(existing));

            assertThatThrownBy(() -> propertyService.create(createPropertyRequest(), owner.getId()))
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(409));
        }

        @Test
        void throwsConflict_whenNearMatchAndNotConfirmed() {
            User owner = user();
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            Property existing = listing(owner).getProperty();
            existing.setAddress("Main Street 10a"); // near match to "Main Street 10"
            when(propertyRepository.findByOwner(owner)).thenReturn(List.of(existing));

            assertThatThrownBy(() -> propertyService.create(createPropertyRequest(), owner.getId()))
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> {
                        assertThat(((ApiException) ex).getStatus().value()).isEqualTo(409);
                        assertThat(ex.getMessage()).contains("NEAR_DUPLICATE");
                    });
        }

        @Test
        void allowsCreate_whenNearMatchConfirmed() {
            User owner = user();
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            Property existing = listing(owner).getProperty();
            existing.setAddress("Main Street 10a");
            when(propertyRepository.findByOwner(owner)).thenReturn(List.of(existing));
            when(propertyRepository.save(any())).thenAnswer(inv -> {
                Property p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
            when(listingRepository.save(any())).thenAnswer(inv -> {
                Listing l = inv.getArgument(0);
                l.setId(UUID.randomUUID());
                return l;
            });
            when(propertyMapper.toDto(any(Listing.class))).thenAnswer(inv -> itemDto(inv.getArgument(0)));

            CreatePropertyRequest req = createPropertyRequest().toBuilder().confirmedDuplicate(true).build();

            assertThat(propertyService.create(req, owner.getId())).isNotNull();
        }

        @Test
        void setsCompletion_whenProvided() {
            User owner = user();
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            when(propertyRepository.save(any())).thenAnswer(inv -> {
                Property p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
            ArgumentCaptor<Listing> captor = ArgumentCaptor.forClass(Listing.class);
            when(listingRepository.save(captor.capture())).thenAnswer(inv -> {
                Listing l = inv.getArgument(0);
                l.setId(UUID.randomUUID());
                return l;
            });
            when(propertyMapper.toDto(any(Listing.class))).thenAnswer(inv -> itemDto(inv.getArgument(0)));
            doAnswer(inv -> {
                inv.<Listing>getArgument(0).setCompletion(inv.<PropertyRequest>getArgument(1).completion());
                return null;
            }).when(propertyMapper).applyListingContent(any(), any());

            CreatePropertyRequest req = createPropertyRequest().toBuilder()
                    .type(ListingType.NEW_PROJECT)
                    .completion(PropertyCompletion.READY)
                    .build();

            propertyService.create(req, owner.getId());

            assertThat(captor.getValue().getCompletion()).isEqualTo(PropertyCompletion.READY);
        }
    }

    @Nested
    class UpdateListing {
        @Test
        void updatesListingFields() {
            User owner = user();
            Listing l = listing(owner);
            when(listingRepository.findById(l.getId())).thenReturn(Optional.of(l));
            when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(propertyMapper.toDto(any(Listing.class))).thenAnswer(inv -> itemDto(inv.getArgument(0)));

            PropertyItemDto result = listingService.updateListing(l.getId(), updateListingRequest(), owner.getId());

            assertThat(result).isNotNull();
            verify(listingRepository).save(l);
        }

        @Test
        void throwsForbidden_whenNotOwner() {
            User owner = user();
            Listing l = listing(owner);
            UUID otherId = UUID.randomUUID();
            when(listingRepository.findById(l.getId())).thenReturn(Optional.of(l));

            assertThatThrownBy(() -> listingService.updateListing(l.getId(), updateListingRequest(), otherId))
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(403));
        }

        @Test
        void throwsNotFound_whenPropertyNotFound() {
            UUID id = UUID.randomUUID();
            when(listingRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> listingService.updateListing(id, updateListingRequest(), UUID.randomUUID()))
                    .isInstanceOf(ApiException.class);
        }

        @Test
        void clearsCompletion_whenBlankInRequest() {
            User owner = user();
            Listing l = listing(owner);
            l.setCompletion(PropertyCompletion.READY);
            when(listingRepository.findById(l.getId())).thenReturn(Optional.of(l));
            when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(propertyMapper.toDto(any(Listing.class))).thenAnswer(inv -> itemDto(inv.getArgument(0)));
            doAnswer(inv -> {
                inv.<Listing>getArgument(0).setCompletion(inv.<PropertyRequest>getArgument(1).completion());
                return null;
            }).when(propertyMapper).applyListingContent(any(), any());

            listingService.updateListing(l.getId(), updateListingRequest(), owner.getId());

            assertThat(l.getCompletion()).isNull();
        }

        @Test
        void deletesRemovedPhotosFromS3() {
            User owner = user();
            Listing l = listingWithPhotos(owner); // photo1, photo2
            when(listingRepository.findById(l.getId())).thenReturn(Optional.of(l));
            when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(propertyMapper.toDto(any(Listing.class))).thenAnswer(inv -> itemDto(inv.getArgument(0)));
            doAnswer(inv -> {
                Listing listing = inv.getArgument(0);
                listing.setPhotos(new ArrayList<>(List.of("https://cdn.test.local/uploads/photo1.jpg")));
                return null;
            }).when(propertyMapper).applyListingContent(any(), any());

            listingService.updateListing(l.getId(), updateListingRequest(), owner.getId());

            verify(mediaCleanupService).enqueue(List.of("https://cdn.test.local/uploads/photo2.jpg"));
        }

        @Test
        void publishesOnlyNewlyAddedPhotos() {
            User owner = user();
            Listing l = listingWithPhotos(owner); // photo1, photo2
            when(listingRepository.findById(l.getId())).thenReturn(Optional.of(l));
            when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(propertyMapper.toDto(any(Listing.class))).thenAnswer(inv -> itemDto(inv.getArgument(0)));
            doAnswer(inv -> {
                Listing listing = inv.getArgument(0);
                listing.setPhotos(new ArrayList<>(List.of(
                        "https://cdn.test.local/uploads/photo1.jpg",
                        "https://cdn.test.local/uploads/photo2.jpg",
                        "https://cdn.test.local/uploads/photo3.jpg")));
                return null;
            }).when(propertyMapper).applyListingContent(any(), any());

            listingService.updateListing(l.getId(), updateListingRequest(), owner.getId());

            verify(imageProcessingPublisher).enqueue(eq(l.getProperty().getId()),
                    eq(List.of("https://cdn.test.local/uploads/photo3.jpg")));
        }
    }

    @Nested
    class UpdateProperty {
        @Test
        void updatesPropertyLocation() {
            User owner = user();
            Property p = listing(owner).getProperty();
            when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));
            when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(propertyMapper.toPropertyDto(any(Property.class))).thenAnswer(inv -> propertyDto(inv.getArgument(0)));

            PropertyDto result = propertyService.updateProperty(p.getId(), updatePropertyRequest(), owner.getId());

            assertThat(result).isNotNull();
            verify(propertyRepository).save(p);
        }

        @Test
        void throwsForbidden_whenNotOwner() {
            User owner = user();
            Property p = listing(owner).getProperty();
            UUID otherId = UUID.randomUUID();
            when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> propertyService.updateProperty(p.getId(), updatePropertyRequest(), otherId))
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(403));
        }

        @Test
        void throwsNotFound_whenPropertyNotFound() {
            UUID id = UUID.randomUUID();
            when(propertyRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> propertyService.updateProperty(id, updatePropertyRequest(), UUID.randomUUID()))
                    .isInstanceOf(ApiException.class);
        }
    }

    @Nested
    class GetProperty {
        @Test
        void returnsProperty_forOwner() {
            User owner = user();
            Property p = listing(owner).getProperty();
            when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));
            when(propertyMapper.toPropertyDto(any(Property.class))).thenAnswer(inv -> propertyDto(inv.getArgument(0)));

            PropertyDto result = propertyQueryService.getProperty(p.getId(), owner.getId());

            assertThat(result).isNotNull();
        }

        @Test
        void throwsForbidden_whenNotOwner() {
            User owner = user();
            Property p = listing(owner).getProperty();
            UUID otherId = UUID.randomUUID();
            when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> propertyQueryService.getProperty(p.getId(), otherId))
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(403));
        }

        @Test
        void throwsNotFound_whenPropertyNotFound() {
            UUID id = UUID.randomUUID();
            when(propertyRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> propertyQueryService.getProperty(id, UUID.randomUUID()))
                    .isInstanceOf(ApiException.class);
        }
    }

    @Nested
    class DeleteListing {
        @Test
        void deletesListingAndEmptyProperty_whenLastListing() {
            User owner = user();
            Listing l = listingWithPhotos(owner);
            when(listingRepository.findById(l.getId())).thenReturn(Optional.of(l));
            when(listingRepository.countByPropertyId(l.getProperty().getId())).thenReturn(1L);

            listingService.deleteListing(l.getId(), owner.getId());

            verify(propertyRepository).delete(l.getProperty());
            verify(mediaCleanupService).enqueue(List.of(
                    "https://cdn.test.local/uploads/photo1.jpg",
                    "https://cdn.test.local/uploads/photo2.jpg"));
        }

        @Test
        void deletesOnlyListing_whenSiblingsRemain() {
            User owner = user();
            Listing l = listingWithPhotos(owner);
            when(listingRepository.findById(l.getId())).thenReturn(Optional.of(l));
            when(listingRepository.countByPropertyId(l.getProperty().getId())).thenReturn(2L);

            listingService.deleteListing(l.getId(), owner.getId());

            verify(listingRepository).delete(l);
            verify(propertyRepository, never()).delete(any());
            verify(mediaCleanupService).enqueue(List.of(
                    "https://cdn.test.local/uploads/photo1.jpg",
                    "https://cdn.test.local/uploads/photo2.jpg"));
        }

        @Test
        void throwsForbidden_whenNotOwner() {
            User owner = user();
            Listing l = listing(owner);
            when(listingRepository.findById(l.getId())).thenReturn(Optional.of(l));

            assertThatThrownBy(() -> listingService.deleteListing(l.getId(), UUID.randomUUID()))
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(403));
        }

        @Test
        void throwsNotFound_whenListingNotFound() {
            UUID id = UUID.randomUUID();
            when(listingRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> listingService.deleteListing(id, UUID.randomUUID()))
                    .isInstanceOf(ApiException.class);
        }
    }

    @Nested
    class DeleteProperty {
        @Test
        void removesProperty_andDeletesS3Photos() {
            User owner = user();
            Listing l = listingWithPhotos(owner);
            Property property = l.getProperty();
            when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
            when(listingRepository.findByPropertyId(property.getId())).thenReturn(List.of(l));

            propertyService.deleteProperty(property.getId(), owner.getId());

            verify(propertyRepository).delete(property);
            verify(mediaCleanupService).enqueue(List.of(
                    "https://cdn.test.local/uploads/photo1.jpg",
                    "https://cdn.test.local/uploads/photo2.jpg"
            ));
        }

        @Test
        void throwsForbidden_whenNotOwner() {
            User owner = user();
            Property property = listing(owner).getProperty();
            when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));

            assertThatThrownBy(() -> propertyService.deleteProperty(property.getId(), UUID.randomUUID()))
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(403));
        }

        @Test
        void throwsNotFound_whenPropertyNotFound() {
            UUID id = UUID.randomUUID();
            when(propertyRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> propertyService.deleteProperty(id, UUID.randomUUID()))
                    .isInstanceOf(ApiException.class);
        }

        @Test
        void doesNotCallS3_whenListingsHaveNoPhotos() {
            User owner = user();
            Listing l = listing(owner); // no photos
            Property property = l.getProperty();
            when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
            when(listingRepository.findByPropertyId(property.getId())).thenReturn(List.of(l));

            propertyService.deleteProperty(property.getId(), owner.getId());

            verify(propertyRepository).delete(property);
            verify(mediaCleanupService, never()).enqueue(any());
        }
    }

    @Nested
    class ReprocessImages {
        @Test
        void publishesAllPhotosAndPlans() {
            User owner = user();
            Listing l = listingWithPhotos(owner); // photo1, photo2
            l.setPlans(new ArrayList<>(List.of("https://cdn.test.local/uploads/plan.jpg")));
            when(listingRepository.findById(l.getId())).thenReturn(Optional.of(l));

            propertyService.reprocessImages(l.getId(), owner.getId());

            verify(imageProcessingPublisher).enqueue(l.getProperty().getId(), l.allMediaUrls());
        }

        @Test
        void throwsForbidden_whenNotOwner() {
            User owner = user();
            Listing l = listingWithPhotos(owner);
            when(listingRepository.findById(l.getId())).thenReturn(Optional.of(l));

            assertThatThrownBy(() -> propertyService.reprocessImages(l.getId(), UUID.randomUUID()))
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(403));
        }
    }

    @Nested
    class AddListing {
        @Test
        void addsSecondListing_toExistingProperty() {
            User owner = user();
            Property property = listing(owner).getProperty();
            when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
            when(listingRepository.save(any())).thenAnswer(inv -> {
                Listing l = inv.getArgument(0);
                l.setId(UUID.randomUUID());
                return l;
            });
            when(propertyMapper.toDto(any(Listing.class))).thenAnswer(inv -> itemDto(inv.getArgument(0)));
            doAnswer(inv -> {
                inv.<Listing>getArgument(0).setListingType(inv.<PropertyRequest>getArgument(1).type());
                return null;
            }).when(propertyMapper).applyListingContent(any(), any());

            PropertyItemDto result = listingService.addListing(property.getId(), addListingRequest(), owner.getId());

            assertThat(result).isNotNull();
            ArgumentCaptor<Listing> captor = ArgumentCaptor.forClass(Listing.class);
            verify(listingRepository).save(captor.capture());
            assertThat(captor.getValue().getProperty()).isEqualTo(property);
            assertThat(captor.getValue().getListingType()).isEqualTo(ListingType.RENT);
        }

        @Test
        void throwsForbidden_whenNotOwner() {
            User owner = user();
            Property property = listing(owner).getProperty();
            when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));

            assertThatThrownBy(() -> listingService.addListing(property.getId(), addListingRequest(), UUID.randomUUID()))
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> assertThat(((ApiException) ex).getStatus().value()).isEqualTo(403));
        }

        @Test
        void throwsNotFound_whenPropertyNotFound() {
            UUID id = UUID.randomUUID();
            when(propertyRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> listingService.addListing(id, addListingRequest(), UUID.randomUUID()))
                    .isInstanceOf(ApiException.class);
        }
    }

    static Stream<Arguments> sortOptions() {
        return Stream.of(
                Arguments.of("newest",          Listing_.POSTED_AT, Sort.Direction.DESC),
                Arguments.of("price-asc",        Listing_.PRICE,     Sort.Direction.ASC),
                Arguments.of("price-desc",       Listing_.PRICE,     Sort.Direction.DESC),
                Arguments.of("m2-desc",          "m2",               Sort.Direction.DESC),
                Arguments.of("price-per-m2-asc", "pricePerM2",       Sort.Direction.ASC)
        );
    }

    private PropertyListItemDto listItemDto(Listing l) {
        Property p = l.getProperty();
        return PropertyListItemDto.builder()
                .id(l.getId())
                .ownerId(l.getOwner().getId())
                .type(l.getListingType())
                .propertyKind(l.getPropertyCategory())
                .price(new Price(l.getPrice(), null))
                .details(PropertyDetails.builder().rooms(l.getRooms()).m2(l.getM2()).build())
                .location(new Location(p.getDistrictSlug(), p.getCitySlug(), p.getAddress(), null))
                .features(List.of())
                .postedAt(l.getPostedAt())
                .expiresAt(l.getExpiresAt())
                .build();
    }

    private PropertyDto propertyDto(Property p) {
        return PropertyDto.builder()
                .id(p.getId())
                .ownerId(p.getOwner().getId())
                .location(new Location(p.getDistrictSlug(), p.getCitySlug(), p.getAddress(),
                        coordsOf(p)))
                .build();
    }

    private PropertyItemDto itemDto(Listing l) {
        Property p = l.getProperty();
        return PropertyItemDto.builder()
                .id(l.getId())
                .ownerId(l.getOwner().getId())
                .type(l.getListingType())
                .propertyKind(l.getPropertyCategory())
                .price(new Price(l.getPrice(), null))
                .details(PropertyDetails.builder().rooms(l.getRooms()).m2(l.getM2()).build())
                .location(new Location(p.getDistrictSlug(), p.getCitySlug(), p.getAddress(),
                        coordsOf(p)))
                .features(List.of())
                .media(Media.builder().photos(List.of()).build())
                .postedAt(l.getPostedAt())
                .expiresAt(l.getExpiresAt())
                .build();
    }

    // The mapper is mocked, so a property built inside the service under test has no coords set.
    private CoordsDto coordsOf(Property p) {
        return p.getLat() == null ? null : new CoordsDto(p.getLat(), p.getLng());
    }
}
