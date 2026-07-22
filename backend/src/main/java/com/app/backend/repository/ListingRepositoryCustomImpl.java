package com.app.backend.repository;

import com.app.backend.dto.property.model.LocalizedText;
import com.app.backend.dto.property.model.Location;
import com.app.backend.dto.property.model.Price;
import com.app.backend.dto.property.model.PropertyDetails;
import com.app.backend.dto.property.response.PropertyListItemDto;
import com.app.backend.entity.Listing;
import com.app.backend.entity.ListingTranslation;
import com.app.backend.entity.Property;
import com.app.backend.enums.CommercialType;
import com.app.backend.enums.LandUse;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyFeature;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@RequiredArgsConstructor
public class ListingRepositoryCustomImpl implements ListingRepositoryCustom {

    private final EntityManager em;

    /**
     * Flat scalar projection of one list-card row. The {@code features} list is hydrated
     * separately (a JPA Criteria projection cannot select a collection), so it is absent
     * here and merged in by {@link #toDto}.
     */
    private record FlatRow(
            UUID id,
            UUID propertyId,
            UUID ownerId,
            ListingType listingType,
            PropertyCategory propertyCategory,
            PropertyCategory newProjectKind,
            CommercialType commercialSubtype,
            LandUse landUse,
            BigDecimal price,
            Boolean vatIncluded,
            Short rooms,
            Short bedrooms,
            Short bathrooms,
            BigDecimal m2,
            BigDecimal landM2,
            Short floor,
            Short totalFloors,
            Short yearBuilt,
            PropertyCompletion completion,
            String districtSlug,
            String citySlug,
            String address,
            OffsetDateTime postedAt,
            OffsetDateTime expiresAt,
            List<String> photos,
            String titleLv,
            String titleEn,
            String titleRu
    ) {}

    @Override
    public Page<PropertyListItemDto> findAllForList(Specification<Listing> spec, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        long total = executeCountQuery(spec, cb);
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        CriteriaQuery<FlatRow> query = cb.createQuery(FlatRow.class);
        Root<Listing> l = query.from(Listing.class);
        Join<Listing, Property> p = l.join("property", JoinType.INNER);

        query.select(cb.construct(FlatRow.class,
            l.get("id"),
            p.get("id"),
            l.get("owner").get("id"),
            l.get("listingType"),
            l.get("propertyCategory"),
            l.get("newProjectKind"),
            l.get("commercialSubtype"),
            l.get("landUse"),
            l.get("price"),
            l.get("vatIncluded"),
            l.get("rooms"),
            l.get("bedrooms"),
            l.get("bathrooms"),
            l.get("m2"),
            l.get("landM2"),
            l.get("floor"),
            l.get("totalFloors"),
            l.get("yearBuilt"),
            l.get("completion"),
            p.get("districtSlug"),
            p.get("citySlug"),
            p.get("address"),
            l.get("postedAt"),
            l.get("expiresAt"),
            l.get("photos"),
            buildTranslationSubquery(query, cb, l, "lv"),
            buildTranslationSubquery(query, cb, l, "en"),
            buildTranslationSubquery(query, cb, l, "ru")
        ));

        Predicate predicate = spec.toPredicate(l, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }

        if (pageable.getSort().isSorted()) {
            List<Order> orders = pageable.getSort().stream()
                .map(o -> buildOrder(cb, l, p, o))
                .toList();
            query.orderBy(orders);
        }

        TypedQuery<FlatRow> tq = em.createQuery(query);
        tq.setFirstResult((int) pageable.getOffset());
        tq.setMaxResults(pageable.getPageSize());
        List<FlatRow> rows = tq.getResultList();

        if (rows.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        List<UUID> ids = rows.stream().map(FlatRow::id).toList();
        Map<UUID, List<PropertyFeature>> featuresMap = fetchFeatures(ids);

        List<PropertyListItemDto> dtos = rows.stream().map(r -> toDto(r, featuresMap)).toList();
        return new PageImpl<>(dtos, pageable, total);
    }

    private long executeCountQuery(Specification<Listing> spec, CriteriaBuilder cb) {
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Listing> cl = cq.from(Listing.class);
        cq.select(cb.count(cl));
        Predicate predicate = spec.toPredicate(cl, cq, cb);
        if (predicate != null) {
            cq.where(predicate);
        }
        Long count = em.createQuery(cq).getSingleResult();
        return count != null ? count : 0L;
    }

    private Subquery<String> buildTranslationSubquery(CriteriaQuery<?> query, CriteriaBuilder cb,
                                                      Root<Listing> l, String locale) {
        Subquery<String> sub = query.subquery(String.class);
        Root<ListingTranslation> lt = sub.from(ListingTranslation.class);
        sub.select(lt.<String>get("title"));
        sub.where(
            cb.equal(lt.get("listing"), l),
            cb.equal(lt.<String>get("locale"), locale)
        );
        return sub;
    }

    private Order buildOrder(CriteriaBuilder cb, Root<Listing> l, Join<Listing, Property> p,
                             org.springframework.data.domain.Sort.Order o) {
        Expression<?> expr = switch (o.getProperty()) {
            case "m2" -> l.get("m2");
            case "pricePerM2" -> cb.quot(l.<BigDecimal>get("price"), l.<BigDecimal>get("m2"));
            default -> l.get(o.getProperty());
        };
        return o.isAscending() ? cb.asc(expr) : cb.desc(expr);
    }

    private Map<UUID, List<PropertyFeature>> fetchFeatures(List<UUID> ids) {
        List<Object[]> rows = em.createQuery(
                "SELECT l.id, f FROM Listing l JOIN l.features f WHERE l.id IN :ids",
                Object[].class)
            .setParameter("ids", ids)
            .getResultList();

        Map<UUID, List<PropertyFeature>> map = new HashMap<>();
        for (Object[] row : rows) {
            UUID listingId = (UUID) row[0];
            PropertyFeature feat = (PropertyFeature) row[1];
            map.computeIfAbsent(listingId, _ -> new ArrayList<>()).add(feat);
        }
        map.values().forEach(list -> list.sort(null)); // deterministic (enum declaration) order
        return map;
    }

    private PropertyListItemDto toDto(FlatRow r, Map<UUID, List<PropertyFeature>> featuresMap) {
        List<PropertyFeature> features = featuresMap.get(r.id());
        String coverPhoto = (r.photos() == null || r.photos().isEmpty()) ? null : r.photos().getFirst();

        PropertyDetails details = PropertyDetails.builder()
            .rooms(r.rooms())
            .bedrooms(r.bedrooms())
            .bathrooms(r.bathrooms())
            .m2(r.m2())
            .landM2(r.landM2())
            .floor(r.floor())
            .totalFloors(r.totalFloors())
            .yearBuilt(r.yearBuilt())
            .build();

        return PropertyListItemDto.builder()
            .id(r.id())
            .propertyId(r.propertyId())
            .ownerId(r.ownerId())
            .type(r.listingType())
            .propertyKind(r.propertyCategory())
            .newProjectKind(r.newProjectKind())
            .commercialSubtype(r.commercialSubtype())
            .landUse(r.landUse())
            .price(new Price(r.price(), Boolean.TRUE.equals(r.vatIncluded()) ? true : null))
            .details(details)
            .translations(translations(r))
            .location(new Location(r.districtSlug(), r.citySlug(), r.address(), null, null, null, null, null))
            .completion(r.completion())
            .postedAt(r.postedAt())
            .expiresAt(r.expiresAt())
            .photo(coverPhoto)
            .features(features != null && !features.isEmpty() ? features : null)
            .build();
    }

    /** Locale-keyed titles (no descriptions on list cards), in {@code lv → en → ru} order. */
    private static Map<String, LocalizedText> translations(FlatRow r) {
        Map<String, LocalizedText> result = new LinkedHashMap<>();
        if (r.titleLv() != null) result.put("lv", new LocalizedText(r.titleLv(), null));
        if (r.titleEn() != null) result.put("en", new LocalizedText(r.titleEn(), null));
        if (r.titleRu() != null) result.put("ru", new LocalizedText(r.titleRu(), null));
        return result;
    }
}
