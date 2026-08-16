package com.igrejahub.churches.repository;

import com.igrejahub.churches.entity.Church;
import com.igrejahub.common.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChurchRepository extends BaseRepository<Church, Long> {
    Page<Church> findByOrganizationId(Long organizationId, Pageable pageable);
    List<Church> findByOrganizationId(Long organizationId);
    Optional<Church> findByOrganizationIdAndId(Long organizationId, Long id);
    Page<Church> findByOrganizationIdAndNameContainingIgnoreCase(Long organizationId, String name, Pageable pageable);
    List<Church> findByOrganizationIdAndStatus(Long organizationId, String status);
    boolean existsByOrganizationIdAndId(Long organizationId, Long id);
    boolean existsByOrganizationIdAndCnpj(Long organizationId, String cnpj);
    long countByOrganizationIdAndStatus(Long organizationId, String status);
}
