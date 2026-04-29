package com.livestock.health.repository;

import com.livestock.health.model.entity.DeviceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 智能设备 Repository
 */
@Repository
public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {

    Optional<DeviceEntity> findByDeviceId(String deviceId);

    Optional<DeviceEntity> findByDeviceSn(String deviceSn);

    List<DeviceEntity> findByFarmId(Long farmId);

    Page<DeviceEntity> findByFarmId(Long farmId, Pageable pageable);

    List<DeviceEntity> findByFarmIdAndStatus(Long farmId, String status);

    @Query("SELECT COUNT(d) FROM DeviceEntity d WHERE d.farmId = :farmId")
    Long countByFarmId(@Param("farmId") Long farmId);

    @Query("SELECT COUNT(d) FROM DeviceEntity d WHERE d.farmId = :farmId AND d.status = 'online'")
    Long countOnlineByFarmId(@Param("farmId") Long farmId);

    @Query("SELECT COUNT(d) FROM DeviceEntity d WHERE d.farmId = :farmId AND d.status = 'offline'")
    Long countOfflineByFarmId(@Param("farmId") Long farmId);

    @Query("SELECT COUNT(d) FROM DeviceEntity d WHERE d.farmId = :farmId AND d.status = 'fault'")
    Long countFaultByFarmId(@Param("farmId") Long farmId);

    @Query("SELECT AVG(d.batteryLevel) FROM DeviceEntity d WHERE d.farmId = :farmId")
    Double avgBatteryLevelByFarmId(@Param("farmId") Long farmId);
}
