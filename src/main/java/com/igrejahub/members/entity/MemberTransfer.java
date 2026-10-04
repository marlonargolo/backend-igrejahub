package com.igrejahub.members.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_transfers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "church_id", nullable = false)
    private Long churchId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "from_congregation_id")
    private Long fromCongregationId;

    @Column(name = "to_congregation_id", nullable = false)
    private Long toCongregationId;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "transferred_by", nullable = false)
    private Long transferredBy;

    @Column(name = "transferred_at", nullable = false)
    private LocalDateTime transferredAt;
}
