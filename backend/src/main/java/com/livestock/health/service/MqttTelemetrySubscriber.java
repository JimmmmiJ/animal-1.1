package com.livestock.health.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livestock.health.model.dto.TelemetryPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttTelemetrySubscriber implements SmartLifecycle {

    private final ObjectMapper objectMapper;
    private final TelemetryIngestionService telemetryIngestionService;

    @Value("${mqtt.enabled:false}")
    private boolean enabled;

    @Value("${mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${mqtt.client-id:livestock-backend}")
    private String clientId;

    @Value("${mqtt.topic:livestock/+/telemetry}")
    private String topic;

    @Value("${mqtt.qos:1}")
    private int qos;

    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    private MqttClient client;
    private volatile boolean running;

    @Override
    public void start() {
        if (!enabled || running) {
            return;
        }

        try {
            client = new MqttClient(brokerUrl, clientId + "-" + UUID.randomUUID().toString().substring(0, 8));
            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    subscribe();
                }

                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("MQTT connection lost: {}", cause == null ? "unknown" : cause.getMessage());
                }

                @Override
                public void messageArrived(String arrivedTopic, MqttMessage message) {
                    handleMessage(arrivedTopic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // This subscriber does not publish messages.
                }
            });

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            if (username != null && !username.isBlank()) {
                options.setUserName(username);
            }
            if (password != null && !password.isBlank()) {
                options.setPassword(password.toCharArray());
            }

            client.connect(options);
            running = true;
            log.info("MQTT telemetry subscriber connected: broker={}, topic={}", brokerUrl, topic);
        } catch (MqttException error) {
            running = false;
            log.warn("MQTT telemetry subscriber not connected: broker={}, message={}", brokerUrl, error.getMessage());
        }
    }

    @Override
    public void stop() {
        running = false;
        if (client == null) {
            return;
        }
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        } catch (MqttException error) {
            log.warn("Failed to stop MQTT telemetry subscriber", error);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    private void subscribe() {
        try {
            client.subscribe(topic, qos);
            log.info("MQTT telemetry subscriber subscribed: topic={}, qos={}", topic, qos);
        } catch (MqttException error) {
            log.warn("Failed to subscribe MQTT telemetry topic: topic={}", topic, error);
        }
    }

    private void handleMessage(String arrivedTopic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        try {
            TelemetryPayload telemetry = objectMapper.readValue(payload, TelemetryPayload.class);
            telemetryIngestionService.ingest(telemetry, "MQTT:" + arrivedTopic);
        } catch (Exception error) {
            log.warn("Failed to ingest MQTT telemetry: topic={}, payload={}", arrivedTopic, payload, error);
        }
    }
}
