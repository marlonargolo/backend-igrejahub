package com.igrejahub.security;

import com.igrejahub.users.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String name;
    private final String username;
    private final String email;
    private final String password;
    private final Long organizationId;
    private final boolean active;
    private final boolean locked;
    private final Set<GrantedAuthority> authorities;
    private final Set<String> permissions;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.username = user.getEmail();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.organizationId = user.getOrganizationId();
        this.active = user.isActive();
        // Delega ao método de domínio correto que verifica lockedUntil
        this.locked = user.isLocked();
        this.authorities = user.getRoles() != null ?
            user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet()) : new HashSet<>();
        this.permissions = user.getRoles() != null ?
            user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName())
                .collect(Collectors.toSet()) : new HashSet<>();
    }

    public UserPrincipal(Long id, String name, String email, String password, Long organizationId,
                         Set<GrantedAuthority> authorities, Set<String> permissions) {
        this.id = id;
        this.name = name;
        this.username = email;
        this.email = email;
        this.password = password;
        this.organizationId = organizationId;
        this.active = true;
        this.locked = false;
        this.authorities = authorities != null ? authorities : new HashSet<>();
        this.permissions = permissions != null ? permissions : new HashSet<>();
    }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /** Conta desativada (is_active = false). */
    @Override
    public boolean isEnabled() {
        return active;
    }

    /**
     * Conta bloqueada por brute-force (locked_until no futuro).
     * O Spring Security lança LockedException quando retorna false —
     * a mensagem correta é exibida ao usuário.
     */
    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    /** Sem lógica de expiração de conta no momento. */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** Sem lógica de expiração de credenciais no momento. */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
