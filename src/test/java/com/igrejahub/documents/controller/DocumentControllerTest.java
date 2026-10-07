package com.igrejahub.documents.controller;

import com.igrejahub.audit.service.AuditLogService;
import com.igrejahub.common.exception.BusinessException;
import com.igrejahub.common.tenant.TenantContext;
import com.igrejahub.documents.entity.IgrejaDocument;
import com.igrejahub.documents.repository.DocumentRepository;
import com.igrejahub.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SETTINGS_UPDATE abriu uploadDocument/deleteDocument pra não-ROOT (antes
 * exigia ROOT_ACCESS, por isso a Secretaria nunca conseguia usar — só
 * visualizar). Isso só é seguro se churchId/congregationId vierem do
 * contexto do chamador pra não-ROOT, nunca do request.
 */
@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private AuditLogService auditLogService;
    @Mock private MultipartFile file;

    @AfterEach
    void clearContext() { TenantContext.clear(); }

    private DocumentController newController() {
        return new DocumentController(documentRepository, securityUtils, auditLogService);
    }

    private MultipartFile fakeFile() {
        when(file.getOriginalFilename()).thenReturn("termo.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(1024L);
        return file;
    }

    /** O "save" real geraria o id via DB — o mock precisa fazer isso, já que toMap() exige não-nulo. */
    private IgrejaDocument withId(IgrejaDocument doc) {
        doc.setId(1L);
        return doc;
    }

    @Test
    void uploadDocument_nonRoot_ignoresRequestChurchId_usesCallerContext() throws Exception {
        TenantContext.setCurrentTenant(1L);
        TenantContext.setCurrentCongregationId(50L);
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(documentRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));

        newController().uploadDocument(fakeFile(), "Termo", null, 999L, 999L);

        ArgumentCaptor<IgrejaDocument> captor = ArgumentCaptor.forClass(IgrejaDocument.class);
        verify(documentRepository).save(captor.capture());
        assertEquals(10L, captor.getValue().getChurchId());
        assertEquals(50L, captor.getValue().getCongregationId());
    }

    @Test
    void uploadDocument_nonRoot_withoutChurch_throws() throws Exception {
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(null);

        DocumentController controller = newController();
        // Lança antes de tocar no arquivo — não precisa (nem deve) estar "preenchido".
        assertThrows(BusinessException.class,
            () -> controller.uploadDocument(file, "Termo", null, null, null));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_rootGlobal_withoutChurchId_throws() throws Exception {
        when(securityUtils.canViewAll()).thenReturn(true);

        DocumentController controller = newController();
        assertThrows(BusinessException.class,
            () -> controller.uploadDocument(file, "Termo", null, null, null));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_rootGlobal_usesRequestedChurchId() throws Exception {
        TenantContext.setCurrentTenant(1L);
        when(securityUtils.canViewAll()).thenReturn(true);
        when(documentRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));

        newController().uploadDocument(fakeFile(), "Termo", null, 20L, 200L);

        ArgumentCaptor<IgrejaDocument> captor = ArgumentCaptor.forClass(IgrejaDocument.class);
        verify(documentRepository).save(captor.capture());
        assertEquals(20L, captor.getValue().getChurchId());
        assertEquals(200L, captor.getValue().getCongregationId());
    }

    @Test
    void deleteDocument_nonRoot_crossChurch_isDenied() {
        TenantContext.setCurrentTenant(1L);
        IgrejaDocument doc = new IgrejaDocument();
        doc.setOrganizationId(1L);
        doc.setChurchId(20L);
        when(documentRepository.findById(5L)).thenReturn(Optional.of(doc));
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);

        DocumentController controller = newController();
        assertThrows(BusinessException.class, () -> controller.deleteDocument(5L));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void deleteDocument_nonRoot_ownChurch_succeeds() {
        TenantContext.setCurrentTenant(1L);
        IgrejaDocument doc = new IgrejaDocument();
        doc.setOrganizationId(1L);
        doc.setChurchId(10L);
        when(documentRepository.findById(5L)).thenReturn(Optional.of(doc));
        when(securityUtils.canViewAll()).thenReturn(false);
        when(securityUtils.getEffectiveChurchId()).thenReturn(10L);
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        newController().deleteDocument(5L);

        assertTrue(doc.isDeleted());
        verify(documentRepository).save(doc);
    }
}
