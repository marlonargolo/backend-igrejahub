package com.igrejahub.organizations.repository;

import com.igrejahub.common.repository.BaseRepository;
import com.igrejahub.organizations.entity.Organization;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends BaseRepository<Organization, Long> {
    Optional<Organization> findByCnpj(String cnpj);
    Optional<Organization> findByEmail(String email);
    boolean existsByCnpj(String cnpj);
    boolean existsByEmail(String email);
}
