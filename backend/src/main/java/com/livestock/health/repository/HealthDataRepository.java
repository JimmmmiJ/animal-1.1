package com.livestock.health.repository;

import com.livestock.health.model.entity.HealthDataEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 健康数据 Repository
 */
@Repository
public interface HealthDataRepository extends JpaRepository<HealthDataEntity, Long> {

    List<HealthDataEntity> findByAnimalIdOrderByDataTimeDesc(Long animalId);

    Page<HealthDataEntity> findByAnimalIdOrderByDataTimeDesc(Long animalId, Pageable pageable);

    List<HealthDataEntity> findByAnimalIdAndDataTimeBetweenOrderByDataTimeDesc(
        Long animalId, LocalDateTime startTime, LocalDateTime endTime);

    List<HealthDataEntity> findByFarmIdAndDataTimeBetweenOrderByDataTimeDesc(
        Long farmId, LocalDateTime startTime, LocalDateTime endTime);

    HealthDataEntity findTopByFarmIdOrderByDataTimeDesc(Long farmId);

    HealthDataEntity findTopByDeviceIdOrderByDataTimeDesc(Long deviceId);

    @Query("SELECT AVG(h.temperature) FROM HealthDataEntity h WHERE h.animalId = :animalId AND h.dataTime >= :startTime")
    Double avgTemperatureByAnimalIdAndTimeAfter(@Param("animalId") Long animalId, @Param("startTime") LocalDateTime startTime);

    @Query("SELECT AVG(h.heartRate) FROM HealthDataEntity h WHERE h.animalId = :animalId AND h.dataTime >= :startTime")
    Double avgHeartRateByAnimalIdAndTimeAfter(@Param("animalId") Long animalId, @Param("startTime") LocalDateTime startTime);

    @Query("SELECT SUM(h.ruminationTime) FROM HealthDataEntity h WHERE h.animalId = :animalId AND DATE(h.dataTime) = :date")
    Integer sumRuminationTimeByAnimalIdAndDate(@Param("animalId") Long animalId, @Param("date") LocalDateTime date);

    @Query("SELECT SUM(h.feedingCount) FROM HealthDataEntity h WHERE h.animalId = :animalId AND DATE(h.dataTime) = :date")
    Integer sumFeedingCountByAnimalIdAndDate(@Param("animalId") Long animalId, @Param("date") LocalDateTime date);
}
