package com.upisimulator.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;

/**
 * Implements {@link UserDetails} directly rather than via a separate wrapper
 * class - one fewer class to maintain, and standard enough in Spring Boot
 * codebases that it isn't worth the extra indirection here. {@code email} is
 * the Spring Security "username".
 * <p>
 * {@code @JsonIgnore} on the password is defense in depth: no controller
 * should ever serialize this entity directly (DTOs handle every response),
 * but a hashed password still shouldn't be one accidental {@code @RestController}
 * return value away from leaking.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity implements UserDetails {

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column
    private String profilePictureUrl;

    /**
     * Self-service soft delete (Phase 3), kept distinct from {@code enabled}
     * (admin-controlled freeze, Phase 12) so the two reasons an account can't
     * log in stay distinguishable in the data. Deleted rows are never
     * removed - Wallet/Transaction rows in later phases will reference this
     * user, and hard-deleting would either cascade-destroy financial records
     * or leave them orphaned.
     */
    @Column(nullable = false)
    private boolean deleted = false;

    @Column
    private LocalDateTime deletedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled && !deleted;
    }

}
