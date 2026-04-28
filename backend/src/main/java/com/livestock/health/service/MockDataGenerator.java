package com.livestock.health.service;

import com.livestock.health.model.entity.AnimalEntity;
import com.livestock.health.model.entity.DeviceEntity;
import com.livestock.health.model.entity.HealthDataEntity;
import com.livestock.health.repository.AnimalRepository;
import com.livestock.health.repository.DeviceRepository;
import com.livestock.health.repository.HealthDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockDataGenerator implements CommandLineRunner {

    private static final long DEMO_FARM_ID = 1L;
    private static final int ANIMAL_COUNT = 48;
    private static final int DAYS_OF_HISTORY = 7;

    private final AnimalRepository animalRepository;
    private final DeviceRepository deviceRepository;
    private final HealthDataRepository healthDataRepository;

    private final Random random = new Random(20260423L);

    @Override
    public void run(String... args) {
        if (animalRepository.count() > 0) {
            log.info("Demo data already present, skipping seed");
            return;
        }

        log.info("Seeding demo data for local development");

        List<AnimalEntity> animals = seedAnimals();
        seedDevices(animals);
        seedHealthData(animals);

        log.info("Demo data seed complete: {} animals, {} devices", animals.size(), animals.size());
    }

    private List<AnimalEntity> seedAnimals() {
        String[] breeds = {"Merino", "Suffolk", "Dorper", "Texel"};
        String[] riskLevels = {"normal", "normal", "low", "medium", "high"};
        String[] behaviorStatuses = {"normal", "normal", "normal", "warning", "abnormal"};
        String[] estrusStatuses = {"normal", "normal", "approaching", "estrus", "pregnant"};

        List<AnimalEntity> animals = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= ANIMAL_COUNT; i++) {
            double estrusProbability = round1(random.nextDouble() * 100.0);
            AnimalEntity animal = AnimalEntity.builder()
                .animalId(String.format("SHEEP%03d", i))
                .farmId(DEMO_FARM_ID)
                .name(String.format("Sheep-%03d", i))
                .breed(breeds[random.nextInt(breeds.length)])
                .gender(i % 4 == 0 ? 1 : 2)
                .age(12 + random.nextInt(48))
                .weight(round1(38 + random.nextDouble() * 32))
                .healthScore(round1(68 + random.nextDouble() * 30))
                .riskLevel(riskLevels[random.nextInt(riskLevels.length)])
                .dailyRuminationTime(250 + random.nextInt(130))
                .dailyFeedingCount(12 + random.nextInt(12))
                .ruminationEfficiency(round1(70 + random.nextDouble() * 25))
                .feedingQuality(round1(68 + random.nextDouble() * 25))
                .behaviorStatus(behaviorStatuses[random.nextInt(behaviorStatuses.length)])
                .estrusStatus(estrusStatuses[random.nextInt(estrusStatuses.length)])
                .estrusProbability(estrusProbability)
                .lastEstrusAt(now.minusDays(random.nextInt(25) + 1))
                .nextEstrusPredictedAt(now.plusDays(random.nextInt(10) + 1))
                .lastBehaviorUpdateAt(now.minusMinutes(random.nextInt(120)))
                .lastDietAdjustAt(now.minusDays(random.nextInt(14) + 1))
                .build();
            animals.add(animal);
        }

        return animalRepository.saveAll(animals);
    }

    private void seedDevices(List<AnimalEntity> animals) {
        String[] statuses = {"online", "online", "online", "offline", "fault"};
        LocalDateTime now = LocalDateTime.now();
        List<DeviceEntity> devices = new ArrayList<>();

        for (int i = 0; i < animals.size(); i++) {
            AnimalEntity animal = animals.get(i);
            DeviceEntity device = DeviceEntity.builder()
                .deviceId(String.format("20260101%04d", i + 1))
                .deviceModel("SMART_COLLAR_V1")
                .deviceSn(String.format("SN%06d", 100000 + i))
                .farmId(DEMO_FARM_ID)
                .animalId(animal.getId())
                .status(statuses[random.nextInt(statuses.length)])
                .batteryLevel(18 + random.nextInt(82))
                .signalStrength(-92 + random.nextInt(58))
                .currentTemperature(round1(38 + random.nextDouble() * 2.2))
                .currentHeartRate(60 + random.nextInt(55))
                .currentActivity(random.nextInt(100))
                .lastOnlineAt(now.minusMinutes(random.nextInt(90)))
                .lastDataUpdateAt(now.minusMinutes(random.nextInt(30)))
                .build();
            devices.add(device);
        }

        List<DeviceEntity> savedDevices = deviceRepository.saveAll(devices);

        for (int i = 0; i < animals.size(); i++) {
            animals.get(i).setDeviceId(savedDevices.get(i).getId());
        }
        animalRepository.saveAll(animals);
    }

    private void seedHealthData(List<AnimalEntity> animals) {
        LocalDateTime baseTime = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        List<HealthDataEntity> records = new ArrayList<>();

        for (AnimalEntity animal : animals) {
            for (int day = 0; day < DAYS_OF_HISTORY; day++) {
                for (int hour = 0; hour < 24; hour++) {
                    LocalDateTime dataTime = baseTime.minusDays(day).withHour(hour);
                    records.add(HealthDataEntity.builder()
                        .deviceId(animal.getDeviceId())
                        .animalId(animal.getId())
                        .farmId(DEMO_FARM_ID)
                        .temperature(round1(38 + random.nextDouble() * 2.2))
                        .heartRate(60 + random.nextInt(55))
                        .activityLevel(random.nextInt(100))
                        .ruminationTime(8 + random.nextInt(18))
                        .feedingCount(1 + random.nextInt(4))
                        .restingTime(5 + random.nextInt(20))
                        .dataTime(dataTime)
                        .build());
                }
            }
        }

        healthDataRepository.saveAll(records);
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
