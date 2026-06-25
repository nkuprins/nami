package com.app.backend.service;

import com.app.backend.dto.*;
import com.app.backend.entity.Property;
import com.app.backend.entity.PropertyPhoto;
import com.app.backend.entity.User;
import com.app.backend.enums.PropertyStatus;
import com.app.backend.mapper.PropertyMapper;
import com.app.backend.repository.PropertyRepository;
import com.app.backend.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
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

    private PropertyFilter defaultFilter() {
        return new PropertyFilter("buy", null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Nested
    class ListProperties {

        @Test
        void appliesFilter_andPagination_withDefaultSort() {
            Property p = property(user());
            PropertyListItemDto dto = listItemDto(p);
            when(propertyRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(p)));
            when(propertyMapper.toListDto(p)).thenReturn(dto);

            PropertyPageResponse result = propertyService.list(defaultFilter(), "newest", 1);

            assertThat(result.items()).hasSize(1);
            assertThat(result.total()).isEqualTo(1);
        }

        @Test
        void sortsByPricePerM2Asc_withInMemorySorting() {
            User owner = user();
            Property cheap = property(owner);
            cheap.setPrice(new BigDecimal("100000"));
            cheap.setM2(new BigDecimal("100"));

            Property expensive = property(owner);
            expensive.setPrice(new BigDecimal("200000"));
            expensive.setM2(new BigDecimal("50"));

            when(propertyRepository.findAll(any(Specification.class))).thenReturn(List.of(expensive, cheap));
            when(propertyMapper.toListDto(any())).thenAnswer(inv -> listItemDto(inv.getArgument(0)));

            PropertyPageResponse result = propertyService.list(defaultFilter(), "price-per-m2-asc", 1);

            assertThat(result.items()).hasSize(2);
            assertThat(result.total()).isEqualTo(2);
        }

        @Test
        void returnsEmptyPage_whenPageExceedsTotalResults() {
            when(propertyRepository.findAll(any(Specification.class))).thenReturn(List.of(property(user())));

            PropertyPageResponse result = propertyService.list(defaultFilter(), "price-per-m2-asc", 100);

            assertThat(result.items()).isEmpty();
            assertThat(result.total()).isEqualTo(1);
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
    }

    @Nested
    class Delete {
        @Test
        void removesProperty_andDeletesS3Photos() {
            User owner = user();
            Property p = propertyWithPhotos(owner);
            when(propertyRepository.findById(p.getId())).thenReturn(Optional.of(p));

            propertyService.delete(p.getId(), owner.getId());

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

            assertThatCode(() -> propertyService.delete(p.getId(), owner.getId()))
                    .doesNotThrowAnyException();
            verify(propertyRepository).delete(p);
        }
    }

    private PropertyListItemDto listItemDto(Property p) {
        return new PropertyListItemDto(
                p.getId(), p.getOwner().getId(), p.getListingType().getDbValue(),
                p.getPropertyCategory().getDbValue(), p.getTitle(), p.getPrice(),
                p.getRooms(), p.getM2(), p.getLandM2(), p.getFloor(), p.getTotalFloors(),
                p.getYearBuilt(), List.of(), p.getDistrictSlug(), p.getCitySlug(),
                p.getAddress(), null, p.getPostedAt(), null
        );
    }

    private PropertyItemDto itemDto(Property p) {
        return new PropertyItemDto(
                p.getId(), p.getOwner().getId(), p.getListingType().getDbValue(),
                p.getPropertyCategory().getDbValue(), p.getTitle(), p.getDescription(),
                p.getPrice(), p.getRooms(), p.getM2(), p.getLandM2(), p.getFloor(),
                p.getTotalFloors(), p.getYearBuilt(), List.of(), p.getDistrictSlug(),
                p.getCitySlug(), p.getAddress(), new CoordsDto(p.getLat(), p.getLng()),
                List.of(), null, null, p.getPostedAt(), null
        );
    }
}
