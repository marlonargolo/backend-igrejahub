package com.igrejahub.accounting.repository;

import com.igrejahub.accounting.entity.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    Page<JournalEntry> findByOrganizationId(Long organizationId, Pageable pageable);
    Page<JournalEntry> findByOrganizationIdAndStatus(Long organizationId, String status, Pageable pageable);
    Optional<JournalEntry> findByOrganizationIdAndId(Long organizationId, Long id);
    long countByOrganizationId(Long organizationId);
    boolean existsByOrganizationIdAndEntryNumber(Long organizationId, String entryNumber);
}