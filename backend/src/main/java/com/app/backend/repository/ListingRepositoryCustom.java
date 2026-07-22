package com.app.backend.repository;

import com.app.backend.dto.property.response.MapPinDto;
import com.app.backend.dto.property.response.PropertyListItemDto;
import com.app.backend.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface ListingRepositoryCustom {
    Page<PropertyListItemDto> findAllForList(Specification<Listing> spec, Pageable pageable);

    /** All matching listings as lightweight map pins, deduped to one per property. Unpaged. */
    List<MapPinDto> findMapPins(Specification<Listing> spec);
}
