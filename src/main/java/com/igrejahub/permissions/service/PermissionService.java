package com.igrejahub.permissions.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.permissions.dto.PermissionDto;
import com.igrejahub.permissions.entity.Permission;
import com.igrejahub.permissions.mapper.PermissionMapper;
import com.igrejahub.permissions.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {
    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.findAll().stream()
            .map(permissionMapper::toDto)
            .collect(Collectors.toList());
    }

    public List<PermissionDto> getActivePermissions() {
        return permissionRepository.findByActiveTrue().stream()
            .map(permissionMapper::toDto)
            .collect(Collectors.toList());
    }

    public PermissionDto getPermission(Long id) {
        Permission p = permissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Permission", id));
        return permissionMapper.toDto(p);
    }

    @Transactional
    public PermissionDto createPermission(PermissionDto dto) {
        if (permissionRepository.existsByName(dto.getName())) {
            throw new BusinessException("Permissão já existe: " + dto.getName());
        }
        Permission p = permissionMapper.toEntity(dto);
        p = permissionRepository.save(p);
        return permissionMapper.toDto(p);
    }

    @Transactional
    public PermissionDto updatePermission(Long id, PermissionDto dto) {
        Permission p = permissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Permission", id));
        if (!p.getName().equals(dto.getName()) && permissionRepository.existsByName(dto.getName())) {
            throw new BusinessException("Permissão já existe: " + dto.getName());
        }
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setCategory(dto.getCategory());
        p.setActive(dto.getActive() != null ? dto.getActive() : p.isActive());
        p = permissionRepository.save(p);
        return permissionMapper.toDto(p);
    }
}
