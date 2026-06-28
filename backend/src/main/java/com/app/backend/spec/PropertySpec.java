package com.app.backend.spec;

import com.app.backend.entity.Property;
import com.app.backend.entity.Property_;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyFeature;
import com.app.backend.enums.PropertyStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class PropertySpec {

    public static Specification<Property> build(PropertySearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get(Property_.status), PropertyStatus.ACTIVE));
            predicates.add(cb.equal(root.get(Property_.listingType), criteria.listingType()));

            if (criteria.locByCity() != null && !criteria.locByCity().isEmpty()) {
                List<Predicate> locPredicates = new ArrayList<>();
                for (var e : criteria.locByCity().entrySet()) {
                    locPredicates.add(cb.and(
                            cb.equal(root.get(Property_.citySlug), e.getKey()),
                            root.get(Property_.districtSlug).in(e.getValue())
                    ));
                }
                predicates.add(cb.or(locPredicates.toArray(new Predicate[0])));
            }

            if (criteria.priceMin() != null) predicates.add(cb.ge(root.get(Property_.price), criteria.priceMin()));
            if (criteria.priceMax() != null) predicates.add(cb.le(root.get(Property_.price), criteria.priceMax()));

            if (criteria.m2Min() != null) predicates.add(cb.ge(root.get(Property_.m2), criteria.m2Min()));
            if (criteria.m2Max() != null) predicates.add(cb.le(root.get(Property_.m2), criteria.m2Max()));

            if (criteria.floorMin() != null) predicates.add(cb.ge(root.get(Property_.floor), criteria.floorMin()));
            if (criteria.floorMax() != null) predicates.add(cb.le(root.get(Property_.floor), criteria.floorMax()));

            if (Boolean.TRUE.equals(criteria.notGround())) {
                predicates.add(cb.or(
                        root.get(Property_.floor).isNull(),
                        cb.notEqual(root.get(Property_.floor), (short) 1)
                ));
            }

            if (Boolean.TRUE.equals(criteria.notTop())) {
                predicates.add(cb.or(
                        root.get(Property_.floor).isNull(),
                        cb.notEqual(root.get(Property_.floor), root.get(Property_.totalFloors))
                ));
            }

            if (criteria.rooms() != null && !criteria.rooms().isEmpty()) {
                List<Predicate> roomPredicates = new ArrayList<>();
                boolean hasSevenPlus = criteria.rooms().contains(7);
                for (int r : criteria.rooms()) {
                    if (r < 7) roomPredicates.add(cb.equal(root.get(Property_.rooms), (short) r));
                }
                if (hasSevenPlus) roomPredicates.add(cb.ge(root.get(Property_.rooms), (short) 7));
                if (!roomPredicates.isEmpty()) {
                    predicates.add(cb.or(roomPredicates.toArray(new Predicate[0])));
                }
            }

            if (criteria.yearMin() != null) predicates.add(cb.ge(root.get(Property_.yearBuilt), criteria.yearMin()));
            if (criteria.yearMax() != null) predicates.add(cb.le(root.get(Property_.yearBuilt), criteria.yearMax()));

            if (criteria.features() != null && !criteria.features().isEmpty()) {
                for (PropertyFeature feat : criteria.features()) {
                    Subquery<Integer> sub = query.subquery(Integer.class);
                    Root<Property> subRoot = sub.correlate(root);
                    var featureJoin = subRoot.join(Property_.features);
                    sub.select(cb.literal(1));
                    sub.where(cb.equal(featureJoin, feat));
                    predicates.add(cb.exists(sub));
                }
            }

            if (criteria.completion() != null) {
                predicates.add(cb.equal(root.get(Property_.completion), criteria.completion()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
