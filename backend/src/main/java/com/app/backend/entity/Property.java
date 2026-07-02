package com.app.backend.entity;

import com.app.backend.enums.BathroomLayout;
import com.app.backend.enums.EnergyClass;
import com.app.backend.enums.HeatingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyFeature;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"features", "photos", "plans", "owner"})
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
    @Column(name = "property_category", nullable = false)
    private PropertyCategory propertyCategory;

    @Column(name = "rooms", nullable = false)
    private Short rooms;

    @Column(name = "bedrooms")
    private @Nullable Short bedrooms;

    @Column(name = "bathrooms")
    private @Nullable Short bathrooms;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "bathroom_layout")
    private @Nullable BathroomLayout bathroomLayout;

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
    @Column(name = "heating")
    private @Nullable HeatingType heating;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "energy_class")
    private @Nullable EnergyClass energyClass;

    @Column(name = "maintenance_cost", precision = 10, scale = 2)
    private @Nullable BigDecimal maintenanceCost;

    @Column(name = "video_url")
    private @Nullable String videoUrl;

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

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ElementCollection
    @CollectionTable(name = "property_features", joinColumns = @JoinColumn(name = "property_id"))
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "feature")
    @BatchSize(size = 20)
    private Set<PropertyFeature> features = new HashSet<>();

    // Ordered arrays of URL strings; list order is display order
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "photos", nullable = false)
    private List<String> photos = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "plans", nullable = false)
    private List<String> plans = new ArrayList<>();

    public List<String> allMediaUrls() {
        return Stream.concat(photos.stream(), plans.stream()).toList();
    }
}
