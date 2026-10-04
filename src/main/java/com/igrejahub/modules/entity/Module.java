package com.igrejahub.modules.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "modules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key", nullable = false, unique = true, length = 50)
    private String key;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
