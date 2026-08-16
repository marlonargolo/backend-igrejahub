package com.igrejahub.members.repository;

import com.igrejahub.common.repository.BaseRepository;
import com.igrejahub.members.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends BaseRepository<Member, Long> {
    Page<Member> findByOrganizationId(Long organizationId, Pageable pageable);
    Page<Member> findByOrganizationIdAndNameContainingIgnoreCase(Long organizationId, String name, Pageable pageable);
    long countByOrganizationIdAndStatus(Long organizationId, String status);

    /**
     * Busca paginada com filtros opcionais.
     * IMPORTANTE: o LOWER() foi removido da comparação com o parâmetro :search
     * porque o Hibernate 6 + PostgreSQL 16 infere o tipo do bind parameter como
     * bytea quando usado dentro de LOWER(), causando "function lower(bytea) does not exist".
     * A solução é concatenar os wildcards no Java e usar LIKE diretamente,
     * ou usar CAST explícito. Usamos CAST(:search AS text) para garantir o tipo.
     */
    @Query("SELECT m FROM Member m WHERE m.organizationId = :orgId " +
           "AND (:congregationId IS NULL OR m.congregationId = :congregationId) " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:search IS NULL OR LOWER(m.name) LIKE LOWER(CAST(:search AS string)))")
    Page<Member> search(@Param("orgId") Long orgId,
                        @Param("congregationId") Long congregationId,
                        @Param("status") String status,
                        @Param("search") String search,
                        Pageable pageable);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.organizationId = :orgId AND m.status = :status " +
           "AND (:churchId IS NULL OR m.churchId = :churchId) " +
           "AND (:congregationId IS NULL OR m.congregationId = :congregationId)")
    long countByFilter(@Param("orgId") Long orgId,
                       @Param("status") String status,
                       @Param("churchId") Long churchId,
                       @Param("congregationId") Long congregationId);
}