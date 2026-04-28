package com.livestock.health.repository;

import com.livestock.health.model.entity.SystemConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfigEntity, Long> {

    Optional<SystemConfigEntity> findByConfigKey(String configKey);
}
