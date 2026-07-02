package com.app.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "listing_translations")
@IdClass(ListingTranslationId.class)
@Getter
@Setter
@NoArgsConstructor
public class ListingTranslation {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Id
    @Column(name = "locale", nullable = false)
    private String locale;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;
}
