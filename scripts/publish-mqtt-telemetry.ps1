param(
    [string]$Topic = "livestock/SN100000/telemetry",
    [string]$DeviceSn = "SN100000",
    [int]$Count = 20,
    [int]$IntervalSeconds = 5
)

$random = [Random]::new()

for ($i = 1; $i -le $Count; $i++) {
    $payload = @{
        deviceSn = $DeviceSn
        timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
        temperature = [math]::Round(38 + $random.NextDouble() * 1.8, 1)
        heartRate = 65 + $random.Next(35)
        activityLevel = 20 + $random.Next(70)
        ruminationTime = 8 + $random.Next(18)
        feedingCount = 1 + $random.Next(4)
        restingTime = 5 + $random.Next(20)
        batteryLevel = 55 + $random.Next(40)
        signalStrength = -95 + $random.Next(35)
    }

    $json = $payload | ConvertTo-Json -Compress -Depth 4
    $json | docker exec -i livestock-mqtt mosquitto_pub -h localhost -p 1883 -t $Topic -s | Out-Null
    Write-Host "[$i/$Count] MQTT telemetry published: topic=$Topic, device=$DeviceSn"

    if ($i -lt $Count) {
        Start-Sleep -Seconds $IntervalSeconds
    }
}
