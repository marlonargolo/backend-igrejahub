package com.igrejahub.reports.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.reports.dto.ReportJobDto;
import com.igrejahub.reports.entity.ReportJob;
import com.igrejahub.reports.enums.ReportFormat;
import com.igrejahub.reports.enums.ReportStatus;
import com.igrejahub.reports.enums.ReportType;
import com.igrejahub.reports.mapper.ReportJobMapper;
import com.igrejahub.reports.repository.ReportJobRepository;
import com.igrejahub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportJobService {
    private final ReportJobRepository reportJobRepository;
    private final ReportJobMapper reportJobMapper;
    private final SecurityUtils securityUtils;

    public Page<ReportJobDto> getUserReports(Pageable pageable) {
        Long orgId = TenantContext.getCurrentTenant();
        Long userId = securityUtils.getCurrentUserId();
        return reportJobRepository.findByOrganizationIdAndUserId(orgId, userId, pageable)
            .map(reportJobMapper::toDto);
    }

    public ReportJobDto getReport(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        ReportJob report = reportJobRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ReportJob", id));
        // Validate organization
        if (!report.getOrganizationId().equals(orgId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        return reportJobMapper.toDto(report);
    }

    @Transactional
    public ReportJobDto createReport(String name, ReportType type, ReportFormat format, Map<String, Object> filters) {
        Long orgId = TenantContext.getCurrentTenant();
        Long userId = securityUtils.getCurrentUserId();
        
        ReportJob report = new ReportJob();
        report.setOrganizationId(orgId);
        report.setUserId(userId);
        report.setName(name);
        report.setType(type.name());
        report.setFormat(format.name());
        report.setStatus(ReportStatus.PENDING.name());
        report.setFilters(filters);
        
        report = reportJobRepository.save(report);
        processReportAsync(report.getId());
        return reportJobMapper.toDto(report);
    }

    @Async
    @Transactional
    public void processReportAsync(Long reportId) {
        try {
            ReportJob report = reportJobRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("ReportJob", reportId));
            report.setStatus(ReportStatus.PROCESSING.name());
            reportJobRepository.save(report);
            Thread.sleep(5000);
            report.setStatus(ReportStatus.COMPLETED.name());
            report.setFileUrl("/reports/" + reportId + ".pdf");
            report.setCompletedAt(LocalDateTime.now());
            reportJobRepository.save(report);
            log.info("Report {} completed", reportId);
        } catch (Exception e) {
            log.error("Error processing report", e);
            ReportJob report = reportJobRepository.findById(reportId).orElse(null);
            if (report != null) {
                report.setStatus(ReportStatus.FAILED.name());
                report.setErrorMessage(e.getMessage());
                reportJobRepository.save(report);
            }
        }
    }

    @Transactional
    public ReportJobDto downloadReport(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        ReportJob report = reportJobRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ReportJob", id));
        if (!report.getOrganizationId().equals(orgId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        if (!report.getStatus().equals(ReportStatus.COMPLETED.name())) {
            throw new BusinessException("Relatório não está pronto para download");
        }
        return reportJobMapper.toDto(report);
    }
}
