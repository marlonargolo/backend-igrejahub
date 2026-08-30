package com.igrejahub.security;

import com.igrejahub.common.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Único ponto de decisão de permissões do sistema.
 *
 * Registrado no SecurityConfig como bean "permissionEvaluator".
 * Usado via @PreAuthorize("hasPermission(targetObject, 'PERMISSAO')") nos controllers.
 *
 * Hierarquia de decisão (em ordem):
 *   1. ROOT_ACCESS → permite tudo, sem verificação adicional
 *   2. Isolamento de Igreja → recurso deve pertencer à Igreja do usuário
 *   3. Isolamento de Congregação → se usuário é congregation-scoped, recurso deve ser da mesma
 *   4. Permissão específica → usuário deve ter a permission string solicitada
 *
 * Uso nos controllers:
 *
 *   // Verificação simples de permissão (sem escopo de objeto):
 *   @PreAuthorize("hasPermission(null, 'FINANCE_VIEW')")
 *
 *   // Verificação com objeto de domínio (valida church + congregação + permissão):
 *   @PreAuthorize("hasPermission(#transaction, 'FINANCE_UPDATE')")
 *
 * Para operações críticas que exigem validação no banco:
 *   Injete ChurchScopeValidator e chame validateCriticalOperation() antes de persistir.
 */
@Slf4j
@Component("permissionEvaluator")
public class PermissionEvaluator implements org.springframework.security.access.PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        UserPrincipal user = extractUser(authentication);
        if (user == null) return false;

        String requiredPermission = String.valueOf(permission);

        // 1. ROOT tem acesso irrestrito
        if (user.hasPermission("ROOT_ACCESS")) {
            log.debug("ACCESS GRANTED (ROOT): user={} perm={}", user.getEmail(), requiredPermission);
            return true;
        }

        // 2. Se há um objeto de domínio, validar isolamento de Igreja/Congregação
        if (targetDomainObject != null) {
            if (!validateChurchScope(user, targetDomainObject)) {
                log.warn("ACCESS DENIED (church scope): user={} church={} object={}",
                    user.getEmail(), user.getChurchId(), targetDomainObject.getClass().getSimpleName());
                return false;
            }
            if (!validateCongregationScope(user, targetDomainObject)) {
                log.warn("ACCESS DENIED (congregation scope): user={} congregation={} object={}",
                    user.getEmail(), user.getCongregationId(), targetDomainObject.getClass().getSimpleName());
                return false;
            }
        }

        // 3. Verificar permissão específica
        boolean granted = user.hasPermission(requiredPermission);
        if (!granted) {
            log.debug("ACCESS DENIED (missing permission): user={} required={} has={}",
                user.getEmail(), requiredPermission, user.getPermissions());
        }
        return granted;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId,
                                 String targetType, Object permission) {
        // Sem objeto de domínio disponível — verifica apenas permissão + ROOT
        UserPrincipal user = extractUser(authentication);
        if (user == null) return false;
        if (user.hasPermission("ROOT_ACCESS")) return true;
        return user.hasPermission(String.valueOf(permission));
    }

    // ─── Validações de escopo ─────────────────────────────────────────────────

    /**
     * Garante que o objeto de domínio pertence à Igreja do usuário.
     * Objetos que implementam ChurchScoped são verificados.
     * Objetos sem a interface passam livre — serviço é responsável pelo filtro.
     */
    private boolean validateChurchScope(UserPrincipal user, Object target) {
        if (!(target instanceof ChurchScoped scoped)) return true; // não é church-scoped, ok
        if (user.getChurchId() == null) return false; // usuário sem Igreja não acessa nada
        return user.getChurchId().equals(scoped.getChurchId());
    }

    /**
     * Garante que usuários restritos a uma Congregação só acessem recursos dela.
     * Se o usuário NÃO tem congregationId (acesso à Igreja toda) → passa.
     * Se o objeto NÃO é CongregationScoped → passa (é recurso de nível Igreja).
     */
    private boolean validateCongregationScope(UserPrincipal user, Object target) {
        if (user.getCongregationId() == null) return true; // acesso Igreja → passa
        if (!(target instanceof CongregationScoped scoped)) return true; // não é congregation-scoped → passa
        Long resourceCongId = scoped.getCongregationId();
        if (resourceCongId == null) return false; // recurso de Igreja, usuário só tem acesso à congregação
        return user.getCongregationId().equals(resourceCongId);
    }

    private UserPrincipal extractUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        if (!(authentication.getPrincipal() instanceof UserPrincipal user)) return null;
        return user;
    }

    // ─── Interfaces de marcação ───────────────────────────────────────────────

    /**
     * Implementar em DTOs/entidades que pertencem a uma Igreja específica.
     * O PermissionEvaluator usa esta interface para validar isolamento.
     *
     * Exemplo:
     *   public class FinancialTransactionDto implements ChurchScoped {
     *       public Long getChurchId() { return churchId; }
     *   }
     */
    public interface ChurchScoped {
        Long getChurchId();
    }

    /**
     * Implementar em DTOs/entidades que pertencem a uma Congregação específica.
     * Combinado com ChurchScoped para validação completa.
     *
     * Exemplo:
     *   public class MemberDto implements ChurchScoped, CongregationScoped {
     *       public Long getChurchId() { return churchId; }
     *       public Long getCongregationId() { return congregationId; }
     *   }
     */
    public interface CongregationScoped {
        Long getCongregationId();
    }
}
