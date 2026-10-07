package com.igrejahub.modules.repository;

import com.igrejahub.modules.entity.ChurchModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChurchModuleRepository extends JpaRepository<ChurchModule, Long> {
    List<ChurchModule> findByChurchId(Long churchId);
    Optional<ChurchModule> findByChurchIdAndModuleId(Long churchId, Long moduleId);
}
