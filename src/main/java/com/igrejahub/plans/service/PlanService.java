package com.igrejahub.plans.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.plans.dto.CreatePlanRequest;
import com.igrejahub.plans.dto.PlanDto;
import com.igrejahub.plans.entity.Plan;
import com.igrejahub.plans.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

    private final PlanRepository planRepository;

    public List<PlanDto> listPlans() {
        return planRepository.findByActiveTrue()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public PlanDto getPlan(Long id) {
        return toDto(findById(id));
    }

    @Transactional
    public PlanDto createPlan(CreatePlanRequest req) {
        if (planRepository.findByName(req.getName()).isPresent()) {
            throw new BusinessException("Já existe um plano com o nome: " + req.getName());
        }
        Plan plan = Plan.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .maxUsers(req.getMaxUsers())
                .maxCongregations(req.getMaxCongregations())
                .maxMembers(req.getMaxMembers())
                .features(req.getFeatures())
                .active(true)
                .system(false)
                .build();
        // Plan extends BaseEntity que exige organizationId — ROOT usa org 1
        plan.setOrganizationId(1L);
        return toDto(planRepository.save(plan));
    }

    @Transactional
    public PlanDto updatePlan(Long id, CreatePlanRequest req) {
        Plan plan = findById(id);
        plan.setName(req.getName());
        plan.setDescription(req.getDescription());
        plan.setPrice(req.getPrice());
        plan.setMaxUsers(req.getMaxUsers());
        plan.setMaxCongregations(req.getMaxCongregations());
        plan.setMaxMembers(req.getMaxMembers());
        plan.setFeatures(req.getFeatures());
        return toDto(planRepository.save(plan));
    }

    @Transactional
    public void deletePlan(Long id) {
        Plan plan = findById(id);
        if (plan.isSystem()) {
            throw new BusinessException("Planos do sistema não podem ser removidos.");
        }
        plan.setActive(false);
        planRepository.save(plan);
    }

    private Plan findById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", id));
    }

    private PlanDto toDto(Plan p) {
        return PlanDto.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .maxUsers(p.getMaxUsers())
                .maxCongregations(p.getMaxCongregations())
                .maxMembers(p.getMaxMembers())
                .features(p.getFeatures())
                .active(p.isActive())
                .system(p.isSystem())
                .build();
    }
}