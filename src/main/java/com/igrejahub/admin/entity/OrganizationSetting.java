package com.igrejahub.admin.entity;

import com.igrejahub.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "organization_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrganizationSetting extends AuditableEntity {

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "setting_value", columnDefinition = "jsonb")
    private Object settingValue;
}
