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

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            for (Long roleId : request.getRoleIds()) {
                roleRepository.findById(roleId).ifPresent(user.getRoles()::add);
            }
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
}
