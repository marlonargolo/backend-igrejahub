package com.igrejahub.assets.service;

import com.igrejahub.assets.dto.AssetDto;
import com.igrejahub.assets.dto.CreateAssetRequest;
import com.igrejahub.assets.dto.UpdateAssetRequest;
import com.igrejahub.assets.entity.Asset;
import com.igrejahub.assets.repository.AssetCategoryRepository;
import com.igrejahub.assets.repository.AssetRepository;
import com.igrejahub.churches.repository.ChurchRepository;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.congregations.repository.CongregationRepository;
import com.igrejahub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * REGRAS DE ISOLAMENTO DE PATRIMÔNIO:
 *
 *   Asset SEMPRE tem churchId (obrigatório, nunca nulo).
 *   Asset OPCIONALMENTE tem congregationId.
 *
 * LISTAGEM:
 *   ROOT global          → todos da organização
 *   ROOT com contexto    → apenas da Igreja atual
 *   Admin/Pastor Igreja  → apenas da sua Igreja
 *   Pastor Congregação   → apenas da sua Congregação
 *
 * CRIAÇÃO:
 *   ROOT global          → deve informar churchId explicitamente
 *   Demais               → churchId forçado pelo TenantContext
 *   Pastor Congregação   → congregationId forçado para a sua Congregação
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository          assetRepository;
    private final ChurchRepository         churchRepository;
    private final CongregationRepository   congregationRepository;
    private final AssetCategoryRepository  categoryRepository;
    private final SecurityUtils            securityUtils;
    private final JdbcTemplate             jdbcTemplate;

    // ── Listagem ──────────────────────────────────────────────────────────────

    public Page<AssetDto> getAssets(Pageable pageable, Long categoryId, String search, String status) {
        Long orgId   = TenantContext.getCurrentTenant();
        String s     = search != null && !search.isEmpty() ? search : null;

        // ROOT modo global → tudo
        if (securityUtils.canViewAll()) {
            return assetRepository.findAllByOrganization(orgId, categoryId, status, s, pageable)
                .map(a -> toDto(a, orgId));
        }

        Long churchId = securityUtils.getEffectiveChurchId();
        Long congId   = TenantContext.getCurrentCongregationId();

        // Pastor de Congregação → só da sua congregação
        if (congId != null && !securityUtils.isRoot()) {
            return assetRepository.findByCongregationId(orgId, congId, categoryId, status, s, pageable)
                .map(a -> toDto(a, orgId));
        }

        // Admin/Pastor Igreja ou ROOT com contexto → só da Igreja
        if (churchId == null) return Page.empty(pageable);

        return assetRepository.findByChurchId(orgId, churchId, categoryId, status, s, pageable)
            .map(a -> toDto(a, orgId));
    }

    public AssetDto getAsset(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        Asset asset = assetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Asset", id));

        if (!asset.getOrganizationId().equals(orgId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        if (!securityUtils.canViewAll()) {
            assertAssetAccess(asset);
        }
        return toDto(asset, orgId);
    }

    // ── Criação ───────────────────────────────────────────────────────────────

    @Transactional
    public AssetDto createAsset(CreateAssetRequest request) {
        Long orgId = TenantContext.getCurrentTenant();

        // Determinar Igreja alvo
        Long targetChurchId;
        if (securityUtils.canViewAll()) {
            if (request.getChurchId() == null) {
                throw new BusinessException("Selecione a Igreja para o novo bem patrimonial.");
            }
            targetChurchId = request.getChurchId();
        } else {
            Long callerChurchId = securityUtils.getEffectiveChurchId();
            if (callerChurchId == null) {
                throw new BusinessException("Seu usuário não está vinculado a nenhuma Igreja.");
            }
            if (request.getChurchId() != null && !request.getChurchId().equals(callerChurchId)) {
                throw new BusinessException("Você não pode criar bens em outra Igreja.");
            }
            targetChurchId = callerChurchId;
        }

        if (!churchRepository.existsByOrganizationIdAndId(orgId, targetChurchId)) {
            throw new BusinessException("Igreja não encontrada.");
        }

        // Determinar Congregação alvo
        Long callerCongId   = TenantContext.getCurrentCongregationId();
        Long targetCongId   = request.getCongregationId();

        if (callerCongId != null && !securityUtils.isRoot()) {
            // Pastor de Congregação: forçado para a sua congregação
            if (targetCongId != null && !targetCongId.equals(callerCongId)) {
                throw new BusinessException("Você só pode criar bens na sua congregação.");
            }
            targetCongId = callerCongId;
        } else if (targetCongId != null) {
            Long finalChurchId = targetChurchId;
            if (!congregationRepository.existsByOrganizationIdAndIdAndChurchId(
                    orgId, targetCongId, finalChurchId)) {
                throw new BusinessException("Congregação não pertence a esta Igreja.");
            }
        }

        // Código único
        if (request.getCode() != null && !request.getCode().isEmpty()) {
            if (assetRepository.existsByOrganizationIdAndCode(orgId, request.getCode())) {
                throw new BusinessException("Código do patrimônio já existe.");
            }
        }

        Asset asset = new Asset();
        asset.setOrganizationId(orgId);
        asset.setChurchId(targetChurchId);
        asset.setCongregationId(targetCongId);
        asset.setCode(request.getCode());
        asset.setDescription(request.getDescription());
        asset.setCategoryId(request.getCategoryId());
        asset.setAcquisitionDate(request.getAcquisitionDate());
        asset.setLocation(request.getLocation());
        asset.setStatus("ACTIVE");
        asset.setNotes(request.getNotes());
        asset.setSerialNumber(request.getSerialNumber());
        asset.setManufacturer(request.getManufacturer());
        asset.setModel(request.getModel());
        asset.setWarrantyEndDate(request.getWarrantyEndDate());
        asset.setResponsibleMemberId(request.getResponsibleMemberId());
        asset.setResponsibleUserId(request.getResponsibleUserId());

        if (request.getOriginalValue() != null) {
            long cents = toCents(request.getOriginalValue());
            asset.setOriginalValueCents(cents);
            asset.setCurrentValueCents(cents);
        }

        asset = assetRepository.save(asset);
        log.info("Asset created: {} churchId={} congregationId={} by={}",
            asset.getDescription(), asset.getChurchId(), asset.getCongregationId(),
            TenantContext.getCurrentUserId());

        return toDto(asset, orgId);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public AssetDto updateAsset(Long id, UpdateAssetRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        Asset asset = assetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Asset", id));

        if (!asset.getOrganizationId().equals(orgId)) {
            throw new BusinessException("Acesso não autorizado");
        }
        if (!securityUtils.canViewAll()) {
            assertAssetAccess(asset);
        }

        if (request.getCode() != null && !request.getCode().isEmpty()
                && !request.getCode().equals(asset.getCode())
                && assetRepository.existsByOrganizationIdAndCode(orgId, request.getCode())) {
            throw new BusinessException("Código do patrimônio já existe.");
        }

        // Trocar Igreja: apenas ROOT
        if (request.getChurchId() != null && !request.getChurchId().equals(asset.getChurchId())) {
            if (!securityUtils.isRoot()) {
                throw new BusinessException("Apenas ROOT pode mover um bem para outra Igreja.");
            }
            asset.setChurchId(request.getChurchId());
        }

        // Trocar Congregação: validar pertencimento à Igreja
        if (request.getCongregationId() != null) {
            Long finalChurchId = request.getChurchId() != null ? request.getChurchId() : asset.getChurchId();
            if (!congregationRepository.existsByOrganizationIdAndIdAndChurchId(
                    orgId, request.getCongregationId(), finalChurchId)) {
                throw new BusinessException("Congregação não pertence à Igreja deste bem.");
            }
            asset.setCongregationId(request.getCongregationId());
        }

        if (request.getCode()            != null) asset.setCode(request.getCode());
        if (request.getDescription()     != null) asset.setDescription(request.getDescription());
        if (request.getCategoryId()      != null) asset.setCategoryId(request.getCategoryId());
        if (request.getAcquisitionDate() != null) asset.setAcquisitionDate(request.getAcquisitionDate());
        if (request.getLocation()        != null) asset.setLocation(request.getLocation());
        if (request.getStatus()          != null) asset.setStatus(request.getStatus());
        if (request.getNotes()           != null) asset.setNotes(request.getNotes());
        if (request.getSerialNumber()    != null) asset.setSerialNumber(request.getSerialNumber());
        if (request.getManufacturer()    != null) asset.setManufacturer(request.getManufacturer());
        if (request.getModel()           != null) asset.setModel(request.getModel());
        if (request.getWarrantyEndDate() != null) asset.setWarrantyEndDate(request.getWarrantyEndDate());
        if (request.getResponsibleMemberId() != null) asset.setResponsibleMemberId(request.getResponsibleMemberId());
        if (request.getResponsibleUserId()   != null) asset.setResponsibleUserId(request.getResponsibleUserId());
        if (request.getOriginalValue()   != null) asset.setOriginalValueCents(toCents(request.getOriginalValue()));
        if (request.getCurrentValue()    != null) asset.setCurrentValueCents(toCents(request.getCurrentValue()));

        return toDto(assetRepository.save(asset), orgId);
    }

    // ── Delete / Write-off ────────────────────────────────────────────────────

    @Transactional
    public void deleteAsset(Long id) {
        Long orgId = TenantContext.getCurrentTenant();
        Asset asset = assetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Asset", id));
        if (!asset.getOrganizationId().equals(orgId)) throw new BusinessException("Acesso não autorizado");
        if (!securityUtils.canViewAll()) assertAssetAccess(asset);
        asset.setStatus("WRITTEN_OFF");
        assetRepository.save(asset);
    }

    @Transactional
    public void writeOffAsset(Long id, String reason) {
        Long orgId = TenantContext.getCurrentTenant();
        Asset asset = assetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Asset", id));
        if (!asset.getOrganizationId().equals(orgId)) throw new BusinessException("Acesso não autorizado");
        if (!securityUtils.canViewAll()) assertAssetAccess(asset);
        asset.setStatus("WRITTEN_OFF");
        asset.setNotes((asset.getNotes() != null ? asset.getNotes() + " " : "") + "Baixado: " + reason);
        assetRepository.save(asset);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void assertAssetAccess(Asset asset) {
        Long churchId = securityUtils.getEffectiveChurchId();
        Long congId   = TenantContext.getCurrentCongregationId();

        if (churchId != null && !churchId.equals(asset.getChurchId())) {
            throw new BusinessException("Você não tem acesso a este bem patrimonial.");
        }
        if (congId != null && !congId.equals(asset.getCongregationId())) {
            throw new BusinessException("Você não tem acesso a este bem patrimonial.");
        }
    }

    private AssetDto toDto(Asset a, Long orgId) {
        if (a == null) return null;

        AssetDto dto = new AssetDto();
        dto.setId(a.getId());
        dto.setChurchId(a.getChurchId());
        dto.setCongregationId(a.getCongregationId());
        dto.setCode(a.getCode());
        dto.setDescription(a.getDescription());
        dto.setCategoryId(a.getCategoryId());
        dto.setAcquisitionDate(a.getAcquisitionDate());
        dto.setLocation(a.getLocation());
        dto.setStatus(a.getStatus());
        dto.setNotes(a.getNotes());
        dto.setSerialNumber(a.getSerialNumber());
        dto.setManufacturer(a.getManufacturer());
        dto.setModel(a.getModel());
        dto.setWarrantyEndDate(a.getWarrantyEndDate());
        dto.setResponsibleMemberId(a.getResponsibleMemberId());
        dto.setResponsibleUserId(a.getResponsibleUserId());

        if (a.getOriginalValueCents() != null) {
            dto.setOriginalValueCents(a.getOriginalValueCents());
            dto.setOriginalValue(a.getOriginalValueCents() / 100.0);
        }
        if (a.getCurrentValueCents() != null) {
            dto.setCurrentValueCents(a.getCurrentValueCents());
            dto.setCurrentValue(a.getCurrentValueCents() / 100.0);
        }

        // Nome da Igreja
        if (a.getChurchId() != null) {
            try { dto.setChurchName(jdbcTemplate.queryForObject(
                "SELECT name FROM churches WHERE id = ? AND organization_id = ? AND deleted = false",
                String.class, a.getChurchId(), orgId)); } catch (Exception ignored) {}
        }
        // Nome da Congregação
        if (a.getCongregationId() != null) {
            try { dto.setCongregationName(jdbcTemplate.queryForObject(
                "SELECT name FROM congregations WHERE id = ? AND organization_id = ? AND deleted = false",
                String.class, a.getCongregationId(), orgId)); } catch (Exception ignored) {}
        }
        // Nome da Categoria
        if (a.getCategoryId() != null) {
            try { dto.setCategoryName(jdbcTemplate.queryForObject(
                "SELECT name FROM asset_categories WHERE id = ? AND organization_id = ?",
                String.class, a.getCategoryId(), orgId)); } catch (Exception ignored) {}
        }
        // Nome do Responsável (membro)
        if (a.getResponsibleMemberId() != null) {
            try { dto.setResponsibleMemberName(jdbcTemplate.queryForObject(
                "SELECT name FROM members WHERE id = ? AND organization_id = ? AND deleted = false",
                String.class, a.getResponsibleMemberId(), orgId)); } catch (Exception ignored) {}
        }
        // Nome do Responsável (usuário)
        if (a.getResponsibleUserId() != null) {
            try { dto.setResponsibleUserName(jdbcTemplate.queryForObject(
                "SELECT name FROM users WHERE id = ? AND organization_id = ? AND (deleted IS NULL OR deleted = false)",
                String.class, a.getResponsibleUserId(), orgId)); } catch (Exception ignored) {}
        }

        return dto;
    }

    private long toCents(Double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }
}