package com.justdebugit.metrics.influxdb;

import com.codahale.metrics.*;
import com.justdebugit.metrics.OSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * reporter for influxdb
 *
 * @author justdebugit
 */
public class InfluxdbReporter extends ScheduledReporter {

    private static final Logger logger = LoggerFactory.getLogger(InfluxdbReporter.class);

    private static final String HOST_TAG_KEY = "host";

    /**
     * 为 {@link InfluxdbReporter} 返回一个 {@link Builder}
     *
     * @param registry
     * @return {@link Builder} 实例
     */
    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    /**
     * 类似于consoleReporter
     */
    public static class Builder {
        private final MetricRegistry registry;
        private Clock clock;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;
        private Map<String, String> tagMap;
        private String appName;

        private Builder(MetricRegistry registry) {
            this.tagMap = new HashMap<String, String>();
            tagMap.put(HOST_TAG_KEY, OSUtil.getHostName());
            this.registry = registry;
            this.clock = Clock.defaultClock();
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
        }


        /**
         * 设置应用名称,必填项
         *
         * @param name
         * @return {@code Builder}
         */
        public Builder appName(String name) {
            this.appName = name;
            return this;
        }


        public Builder addTag(String key, String value) {
            tagMap.put(key, value);
            return this;
        }

        /**
         * 用于获取时间
         *
         * @param clock a {@link Clock} instance
         * @return {@code Builder}
         */
        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * 依据传入时间单位作为频率
         *
         * @param rateUnit 时间单位
         * @return {@code Builder}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * 用于记录耗时的时间单位
         *
         * @param durationUnit a unit of time
         * @return {@code Builder}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * 可以过滤指定名称的metric
         *
         * @param filter a {@link MetricFilter}
         * @return {@code Builder}
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * 移出默认tag:pid,appName,host
         *
         * @return
         */
        public Builder withOutDefaultTags() {
            tagMap.remove(HOST_TAG_KEY);
            return this;
        }

        /**
         * 构建 {@link InfluxdbReporter}
         *
         * @return a {@link InfluxdbReporter}
         */
        public InfluxdbReporter build(Influxdb influxdb) {
            if (influxdb == null) {
                throw new IllegalArgumentException("influxdb can not be null");
            }
            if (appName == null) {
                throw new IllegalArgumentException("appname can not be null");
            }
            return new InfluxdbReporter(registry, influxdb, clock, rateUnit, durationUnit, filter,
                    tagMap, appName);
        }
    }


    private final Influxdb influxdb;
    private final Clock clock;
    private final Map<String, String> tagMap;
    private final String appName;

    private InfluxdbReporter(MetricRegistry registry, Influxdb influxdb, Clock clock,
                             TimeUnit rateUnit, TimeUnit durationUnit, MetricFilter filter, Map<String, String> tagMap,
                             String appName) {
        super(registry, "influxdb-reporter", filter, rateUnit, durationUnit);
        this.influxdb = influxdb;
        this.clock = clock;
        this.appName = appName;
        this.tagMap = Collections.unmodifiableMap(tagMap);
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        try {
            long nanoTimestamp = clock.getTime() * 1000 * 1000;
            reportMetrics(gauges, "gauge", nanoTimestamp);
            reportMetrics(counters, "counter", nanoTimestamp);
            reportMetrics(histograms, "histogram", nanoTimestamp);
            reportMetrics(meters, "meter", nanoTimestamp);
            reportMetrics(timers, "timer", nanoTimestamp);
            influxdb.flush();
        } catch (InfluxdbException e) {
            logger.warn("Report metrics data failed,please make sure remote influxdb is OK", e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private <T extends Metric> void reportMetrics(Map<String, T> metrics, String type, long timestamp) throws InfluxdbException {
        if (metrics == null || metrics.isEmpty()) {
            return;
        }
        Map<String, Map<String, T>> combined = combineMeasurement(metrics);
        for (Map.Entry<String, Map<String, T>> entry : combined.entrySet()) {
            reportMetrics(entry.getKey(), entry.getValue(), type, timestamp);
        }
    }

    private <T extends Metric> void reportMetrics(String measurement, Map<String, T> metrics, String type, long timestamp) throws InfluxdbException {
        String fullMeasurement = InfluxdbMetrics.makeMeasurementName(appName, measurement, type);
        MeasurementFields<T> mf;
        if ("meter".equals(type)) {
            mf = (MeasurementFields<T>) MeasurementFields.METER;
        } else if ("timer".equals(type)) {
            mf = (MeasurementFields<T>) MeasurementFields.TIMER;
        } else if ("counter".equals(type)) {
            mf = (MeasurementFields<T>) MeasurementFields.COUNTER;
        } else if ("gauge".equals(type)) {
            mf = (MeasurementFields<T>) MeasurementFields.GAUGE;
        } else if ("histogram".equals(type)) {
            mf = (MeasurementFields<T>) MeasurementFields.HISTOGRAM;
        } else {
            throw new IllegalArgumentException("unknown type: " + type);
        }
        influxdb.writeData(buildSinglePoint(fullMeasurement, assembleTagMap(tagMap, measurement), mf.getFields(metrics, reporterDelegate), timestamp));
    }

    private final MeasurementFields.ReporterDelegate reporterDelegate = new MeasurementFields.ReporterDelegate() {
        @Override
        public double convertRate(double rate) {
            return InfluxdbReporter.this.convertRate(rate);
        }

        @Override
        public double convertDuration(double duration) {
            return InfluxdbReporter.this.convertDuration(duration);
        }
    };

    private <T extends Metric> Map<String, Map<String, T>> combineMeasurement(Map<String, T> metrics) {
        Map<String, Map<String, T>> combined = new HashMap<>(metrics.size());
        for (Map.Entry<String, T> entry : metrics.entrySet()) {
            String measurement = InfluxdbMetrics.resolveMeasurement(entry.getKey());
            Map<String, T> metricList = combined.get(measurement);
            if (metricList == null) {
                metricList = new HashMap<>();
                combined.put(measurement, metricList);
            }
            metricList.put(entry.getKey(), entry.getValue());
        }
        return combined;
    }

    private static SinglePoint buildSinglePoint(String name, Map<String, String> tagMap,
                                                Map<String, Object> fieldMap, long timestamp) {
        Map<String, Object> finalMap = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : fieldMap.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Number) {
                if (value instanceof Double) {
                    finalMap.put(entry.getKey(), String.format("%.2f", (Double) value));
                } else {
                    finalMap.put(entry.getKey(), value);
                }
            }
        }
        return SinglePoint.newBuilder(name).addAllTag(tagMap).addAllField(finalMap)
                .setTimestamp(timestamp).build();
    }

    private static Map<String, String> assembleTagMap(Map<String, String> commonTagMap, String metric) {
        Map<String, String> metricTagMap = InfluxdbMetrics.getTagFromMetricName(metric);
        if (metricTagMap == null || metricTagMap.isEmpty()) {
            return commonTagMap;
        }
        metricTagMap.putAll(commonTagMap);
        return metricTagMap;
    }
}
