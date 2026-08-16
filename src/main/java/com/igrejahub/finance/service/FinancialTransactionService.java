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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialTransactionService {

    private final FinancialTransactionRepository transactionRepository;
    private final FinancialCategoryRepository categoryRepository;
    private final FinancialAccountService accountService;
    private final FinancialTransactionMapper transactionMapper;

    public Page<FinancialTransactionDto> getTransactions(Pageable pageable, FinancialFilterDto filter) {
        Long orgId = TenantContext.getCurrentTenant();
        Page<FinancialTransaction> page;
        if (filter != null && filter.getStatus() != null) {
            page = transactionRepository.findByOrganizationIdAndStatus(
                    orgId, FinancialTransaction.TransactionStatus.valueOf(filter.getStatus()), pageable);
        } else {
            page = transactionRepository.findByOrganizationId(orgId, pageable);
        }
        return page.map(this::toDtoWithRelations);
    }

    public FinancialTransactionDto getTransaction(Long id) {
        return toDtoWithRelations(getOwnedTransaction(id));
    }

    @Transactional
    public FinancialTransactionDto createTransaction(CreateTransactionRequest request) {
        Long orgId = TenantContext.getCurrentTenant();
        FinancialAccount account = accountService.getOwnedAccount(request.getAccountId());
        Long categoryId = validateOwnedCategory(request.getCategoryId(), orgId);

        FinancialTransaction transaction = FinancialTransaction.builder()
                .churchId(request.getChurchId())
                .congregationId(request.getCongregationId())
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
        return toDtoWithRelations(transaction);
    }

    @Transactional
    public FinancialTransactionDto updateTransaction(Long id, UpdateTransactionRequest request) {
        FinancialTransaction transaction = getOwnedTransaction(id);
        if (transaction.getStatus() != FinancialTransaction.TransactionStatus.PENDING) {
            throw new BusinessException("Somente transações pendentes podem ser editadas");
        }
        if (request.getAccountId() != null) {
            accountService.getOwnedAccount(request.getAccountId());
            transaction.setAccountId(request.getAccountId());
        }
        if (request.getCategoryId() != null) {
            transaction.setCategoryId(validateOwnedCategory(request.getCategoryId(), transaction.getOrganizationId()));
        }
        if (request.getDescription() != null) transaction.setDescription(request.getDescription());
        if (request.getAmount() != null) transaction.setAmountCents(FinancialAccountMapper.amountToCents(request.getAmount()));
        if (request.getTransactionDate() != null) transaction.setTransactionDate(request.getTransactionDate());
        if (request.getPaymentMethod() != null) transaction.setPaymentMethod(FinancialTransaction.PaymentMethod.valueOf(request.getPaymentMethod()));
        if (request.getReference() != null) transaction.setReference(request.getReference());
        if (request.getNotes() != null) transaction.setNotes(request.getNotes());
        return toDtoWithRelations(transactionRepository.save(transaction));
    }

    /** Confirma a transação e efetiva o impacto no saldo da conta (crédito para receita, débito para despesa). */
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
        transaction.setConfirmedAt(request != null && request.getConfirmedAt() != null ? request.getConfirmedAt() : LocalDate.now());
        if (request != null && request.getNotes() != null) transaction.setNotes(request.getNotes());
        return toDtoWithRelations(transactionRepository.save(transaction));
    }

    @Transactional
    public FinancialTransactionDto cancelTransaction(Long id, CancelTransactionRequest request) {
        FinancialTransaction transaction = getOwnedTransaction(id);
        if (transaction.getStatus() == FinancialTransaction.TransactionStatus.CANCELLED) {
            throw new BusinessException("Transação já está cancelada");
        }
        // Se já estava confirmada, estorna o saldo antes de cancelar
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

    private FinancialTransactionDto toDtoWithRelations(FinancialTransaction transaction) {
        FinancialAccount account = transaction.getAccountId() != null
                ? accountService.getOwnedAccount(transaction.getAccountId()) : null;
        FinancialCategory category = transaction.getCategoryId() != null
                ? categoryRepository.findByIdAndOrganizationId(transaction.getCategoryId(), transaction.getOrganizationId()).orElse(null)
                : null;
        return transactionMapper.toDto(transaction, account, category);
    }

    private FinancialTransaction getOwnedTransaction(Long id) {
        FinancialTransaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialTransaction", id));
        if (!transaction.getOrganizationId().equals(TenantContext.getCurrentTenant())) {
            throw new BusinessException("Acesso não autorizado");
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