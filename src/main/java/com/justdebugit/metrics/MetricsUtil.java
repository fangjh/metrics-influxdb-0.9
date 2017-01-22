package com.justdebugit.metrics;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * USER: fangjiahao
 * DATE: 2017/1/13
 * TIME: 10:20
 */
public class MetricsUtil {

    private static final MetricFilter DEFAULT_JVM_METRIC_FILTER = new DefaultJvmMetricFilter();

    /**
     * 使用[]可对多个metric进行分组，相同组的metric会写到同一个measurement中，[]中的内容作为measurement名称，后续部分成为field
     *
     * @return
     */
    public static MetricRegistry defaultMetricRegistry() {
        MetricRegistry metrics = new MetricRegistry();
        metrics.register("[jvm.mem]", new MemoryUsageGaugeSet());
        metrics.register("[jvm.gc]", new GarbageCollectorMetricSet());
        metrics.register("[jvm.thread]", new ThreadStatesGaugeSet());
        metrics.register("[jvm.cpu]", new CpuMetricSet());
        metrics.removeMatching(DEFAULT_JVM_METRIC_FILTER);
        return metrics;
    }

    private static class DefaultJvmMetricFilter implements MetricFilter {

        private final List<Pattern> patterns = new ArrayList<>();

        private DefaultJvmMetricFilter() {
            patterns.add(Pattern.compile("\\[jvm\\.mem\\]\\..+\\.(init|committed|max)$"));
            patterns.add(Pattern.compile("\\[jvm\\.thread\\]\\.daemon\\.count$"));
            patterns.add(Pattern.compile("\\[jvm\\.thread\\]\\.deadlock\\.count$"));
            patterns.add(Pattern.compile("\\[jvm\\.thread\\]\\.deadlocks$"));
        }

        @Override
        public boolean matches(String name, Metric metric) {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(name).matches()) {
                    return true;
                }
            }
            return false;
        }
    }
}
