package com.app.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A physical address shared by the listings that sit at it. Every physical and
 * media attribute lives on the {@link Listing}; a property only groups listings
 * at one address (shared map pin, per-owner cap, duplicate-address guard).
 */
@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"owner"})
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

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
}
