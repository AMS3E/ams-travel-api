package ams.asmtravel.api.services.authorization.role.domain;

import ams.asmtravel.api.services.user.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/** A role a person holds, and since when. Written from their token at sign-in. */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(schema = "auth", name = "user_roles")
public class UserRole {

    @EmbeddedId
    private UserRoleId id = new UserRoleId();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant assignedAt;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id")
    private Role role;

    /** Both entities must already have ids; the key is built from them. */
    public UserRole(User user, Role role) {
        this.id.setUserId(user.getId());
        this.id.setRoleId(role.getId());
        this.user = user;
        this.role = role;
    }
}
