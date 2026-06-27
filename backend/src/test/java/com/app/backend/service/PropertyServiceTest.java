package com.app.backend.service;

import com.app.backend.dto.*;
import com.app.backend.entity.Property;
import com.app.backend.entity.Property_;
import com.app.backend.entity.PropertyTranslation;
import com.app.backend.entity.User;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyStatus;
import com.app.backend.mapper.PropertyMapper;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.app.backend.testutil.TestData.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private UserRepository userRepository;
    @Mock private PropertyMapper propertyMapper;
    @Mock private UploadService uploadService;

    @InjectMocks
    private PropertyService propertyService;

    @BeforeEach
    void initTransactionSync() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransactionSync() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void triggerAfterCommit() {
        new ArrayList<>(TransactionSynchronizationManager.getSynchronizations())
                .forEach(TransactionSynchronization::afterCommit);
    }

    private PropertyFilter defaultFilter() {
        return new PropertyFilter("buy", null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Nested
    class ListProperties {

        @Test
        void appliesFilter_andPagination_withDefaultSort() {
            Property p = property(user());
            PropertyListItemDto dto = listItemDto(p);
            when(propertyRepository.findAll(ArgumentMatchers.<Specification<Property>>any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(p)));
            when(propertyMapper.toListDto(p)).thenReturn(dto);

            PropertyPageResponse result = propertyService.list(defaultFilter(), "newest", 1);

            assertThat(result.items()).hasSize(1);
            assertThat(result.total()).isEqualTo(1);
        }

        @ParameterizedTest
        @MethodSource("com.app.backend.service.PropertyServiceTest#sortOptions")
        void list_passesCorrectSortToRepository(String sortParam, String expectedField, Sort.Direction expectedDir) {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(propertyRepository.findAll(ArgumentMatchers.<Specification<Property>>any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            propertyService.list(defaultFilter(), sortParam, 1);

            verify(propertyRepository).findAll(ArgumentMatchers.<Specification<Property>>any(), captor.capture());
            Sort.Order order = captor.getValue().getSort().iterator().next();
            assertThat(order.getProperty()).isEqualTo(expectedField);
            assertThat(order.getDirection()).isEqualTo(expectedDir);
        }

        @Test
        void returnsEmptyPage_whenPageExceedsTotalResults() {
            when(propertyRepository.findAll(ArgumentMatchers.<Specification<Property>>any(), any(Pageable.class)))
                    .thenAnswer(inv -> new PageImpl<>(List.of(), inv.getArgument(1), 1));

            PropertyPageResponse result = propertyService.list(defaultFilter(), "price-per-m2-asc", 100);

            assertThat(result.items()).isEmpty();
            assertThat(result.total()).isEqualTo(1);
        }

        @Test
        void mapsFeatureFilter_whenFeaturesPresent() {
            PropertyFilter filter = new PropertyFilter("buy", null, null, null, null, null, null,
                    null, null, null, null, null, null, List.of("balcony"), null);
            when(propertyRepository.findAll(ArgumentMatchers.<Specification<Property>>any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            assertThatCode(() -> propertyService.list(filter, "newest", 1))
                    .doesNotThrowAnyException();
        }

        @Test
        void mapsCompletionFilter_whenCompletionPresent() {
            PropertyFilter filter = new PropertyFilter("buy", null, null, null, null, null, null,
                    null, null, null, null, null, null, null, "ready");
            when(propertyRepository.findAll(ArgumentMatchers.<Specification<Property>>any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            assertThatCode(() -> propertyService.list(filter, "newest", 1))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class ListByOwner {
        @Test
        void returnsOwnerProperties() {
            User owner = user();
            Property p = property(owner);
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            when(propertyRepository.findByOwner(owner)).thenReturn(List.of(p));
            when(propertyMapper.toListDto(p)).thenReturn(listItemDto(p));

            List<PropertyListItemDto> result = propertyService.listByOwner(owner.getId());

            assertThat(result).hasSize(1);
        }

        @Test
        void throwsNotFound_whenOwnerNotFound() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> propertyService.listByOwner(id))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
        }
    }

    @Nested
    class GetById {
        @Test
        void returnsProperty_whenActiveAndExists() {
            Property p = property(user());
            PropertyItemDto dto = itemDto(p);
            when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));
            when(propertyMapper.toDto(p)).thenReturn(dto);

            PropertyItemDto result = propertyService.getById(p.getId());

            assertThat(result.id()).isEqualTo(p.getId());
        }

        @Test
        void throwsNotFound_whenPropertyNotFound() {
            UUID id = UUID.randomUUID();
            when(propertyRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> propertyService.getById(id))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        void throwsNotFound_whenPropertyInactive() {
            Property p = property(user());
            p.setStatus(PropertyStatus.INACTIVE);
            when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> propertyService.getById(p.getId()))
                    .isInstanceOf(ResponseStatusException.class);
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
            when(propertyMapper.toDto(any())).thenAnswer(inv -> itemDto(inv.getArgument(0)));

            PropertyItemDto result = propertyService.create(createPropertyRequest(), owner.getId());

            assertThat(result).isNotNull();
            verify(propertyRepository).save(any(Property.class));
        }

        @Test
        void throwsNotFound_whenOwnerNotFound() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> propertyService.create(createPropertyRequest(), id))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        void throwsBadRequest_whenBothTitlesBlank() {
            User owner = user();
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

            CreatePropertyRequest req = new CreatePropertyRequest(
                    "buy", "apartment", null, null, null,
                    "Desc LV", null, null,
                    new BigDecimal("100000"), (short) 2, new BigDecimal("50"),
                    null, (short) 1, (short) 5, (short) 2010, null,
                    "centre", "riga", "Street 1", new CoordsDto(56.9, 24.1),
                    null, null, null, null, null, 3
            );

            assertThatThrownBy(() -> propertyService.create(req, owner.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        }

        @Test
        void throwsBadRequest_whenBothDescriptionsBlank() {
            User owner = user();
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

            CreatePropertyRequest req = new CreatePropertyRequest(
                    "buy", "apartment", "Title LV", null, null,
                    null, null, null,
                    new BigDecimal("100000"), (short) 2, new BigDecimal("50"),
                    null, (short) 1, (short) 5, (short) 2010, null,
                    "centre", "riga", "Street 1", new CoordsDto(56.9, 24.1),
                    null, null, null, null, null, 3
            );

            assertThatThrownBy(() -> propertyService.create(req, owner.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        }

        @Test
        void setsCompletion_whenProvided() {
            User owner = user();
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            ArgumentCaptor<Property> captor = ArgumentCaptor.forClass(Property.class);
            when(propertyRepository.save(captor.capture())).thenAnswer(inv -> {
                Property p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
            when(propertyMapper.toDto(any())).thenAnswer(inv -> itemDto(inv.getArgument(0)));

            CreatePropertyRequest req = new CreatePropertyRequest(
                    "buy", "apartment", "Title LV", null, null,
                    "Desc LV", null, null,
                    new BigDecimal("100000"), (short) 2, new BigDecimal("50"),
                    null, (short) 1, (short) 5, (short) 2010, null,
                    "centre", "riga", "Street 1", new CoordsDto(56.9, 24.1),
                    null, null, null, null, "ready", 3
            );

            propertyService.create(req, owner.getId());

            assertThat(captor.getValue().getCompletion()).isEqualTo(PropertyCompletion.READY);
        }
    }

    @Nested
    class Update {
        @Test
        void updatesAllFields() {
            User owner = user();
            Property p = property(owner);
            when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));
            when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(propertyMapper.toDto(any())).thenAnswer(inv -> itemDto(inv.getArgument(0)));

            PropertyItemDto result = propertyService.update(p.getId(), updatePropertyRequest(), owner.getId());

            assertThat(result).isNotNull();
            verify(propertyRepository).save(p);
        }

        @Test
        void throwsForbidden_whenNotOwner() {
            User owner = user();
            Property p = property(owner);
            UUID otherId = UUID.randomUUID();
            when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> propertyService.update(p.getId(), updatePropertyRequest(), otherId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));
        }

        @Test
        void throwsNotFound_whenPropertyNotFound() {
            UUID id = UUID.randomUUID();
            when(propertyRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> propertyService.update(id, updatePropertyRequest(), UUID.randomUUID()))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        void throwsBadRequest_whenBothTitlesBlank() {
            User owner = user();
            Property p = property(owner);
            when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));

            UpdatePropertyRequest req = new UpdatePropertyRequest(
                    "buy", "apartment", null, null, null,
                    "Desc LV", null, null,
                    new BigDecimal("100000"), (short) 2, new BigDecimal("50"),
                    null, (short) 1, (short) 5, (short) 2010,
                    null, null, null, null
            );

            assertThatThrownBy(() -> propertyService.update(p.getId(), req, owner.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        }

        @Test
        void throwsBadRequest_whenBothDescriptionsBlank() {
            User owner = user();
            Property p = property(owner);
            when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));

            UpdatePropertyRequest req = new UpdatePropertyRequest(
                    "buy", "apartment", "Title LV", null, null,
                    null, null, null,
                    new BigDecimal("100000"), (short) 2, new BigDecimal("50"),
                    null, (short) 1, (short) 5, (short) 2010,
                    null, null, null, null
            );

            assertThatThrownBy(() -> propertyService.update(p.getId(), req, owner.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
        }

        @Test
        void clearsCompletion_whenBlankInRequest() {
            User owner = user();
            Property p = property(owner);
            p.setCompletion(PropertyCompletion.READY);
            when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));
            when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(propertyMapper.toDto(any())).thenAnswer(inv -> itemDto(inv.getArgument(0)));

            UpdatePropertyRequest req = new UpdatePropertyRequest(
                    "buy", "apartment", "Title LV", null, null,
                    "Desc LV", null, null,
                    new BigDecimal("100000"), (short) 2, new BigDecimal("50"),
                    null, (short) 1, (short) 5, (short) 2010,
                    null, null, null, null
            );

            propertyService.update(p.getId(), req, owner.getId());

            assertThat(p.getCompletion()).isNull();
        }
    }

    @Nested
    class Delete {
        @Test
        void removesProperty_andDeletesS3Photos() {
            User owner = user();
            Property p = propertyWithPhotos(owner);
            when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));

            propertyService.delete(p.getId(), owner.getId());
            triggerAfterCommit();

            verify(propertyRepository).delete(p);
            verify(uploadService).deleteObjects(List.of(
                    "https://cdn.test.local/uploads/photo1.jpg",
                    "https://cdn.test.local/uploads/photo2.jpg"
            ));
        }

        @Test
        void throwsForbidden_whenNotOwner() {
            User owner = user();
            Property p = property(owner);
            when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> propertyService.delete(p.getId(), UUID.randomUUID()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));
        }

        @Test
        void handlesS3Failure_gracefully() {
            User owner = user();
            Property p = propertyWithPhotos(owner);
            when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));
            doThrow(new RuntimeException("S3 down")).when(uploadService).deleteObjects(any());

            propertyService.delete(p.getId(), owner.getId());
            triggerAfterCommit();  // S3 throws inside afterCommit but is caught there
            verify(propertyRepository).delete(p);
        }

        @Test
        void doesNotCallS3_whenPropertyHasNoPhotos() {
            User owner = user();
            Property p = property(owner); // no photos
            when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));

            propertyService.delete(p.getId(), owner.getId());

            verify(propertyRepository).delete(p);
            verify(uploadService, never()).deleteObjects(any());
        }
    }

    static Stream<Arguments> sortOptions() {
        return Stream.of(
                Arguments.of("newest",          Property_.POSTED_AT,    Sort.Direction.DESC),
                Arguments.of("price-asc",        Property_.PRICE,        Sort.Direction.ASC),
                Arguments.of("price-desc",       Property_.PRICE,        Sort.Direction.DESC),
                Arguments.of("m2-desc",          Property_.M2,           Sort.Direction.DESC),
                Arguments.of("price-per-m2-asc", Property_.PRICE_PER_M2, Sort.Direction.ASC)
        );
    }

    private PropertyListItemDto listItemDto(Property p) {
        var tr = p.getTranslations();
        return new PropertyListItemDto(
                p.getId(), p.getOwner().getId(), p.getListingType().getDbValue(),
                p.getPropertyCategory().getDbValue(),
                t(tr, "lv", PropertyTranslation::getTitle),
                t(tr, "en", PropertyTranslation::getTitle),
                t(tr, "ru", PropertyTranslation::getTitle),
                p.getPrice(), p.getRooms(), p.getM2(), p.getLandM2(), p.getFloor(),
                p.getTotalFloors(), p.getYearBuilt(), List.of(), p.getDistrictSlug(),
                p.getCitySlug(), p.getAddress(), null, p.getPostedAt(), null, p.getExpiresAt()
        );
    }

    private PropertyItemDto itemDto(Property p) {
        var tr = p.getTranslations();
        return new PropertyItemDto(
                p.getId(), p.getOwner().getId(), p.getListingType().getDbValue(),
                p.getPropertyCategory().getDbValue(),
                t(tr, "lv", PropertyTranslation::getTitle),
                t(tr, "en", PropertyTranslation::getTitle),
                t(tr, "ru", PropertyTranslation::getTitle),
                t(tr, "lv", PropertyTranslation::getDescription),
                t(tr, "en", PropertyTranslation::getDescription),
                t(tr, "ru", PropertyTranslation::getDescription),
                p.getPrice(), p.getRooms(),
                p.getM2(), p.getLandM2(), p.getFloor(), p.getTotalFloors(), p.getYearBuilt(),
                List.of(), p.getDistrictSlug(), p.getCitySlug(), p.getAddress(),
                new CoordsDto(p.getLat(), p.getLng()), List.of(), null, null, null, p.getPostedAt(), null, p.getExpiresAt()
        );
    }

    private static <R> R t(java.util.Map<String, PropertyTranslation> tr, String locale,
                           java.util.function.Function<PropertyTranslation, R> fn) {
        PropertyTranslation pt = tr.get(locale);
        return pt != null ? fn.apply(pt) : null;
    }
}
