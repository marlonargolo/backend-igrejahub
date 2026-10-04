package com.igrejahub.modules.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "church_modules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChurchModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "church_id", nullable = false)
    private Long churchId;

    @Column(name = "module_id", nullable = false)
    private Long moduleId;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;
}
