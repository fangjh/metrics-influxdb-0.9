package com.justdebugit.metrics.influxdb;

import com.codahale.metrics.*;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * User: fangjh@gmail.com
 * Date: 2017/1/15
 * Time: 21:16
 */
abstract class MeasurementFields<T extends Metric> {
    public Map<String, Object> getFields(T metric, ReporterDelegate delegate) {
        return getFields("", metric, delegate);
    }

    public Map<String, Object> getFields(Map<String, T> metrics, ReporterDelegate delegate) {
        Map<String, Object> fieldMap = new HashMap<>(metrics.size() * 10);
        for (Map.Entry<String, T> metric : metrics.entrySet()) {
            String prefix = InfluxdbMetrics.resolveMeasurementFieldPrefix(metric.getKey());
            if (StringUtils.isNotEmpty(prefix)) {
                prefix += "_";
            }
            fieldMap.putAll(getFields(prefix.toLowerCase(), metric.getValue(), delegate));
        }
        return fieldMap;
    }

    protected abstract Map<String, Object> getFields(String prefix, T metric, ReporterDelegate delegate);

    static final MeasurementFields<Timer> TIMER = new MeasurementFields<Timer>() {
        @Override
        protected Map<String, Object> getFields(String prefix, Timer metric, ReporterDelegate delegate) {
            final Snapshot snapshot = metric.getSnapshot();
            Map<String, Object> fieldMap = new HashMap<String, Object>();
            fieldMap.put(prefix + "count", metric.getCount());
            fieldMap.put(prefix + "mean_rate", delegate.convertRate(metric.getMeanRate()));
            fieldMap.put(prefix + "m1_rate", delegate.convertRate(metric.getOneMinuteRate()));
//        fieldMap.put("m5_rate", convertRate(timer.getFiveMinuteRate()));
//        fieldMap.put("m15_rate", convertRate(timer.getFifteenMinuteRate()));

            fieldMap.put(prefix + "max", delegate.convertDuration(snapshot.getMax()));
            fieldMap.put(prefix + "mean", delegate.convertDuration(snapshot.getMean()));
            fieldMap.put(prefix + "min", delegate.convertDuration(snapshot.getMin()));
//        fieldMap.put("stddev", convertDuration(snapshot.getStdDev()));
            fieldMap.put(prefix + "p50", delegate.convertDuration(snapshot.getMedian()));
            fieldMap.put(prefix + "p75", delegate.convertDuration(snapshot.get75thPercentile()));
//        fieldMap.put("p95", convertDuration(snapshot.get95thPercentile()));
            fieldMap.put(prefix + "p99", delegate.convertDuration(snapshot.get99thPercentile()));
//        fieldMap.put("p999", convertDuration(snapshot.get999thPercentile()));

            return fieldMap;
        }
    };

    static final MeasurementFields<Meter> METER = new MeasurementFields<Meter>() {
        @Override
        protected Map<String, Object> getFields(String prefix, Meter metric, ReporterDelegate delegate) {
            Map<String, Object> fieldMap = new HashMap<String, Object>();
            fieldMap.put(prefix + "count", metric.getCount());
            fieldMap.put(prefix + "m1_rate", delegate.convertRate(metric.getOneMinuteRate()));
//        fieldMap.put("m5_rate", convertRate(meter.getFiveMinuteRate()));
//        fieldMap.put("m15_rate", convertRate(meter.getFifteenMinuteRate()));
            fieldMap.put(prefix + "mean_rate", delegate.convertRate(metric.getMeanRate()));
            return fieldMap;
        }
    };

    static final MeasurementFields<Histogram> HISTOGRAM = new MeasurementFields<Histogram>() {
        @Override
        protected Map<String, Object> getFields(String prefix, Histogram metric, ReporterDelegate delegate) {
            final Snapshot snapshot = metric.getSnapshot();
            Map<String, Object> fieldMap = new HashMap<String, Object>();
            fieldMap.put(prefix + "count", metric.getCount());
            fieldMap.put(prefix + "max", snapshot.getMax());
            fieldMap.put(prefix + "mean", snapshot.getMean());
            fieldMap.put(prefix + "min", snapshot.getMin());
//        fieldMap.put("stddev", snapshot.getStdDev());
            fieldMap.put(prefix + "p50", snapshot.getMedian());
            fieldMap.put(prefix + "p75", snapshot.get75thPercentile());
//        fieldMap.put("p95", snapshot.get95thPercentile());
//        fieldMap.put("p98", snapshot.get98thPercentile());
            fieldMap.put(prefix + "p99", snapshot.get99thPercentile());
//        fieldMap.put("p999", snapshot.get999thPercentile());
            return fieldMap;
        }
    };

    static final MeasurementFields<Gauge> GAUGE = new MeasurementFields<Gauge>() {
        @Override
        protected Map<String, Object> getFields(String prefix, Gauge metric, ReporterDelegate delegate) {
            Map<String, Object> fieldMap = new HashMap<String, Object>();
            fieldMap.put(prefix + "value", metric.getValue());
            return fieldMap;
        }
    };

    static final MeasurementFields<Counter> COUNTER = new MeasurementFields<Counter>() {
        @Override
        protected Map<String, Object> getFields(String prefix, Counter metric, ReporterDelegate delegate) {
            Map<String, Object> fieldMap = new HashMap<String, Object>();
            fieldMap.put(prefix + "count", metric.getCount());
            return fieldMap;
        }
    };


    interface ReporterDelegate {
        double convertRate(double rate);

        double convertDuration(double duration);
    }


}
