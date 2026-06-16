package com.app.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "saved_properties")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"user", "property"})
public class SavedProperty {

    @EmbeddedId
    private SavedPropertyId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("propertyId")
    @JoinColumn(name = "property_id")
    private Property property;

    @CreationTimestamp
    @Column(name = "saved_at", nullable = false, updatable = false)
    private OffsetDateTime savedAt;
}
