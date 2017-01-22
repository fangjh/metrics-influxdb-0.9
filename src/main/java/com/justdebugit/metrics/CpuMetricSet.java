package com.justdebugit.metrics;


import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;


/**
 * USER: fangjiahao
 * DATE: 2017/1/13
 * TIME: 17:01
 */
public class CpuMetricSet implements MetricSet {
    private final OperatingSystemMXBean operatingSystemMXBean;

    public CpuMetricSet() {
        operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<String, Metric>();
        if (operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean) {
            final com.sun.management.OperatingSystemMXBean mxBean = (com.sun.management.OperatingSystemMXBean) operatingSystemMXBean;
            gauges.put("systemCpuLoad", new Gauge<Double>() {
                @Override
                public Double getValue() {
                    double systemCpuLoad = mxBean.getSystemCpuLoad();
                    return systemCpuLoad < 0 ? null : systemCpuLoad;
                }
            });
            gauges.put("processCpuLoad", new Gauge<Double>() {
                @Override
                public Double getValue() {
                    double processCpuLoad = mxBean.getProcessCpuLoad();
                    return processCpuLoad < 0 ? null : processCpuLoad;
                }
            });
        }
        return gauges;
    }
}
