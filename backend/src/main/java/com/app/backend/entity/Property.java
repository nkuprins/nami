package com.app.backend.entity;

import com.app.backend.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"features", "photos", "plans", "phones", "savedByUsers", "translations"})
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "listing_type", nullable = false)
    private ListingType listingType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "property_category", nullable = false)
    private PropertyCategory propertyCategory;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private PropertyStatus status;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @MapKey(name = "locale")
    @BatchSize(size = 20)
    private Map<String, PropertyTranslation> translations = new HashMap<>();

    @Column(name = "price", nullable = false, precision = 14, scale = 2)
    private BigDecimal price;

    @Column(name = "price_per_m2", precision = 14, scale = 6, insertable = false, updatable = false)
    private @Nullable BigDecimal pricePerM2;

    @Column(name = "rooms", nullable = false)
    private Short rooms;

    @Column(name = "m2", nullable = false, precision = 6, scale = 2)
    private BigDecimal m2;

    @Column(name = "land_m2", precision = 8, scale = 2)
    private @Nullable BigDecimal landM2;

    @Column(name = "floor")
    private @Nullable Short floor;

    @Column(name = "total_floors")
    private @Nullable Short totalFloors;

    @Column(name = "year_built")
    private @Nullable Short yearBuilt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "completion")
    private @Nullable PropertyCompletion completion;

    @Column(name = "district_slug", nullable = false)
    private String districtSlug;

    @Column(name = "city_slug", nullable = false)
    private String citySlug;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "video_url")
    private @Nullable String videoUrl;

    @Column(name = "lat", nullable = false)
    private Double lat;

    @Column(name = "lng", nullable = false)
    private Double lng;

    @CreationTimestamp
    @Column(name = "posted_at", nullable = false, updatable = false)
    private OffsetDateTime postedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "expiry_warning_sent", nullable = false)
    private boolean expiryWarningSent;

    @ElementCollection
    @CollectionTable(name = "property_features", joinColumns = @JoinColumn(name = "property_id"))
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "feature")
    @BatchSize(size = 20)
    private Set<PropertyFeature> features = new HashSet<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @BatchSize(size = 20)
    private List<PropertyPhoto> photos = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<PropertyPlan> plans = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<PropertyPhone> phones = new ArrayList<>();

    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY)
    private List<SavedProperty> savedByUsers = new ArrayList<>();

    public List<String> allMediaUrls() {
        return Stream.concat(
                photos.stream().map(PropertyPhoto::getUrl),
                plans.stream().map(PropertyPlan::getUrl))
                .toList();
    }
}
