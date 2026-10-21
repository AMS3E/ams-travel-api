package ams.asmtravel.api.services.destination.activity.domain;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.KhmerText;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Something a traveller does rather than visits: a dive trip, a kayak tour, a trek, a cruise. The fourth
 * kind of destination (2026-10-21), written like an attraction, with a type of its own (its interests, the
 * Activities & Experiences shelf: no tourism region, category or tag, as for a stay or a food place) and the
 * contact details of whoever runs it, which is why it is a partner's listing like a stay or a food place.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "destination", name = "destination_activities")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The partner account that runs it; null for one the staff wrote. */
    private UUID ownerUserId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ActivityType type;

    @Column(length = 500)
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String description;

    /** One line someone said about it, quoted at the top of its page on the app. */
    @Column(length = 300)
    private String slogan;

    /** Who said it. */
    @Column(length = 120)
    private String sloganBy;

    /** Khmer twins of the English text, typed by hand; null until written. */
    @Column(length = 255)
    private String nameKm;

    @Column(columnDefinition = "text")
    private String descriptionKm;

    @Column(length = 300)
    private String sloganKm;

    @Column(length = 120)
    private String sloganByKm;

    @Column(columnDefinition = "text")
    private String addressKm;

    @Column(columnDefinition = "text")
    private String address;

    /** The province it is in; its zone follows from it, as for an attraction. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id", nullable = false)
    private Province province;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    /* How to reach whoever runs it. */
    @Column(length = 50)
    private String phone;

    private String contactEmail;

    private String website;

    @Column(length = 100)
    private String telegram;

    @Column(length = 50)
    private String whatsapp;

    @Column(length = 512)
    private String facebookUrl;

    @Column(length = 512)
    private String coverKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ListingStatus status = ListingStatus.DRAFT;

    /** First opens of a day, signed in or not, all time: the popular service bumps it. */
    @Column(nullable = false)
    private Long viewCount = 0L;

    private UUID createdBy;

    private Instant publishedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /** The attraction it is run at or from, when there is one; deleting it keeps the activity. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Destination destination;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(schema = "destination", name = "destination_activity_interests",
            joinColumns = @JoinColumn(name = "activity_id"),
            inverseJoinColumns = @JoinColumn(name = "interest_id"))
    private Set<Interest> interests = new HashSet<>();

    /** Filled English fields with no Khmer yet, by request name; the dashboard counts them. */
    public List<String> missingKhmer() {
        List<String> missing = new ArrayList<>();
        if (KhmerText.lacks(name, nameKm)) {
            missing.add("nameKm");
        }
        if (KhmerText.lacks(description, descriptionKm)) {
            missing.add("descriptionKm");
        }
        if (KhmerText.lacks(slogan, sloganKm)) {
            missing.add("sloganKm");
        }
        if (KhmerText.lacks(sloganBy, sloganByKm)) {
            missing.add("sloganByKm");
        }
        if (KhmerText.lacks(address, addressKm)) {
            missing.add("addressKm");
        }
        return missing;
    }
}
