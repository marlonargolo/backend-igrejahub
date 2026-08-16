package com.igrejahub.permissions.repository;

import com.igrejahub.common.repository.BaseRepository;
import com.igrejahub.permissions.entity.Permission;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends BaseRepository<Permission, Long> {
    Optional<Permission> findByName(String name);
    List<Permission> findByCategory(String category);
    List<Permission> findByActiveTrue();
    boolean existsByName(String name);
}
