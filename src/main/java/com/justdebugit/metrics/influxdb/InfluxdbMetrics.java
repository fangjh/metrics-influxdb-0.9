package com.justdebugit.metrics.influxdb;

import com.codahale.metrics.MetricRegistry;
import com.justdebugit.metrics.Tuple2;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: fangjh@gmail.com
 * Date: 2017/1/15
 * Time: 20:10
 */
public class InfluxdbMetrics {
    private static final Logger LOGGER = LoggerFactory.getLogger(InfluxdbMetrics.class);

    private static final Pattern MEASUREMENT_MATCHER = Pattern.compile("\\[(.+)\\]\\.*(.+)$");

    @SafeVarargs
    public static String makeMetricName(String name, Tuple2<String, String>... tags) {
        if (tags == null || tags.length == 0) {
            return name;
        }
        StringBuilder builder = new StringBuilder(name);
        builder.append('|');
        int i = 0;
        for (Tuple2<String, String> tag : tags) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(tag.getFirst()).append('=').append(tag.getSecond());
            i++;
        }
        return builder.toString();
    }

    /**
     * 过滤点号,会造成查询时出错，点号会被认为是db与留存策略的分隔
     *
     * @param appname
     * @param name
     * @param type
     * @return
     */
    static String makeMeasurementName(String appname, String name, String type) {
        return MetricRegistry.name(appname, getRealMetricName(name), type).replaceAll("\\.", "_").toLowerCase();
    }


    static String getRealMetricName(String name) {
        return StringUtils.split(name, '|')[0];
    }

    static String resolveMeasurement(String metric) {
        Matcher matcher = MEASUREMENT_MATCHER.matcher(metric);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return metric;
    }

    static String resolveMeasurementFieldPrefix(String metric) {
        Matcher matcher = MEASUREMENT_MATCHER.matcher(metric);
        if (!matcher.find()) {
            return "";
        }
        String prefix = matcher.group(2);
        return prefix.replace('.', '_');
    }

    static boolean hasDefinedMeasurement(String metric) {
        return MEASUREMENT_MATCHER.matcher(metric).find();
    }

    static Map<String, String> getTagFromMetricName(String name) {
        String[] split = StringUtils.split(name, '|');
        if (split.length < 2) {
            LOGGER.debug("no tags found for metric: {}", name);
            return null;
        }
        String[] tags = StringUtils.split(split[1], ',');
        Map<String, String> tagMap = new HashMap<>(tags.length);
        for (String tag : tags) {
            String[] s = StringUtils.split(tag, '=');
            if (s.length != 2) {
                LOGGER.warn("invalid tag {} for metric: {}", tag, name);
                continue;
            }
            tagMap.put(s[0], s[1]);
        }
        return tagMap;
    }

}
