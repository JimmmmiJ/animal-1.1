package com.livestock.health.repository;

import com.livestock.health.model.entity.AnimalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 牲畜档案 Repository
 */
@Repository
public interface AnimalRepository extends JpaRepository<AnimalEntity, Long> {

    Optional<AnimalEntity> findByAnimalId(String animalId);

    List<AnimalEntity> findByFarmId(Long farmId);

    Page<AnimalEntity> findByFarmId(Long farmId, Pageable pageable);

    List<AnimalEntity> findByFarmIdAndBehaviorStatus(Long farmId, String behaviorStatus);

    List<AnimalEntity> findByFarmIdAndRiskLevel(Long farmId, String riskLevel);

    List<AnimalEntity> findByFarmIdAndEstrusStatus(Long farmId, String estrusStatus);

    @Query("SELECT a FROM AnimalEntity a WHERE a.farmId = :farmId AND a.deviceId IS NOT NULL")
    List<AnimalEntity> findByFarmIdWithDevice(@Param("farmId") Long farmId);

    @Query("SELECT COUNT(a) FROM AnimalEntity a WHERE a.farmId = :farmId")
    Long countByFarmId(@Param("farmId") Long farmId);

    @Query("SELECT COUNT(a) FROM AnimalEntity a WHERE a.farmId = :farmId AND a.behaviorStatus = :status")
    Long countByFarmIdAndBehaviorStatus(@Param("farmId") Long farmId, @Param("status") String status);

    @Query("SELECT AVG(a.healthScore) FROM AnimalEntity a WHERE a.farmId = :farmId")
    Double avgHealthScoreByFarmId(@Param("farmId") Long farmId);
}
