package com.igrejahub.members.repository;

import com.igrejahub.members.entity.MemberOccurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberOccurrenceRepository extends JpaRepository<MemberOccurrence, Long> {
    List<MemberOccurrence> findByMemberIdAndDeletedFalseOrderByOccurrenceDateDesc(Long memberId);
}