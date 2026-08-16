package com.igrejahub.permissions.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "permissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;
    @Column(name = "description", length = 255)
    private String description;
    @Column(name = "category", length = 50)
    private String category;
    @Column(name = "is_active")
    @Builder.Default private boolean active = true;
    @Column(name = "is_system")
    @Builder.Default private boolean system = false;
}
