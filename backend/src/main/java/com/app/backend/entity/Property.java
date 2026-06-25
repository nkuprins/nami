package com.app.backend.entity;

import com.app.backend.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"features", "photos", "savedByUsers"})
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

    @Column(name = "title_lv")
    private String titleLv;

    @Column(name = "title_en")
    private String titleEn;

    @Column(name = "description_lv")
    private String descriptionLv;

    @Column(name = "description_en")
    private String descriptionEn;

    @Column(name = "price", nullable = false, precision = 14, scale = 2)
    private BigDecimal price;

    @Column(name = "rooms", nullable = false)
    private Short rooms;

    @Column(name = "m2", nullable = false, precision = 6, scale = 2)
    private BigDecimal m2;

    @Column(name = "land_m2", precision = 8, scale = 2)
    private BigDecimal landM2;

    @Column(name = "floor")
    private Short floor;

    @Column(name = "total_floors")
    private Short totalFloors;

    @Column(name = "year_built")
    private Short yearBuilt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "completion")
    private PropertyCompletion completion;

    @Column(name = "district_slug", nullable = false)
    private String districtSlug;

    @Column(name = "city_slug", nullable = false)
    private String citySlug;

    @Column(name = "address", nullable = false)
    private String address;

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

    @ElementCollection
    @CollectionTable(name = "property_features", joinColumns = @JoinColumn(name = "property_id"))
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "feature")
    private Set<PropertyFeature> features = new HashSet<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<PropertyPhoto> photos = new ArrayList<>();

    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY)
    private List<SavedProperty> savedByUsers = new ArrayList<>();

}
