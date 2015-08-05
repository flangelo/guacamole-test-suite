
/**
 * Copyright (C) 2015 Glyptodon LLC
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package org.glyptodon.guacamole;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class holding simple metrics.
 */
public class Metrics {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(Metrics.class);
    
    /**
     * Number formatter helper object.
     */
    private final DecimalFormat df = new DecimalFormat(",##0.#");

    /**
     * Internal class for holding a single metric.
     */
    class Metric {

        /**
         * Metric value within the current cycle.
         */
        double current = 0;

        /**
         * Peak metric within a cycle for the run.
         */
        double peak = 0;

        /**
         * Sum of all metrics for all cycles.
         */
        double total = 0;
    }

    /**
     * Hash map of all metrics.
     */
    ConcurrentHashMap<String, Metric> metrics;

    /**
     * Start time.
     */
    long metricStart = 0;

    /**
     * Constructor.
     *
     * @param start
     *     Time when metrics collection started.
     */
    public Metrics(long start) {

        metricStart = start;
        metrics = new ConcurrentHashMap<String, Metric>();

    }

    /**
     * Log the current stats within a cycle.
     */
    public void log() {

        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Metric> entry : metrics.entrySet()) {
            sb.append(entry.getKey());
            sb.append(": ");
            sb.append(df.format(entry.getValue().current));
            sb.append(' ');
        }

        logger.info("Metric stats: {}", sb.toString().trim());
    }

    /**
     * Log totals and averages at the end of a run.
     *
     * @param end
     *     The end time of the run.
     */
    public void logTotals(long end) {

        long time = (end - metricStart) / 1000;

        logger.info("** Logging metrics. Total run time: {} seconds **", time);

        for (Map.Entry<String, Metric> entry : metrics.entrySet()) {

            Metric metric = entry.getValue();
            double average = metric.total / (double)time;
            logger.info(String.format("%s: average: %s peak: %s total: %s",
                                      entry.getKey(), df.format(average),
                                      df.format(metric.peak),
                                      df.format(metric.total)));
        }

    }

    /**
     * Reset counts, update max and start a new cycle.
     */
    public void reset() {

        Enumeration<Metric> allMetrics;
        allMetrics = metrics.elements();

        while (allMetrics.hasMoreElements()) {
            Metric metric = allMetrics.nextElement();
            metric.peak = Math.max(metric.peak, metric.current);
            metric.current = 0;
        }

    }

    /**
     * Add metric value to a cycle.
     *
     * @param key
     *     Metric key
     *
     * @param value
     *     Metric value
     */
    public void addValue(String key, double value) {

        Metric metric = metrics.get(key);

        if (metric == null) {
            metric = new Metric();
            metrics.put(key, metric);
        }

        metric.current += value;
        metric.total += value;

    }
}
