/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.monitoring.metrics.impl;

import io.gravitee.gateway.services.monitoring.metrics.JvmMetricsService;
import io.gravitee.reporter.api.metrics.Gauge;
import io.gravitee.reporter.api.metrics.Metric;
import io.gravitee.reporter.api.metrics.RatioGauge;

import java.lang.management.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class JvmMetricsServiceImpl implements JvmMetricsService {

    // Do not compute stack traces.
    private final static int STACK_TRACE_DEPTH = 0;
    private static final int MAX_STACK_TRACE_DEPTH = 100;
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private final List<GarbageCollectorMXBean> garbageCollectors;
    private final ThreadMXBean threads;
    private final OperatingSystemMXBean operatingSystemMXBean;
    private final MemoryMXBean memoryMxBean;
    private final List<MemoryPoolMXBean> memoryPools;

    public JvmMetricsServiceImpl() {
        this.threads = ManagementFactory.getThreadMXBean();
        this.garbageCollectors = new ArrayList<>(
                ManagementFactory.getGarbageCollectorMXBeans());
        this.memoryMxBean = ManagementFactory.getMemoryMXBean();
        this.memoryPools = ManagementFactory.getMemoryPoolMXBeans();
        this.operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
    }

    @Override
    public Gauge<Double> getCpuUsage() {
        return operatingSystemMXBean::getSystemLoadAverage;
    }

    @Override
    public Map<String, Metric> getMemoryUsage() {
        final Map<String, Metric> gauges = new HashMap<>();

        gauges.put("total.init", (Gauge) () -> memoryMxBean.getHeapMemoryUsage().getInit()
                + memoryMxBean.getNonHeapMemoryUsage().getInit());
        gauges.put("total.used", (Gauge<Long>) () -> memoryMxBean.getHeapMemoryUsage().getUsed()
                + memoryMxBean.getNonHeapMemoryUsage().getUsed());
        gauges.put("total.max", (Gauge<Long>) () -> memoryMxBean.getHeapMemoryUsage().getMax()
                + memoryMxBean.getNonHeapMemoryUsage().getMax());
        gauges.put("total.committed", (Gauge<Long>) () -> memoryMxBean.getHeapMemoryUsage().getCommitted()
                + memoryMxBean.getNonHeapMemoryUsage().getCommitted());

        gauges.put("heap.init", (Gauge<Long>) () -> memoryMxBean.getHeapMemoryUsage().getInit());
        gauges.put("heap.used", (Gauge<Long>) () -> memoryMxBean.getHeapMemoryUsage().getUsed());
        gauges.put("heap.max", (Gauge<Long>) () -> memoryMxBean.getHeapMemoryUsage().getMax());
        gauges.put("heap.committed", (Gauge<Long>) () -> memoryMxBean.getHeapMemoryUsage().getCommitted());
        gauges.put("heap.usage", new RatioGauge() {
            @Override
            protected RatioGauge.Ratio getRatio() {
                final MemoryUsage usage = memoryMxBean.getHeapMemoryUsage();
                return RatioGauge.Ratio.of(usage.getUsed(), usage.getMax());
            }
        });

        gauges.put("non-heap.init", (Gauge<Long>) () -> memoryMxBean.getNonHeapMemoryUsage().getInit());
        gauges.put("non-heap.used", (Gauge<Long>) () -> memoryMxBean.getNonHeapMemoryUsage().getUsed());
        gauges.put("non-heap.max", (Gauge<Long>) () -> memoryMxBean.getNonHeapMemoryUsage().getMax());
        gauges.put("non-heap.committed", (Gauge<Long>) () -> memoryMxBean.getNonHeapMemoryUsage().getCommitted());
        gauges.put("non-heap.usage", new RatioGauge() {
            @Override
            protected RatioGauge.Ratio getRatio() {
                final MemoryUsage usage = memoryMxBean.getNonHeapMemoryUsage();
                return RatioGauge.Ratio.of(usage.getUsed(), usage.getMax());
            }
        });

        for (final MemoryPoolMXBean pool : memoryPools) {
            final String poolName = name("pools", WHITESPACE.matcher(pool.getName()).replaceAll("-"));

            gauges.put(name(poolName, "usage"),
                    new RatioGauge() {
                        @Override
                        protected RatioGauge.Ratio getRatio() {
                            final long max = pool.getUsage().getMax() == -1
                                    ? pool.getUsage().getCommitted()
                                    : pool.getUsage().getMax();
                            return RatioGauge.Ratio.of(pool.getUsage().getUsed(), max);
                        }
                    });

            gauges.put(name(poolName, "max"), (Gauge<Long>) () -> pool.getUsage().getMax());
            gauges.put(name(poolName, "used"), (Gauge<Long>) () -> pool.getUsage().getUsed());
            gauges.put(name(poolName, "committed"), (Gauge<Long>) () -> pool.getUsage().getCommitted());
            gauges.put(name(poolName, "init"), (Gauge<Long>) () -> pool.getUsage().getInit());
        }

        return Collections.unmodifiableMap(gauges);
    }

    @Override
    public Map<String, Metric> getGarbageCollector() {
        final Map<String, Metric> gauges = new HashMap<>();
        for (final GarbageCollectorMXBean gc : garbageCollectors) {
            final String name = WHITESPACE.matcher(gc.getName()).replaceAll("-");

            gauges.put(name(name, "count"), (Gauge<Long>) gc::getCollectionCount);
            gauges.put(name(name, "time"), (Gauge<Long>) gc::getCollectionTime);
        }
        return Collections.unmodifiableMap(gauges);
    }

    @Override
    public Map<String, Metric> getThreadStates() {
        final Map<String, Metric> gauges = new HashMap<>();

        for (final Thread.State state : Thread.State.values()) {
            gauges.put(name(state.toString().toLowerCase(), "count"),
                    (Gauge<Object>) () -> getThreadCount(state));
        }

        gauges.put("count", (Gauge<Integer>) threads::getThreadCount);

        gauges.put("daemon.count", (Gauge<Integer>) threads::getDaemonThreadCount);

        gauges.put("deadlock.count", (Gauge<Integer>) () -> getDeadlockedThreads().size());

        gauges.put("deadlocks", (Gauge<Set<String>>) this::getDeadlockedThreads);

        return Collections.unmodifiableMap(gauges);
    }

    private int getThreadCount(Thread.State state) {
        final ThreadInfo[] allThreads = getThreadInfo();
        int count = 0;
        for (ThreadInfo info : allThreads) {
            if (info != null && info.getThreadState() == state) {
                count++;
            }
        }
        return count;
    }

    ThreadInfo[] getThreadInfo() {
        return threads.getThreadInfo(threads.getAllThreadIds(), STACK_TRACE_DEPTH);
    }

    /**
     * Returns a set of diagnostic stack traces for any deadlocked threads. If
     * no threads are deadlocked, returns an empty set.
     *
     * @return stack traces for deadlocked threads or an empty set
     */
    private Set<String> getDeadlockedThreads() {
        final long[] ids = threads.findDeadlockedThreads();
        if (ids != null) {
            final Set<String> deadlocks = new HashSet<>();
            for (ThreadInfo info : threads.getThreadInfo(ids, MAX_STACK_TRACE_DEPTH)) {
                final StringBuilder stackTrace = new StringBuilder();
                for (StackTraceElement element : info.getStackTrace()) {
                    stackTrace.append("\t at ")
                            .append(element.toString())
                            .append(String.format("%n"));
                }

                deadlocks.add(
                        String.format("%s locked on %s (owned by %s):%n%s",
                                info.getThreadName(),
                                info.getLockName(),
                                info.getLockOwnerName(),
                                stackTrace.toString()));
            }
            return Collections.unmodifiableSet(deadlocks);
        }
        return Collections.emptySet();
    }

    private static String name(String name, String... names) {
        final StringBuilder builder = new StringBuilder();
        append(builder, name);
        if (names != null) {
            for (String s : names) {
                append(builder, s);
            }
        }
        return builder.toString();
    }

    private static void append(StringBuilder builder, String part) {
        if (part != null && !part.isEmpty()) {
            if (builder.length() > 0) {
                builder.append('.');
            }
            builder.append(part);
        }
    }
}
