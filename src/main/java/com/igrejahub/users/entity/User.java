package com.igrejahub.users.entity;

import com.igrejahub.common.entity.BaseEntity;
import com.igrejahub.roles.entity.Role;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_verified")
    @Builder.Default
    private boolean verified = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "failed_attempts")
    @Builder.Default
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    /** Igreja à qual o usuário pertence. Null apenas para ROOT sem Igreja associada. */
    @Column(name = "church_id")
    private Long churchId;

    /**
     * Congregação à qual o usuário pertence (opcional).
     * Null = acesso à Igreja toda (admin, pastor principal, tesoureiro da sede).
     * Preenchido = acesso restrito à congregação (pastor_congregacao, membro).
     * Adicionado na migration V29.
     */
    @Column(name = "congregation_id")
    private Long congregationId;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    public void incrementFailedAttempts() { this.failedAttempts++; }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }

    public void lockAccount(LocalDateTime until) { this.lockedUntil = until; }

    public boolean isLocked() {
        if (lockedUntil == null) return false;
        return lockedUntil.isAfter(LocalDateTime.now());
    }
}