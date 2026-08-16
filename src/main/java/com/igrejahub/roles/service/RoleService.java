package com.igrejahub.roles.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.permissions.repository.PermissionRepository;
import com.igrejahub.roles.dto.CreateRoleRequest;
import com.igrejahub.roles.dto.RoleDto;
import com.igrejahub.roles.dto.UpdateRoleRequest;
import com.igrejahub.roles.entity.Role;
import com.igrejahub.roles.mapper.RoleMapper;
import com.igrejahub.roles.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    public Page<RoleDto> getRoles(Pageable pageable) {
        return roleRepository.findAll(pageable).map(roleMapper::toDto);
    }

    public RoleDto getRole(Long id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role", id));
        return roleMapper.toDto(role);
    }

    @Transactional
    public RoleDto createRole(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new BusinessException("Role já existe: " + request.getName());
        }
        Role role = new Role();
        role.setName(request.getName().toUpperCase());
        role.setDescription(request.getDescription());
        role.setActive(true);
        role.setSystem(false);
        
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            for (Long permissionId : request.getPermissionIds()) {
                permissionRepository.findById(permissionId).ifPresent(role.getPermissions()::add);
            }
        }
        role = roleRepository.save(role);
        return roleMapper.toDto(role);
    }

    @Transactional
    public RoleDto updateRole(Long roleId, UpdateRoleRequest request) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
        if (role.isSystem()) {
            throw new BusinessException("Não é possível modificar roles do sistema");
        }
        if (request.getName() != null && !request.getName().equals(role.getName()) &&
            roleRepository.existsByName(request.getName())) {
            throw new BusinessException("Role já existe: " + request.getName());
        }
        if (request.getName() != null) role.setName(request.getName().toUpperCase());
        if (request.getDescription() != null) role.setDescription(request.getDescription());
        if (request.getActive() != null) role.setActive(request.getActive());
        
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            role.getPermissions().clear();
            for (Long permissionId : request.getPermissionIds()) {
                permissionRepository.findById(permissionId).ifPresent(role.getPermissions()::add);
            }
        }
        role = roleRepository.save(role);
        return roleMapper.toDto(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role", id));
        if (role.isSystem()) {
            throw new BusinessException("Não é possível excluir roles do sistema");
        }
        role.setActive(false);
        roleRepository.save(role);
    }
}
