package com.igrejahub.roles.repository;

import com.igrejahub.common.repository.BaseRepository;
import com.igrejahub.roles.entity.Role;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends BaseRepository<Role, Long> {
    Optional<Role> findByName(String name);
    boolean existsByName(String name);
}
