package com.igrejahub.members.repository;

import com.igrejahub.members.entity.MemberDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberDocumentRepository extends JpaRepository<MemberDocument, Long> {
    List<MemberDocument> findByMemberIdAndDeletedFalseOrderByCreatedAtDesc(Long memberId);
}
