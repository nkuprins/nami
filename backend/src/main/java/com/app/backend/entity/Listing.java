package com.app.backend.entity;

import com.app.backend.enums.BathroomLayout;
import com.app.backend.enums.Communication;
import com.app.backend.enums.EnergyClass;
import com.app.backend.enums.HeatingType;
import com.app.backend.enums.ListingType;
import com.app.backend.enums.ParkingType;
import com.app.backend.enums.PropertyCategory;
import com.app.backend.enums.PropertyCompletion;
import com.app.backend.enums.PropertyExtra;
import com.app.backend.enums.PropertyFeature;
import com.app.backend.enums.PropertyStatus;
import com.app.backend.enums.RoofType;
import com.app.backend.enums.SecurityFeature;
import com.app.backend.enums.SewageType;
import com.app.backend.enums.StoveType;
import com.app.backend.enums.VentilationSystem;
import com.app.backend.enums.VentilationType;
import com.app.backend.dto.property.model.PhoneContact;
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
@Table(name = "listings")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"property", "owner", "translations", "phones", "features", "photos", "plans",
        "ventilationSystems", "communications", "stove", "security", "extras", "parking"})
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "listing_type", nullable = false)
    private ListingType listingType;

    @Column(name = "price", nullable = false, precision = 14, scale = 2)
    private BigDecimal price;

    @Column(name = "vat_included", nullable = false)
    private boolean vatIncluded;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "property_category", nullable = false)
    private PropertyCategory propertyCategory;

    @Column(name = "rooms", nullable = false)
    private Short rooms;

    @Column(name = "bedrooms")
    private Short bedrooms;

    @Column(name = "bathrooms")
    private Short bathrooms;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "bathroom_layout")
    private BathroomLayout bathroomLayout;

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
    @Column(name = "heating")
    private HeatingType heating;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "energy_class")
    private EnergyClass energyClass;

    @Column(name = "maintenance_cost", precision = 10, scale = 2)
    private BigDecimal maintenanceCost;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "sewage")
    private SewageType sewage;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "ventilation")
    private VentilationType ventilation;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "roof")
    private RoofType roof;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "website_url")
    private String websiteUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "completion")
    private PropertyCompletion completion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private PropertyStatus status;

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

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @MapKey(name = "locale")
    @BatchSize(size = 20)
    private Map<String, ListingTranslation> translations = new HashMap<>();

    // Ordered array of contact entries (phone + name + email); list order is display order
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "phones", nullable = false)
    private List<PhoneContact> phones = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "listing_features", joinColumns = @JoinColumn(name = "listing_id"))
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "feature")
    @BatchSize(size = 20)
    private Set<PropertyFeature> features = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "listing_ventilation_systems", joinColumns = @JoinColumn(name = "listing_id"))
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "ventilation_system")
    @BatchSize(size = 20)
    private Set<VentilationSystem> ventilationSystems = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "listing_communications", joinColumns = @JoinColumn(name = "listing_id"))
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "communication")
    @BatchSize(size = 20)
    private Set<Communication> communications = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "listing_stove", joinColumns = @JoinColumn(name = "listing_id"))
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "stove")
    @BatchSize(size = 20)
    private Set<StoveType> stove = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "listing_security", joinColumns = @JoinColumn(name = "listing_id"))
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "security")
    @BatchSize(size = 20)
    private Set<SecurityFeature> security = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "listing_extras", joinColumns = @JoinColumn(name = "listing_id"))
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "extra")
    @BatchSize(size = 20)
    private Set<PropertyExtra> extras = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "listing_parking", joinColumns = @JoinColumn(name = "listing_id"))
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "parking")
    @BatchSize(size = 20)
    private Set<ParkingType> parking = new HashSet<>();

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
