package com.igrejahub.audit.service;

import com.igrejahub.audit.repository.AuditLogRepository;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Regressão: uma falha ao gravar o log de auditoria (ex.: coluna ausente por
 * migration pendente) estava derrubando a operação de negócio inteira com
 * 500 — criar usuário, alterar permissões, etc. — porque a escrita de
 * auditoria compartilhava a MESMA transação/conexão da operação chamadora.
 * No Postgres, um erro em qualquer statement aborta a transação corrente até
 * o ROLLBACK, então mesmo capturando a exceção em Java o COMMIT da operação
 * de negócio falhava em seguida. logAction agora roda em transação própria
 * (REQUIRES_NEW) e nunca deixa uma exceção escapar.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceResilienceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private TransactionStatus transactionStatus;

    @AfterEach
    void clearContext() { TenantContext.clear(); }

    private AuditLogService newService() {
        return new AuditLogService(auditLogRepository, securityUtils, transactionManager);
    }

    @Test
    void logAction_neverPropagatesFailure_toTheCaller() {
        TenantContext.setCurrentTenant(1L);
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(auditLogRepository.save(any()))
            .thenThrow(new RuntimeException("column \"church_id\" of relation \"audit_logs\" does not exist"));

        AuditLogService service = newService();

        assertDoesNotThrow(() -> service.logAction(
            "CREATE_USER", "USER", 1L, null, Map.of("email", "a@b.com")));

        verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void logAction_runsInItsOwnTransaction_separateFromCaller() {
        TenantContext.setCurrentTenant(1L);
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newService().logAction("CREATE_USER", "USER", 1L, null, Map.of("k", "v"));

        verify(transactionManager).getTransaction(argThat(def ->
            def.getPropagationBehavior() == org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        verify(transactionManager).commit(transactionStatus);
    }
}
