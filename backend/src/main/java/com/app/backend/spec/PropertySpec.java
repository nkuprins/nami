package com.app.backend.spec;

import com.app.backend.entity.Property;
import com.app.backend.enums.*;
import jakarta.persistence.criteria.*;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PropertySpec {

    public static Specification<Property> build(
            ListingType listingType,
            List<String> loc,
            BigDecimal priceMin, BigDecimal priceMax,
            List<Integer> rooms,
            BigDecimal m2Min, BigDecimal m2Max,
            Integer floorMin, Integer floorMax,
            Boolean notGround, Boolean notTop,
            Integer yearMin, Integer yearMax,
            List<PropertyFeature> features,
            PropertyCompletion completion
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), PropertyStatus.ACTIVE));
            predicates.add(cb.equal(root.get("listingType"), listingType));

            if (loc != null && !loc.isEmpty()) {
                Map<String, List<String>> byCity = new LinkedHashMap<>();
                for (String entry : loc) {
                    int colon = entry.indexOf(':');
                    if (colon <= 0 || colon >= entry.length() - 1) continue;
                    byCity.computeIfAbsent(entry.substring(0, colon), k -> new ArrayList<>())
                            .add(entry.substring(colon + 1));
                }
                if (!byCity.isEmpty()) {
                    List<Predicate> locPredicates = new ArrayList<>();
                    for (var e : byCity.entrySet()) {
                        locPredicates.add(cb.and(
                                cb.equal(root.get("citySlug"), e.getKey()),
                                root.get("districtSlug").in(e.getValue())
                        ));
                    }
                    predicates.add(cb.or(locPredicates.toArray(new Predicate[0])));
                }
            }

            if (priceMin != null) predicates.add(cb.ge(root.get("price"), priceMin));
            if (priceMax != null) predicates.add(cb.le(root.get("price"), priceMax));

            if (m2Min != null) predicates.add(cb.ge(root.get("m2"), m2Min));
            if (m2Max != null) predicates.add(cb.le(root.get("m2"), m2Max));

            if (floorMin != null) predicates.add(cb.ge(root.<Short>get("floor"), floorMin.shortValue()));
            if (floorMax != null) predicates.add(cb.le(root.<Short>get("floor"), floorMax.shortValue()));

            if (Boolean.TRUE.equals(notGround)) {
                predicates.add(cb.or(
                        root.get("floor").isNull(),
                        cb.notEqual(root.<Short>get("floor"), (short) 1)
                ));
            }

            if (Boolean.TRUE.equals(notTop)) {
                predicates.add(cb.or(
                        root.get("floor").isNull(),
                        cb.notEqual(root.get("floor"), root.get("totalFloors"))
                ));
            }

            if (rooms != null && !rooms.isEmpty()) {
                List<Predicate> roomPredicates = new ArrayList<>();
                boolean hasFivePlus = rooms.contains(5);
                for (int r : rooms) {
                    if (r < 5) roomPredicates.add(cb.equal(root.<Short>get("rooms"), (short) r));
                }
                if (hasFivePlus) roomPredicates.add(cb.ge(root.<Short>get("rooms"), (short) 5));
                if (!roomPredicates.isEmpty()) {
                    predicates.add(cb.or(roomPredicates.toArray(new Predicate[0])));
                }
            }

            if (yearMin != null) predicates.add(cb.ge(root.<Short>get("yearBuilt"), yearMin.shortValue()));
            if (yearMax != null) predicates.add(cb.le(root.<Short>get("yearBuilt"), yearMax.shortValue()));

            if (features != null && !features.isEmpty()) {
                for (PropertyFeature feat : features) {
                    Subquery<Integer> sub = query.subquery(Integer.class);
                    Root<Property> subRoot = sub.correlate(root);
                    Join<Property, PropertyFeature> featureJoin = subRoot.join("features");
                    sub.select(cb.literal(1));
                    sub.where(cb.equal(featureJoin, feat));
                    predicates.add(cb.exists(sub));
                }
            }

            if (completion != null) {
                predicates.add(cb.equal(root.get("completion"), completion));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
