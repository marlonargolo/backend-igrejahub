package com.igrejahub.members.entity;

import com.igrejahub.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "members")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Member extends BaseEntity {
    @Column(name = "church_id", nullable = false)
    private Long churchId;
    @Column(name = "congregation_id")
    private Long congregationId;
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    @Column(name = "email", length = 100)
    private String email;
    @Column(name = "phone", length = 20)
    private String phone;
    @Column(name = "birth_date")
    private LocalDate birthDate;
    @Column(name = "gender", length = 20)
    private String gender;
    @Column(name = "marital_status", length = 30)
    private String maritalStatus;
    @Column(name = "profession", length = 100)
    private String profession;
    @Column(name = "baptism_date")
    private LocalDate baptismDate;
    @Column(name = "member_since")
    private LocalDate memberSince;
    @Column(name = "address", length = 255)
    private String address;
    @Column(name = "notes", length = 500)
    private String notes;
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    @Column(name = "role", length = 30)
    private String role;
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ACTIVE";
}
