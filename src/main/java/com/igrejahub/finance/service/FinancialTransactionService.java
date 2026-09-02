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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * ISOLAMENTO: transações financeiras pertencem a uma Igreja.
 *
 *   ROOT global        → vê todas as transações da organização
 *   ROOT com contexto  → vê apenas da Igreja atual
 *   Admin/Pastor       → vê apenas da sua Igreja
 *   Pastor Congregação → vê apenas da sua Congregação
 *
 * CRIAÇÃO:
 *   churchId sempre forçado pelo TenantContext (nunca vem do request para não-ROOT)
 */
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

    // ── Listagem ──────────────────────────────────────────────────────────────

    public Page<FinancialTransactionDto> getTransactions(Pageable pageable, FinancialFilterDto filter) {
        Long orgId    = TenantContext.getCurrentTenant();
        Long churchId = securityUtils.getEffectiveChurchId();
        Long congId   = TenantContext.getCurrentCongregationId();

        FinancialTransaction.TransactionStatus status = null;
        if (filter != null && filter.getStatus() != null) {
            status = FinancialTransaction.TransactionStatus.valueOf(filter.getStatus());
        }

        // ROOT global → tudo
        if (securityUtils.canViewAll()) {
            Page<FinancialTransaction> page = status != null
                ? transactionRepository.findByOrganizationIdAndStatus(orgId, status, pageable)
                : transactionRepository.findByOrganizationId(orgId, pageable);
            return page.map(this::toDtoWithRelations);
        }

        // Pastor de Congregação → só da sua congregação
        if (congId != null && !securityUtils.isRoot()) {
            return transactionRepository.findByCongregationId(orgId, congId, status, pageable)
                .map(this::toDtoWithRelations);
        }

        // Admin/Pastor Igreja ou ROOT com contexto → só da Igreja
        if (churchId == null) return Page.empty(pageable);
        return transactionRepository.findByChurchId(orgId, churchId, status, pageable)
            .map(this::toDtoWithRelations);
    }

    public FinancialTransactionDto getTransaction(Long id) {
        return toDtoWithRelations(getOwnedTransaction(id));
    }

    // ── Criação ───────────────────────────────────────────────────────────────

    @Transactional
    public FinancialTransactionDto createTransaction(CreateTransactionRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        FinancialAccount account = accountService.getOwnedAccount(request.getAccountId());
        Long categoryId = validateOwnedCategory(request.getCategoryId(), orgId);

        // Determinar Igreja da transação — nunca confia no request para não-ROOT
        Long targetChurchId;
        Long targetCongId;

        if (securityUtils.canViewAll()) {
            // ROOT global: usa o que vier no request (pode escolher a Igreja)
            targetChurchId = request.getChurchId();
            targetCongId   = request.getCongregationId();
        } else {
            // Todos os outros: forçado pelo TenantContext
            targetChurchId = securityUtils.getEffectiveChurchId();
            targetCongId   = TenantContext.getCurrentCongregationId();
            if (targetChurchId == null) {
                throw new BusinessException("Seu usuário não está vinculado a nenhuma Igreja.");
            }
        }

        FinancialTransaction transaction = FinancialTransaction.builder()
            .churchId(targetChurchId)
            .congregationId(targetCongId)
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
        log.info("Transaction created: {} churchId={} by={}",
            transaction.getDescription(), transaction.getChurchId(),
            TenantContext.getCurrentUserId());

        return toDtoWithRelations(transaction);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public FinancialTransactionDto updateTransaction(Long id, UpdateTransactionRequest request) {
        FinancialTransaction transaction = getOwnedTransaction(id);
        if (transaction.getStatus() != FinancialTransaction.TransactionStatus.PENDING) {
            throw new BusinessException("Somente transações pendentes podem ser editadas");
        }
        if (request.getDescription() != null) transaction.setDescription(request.getDescription());
        if (request.getAmount() != null) transaction.setAmountCents(FinancialAccountMapper.amountToCents(request.getAmount()));
        if (request.getTransactionDate() != null) transaction.setTransactionDate(request.getTransactionDate());
        if (request.getCategoryId() != null) transaction.setCategoryId(
            validateOwnedCategory(request.getCategoryId(), transaction.getOrganizationId()));
        if (request.getPaymentMethod() != null) transaction.setPaymentMethod(
            FinancialTransaction.PaymentMethod.valueOf(request.getPaymentMethod()));
        if (request.getReference() != null) transaction.setReference(request.getReference());
        if (request.getNotes() != null) transaction.setNotes(request.getNotes());
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
        return transactionMapper.toDto(transaction, account, category);
    }

    private FinancialTransaction getOwnedTransaction(Long id) {
        FinancialTransaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("FinancialTransaction", id));

        if (!transaction.getOrganizationId().equals(TenantContext.getCurrentTenant())) {
            throw new BusinessException("Acesso não autorizado");
        }

        // Validar isolamento de Igreja para não-ROOT
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