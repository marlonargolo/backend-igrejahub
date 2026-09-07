package com.igrejahub.finance.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.exception.ResourceNotFoundException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.finance.dto.*;
import com.igrejahub.finance.entity.FinancialAccount;
import com.igrejahub.finance.entity.FinancialCategory;
import com.igrejahub.finance.entity.FinancialTransaction;
import com.igrejahub.finance.mapper.FinancialAccountMapper;
import com.igrejahub.finance.mapper.FinancialTransactionMapper;
import com.igrejahub.finance.repository.FinancialCategoryRepository;
import com.igrejahub.finance.repository.FinancialTransactionRepository;
import com.igrejahub.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialTransactionService {

    private final FinancialTransactionRepository transactionRepository;
    private final FinancialCategoryRepository    categoryRepository;
    private final FinancialAccountService        accountService;
    private final FinancialTransactionMapper     transactionMapper;
    private final SecurityUtils                  securityUtils;
    private final JdbcTemplate                   jdbcTemplate;

    // ── Listagem com filtros ───────────────────────────────────────────────────

    public Page<FinancialTransactionDto> getTransactions(Pageable pageable, FinancialFilterDto filter) {
        Long orgId    = TenantContext.getCurrentTenant();
        Long churchId = securityUtils.getEffectiveChurchId();   // null só para ROOT global
        Long congId   = TenantContext.getCurrentCongregationId();

        // Extrair parâmetros do filtro
        FinancialTransaction.TransactionType   typeEnum   = null;
        FinancialTransaction.TransactionStatus statusEnum = null;
        Long memberId = null;

        if (filter != null) {
            if (filter.getType()   != null) typeEnum   = FinancialTransaction.TransactionType.valueOf(filter.getType());
            if (filter.getStatus() != null) statusEnum = FinancialTransaction.TransactionStatus.valueOf(filter.getStatus());
            memberId = filter.getMemberId();

            // Parâmetro churchId do filter pode sobrescrever só para ROOT
            if (filter.getChurchId() != null && securityUtils.isRoot()) {
                churchId = filter.getChurchId();
            }
            // congregationId do filter pode refinar para admin de Igreja
            if (filter.getCongregationId() != null) {
                congId = filter.getCongregationId();
            }
        }

        // Para PASTOR_CONGREGACAO: forçar congregationId
        Long effectiveCongId = (congId != null && !securityUtils.isRoot()) ? congId : null;

        // findByFilters: churchId = null → ROOT global vê tudo; não-null → filtra
        return transactionRepository.findByFilters(
                orgId, typeEnum, churchId, effectiveCongId, statusEnum, memberId, pageable)
            .map(t -> toDtoWithRelations(t));
    }

    public FinancialTransactionDto getTransaction(Long id) {
        return toDtoWithRelations(getOwnedTransaction(id));
    }

    // ── Contribuições de um membro ────────────────────────────────────────────

    public List<FinancialTransactionDto> getContributions(Long memberId) {
        Long orgId    = TenantContext.getCurrentTenant();
        Long churchId = securityUtils.getEffectiveChurchId();

        return transactionRepository.findContributionsByMember(orgId, memberId, churchId)
            .stream().map(t -> toDtoWithRelations(t)).collect(Collectors.toList());
    }

    // ── Criação ───────────────────────────────────────────────────────────────

    @Transactional
    public FinancialTransactionDto createTransaction(CreateTransactionRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        FinancialAccount account = accountService.getOwnedAccount(request.getAccountId());
        Long categoryId = validateOwnedCategory(request.getCategoryId(), orgId);

        // churchId forçado pelo contexto para não-ROOT
        Long targetChurchId = securityUtils.canViewAll()
            ? request.getChurchId()
            : securityUtils.getEffectiveChurchId();
        Long targetCongId = securityUtils.canViewAll()
            ? request.getCongregationId()
            : TenantContext.getCurrentCongregationId();

        FinancialTransaction transaction = FinancialTransaction.builder()
            .churchId(targetChurchId)
            .congregationId(targetCongId)
            .memberId(request.getMemberId())
            .accountId(account.getId())
            .categoryId(categoryId)
            .type(FinancialTransaction.TransactionType.valueOf(request.getType()))
            .description(request.getDescription())
            .amountCents(FinancialAccountMapper.amountToCents(request.getAmount()))
            .transactionDate(request.getTransactionDate())
            .status(FinancialTransaction.TransactionStatus.PENDING)
            .paymentMethod(request.getPaymentMethod() != null
                ? FinancialTransaction.PaymentMethod.valueOf(request.getPaymentMethod()) : null)
            .reference(request.getReference())
            .notes(request.getNotes())
            .build();
        transaction.setOrganizationId(orgId);

        transaction = transactionRepository.save(transaction);
        log.info("Transaction created: type={} churchId={} memberId={} by={}",
            transaction.getType(), transaction.getChurchId(),
            transaction.getMemberId(), TenantContext.getCurrentUserId());
        return toDtoWithRelations(transaction);
    }

    // ── Update / Confirm / Cancel ─────────────────────────────────────────────

    @Transactional
    public FinancialTransactionDto updateTransaction(Long id, UpdateTransactionRequest request) {
        FinancialTransaction transaction = getOwnedTransaction(id);
        if (transaction.getStatus() != FinancialTransaction.TransactionStatus.PENDING) {
            throw new BusinessException("Somente transações pendentes podem ser editadas");
        }
        if (request.getAccountId()   != null) { accountService.getOwnedAccount(request.getAccountId()); transaction.setAccountId(request.getAccountId()); }
        if (request.getCategoryId()  != null) transaction.setCategoryId(validateOwnedCategory(request.getCategoryId(), transaction.getOrganizationId()));
        if (request.getDescription() != null) transaction.setDescription(request.getDescription());
        if (request.getAmount()      != null) transaction.setAmountCents(FinancialAccountMapper.amountToCents(request.getAmount()));
        if (request.getTransactionDate() != null) transaction.setTransactionDate(request.getTransactionDate());
        if (request.getPaymentMethod()   != null) transaction.setPaymentMethod(FinancialTransaction.PaymentMethod.valueOf(request.getPaymentMethod()));
        if (request.getReference()   != null) transaction.setReference(request.getReference());
        if (request.getNotes()       != null) transaction.setNotes(request.getNotes());
        return toDtoWithRelations(transactionRepository.save(transaction));
    }

    @Transactional
    public FinancialTransactionDto confirmTransaction(Long id, ConfirmTransactionRequest request) {
        FinancialTransaction transaction = getOwnedTransaction(id);
        if (transaction.getStatus() != FinancialTransaction.TransactionStatus.PENDING) {
            throw new BusinessException("Somente transações pendentes podem ser confirmadas");
        }
        Long userId = TenantContext.getCurrentUserId();
        long delta = transaction.getType() == FinancialTransaction.TransactionType.REVENUE
            ? transaction.getAmountCents() : -transaction.getAmountCents();
        accountService.adjustBalance(transaction.getAccountId(), delta);
        transaction.setStatus(FinancialTransaction.TransactionStatus.CONFIRMED);
        transaction.setApprovedBy(userId);
        transaction.setApprovedAt(LocalDate.now());
        transaction.setConfirmedAt(request != null && request.getConfirmedAt() != null
            ? request.getConfirmedAt() : LocalDate.now());
        if (request != null && request.getNotes() != null) transaction.setNotes(request.getNotes());
        return toDtoWithRelations(transactionRepository.save(transaction));
    }

    @Transactional
    public FinancialTransactionDto cancelTransaction(Long id, CancelTransactionRequest request) {
        FinancialTransaction transaction = getOwnedTransaction(id);
        if (transaction.getStatus() == FinancialTransaction.TransactionStatus.CANCELLED) {
            throw new BusinessException("Transação já está cancelada");
        }
        if (transaction.getStatus() == FinancialTransaction.TransactionStatus.CONFIRMED) {
            long reversal = transaction.getType() == FinancialTransaction.TransactionType.REVENUE
                ? -transaction.getAmountCents() : transaction.getAmountCents();
            accountService.adjustBalance(transaction.getAccountId(), reversal);
        }
        transaction.setStatus(FinancialTransaction.TransactionStatus.CANCELLED);
        transaction.setCancelledBy(TenantContext.getCurrentUserId());
        transaction.setCancelledAt(LocalDate.now());
        transaction.setNotes(((transaction.getNotes() != null ? transaction.getNotes() + " | " : "")
            + "Cancelado: " + request.getReason()));
        return toDtoWithRelations(transactionRepository.save(transaction));
    }

    @Transactional
    public void updateAttachment(Long id, String url) {
        FinancialTransaction tx = transactionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        tx.setNotes((tx.getNotes() != null ? tx.getNotes() + " | " : "") + "ATTACHMENT:" + url);
        transactionRepository.save(tx);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FinancialTransactionDto toDtoWithRelations(FinancialTransaction transaction) {
        FinancialAccount account = transaction.getAccountId() != null
            ? accountService.getOwnedAccount(transaction.getAccountId()) : null;
        FinancialCategory category = transaction.getCategoryId() != null
            ? categoryRepository.findByIdAndOrganizationId(
                transaction.getCategoryId(), transaction.getOrganizationId()).orElse(null)
            : null;
        // Resolver nome do membro via SQL direto (sem criar dependência circular)
        String memberName = null;
        if (transaction.getMemberId() != null) {
            try {
                memberName = jdbcTemplate.queryForObject(
                    "SELECT name FROM members WHERE id = ? AND deleted = false",
                    String.class, transaction.getMemberId());
            } catch (Exception ignored) {}
        }
        return transactionMapper.toDto(transaction, account, category, memberName);
    }

    private FinancialTransaction getOwnedTransaction(Long id) {
        FinancialTransaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("FinancialTransaction", id));
        if (!transaction.getOrganizationId().equals(TenantContext.getCurrentTenant())) {
            throw new BusinessException("Acesso não autorizado");
        }
        if (!securityUtils.canViewAll()) {
            Long callerChurchId = securityUtils.getEffectiveChurchId();
            Long callerCongId   = TenantContext.getCurrentCongregationId();
            if (callerCongId != null && !callerCongId.equals(transaction.getCongregationId())) {
                throw new BusinessException("Você não tem acesso a esta transação.");
            } else if (callerChurchId != null && !callerChurchId.equals(transaction.getChurchId())) {
                throw new BusinessException("Você não tem acesso a esta transação.");
            }
        }
        return transaction;
    }

    private Long validateOwnedCategory(Long categoryId, Long orgId) {
        if (categoryId == null) return null;
        categoryRepository.findByIdAndOrganizationId(categoryId, orgId)
            .orElseThrow(() -> new BusinessException("Categoria inválida"));
        return categoryId;
    }
}