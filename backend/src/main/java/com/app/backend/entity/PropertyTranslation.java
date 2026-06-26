package com.app.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "property_translations")
@IdClass(PropertyTranslationId.class)
@Getter
@Setter
@NoArgsConstructor
public class PropertyTranslation {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Id
    @Column(name = "locale", nullable = false)
    private String locale;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;
}
