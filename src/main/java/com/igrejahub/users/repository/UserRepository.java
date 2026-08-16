package com.igrejahub.users.repository;

import com.igrejahub.common.repository.BaseRepository;
import com.igrejahub.users.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends BaseRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByOrganizationIdAndId(Long organizationId, Long id);
    Page<User> findByOrganizationId(Long organizationId, Pageable pageable);
    Page<User> findByOrganizationIdAndNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        Long organizationId, String name, String email, Pageable pageable);
    boolean existsByEmail(String email);
    long countByOrganizationIdAndActive(Long organizationId, boolean active);
}
