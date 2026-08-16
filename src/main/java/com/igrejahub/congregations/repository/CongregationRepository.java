package com.igrejahub.congregations.repository;

import com.igrejahub.congregations.entity.Congregation;
import com.igrejahub.common.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CongregationRepository extends BaseRepository<Congregation, Long> {
    Page<Congregation> findByOrganizationId(Long orgId, Pageable pageable);
    Optional<Congregation> findByOrganizationIdAndId(Long orgId, Long id);
    Page<Congregation> findByOrganizationIdAndChurchId(Long orgId, Long churchId, Pageable pageable);
    Page<Congregation> findByOrganizationIdAndNameContainingIgnoreCase(Long orgId, String name, Pageable pageable);
    long countByOrganizationIdAndStatus(Long orgId, String status);
}
