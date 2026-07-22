package com.app.backend.spec;

import com.app.backend.entity.Listing;
import com.app.backend.entity.Listing_;
import com.app.backend.entity.Property;
import com.app.backend.entity.Property_;
import com.app.backend.enums.PropertyFeature;
import com.app.backend.enums.PropertyStatus;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.SetAttribute;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PropertySpec {

    /**
     * Highest room / bedroom / bathroom bucket offered in the filters; the top value
     * matches "that many or more". Mirror of the frontend's ROOM_COUNT_MAX
     * (src/types/filter.ts) — keep the two in sync.
     */
    private static final int ROOM_COUNT_MAX = 7;

    public static Specification<Listing> build(PropertySearchCriteria criteria) {
        return (root, query, cb) -> {
            // A listing is self-contained; only the shared location lives on the property.
            Join<Listing, Property> p = root.join(Listing_.property, JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();

            Path<Short> floor = root.get(Listing_.floor);
            Path<Short> totalFloors = root.get(Listing_.totalFloors);

            predicates.add(cb.equal(root.get(Listing_.status), PropertyStatus.ACTIVE));
            predicates.add(cb.equal(root.get(Listing_.listingType), criteria.listingType()));

            if (criteria.kind() != null) {
                predicates.add(cb.equal(root.get(Listing_.propertyCategory), criteria.kind()));
            }

            if (criteria.commercialSubtype() != null) {
                predicates.add(cb.equal(root.get(Listing_.commercialSubtype), criteria.commercialSubtype()));
            }

            if (criteria.landUse() != null) {
                predicates.add(cb.equal(root.get(Listing_.landUse), criteria.landUse()));
            }

            if (criteria.locByCity() != null && !criteria.locByCity().isEmpty()) {
                List<Predicate> locPredicates = new ArrayList<>();
                for (var e : criteria.locByCity().entrySet()) {
                    locPredicates.add(cb.and(
                            cb.equal(p.get(Property_.citySlug), e.getKey()),
                            p.get(Property_.districtSlug).in(e.getValue())
                    ));
                }
                predicates.add(cb.or(locPredicates.toArray(new Predicate[0])));
            }

            if (criteria.priceMin() != null) predicates.add(cb.ge(root.get(Listing_.price), criteria.priceMin()));
            if (criteria.priceMax() != null) predicates.add(cb.le(root.get(Listing_.price), criteria.priceMax()));

            if (criteria.m2Min() != null) predicates.add(cb.ge(root.get(Listing_.m2), criteria.m2Min()));
            if (criteria.m2Max() != null) predicates.add(cb.le(root.get(Listing_.m2), criteria.m2Max()));

            // Land area lives only on houses; apartments have a null landM2, so a
            // ge/le bound naturally excludes them (no extra null guard needed).
            if (criteria.landM2Min() != null) predicates.add(cb.ge(root.get(Listing_.landM2), criteria.landM2Min()));
            if (criteria.landM2Max() != null) predicates.add(cb.le(root.get(Listing_.landM2), criteria.landM2Max()));

            if (criteria.floorMin() != null) predicates.add(cb.ge(floor, criteria.floorMin()));
            if (criteria.floorMax() != null) predicates.add(cb.le(floor, criteria.floorMax()));

            if (Boolean.TRUE.equals(criteria.notGround())) {
                predicates.add(cb.or(
                        cb.isNull(floor),
                        cb.notEqual(floor, (short) 1)
                ));
            }

            if (Boolean.TRUE.equals(criteria.notTop())) {
                predicates.add(cb.or(
                        cb.isNull(floor),
                        cb.notEqual(floor, totalFloors)
                ));
            }

            addCountFilter(predicates, cb, root.get(Listing_.rooms), criteria.rooms());
            addCountFilter(predicates, cb, root.get(Listing_.bedrooms), criteria.bedrooms());
            addCountFilter(predicates, cb, root.get(Listing_.bathrooms), criteria.bathrooms());

            if (criteria.heating() != null && !criteria.heating().isEmpty()) {
                predicates.add(root.get(Listing_.heating).in(criteria.heating()));
            }

            if (criteria.energyClass() != null && !criteria.energyClass().isEmpty()) {
                predicates.add(root.get(Listing_.energyClass).in(criteria.energyClass()));
            }

            if (criteria.sewage() != null && !criteria.sewage().isEmpty()) {
                predicates.add(root.get(Listing_.sewage).in(criteria.sewage()));
            }

            if (criteria.ventilation() != null && !criteria.ventilation().isEmpty()) {
                predicates.add(root.get(Listing_.ventilation).in(criteria.ventilation()));
            }

            if (criteria.roof() != null && !criteria.roof().isEmpty()) {
                predicates.add(root.get(Listing_.roof).in(criteria.roof()));
            }

            if (criteria.yearMin() != null) predicates.add(cb.ge(root.get(Listing_.yearBuilt), criteria.yearMin()));
            if (criteria.yearMax() != null) predicates.add(cb.le(root.get(Listing_.yearBuilt), criteria.yearMax()));

            if (criteria.maintenanceCostMax() != null) {
                predicates.add(cb.le(root.get(Listing_.maintenanceCost), criteria.maintenanceCostMax()));
            }

            if (criteria.bathroomLayout() != null) {
                predicates.add(cb.equal(root.get(Listing_.bathroomLayout), criteria.bathroomLayout()));
            }

            if (Boolean.TRUE.equals(criteria.vatIncluded())) {
                predicates.add(cb.isTrue(root.get(Listing_.vatIncluded)));
            }

            if (criteria.features() != null && !criteria.features().isEmpty()) {
                Subquery<UUID> sub = query.subquery(UUID.class);
                Root<Listing> subListing = sub.from(Listing.class);
                Join<Listing, PropertyFeature> featureJoin = subListing.join(Listing_.features);
                sub.select(subListing.get(Listing_.id));
                sub.where(
                        cb.equal(subListing.get(Listing_.id), root.get(Listing_.id)),
                        featureJoin.in(criteria.features())
                );
                sub.groupBy(subListing.get(Listing_.id));
                sub.having(cb.equal(cb.countDistinct(featureJoin), (long) criteria.features().size()));
                predicates.add(cb.exists(sub));
            }

            addAnyOfFilter(predicates, query, cb, root, Listing_.ventilationSystems, criteria.ventilationSystems());
            addAnyOfFilter(predicates, query, cb, root, Listing_.communications, criteria.communications());
            addAnyOfFilter(predicates, query, cb, root, Listing_.stove, criteria.stove());
            addAnyOfFilter(predicates, query, cb, root, Listing_.security, criteria.security());
            addAnyOfFilter(predicates, query, cb, root, Listing_.extras, criteria.extras());
            addAnyOfFilter(predicates, query, cb, root, Listing_.parking, criteria.parking());

            if (criteria.completion() != null) {
                predicates.add(cb.equal(root.get(Listing_.completion), criteria.completion()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * ANY-of set filter: matches a listing that has at least one of the selected values in the
     * given collection attribute (EXISTS subquery, no all-of {@code having} clause). Shared by the
     * ventilation-systems / communications / stove / security / extras / parking attribute sets.
     */
    private static <T> void addAnyOfFilter(List<Predicate> predicates, CriteriaQuery<?> query,
                                           CriteriaBuilder cb, Root<Listing> root,
                                           SetAttribute<Listing, T> attribute, List<T> values) {
        if (values == null || values.isEmpty()) return;
        Subquery<UUID> sub = query.subquery(UUID.class);
        Root<Listing> subListing = sub.from(Listing.class);
        Join<Listing, T> join = subListing.join(attribute);
        sub.select(subListing.get(Listing_.id));
        sub.where(
                cb.equal(subListing.get(Listing_.id), root.get(Listing_.id)),
                join.in(values)
        );
        predicates.add(cb.exists(sub));
    }

    /**
     * Multi-select count filter: exact match on each selected value, with the top bucket
     * ({@link #ROOM_COUNT_MAX}) matching "that many or more". Shared by rooms / bedrooms / bathrooms.
     */
    private static void addCountFilter(List<Predicate> predicates, CriteriaBuilder cb,
                                       Path<Short> path, List<Integer> values) {
        if (values == null || values.isEmpty()) return;
        List<Predicate> or = new ArrayList<>();
        boolean hasTopBucket = false;
        for (int v : values) {
            if (v >= ROOM_COUNT_MAX) hasTopBucket = true;
            else or.add(cb.equal(path, (short) v));
        }
        if (hasTopBucket) or.add(cb.ge(path, (short) ROOM_COUNT_MAX));
        if (!or.isEmpty()) predicates.add(cb.or(or.toArray(new Predicate[0])));
    }
}
