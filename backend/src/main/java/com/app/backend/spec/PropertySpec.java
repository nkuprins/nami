package com.app.backend.spec;

import com.app.backend.entity.Listing;
import com.app.backend.entity.Listing_;
import com.app.backend.entity.Property;
import com.app.backend.entity.Property_;
import com.app.backend.enums.PropertyFeature;
import com.app.backend.enums.PropertyStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PropertySpec {

    public static Specification<Listing> build(PropertySearchCriteria criteria) {
        return (root, query, cb) -> {
            Join<Listing, Property> p = root.join(Listing_.property, JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get(Listing_.status), PropertyStatus.ACTIVE));
            predicates.add(cb.equal(root.get(Listing_.listingType), criteria.listingType()));

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

            if (criteria.m2Min() != null) predicates.add(cb.ge(p.get(Property_.m2), criteria.m2Min()));
            if (criteria.m2Max() != null) predicates.add(cb.le(p.get(Property_.m2), criteria.m2Max()));

            if (criteria.floorMin() != null) predicates.add(cb.ge(p.get(Property_.floor), criteria.floorMin()));
            if (criteria.floorMax() != null) predicates.add(cb.le(p.get(Property_.floor), criteria.floorMax()));

            if (Boolean.TRUE.equals(criteria.notGround())) {
                predicates.add(cb.or(
                        p.get(Property_.floor).isNull(),
                        cb.notEqual(p.get(Property_.floor), (short) 1)
                ));
            }

            if (Boolean.TRUE.equals(criteria.notTop())) {
                predicates.add(cb.or(
                        p.get(Property_.floor).isNull(),
                        cb.notEqual(p.get(Property_.floor), p.get(Property_.totalFloors))
                ));
            }

            addCountFilter(predicates, cb, p.get(Property_.rooms), criteria.rooms());
            addCountFilter(predicates, cb, p.get(Property_.bedrooms), criteria.bedrooms());
            addCountFilter(predicates, cb, p.get(Property_.bathrooms), criteria.bathrooms());

            if (criteria.heating() != null && !criteria.heating().isEmpty()) {
                predicates.add(p.get(Property_.heating).in(criteria.heating()));
            }

            if (criteria.energyClass() != null && !criteria.energyClass().isEmpty()) {
                predicates.add(p.get(Property_.energyClass).in(criteria.energyClass()));
            }

            if (criteria.yearMin() != null) predicates.add(cb.ge(p.get(Property_.yearBuilt), criteria.yearMin()));
            if (criteria.yearMax() != null) predicates.add(cb.le(p.get(Property_.yearBuilt), criteria.yearMax()));

            if (criteria.features() != null && !criteria.features().isEmpty()) {
                Subquery<UUID> sub = query.subquery(UUID.class);
                Root<Property> subProp = sub.from(Property.class);
                Join<Property, PropertyFeature> featureJoin = subProp.join(Property_.features);
                sub.select(subProp.get(Property_.id));
                sub.where(
                        cb.equal(subProp.get(Property_.id), p.get(Property_.id)),
                        featureJoin.in(criteria.features())
                );
                sub.groupBy(subProp.get(Property_.id));
                sub.having(cb.equal(cb.countDistinct(featureJoin), (long) criteria.features().size()));
                predicates.add(cb.exists(sub));
            }

            if (criteria.completion() != null) {
                predicates.add(cb.equal(root.get(Listing_.completion), criteria.completion()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Multi-select count filter: exact match on each selected value, with the top bucket
     * ({@code 7}) matching "7 or more". Shared by rooms / bedrooms / bathrooms.
     */
    private static void addCountFilter(List<Predicate> predicates, CriteriaBuilder cb,
                                       Path<Short> path, List<Integer> values) {
        if (values == null || values.isEmpty()) return;
        List<Predicate> or = new ArrayList<>();
        for (int v : values) {
            if (v < 7) or.add(cb.equal(path, (short) v));
        }
        if (values.contains(7)) or.add(cb.ge(path, (short) 7));
        if (!or.isEmpty()) predicates.add(cb.or(or.toArray(new Predicate[0])));
    }
}
