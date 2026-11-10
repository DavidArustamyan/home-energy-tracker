package com.kafka_sc.usage_service.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.kafka_sc.kafka.event.EnergyUsageEvent;
import com.kafka_sc.usage_service.exception.UsageDataUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class EnergyUsageRepository {

    private final InfluxDBClient influxDBClient;
    private final String bucket;
    private final String org;

    public EnergyUsageRepository(InfluxDBClient influxDBClient,
                                 @Value("${influx.bucket}") String bucket,
                                 @Value("${influx.org}") String org
    ) {
        this.influxDBClient = influxDBClient;
        this.bucket = bucket;
        this.org = org;
    }

    public void save(EnergyUsageEvent event) {
        Point point = Point.measurement("energy_usage")
                .addTag("deviceId", String.valueOf(event.deviceId()))
                .addField("energyConsumed", event.energyConsumed())
                .time(event.timestamp(), WritePrecision.MS);
        influxDBClient.getWriteApiBlocking().writePoint(bucket, org, point);
    }

    public Map<Long, Double> sumByDevice(Instant from, Instant to) {
        return query(from, to, "");
    }

    public Map<Long, Double> sumByDevice(Instant from, Instant to, Collection<Long> deviceIds) {
        if (deviceIds.isEmpty()) return Map.of();

        String deviceFilter = deviceIds.stream()
                .map(id -> "r[\"deviceId\"] == \"" + id + "\"")
                .collect(Collectors.joining(" or ", "|> filter(fn: (r) => ", ")"));
        return query(from, to, deviceFilter);
    }

    private Map<Long, Double> query(Instant from, Instant to, String extraFilter) {
        String flux = String.format("""
                from(bucket: "%s")
                  |> range(start: time(v: "%s"), stop: time(v: "%s"))
                  |> filter(fn: (r) => r["_measurement"] == "energy_usage")
                  |> filter(fn: (r) => r["_field"] == "energyConsumed")
                  %s
                  |> group(columns: ["deviceId"])
                  |> sum(column: "_value")
                """, bucket, from, to, extraFilter);
        try {
            Map<Long, Double> result = new HashMap<>();
            for (FluxTable table : influxDBClient.getQueryApi().query(flux, org)) {
                for (FluxRecord record : table.getRecords()) {
                    Object deviceId = record.getValueByKey("deviceId");
                    if (deviceId != null && record.getValue() instanceof Number value) {
                        result.merge(Long.valueOf(deviceId.toString()), value.doubleValue(), Double::sum);
                    }
                }
            }
            return result;
        } catch (InfluxException e) {
            throw new UsageDataUnavailableException(e);
        }
    }
}
