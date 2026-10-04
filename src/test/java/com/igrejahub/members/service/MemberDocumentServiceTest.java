package com.igrejahub.members.service;

import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.members.entity.MemberDocument;
import com.igrejahub.members.repository.MemberDocumentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Documentos do membro reaproveitam o mesmo isolamento de MemberService.getMember
 * — nunca devem ficar acessíveis a quem não pode ver o próprio membro.
 */
@ExtendWith(MockitoExtension.class)
class MemberDocumentServiceTest {

    @Mock private MemberDocumentRepository documentRepository;
    @Mock private MemberService memberService;

    @AfterEach
    void clearContext() { TenantContext.clear(); }

    private MemberDocumentService newService() {
        return new MemberDocumentService(documentRepository, memberService);
    }

    @Test
    void getDocuments_whenMemberNotAccessible_propagatesException() {
        when(memberService.getMember(5L)).thenThrow(new BusinessException("Você não tem acesso a este membro."));

        assertThrows(BusinessException.class, () -> newService().getDocuments(5L));
    }

    @Test
    void deleteDocument_belongingToAnotherMember_isRejected() {
        MemberDocument doc = MemberDocument.builder().id(10L).memberId(99L).deleted(false).build();
        when(documentRepository.findById(10L)).thenReturn(Optional.of(doc));

        assertThrows(BusinessException.class, () -> newService().deleteDocument(5L, 10L));
    }
}
