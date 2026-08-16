package com.igrejahub.reports.repository;

import com.igrejahub.common.repository.BaseRepository;
import com.igrejahub.reports.entity.ReportJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReportJobRepository extends BaseRepository<ReportJob, Long> {
    Page<ReportJob> findByOrganizationIdAndUserId(Long orgId, Long userId, Pageable pageable);
    List<ReportJob> findByOrganizationIdAndStatus(Long orgId, String status);
}
