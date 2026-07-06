package com.app.backend.repository;

import com.app.backend.dto.property.response.PropertyListItemDto;
import com.app.backend.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface ListingRepositoryCustom {
    Page<PropertyListItemDto> findAllForList(Specification<Listing> spec, Pageable pageable);
}
