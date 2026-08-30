package com.igrejahub.users.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.roles.entity.Role;
import com.igrejahub.roles.repository.RoleRepository;
import com.igrejahub.security.SecurityUtils;
import com.igrejahub.users.dto.ChangePasswordRequest;
import com.igrejahub.users.dto.CreateUserRequest;
import com.igrejahub.users.dto.UpdateUserRequest;
import com.igrejahub.users.dto.UserDto;
import com.igrejahub.users.entity.User;
import com.igrejahub.users.mapper.UserMapper;
import com.igrejahub.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REGRAS DE NEGÓCIO:
 *
 * LISTAGEM:
 *   ROOT  → todos da organização
 *   Outros → apenas usuários da mesma Igreja (churchId do TenantContext)
 *
 * CRIAÇÃO:
 *   ROOT               → escolhe churchId livremente
 *   Não-ROOT com USER_CREATE → churchId herdado do criador; congregationId herdado se for scoped
 *   Roles atribuíveis  → apenas roles cujas permissões são subconjunto das permissões do criador
 *                        (usuário criado nunca pode ter mais poder que o criador)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    // Permissões que NUNCA podem ser delegadas a um usuário criado por não-ROOT
    private static final Set<String> NEVER_DELEGATABLE = Set.of(
        "ROOT_ACCESS", "PLAN_MANAGE", "CHURCH_MANAGE",
        "BILLING_MANAGE", "BILLING_VIEW", "ACCOUNTING_ADMIN", "AUDIT_VIEW"
    );

    private final UserRepository  userRepository;
    private final RoleRepository  roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper      userMapper;
    private final SecurityUtils   securityUtils;
    private final JdbcTemplate    jdbcTemplate;

    // ── Listagem ──────────────────────────────────────────────────────────────

    public Page<UserDto> getUsers(Pageable pageable, String search) {
        Long orgId = TenantContext.getCurrentTenant();

        // ROOT vê todos
        if (securityUtils.isRoot()) {
            return search != null && !search.isEmpty()
                ? userRepository.findByOrganizationIdAndNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    orgId, search, search, pageable).map(userMapper::toDto)
                : userRepository.findByOrganizationId(orgId, pageable).map(userMapper::toDto);
        }

        // Demais: apenas da mesma Igreja
        Long churchId = TenantContext.getCurrentChurchId();
        if (churchId == null) return Page.empty(pageable);

        return search != null && !search.isEmpty()
            ? userRepository.findByOrganizationIdAndChurchIdAndSearch(
                orgId, churchId, search, pageable).map(userMapper::toDto)
            : userRepository.findByOrganizationIdAndChurchId(
                orgId, churchId, pageable).map(userMapper::toDto);
    }

    public UserDto getUser(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        User user = userRepository.findByOrganizationIdAndId(orgId, id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        // Não-ROOT só vê usuários da mesma Igreja
        if (!securityUtils.isRoot()) {
            Long myChurch = TenantContext.getCurrentChurchId();
            if (myChurch != null && !myChurch.equals(user.getChurchId())) {
                throw new BusinessException("Acesso não autorizado a este usuário.");
            }
        }
        return userMapper.toDto(user);
    }

    // ── Criação ───────────────────────────────────────────────────────────────

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        Long orgId = TenantContext.getCurrentTenant();

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email já cadastrado: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setOrganizationId(orgId);
        user.setActive(request.getActive() != null ? request.getActive() : true);
        user.setVerified(false);

        if (securityUtils.isRoot()) {
            // ROOT: usa churchId do request (pode ser null)
            user.setChurchId(request.getChurchId());
            user.setCongregationId(null);
        } else {
            // Não-ROOT: churchId e congregationId herdados do criador
            Long callerChurchId = TenantContext.getCurrentChurchId();
            Long callerCongId   = TenantContext.getCurrentCongregationId();

            if (callerChurchId == null) {
                throw new BusinessException("Seu usuário não está vinculado a nenhuma Igreja.");
            }

            // Quota do plano
            assertUserQuota(callerChurchId);

            user.setChurchId(callerChurchId);
            // Se criador está scoped a uma congregação, novo usuário herda
            if (callerCongId != null) {
                user.setCongregationId(callerCongId);
            }
        }

        // Atribuir role — validando que o criador não delega mais poder do que tem
        assignRole(user, request.getRoleIds(), request.getRoleName());

        user = userRepository.save(user);

        // Registrar vínculos de acesso
        if (user.getChurchId() != null) {
            jdbcTemplate.update(
                "INSERT INTO user_church_access(user_id,church_id) VALUES(?,?) ON CONFLICT DO NOTHING",
                user.getId(), user.getChurchId());
        }
        if (user.getCongregationId() != null) {
            jdbcTemplate.update(
                "INSERT INTO user_congregation_access(user_id,congregation_id) VALUES(?,?) ON CONFLICT DO NOTHING",
                user.getId(), user.getCongregationId());
        }

        log.info("User created: {} churchId={} by userId={}",
            user.getEmail(), user.getChurchId(), TenantContext.getCurrentUserId());

        return userMapper.toDto(user);
    }

    // ── Update / Disable / Enable ─────────────────────────────────────────────

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        User user = userRepository.findByOrganizationIdAndId(orgId, id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        assertSameChurch(user);

        if (request.getName()   != null) user.setName(request.getName());
        if (request.getPhone()  != null) user.setPhone(request.getPhone());
        if (request.getActive() != null) user.setActive(request.getActive());

        if (request.getRoleIds() != null) {
            user.getRoles().clear();
            for (Long roleId : request.getRoleIds()) {
                roleRepository.findById(roleId).ifPresent(role -> {
                    assertRoleDelegatable(role);
                    user.getRoles().add(role);
                });
            }
        }

        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public void disableUser(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        User user = userRepository.findByOrganizationIdAndId(orgId, id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        assertSameChurch(user);
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void enableUser(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        User user = userRepository.findByOrganizationIdAndId(orgId, id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        assertSameChurch(user);
        user.setActive(true);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Senha atual incorreta");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // ── Vínculos Igreja / Congregação ─────────────────────────────────────────

    public List<Map<String, Object>> getUserChurches(Long userId) {
        return jdbcTemplate.queryForList(
            "SELECT c.id, c.name, c.city, c.state FROM churches c " +
            "JOIN user_church_access uca ON c.id = uca.church_id WHERE uca.user_id = ?", userId);
    }

    @Transactional
    public void setUserChurches(Long userId, Set<Long> churchIds) {
        jdbcTemplate.update("DELETE FROM user_church_access WHERE user_id = ?", userId);
        if (churchIds != null) {
            for (Long churchId : churchIds) {
                jdbcTemplate.update(
                    "INSERT INTO user_church_access(user_id,church_id) VALUES(?,?) ON CONFLICT DO NOTHING",
                    userId, churchId);
            }
        }
    }

    public List<Map<String, Object>> getUserCongregations(Long userId) {
        return jdbcTemplate.queryForList(
            "SELECT cg.id, cg.name, cg.church_id, c.name as church_name FROM congregations cg " +
            "JOIN user_congregation_access uca ON cg.id = uca.congregation_id " +
            "JOIN churches c ON c.id = cg.church_id WHERE uca.user_id = ?", userId);
    }

    @Transactional
    public void setUserCongregations(Long userId, Set<Long> congregationIds) {
        jdbcTemplate.update("DELETE FROM user_congregation_access WHERE user_id = ?", userId);
        if (congregationIds != null) {
            for (Long congId : congregationIds) {
                jdbcTemplate.update(
                    "INSERT INTO user_congregation_access(user_id,congregation_id) VALUES(?,?) ON CONFLICT DO NOTHING",
                    userId, congId);
            }
        }
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    /**
     * Atribui role ao usuário novo, validando que o criador não delega
     * mais permissões do que possui.
     */
    private void assignRole(User user, Set<Long> roleIds, String roleName) {
        Role role = null;

        if (roleIds != null && !roleIds.isEmpty()) {
            role = roleRepository.findById(roleIds.iterator().next()).orElse(null);
        } else if (roleName != null && !roleName.isBlank()) {
            role = roleRepository.findByName(roleName.toUpperCase()).orElse(null);
        }

        if (role == null) return;

        assertRoleDelegatable(role);
        user.getRoles().add(role);
    }

    /**
     * Garante que a role pode ser delegada pelo criador.
     *
     * Regras:
     * 1. ROOT nunca pode ser delegado
     * 2. Não-ROOT não pode delegar permissões que não possui
     * 3. Permissões em NEVER_DELEGATABLE são sempre bloqueadas para não-ROOT
     */
    private void assertRoleDelegatable(Role role) {
        if (securityUtils.isRoot()) return; // ROOT pode tudo

        if ("ROOT".equals(role.getName())) {
            throw new BusinessException("A role ROOT não pode ser atribuída.");
        }

        // Permissões da role que está sendo atribuída
        Set<String> rolePerms = role.getPermissions().stream()
            .map(p -> p.getName())
            .collect(java.util.stream.Collectors.toSet());

        // Verificar NEVER_DELEGATABLE
        for (String perm : rolePerms) {
            if (NEVER_DELEGATABLE.contains(perm)) {
                throw new BusinessException(
                    "A role '" + role.getName() + "' contém permissões de sistema que não podem ser delegadas.");
            }
        }

        // Permissões do criador (role + individuais)
        Set<String> callerPerms = securityUtils.getCurrentUser()
            .map(u -> u.getPermissions())
            .orElse(Set.of());

        // O criador não pode dar permissões que não tem
        for (String perm : rolePerms) {
            if (!callerPerms.contains(perm)) {
                throw new BusinessException(
                    "Você não pode atribuir a role '" + role.getName() +
                    "' pois ela contém a permissão '" + perm + "' que você não possui.");
            }
        }
    }

    private void assertSameChurch(User targetUser) {
        if (securityUtils.isRoot()) return;
        Long callerChurchId = TenantContext.getCurrentChurchId();
        if (callerChurchId != null && !callerChurchId.equals(targetUser.getChurchId())) {
            throw new BusinessException("Você não tem permissão para gerenciar este usuário.");
        }
    }

    private void assertUserQuota(Long churchId) {
        List<Long> maxList = jdbcTemplate.queryForList(
            "SELECT p.max_users FROM plans p JOIN churches c ON c.plan_id = p.id WHERE c.id = ?",
            Long.class, churchId);
        if (maxList.isEmpty()) return;
        long max = maxList.get(0);
        long current = userRepository.countByChurchId(churchId);
        if (current >= max) {
            throw new BusinessException(
                "Limite de usuários do plano atingido (" + max + "). Solicite upgrade.");
        }
    }
}