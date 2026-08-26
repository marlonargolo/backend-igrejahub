package com.igrejahub.users.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;
    private final JdbcTemplate jdbcTemplate;

    public Page<UserDto> getUsers(Pageable pageable, String search) {
        Long organizationId = TenantContext.getCurrentTenant();
        if (search != null && !search.isEmpty()) {
            return userRepository.findByOrganizationIdAndNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                organizationId, search, search, pageable)
                .map(userMapper::toDto);
        }
        return userRepository.findByOrganizationId(organizationId, pageable)
            .map(userMapper::toDto);
    }

    public UserDto getUser(Long id) {
        Long organizationId = TenantContext.getCurrentTenant();
        User user = userRepository.findByOrganizationIdAndId(organizationId, id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        Long organizationId = TenantContext.getCurrentTenant();

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email já cadastrado: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setOrganizationId(organizationId);
        user.setActive(request.getActive() != null ? request.getActive() : true);
        user.setVerified(false);
        if (request.getChurchId() != null) {
            user.setChurchId(request.getChurchId());
        }

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            for (Long roleId : request.getRoleIds()) {
                roleRepository.findById(roleId).ifPresent(user.getRoles()::add);
            }
        } else if (request.getRoleName() != null && !request.getRoleName().isBlank()) {
            roleRepository.findByName(request.getRoleName())
                .ifPresent(user.getRoles()::add);
        }

        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        Long organizationId = TenantContext.getCurrentTenant();
        User user = userRepository.findByOrganizationIdAndId(organizationId, id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (request.getName() != null) user.setName(request.getName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getActive() != null) user.setActive(request.getActive());

        if (request.getRoleIds() != null) {
            user.getRoles().clear();
            for (Long roleId : request.getRoleIds()) {
                roleRepository.findById(roleId).ifPresent(user.getRoles()::add);
            }
        }

        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional
    public void disableUser(Long id) {
        Long organizationId = TenantContext.getCurrentTenant();
        User user = userRepository.findByOrganizationIdAndId(organizationId, id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void enableUser(Long id) {
        Long organizationId = TenantContext.getCurrentTenant();
        User user = userRepository.findByOrganizationIdAndId(organizationId, id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
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

    public java.util.List<java.util.Map<String,Object>> getUserChurches(Long userId) {
        return jdbcTemplate.queryForList(
            "SELECT c.id, c.name, c.city, c.state FROM churches c " +
            "JOIN user_church_access uca ON c.id = uca.church_id " +
            "WHERE uca.user_id = ?", userId);
    }

    @Transactional
    public void setUserChurches(Long userId, java.util.Set<Long> churchIds) {
        jdbcTemplate.update("DELETE FROM user_church_access WHERE user_id = ?", userId);
        if (churchIds != null) {
            for (Long churchId : churchIds) {
                jdbcTemplate.update(
                    "INSERT INTO user_church_access(user_id, church_id) VALUES(?,?) ON CONFLICT DO NOTHING",
                    userId, churchId);
            }
        }
    }

    public java.util.List<java.util.Map<String,Object>> getUserCongregations(Long userId) {
        return jdbcTemplate.queryForList(
            "SELECT cg.id, cg.name, cg.church_id, c.name as church_name FROM congregations cg " +
            "JOIN user_congregation_access uca ON cg.id = uca.congregation_id " +
            "JOIN churches c ON c.id = cg.church_id " +
            "WHERE uca.user_id = ?", userId);
    }

    @Transactional
    public void setUserCongregations(Long userId, java.util.Set<Long> congregationIds) {
        jdbcTemplate.update("DELETE FROM user_congregation_access WHERE user_id = ?", userId);
        if (congregationIds != null) {
            for (Long congId : congregationIds) {
                jdbcTemplate.update(
                    "INSERT INTO user_congregation_access(user_id, congregation_id) VALUES(?,?) ON CONFLICT DO NOTHING",
                    userId, congId);
            }
        }
    }
}