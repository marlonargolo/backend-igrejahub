package com.igrejahub.users.repository;

import com.igrejahub.common.repository.BaseRepository;
import com.igrejahub.users.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends BaseRepository<User, Long> {

    // ── Existentes ────────────────────────────────────────────────────────────
    Optional<User> findByEmail(String email);
    Optional<User> findByOrganizationIdAndId(Long organizationId, Long id);
    Page<User> findByOrganizationId(Long organizationId, Pageable pageable);
    Page<User> findByOrganizationIdAndNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        Long organizationId, String name, String email, Pageable pageable);
    boolean existsByEmail(String email);
    long countByOrganizationIdAndActive(Long organizationId, boolean active);

    // ── Por Igreja ────────────────────────────────────────────────────────────
    Page<User> findByOrganizationIdAndChurchId(Long organizationId, Long churchId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.organizationId = :orgId AND u.churchId = :churchId " +
           "AND u.deleted = false AND " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%',:s,'%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%',:s,'%')))")
    Page<User> findByOrganizationIdAndChurchIdAndSearch(
        @Param("orgId") Long organizationId,
        @Param("churchId") Long churchId,
        @Param("s") String search,
        Pageable pageable);

    long countByChurchId(Long churchId);

    // ── Por Congregação ───────────────────────────────────────────────────────
    Page<User> findByOrganizationIdAndCongregationId(
        Long organizationId, Long congregationId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.organizationId = :orgId AND u.congregationId = :congId " +
           "AND u.deleted = false AND " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%',:s,'%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%',:s,'%')))")
    Page<User> findByOrganizationIdAndCongregationIdAndSearch(
        @Param("orgId") Long organizationId,
        @Param("congId") Long congregationId,
        @Param("s") String search,
        Pageable pageable);
}