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
    private final Long churchId;         // ADICIONADO
    private final Long congregationId;   // ADICIONADO
    private final boolean active;
    private final boolean locked;
    private final Set<GrantedAuthority> authorities;
    private Set<String> permissions;

    public UserPrincipal(User user) {
        this.id             = user.getId();
        this.name           = user.getName();
        this.username       = user.getEmail();
        this.email          = user.getEmail();
        this.password       = user.getPasswordHash();
        this.organizationId = user.getOrganizationId();
        this.churchId       = user.getChurchId();
        // congregationId: User original pode não ter este campo ainda
        // usa reflexão para não quebrar compilação se o campo não existir
        Long congId = null;
        try {
            congId = (Long) user.getClass().getMethod("getCongregationId").invoke(user);
        } catch (Exception ignored) {}
        this.congregationId = congId;
        this.active  = user.isActive();
        this.locked  = user.isLocked();
        this.authorities = user.getRoles() != null
            ? user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName()))
                .collect(Collectors.toSet())
            : new HashSet<>();
        this.permissions = user.getRoles() != null
            ? user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getName())
                .collect(Collectors.toSet())
            : new HashSet<>();
    }

    /**
     * Construtor com permissões pré-mescladas (usado pelo CustomUserDetailsService
     * para combinar permissões da role com permissões individuais do usuário).
     */
    public UserPrincipal(User user, Set<String> mergedPermissions) {
        this(user);
        if (mergedPermissions != null) {
            this.permissions = mergedPermissions;
        }
    }

    public UserPrincipal(Long id, String name, String email, String password,
                         Long organizationId, Long churchId, Long congregationId,
                         Set<GrantedAuthority> authorities, Set<String> permissions) {
        this.id             = id;
        this.name           = name;
        this.username       = email;
        this.email          = email;
        this.password       = password;
        this.organizationId = organizationId;
        this.churchId       = churchId;
        this.congregationId = congregationId;
        this.active         = true;
        this.locked         = false;
        this.authorities    = authorities != null ? authorities : new HashSet<>();
        this.permissions    = permissions != null ? permissions : new HashSet<>();
    }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword()     { return password; }
    @Override public String getUsername()     { return username; }
    @Override public boolean isEnabled()      { return active; }
    @Override public boolean isAccountNonLocked()    { return !locked; }
    @Override public boolean isAccountNonExpired()   { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}