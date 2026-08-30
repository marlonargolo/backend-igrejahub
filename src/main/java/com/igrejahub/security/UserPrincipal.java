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
    private final Long churchId;
    private final Long congregationId;
    private final boolean active;
    private final boolean locked;
    private final Set<GrantedAuthority> authorities;
    private final Set<String> permissions;

    /**
     * Construtor padrão — usado quando não há permissões individuais a mesclar.
     * Carrega apenas as permissões que vêm das roles.
     */
    public UserPrincipal(User user) {
        this(user, buildRolePermissions(user));
    }

    /**
     * Construtor principal — usado pelo CustomUserDetailsService.
     * Recebe as permissões já mescladas (role + individuais).
     */
    public UserPrincipal(User user, Set<String> mergedPermissions) {
        this.id             = user.getId();
        this.name           = user.getName();
        this.username       = user.getEmail();
        this.email          = user.getEmail();
        this.password       = user.getPasswordHash();
        this.organizationId = user.getOrganizationId();
        this.churchId       = user.getChurchId();
        this.congregationId = user.getCongregationId();
        this.active         = user.isActive();
        this.locked         = user.isLocked();
        this.authorities    = user.getRoles() != null
            ? user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet())
            : new HashSet<>();
        this.permissions    = mergedPermissions != null ? mergedPermissions : new HashSet<>();
    }

    /** Construtor legado para compatibilidade */
    public UserPrincipal(Long id, String name, String email, String password, Long organizationId,
                         Set<GrantedAuthority> authorities, Set<String> permissions) {
        this.id             = id;
        this.name           = name;
        this.username       = email;
        this.email          = email;
        this.password       = password;
        this.organizationId = organizationId;
        this.churchId       = null;
        this.congregationId = null;
        this.active         = true;
        this.locked         = false;
        this.authorities    = authorities != null ? authorities : new HashSet<>();
        this.permissions    = permissions != null ? permissions : new HashSet<>();
    }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    private static Set<String> buildRolePermissions(User user) {
        if (user.getRoles() == null) return new HashSet<>();
        return user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(p -> p.getName())
            .collect(Collectors.toSet());
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword()    { return password; }
    @Override public String getUsername()    { return username; }
    @Override public boolean isEnabled()     { return active; }
    @Override public boolean isAccountNonLocked()      { return !locked; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}