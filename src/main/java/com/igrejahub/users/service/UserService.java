package com.igrejahub.users.service;

import com.igrejahub.audit.service.AuditLogService;
import com.igrejahub.churches.repository.ChurchRepository;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.congregations.repository.CongregationRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final Set<String> NEVER_DELEGATABLE = Set.of(
        "ROOT_ACCESS", "PLAN_MANAGE", "CHURCH_MANAGE",
        "BILLING_MANAGE", "BILLING_VIEW", "ACCOUNTING_ADMIN", "AUDIT_VIEW"
    );

    private static final Set<String> BLOCKED_INDIVIDUAL_PERMISSIONS = Set.of(
        "ROOT_ACCESS", "BILLING_MANAGE", "BILLING_VIEW",
        "ACCOUNTING_ADMIN", "AUDIT_VIEW"
    );

    private final UserRepository  userRepository;
    private final RoleRepository  roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper      userMapper;
    private final SecurityUtils   securityUtils;
    private final JdbcTemplate    jdbcTemplate;
    private final AuditLogService auditLogService;
    private final ChurchRepository       churchRepository;
    private final CongregationRepository congregationRepository;

    // ── Listagem ──────────────────────────────────────────────────────────────

    public Page<UserDto> getUsers(Pageable pageable, String search) {
        Long orgId             = TenantContext.getCurrentTenant();
        Long effectiveChurchId = securityUtils.getEffectiveChurchId();

        if (securityUtils.canViewAll()) {
            return search != null && !search.isEmpty()
                ? userRepository.findByOrganizationIdAndNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    orgId, search, search, pageable).map(userMapper::toDto)
                : userRepository.findByOrganizationId(orgId, pageable).map(userMapper::toDto);
        }

        if (effectiveChurchId == null) return Page.empty(pageable);

        Long userCongId = TenantContext.getCurrentCongregationId();
        if (userCongId != null && !securityUtils.isRoot()) {
            return search != null && !search.isEmpty()
                ? userRepository.findByOrganizationIdAndCongregationIdAndSearch(
                    orgId, userCongId, search, pageable).map(userMapper::toDto)
                : userRepository.findByOrganizationIdAndCongregationId(
                    orgId, userCongId, pageable).map(userMapper::toDto);
        }

        return search != null && !search.isEmpty()
            ? userRepository.findByOrganizationIdAndChurchIdAndSearch(
                orgId, effectiveChurchId, search, pageable).map(userMapper::toDto)
            : userRepository.findByOrganizationIdAndChurchId(
                orgId, effectiveChurchId, pageable).map(userMapper::toDto);
    }

    public UserDto getUser(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        User user = userRepository.findByOrganizationIdAndId(orgId, id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (!securityUtils.canViewAll()) assertManageable(user);
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

        Long callerCongId  = TenantContext.getCurrentCongregationId();
        boolean rootGlobal = securityUtils.isRoot() && securityUtils.canViewAll();

        Long targetChurchId;
        Long targetCongId;

        if (rootGlobal) {
            // ROOT sem Igreja selecionada: churchId e congregationId vêm do corpo,
            // mas a congregação precisa pertencer à Igreja informada.
            if (request.getChurchId() == null) {
                throw new BusinessException("Selecione a Igreja para o novo usuário.");
            }
            targetChurchId = request.getChurchId();
            if (!churchRepository.existsByOrganizationIdAndId(orgId, targetChurchId)) {
                throw new BusinessException("Igreja não encontrada.");
            }
            targetCongId = request.getCongregationId();
            if (targetCongId != null && !congregationRepository
                    .existsByOrganizationIdAndIdAndChurchId(orgId, targetCongId, targetChurchId)) {
                throw new BusinessException("A congregação selecionada não pertence à Igreja informada.");
            }
        } else if (callerCongId != null) {
            // Usuário de congregação (ex.: PASTOR_CONGREGACAO): só pode criar
            // usuários dentro da própria congregação — ignora qualquer valor do corpo.
            targetChurchId = securityUtils.getEffectiveChurchId();
            if (targetChurchId == null) {
                throw new BusinessException("Seu usuário não está vinculado a nenhuma Igreja.");
            }
            targetCongId = callerCongId;
        } else {
            // Admin/pastor principal de Igreja (sem congregação própria) ou ROOT
            // em modo filtrado: churchId vem do contexto; congregationId pode ser
            // escolhido explicitamente, desde que pertença à própria Igreja.
            targetChurchId = securityUtils.getEffectiveChurchId();
            if (targetChurchId == null) {
                throw new BusinessException("Seu usuário não está vinculado a nenhuma Igreja.");
            }
            targetCongId = request.getCongregationId();
            if (targetCongId != null && !congregationRepository
                    .existsByOrganizationIdAndIdAndChurchId(orgId, targetCongId, targetChurchId)) {
                throw new BusinessException("Você só pode vincular o usuário a uma congregação da sua própria Igreja.");
            }
        }
        user.setChurchId(targetChurchId);
        user.setCongregationId(targetCongId);

        assignRole(user, request.getRoleIds(), request.getRoleName());
        user = userRepository.save(user);

        jdbcTemplate.update(
            "INSERT INTO user_church_access(user_id,church_id) VALUES(?,?) ON CONFLICT DO NOTHING",
            user.getId(), user.getChurchId());
        if (user.getCongregationId() != null) {
            jdbcTemplate.update(
                "INSERT INTO user_congregation_access(user_id,congregation_id) VALUES(?,?) ON CONFLICT DO NOTHING",
                user.getId(), user.getCongregationId());
        }

        log.info("User created: {} church={} congregation={} by={}", user.getEmail(),
            user.getChurchId(), user.getCongregationId(), TenantContext.getCurrentUserId());
        auditLogService.logAction("CREATE_USER", "USER", user.getId(), null,
            Map.of("email", user.getEmail(), "churchId", String.valueOf(user.getChurchId()),
                "congregationId", String.valueOf(user.getCongregationId())));
        return userMapper.toDto(user);
    }

    // ── Update / Delete ───────────────────────────────────────────────────────

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        User user = userRepository.findByOrganizationIdAndId(orgId, id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (!securityUtils.canViewAll()) assertManageable(user);

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
        if (!securityUtils.canViewAll()) assertManageable(user);
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void enableUser(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        User user = userRepository.findByOrganizationIdAndId(orgId, id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (!securityUtils.canViewAll()) assertManageable(user);
        user.setActive(true);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        User user = userRepository.findByOrganizationIdAndId(orgId, id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (!securityUtils.canViewAll()) assertManageable(user);
        user.softDelete(securityUtils.getCurrentUserId());
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

    // ── Vínculos ──────────────────────────────────────────────────────────────
    // user_church_access / user_congregation_access concedem acesso ADICIONAL
    // (além da Igreja/Congregação primária do usuário). Só podem ser geridos
    // dentro do próprio escopo do chamador — nunca para fora dele.

    public List<Map<String, Object>> getUserChurches(Long userId) {
        getManageableUser(userId);
        return jdbcTemplate.queryForList(
            "SELECT c.id, c.name, c.city, c.state FROM churches c " +
            "JOIN user_church_access uca ON c.id = uca.church_id WHERE uca.user_id = ?", userId);
    }

    @Transactional
    public void setUserChurches(Long userId, Set<Long> churchIds) {
        getManageableUser(userId);
        Long orgId = TenantContext.getCurrentTenant();
        if (!securityUtils.canViewAll() && churchIds != null) {
            Long callerChurchId = securityUtils.getEffectiveChurchId();
            for (Long churchId : churchIds) {
                if (callerChurchId == null || !callerChurchId.equals(churchId)) {
                    throw new BusinessException("Você só pode conceder acesso à sua própria Igreja.");
                }
            }
        } else if (churchIds != null) {
            for (Long churchId : churchIds) {
                if (!churchRepository.existsByOrganizationIdAndId(orgId, churchId)) {
                    throw new BusinessException("Igreja não encontrada: " + churchId);
                }
            }
        }
        jdbcTemplate.update("DELETE FROM user_church_access WHERE user_id = ?", userId);
        if (churchIds != null) {
            for (Long churchId : churchIds) {
                jdbcTemplate.update(
                    "INSERT INTO user_church_access(user_id,church_id) VALUES(?,?) ON CONFLICT DO NOTHING",
                    userId, churchId);
            }
        }
        auditLogService.logAction("SET_USER_CHURCHES", "USER", userId, null, churchIds);
    }

    public List<Map<String, Object>> getUserCongregations(Long userId) {
        getManageableUser(userId);
        return jdbcTemplate.queryForList(
            "SELECT cg.id, cg.name, cg.church_id FROM congregations cg " +
            "JOIN user_congregation_access uca ON cg.id = uca.congregation_id WHERE uca.user_id = ?", userId);
    }

    @Transactional
    public void setUserCongregations(Long userId, Set<Long> congregationIds) {
        getManageableUser(userId);
        Long orgId = TenantContext.getCurrentTenant();
        if (congregationIds != null) {
            Long callerCongId   = TenantContext.getCurrentCongregationId();
            Long callerChurchId = securityUtils.getEffectiveChurchId();
            for (Long congId : congregationIds) {
                if (securityUtils.canViewAll()) {
                    continue; // ROOT global: livre, desde que exista na organização
                }
                if (callerCongId != null && !securityUtils.isRoot()) {
                    if (!callerCongId.equals(congId)) {
                        throw new BusinessException("Você só pode conceder acesso à sua própria Congregação.");
                    }
                } else if (callerChurchId == null || !congregationRepository
                        .existsByOrganizationIdAndIdAndChurchId(orgId, congId, callerChurchId)) {
                    throw new BusinessException("Você só pode conceder acesso a Congregações da sua própria Igreja.");
                }
            }
        }
        jdbcTemplate.update("DELETE FROM user_congregation_access WHERE user_id = ?", userId);
        if (congregationIds != null) {
            for (Long congId : congregationIds) {
                jdbcTemplate.update(
                    "INSERT INTO user_congregation_access(user_id,congregation_id) VALUES(?,?) ON CONFLICT DO NOTHING",
                    userId, congId);
            }
        }
        auditLogService.logAction("SET_USER_CONGREGATIONS", "USER", userId, null, congregationIds);
    }

    // ── Permissões individuais ────────────────────────────────────────────────

    public List<String> getUserPermissions(Long userId) {
        getManageableUser(userId);
        return jdbcTemplate.queryForList(
            "SELECT p.name FROM permissions p " +
            "JOIN user_permissions up ON p.id = up.permission_id WHERE up.user_id = ?",
            String.class, userId);
    }

    @Transactional
    public void setUserPermissions(Long userId, Set<String> permissionNames) {
        getManageableUser(userId);
        Long grantedBy = securityUtils.getCurrentUserId();
        boolean isRoot = securityUtils.isRoot();
        Set<String> callerPermissions = securityUtils.getCurrentUser()
            .map(u -> u.getPermissions()).orElse(Set.of());
        Set<String> toGrant = permissionNames.stream()
            .filter(n -> isRoot || !BLOCKED_INDIVIDUAL_PERMISSIONS.contains(n))
            // não é possível conceder a outro usuário uma permissão que o próprio chamador não tem
            .filter(n -> isRoot || callerPermissions.contains(n))
            .collect(java.util.stream.Collectors.toSet());
        jdbcTemplate.update("DELETE FROM user_permissions WHERE user_id = ?", userId);
        for (String name : toGrant) {
            jdbcTemplate.queryForList("SELECT id FROM permissions WHERE name = ?", Long.class, name)
                .stream().findFirst()
                .ifPresent(permId -> jdbcTemplate.update(
                    "INSERT INTO user_permissions(user_id,permission_id,granted_by) VALUES(?,?,?) ON CONFLICT DO NOTHING",
                    userId, permId, grantedBy));
        }
        auditLogService.logAction("SET_USER_PERMISSIONS", "USER", userId, null, toGrant);
    }

    /** Busca o usuário alvo garantindo organização e escopo (Igreja/Congregação) do chamador. */
    private User getManageableUser(Long userId) {
        Long orgId = TenantContext.getCurrentTenant();
        User user = userRepository.findByOrganizationIdAndId(orgId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!securityUtils.canViewAll()) assertManageable(user);
        return user;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    private void assertRoleDelegatable(Role role) {
        if (securityUtils.isRoot()) return;
        if ("ROOT".equals(role.getName())) {
            throw new BusinessException("A role ROOT não pode ser atribuída.");
        }
        Set<String> rolePerms = role.getPermissions().stream()
            .map(p -> p.getName())
            .collect(java.util.stream.Collectors.toSet());
        for (String p : rolePerms) {
            if (NEVER_DELEGATABLE.contains(p)) {
                throw new BusinessException(
                    "A role '" + role.getName() + "' contém permissões de sistema.");
            }
        }
        Set<String> callerPerms = securityUtils.getCurrentUser()
            .map(u -> u.getPermissions()).orElse(Set.of());
        for (String p : rolePerms) {
            if (!callerPerms.contains(p)) {
                throw new BusinessException(
                    "Você não pode atribuir a role '" + role.getName() +
                    "' pois ela contém a permissão '" + p + "' que você não possui.");
            }
        }
    }

    /**
     * Garante que o usuário alvo está dentro do escopo do chamador:
     *   - mesma Igreja (para todos os não-ROOT);
     *   - mesma Congregação, quando o chamador é restrito a uma congregação
     *     (ex.: PASTOR_CONGREGACAO não pode gerenciar usuários de outra
     *     congregação, mesmo que da mesma Igreja).
     */
    private void assertManageable(User target) {
        Long callerChurchId = securityUtils.getEffectiveChurchId();
        if (callerChurchId != null && !callerChurchId.equals(target.getChurchId())) {
            throw new BusinessException("Você não tem permissão para gerenciar este usuário.");
        }
        Long callerCongId = TenantContext.getCurrentCongregationId();
        if (callerCongId != null && !securityUtils.isRoot()
                && !callerCongId.equals(target.getCongregationId())) {
            throw new BusinessException("Você não tem permissão para gerenciar este usuário.");
        }
    }
}