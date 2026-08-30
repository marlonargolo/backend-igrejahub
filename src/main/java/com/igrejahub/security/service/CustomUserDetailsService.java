package com.igrejahub.security.service;

import com.igrejahub.security.UserPrincipal;
import com.igrejahub.users.entity.User;
import com.igrejahub.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("🔐 Loading user by email: {}", email);

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.error("❌ User not found with email: {}", email);
                return new UsernameNotFoundException("Usuário não encontrado com email: " + email);
            });

        log.info("✅ User found: {}", user.getEmail());

        // Permissões das roles do usuário
        Set<String> rolePermissions = user.getRoles() != null
            ? user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(p -> p.getName())
                .collect(Collectors.toSet())
            : new HashSet<>();

        // Permissões individuais da tabela user_permissions
        Set<String> individualPermissions = new HashSet<>();
        try {
            List<String> extra = jdbcTemplate.queryForList(
                "SELECT p.name FROM permissions p " +
                "JOIN user_permissions up ON p.id = up.permission_id " +
                "WHERE up.user_id = ? AND p.is_active = true",
                String.class, user.getId()
            );
            individualPermissions.addAll(extra);
            if (!extra.isEmpty()) {
                log.debug("User {} has {} individual permissions: {}",
                    email, extra.size(), extra);
            }
        } catch (Exception e) {
            // Tabela pode não existir ainda (antes da migration V30)
            log.debug("Could not load individual permissions for {}: {}", email, e.getMessage());
        }

        // Mesclar: role permissions + individual permissions
        Set<String> allPermissions = new HashSet<>(rolePermissions);
        allPermissions.addAll(individualPermissions);

        return new UserPrincipal(user, allPermissions);
    }
}