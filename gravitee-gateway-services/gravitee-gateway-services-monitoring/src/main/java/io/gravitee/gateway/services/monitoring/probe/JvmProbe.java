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
package io.gravitee.gateway.services.monitoring.probe;

import io.gravitee.reporter.api.monitor.JvmInfo;

import java.lang.management.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class JvmProbe {

    private final static RuntimeMXBean runtimeMXBean;
    private final static MemoryMXBean memoryMXBean;
    private final static ThreadMXBean threadMXBean;

    static {
        runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        memoryMXBean = ManagementFactory.getMemoryMXBean();
        threadMXBean = ManagementFactory.getThreadMXBean();
    }

    private static class JvmProbeHolder {
        private final static JvmProbe INSTANCE = new JvmProbe();
    }

    public static JvmProbe getInstance() {
        return JvmProbeHolder.INSTANCE;
    }

    private JvmProbe() {
    }

    public JvmInfo jvmInfo() {
        JvmInfo info = new JvmInfo(System.currentTimeMillis(), runtimeMXBean.getUptime());

        info.mem = new JvmInfo.Mem();

        MemoryUsage memUsage = memoryMXBean.getHeapMemoryUsage();
        info.mem.heapUsed = memUsage.getUsed() < 0 ? 0 : memUsage.getUsed();
        info.mem.heapCommitted = memUsage.getCommitted() < 0 ? 0 : memUsage.getCommitted();
        info.mem.heapMax = memUsage.getMax() < 0 ? 0 : memUsage.getMax();
        memUsage = memoryMXBean.getNonHeapMemoryUsage();
        info.mem.nonHeapUsed = memUsage.getUsed() < 0 ? 0 : memUsage.getUsed();
        info.mem.nonHeapCommitted = memUsage.getCommitted() < 0 ? 0 : memUsage.getCommitted();

        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        List<JvmInfo.MemoryPool> pools = new ArrayList<>();
        for (int i = 0; i < memoryPoolMXBeans.size(); i++) {
            try {
                MemoryPoolMXBean memoryPoolMXBean = memoryPoolMXBeans.get(i);
                MemoryUsage usage = memoryPoolMXBean.getUsage();
                MemoryUsage peakUsage = memoryPoolMXBean.getPeakUsage();
                String name = getByMemoryPoolName(memoryPoolMXBean.getName(), null);
                if (name == null) { // if we can't resolve it, its not interesting.... (Per Gen, Code Cache)
                    continue;
                }
                pools.add(new JvmInfo.MemoryPool(name,
                        usage.getUsed() < 0 ? 0 : usage.getUsed(),
                        usage.getMax() < 0 ? 0 : usage.getMax(),
                        peakUsage.getUsed() < 0 ? 0 : peakUsage.getUsed(),
                        peakUsage.getMax() < 0 ? 0 : peakUsage.getMax()
                ));
            } catch (OutOfMemoryError err) {
                throw err; // rethrow
            } catch (Throwable ex) {
                /* ignore some JVMs might barf here with:
                 * java.lang.InternalError: Memory Pool not found*/
            }
        }
        info.mem.pools = pools.toArray(new JvmInfo.MemoryPool[pools.size()]);

        info.threads = new JvmInfo.Threads();
        info.threads.count = threadMXBean.getThreadCount();
        info.threads.peakCount = threadMXBean.getPeakThreadCount();

        List<GarbageCollectorMXBean> gcMxBeans = ManagementFactory.getGarbageCollectorMXBeans();
        info.gc = new JvmInfo.GarbageCollectors();
        info.gc.collectors = new JvmInfo.GarbageCollector[gcMxBeans.size()];
        for (int i = 0; i < info.gc.collectors.length; i++) {
            GarbageCollectorMXBean gcMxBean = gcMxBeans.get(i);
            info.gc.collectors[i] = new JvmInfo.GarbageCollector();
            info.gc.collectors[i].name = getByGcName(gcMxBean.getName(), gcMxBean.getName());
            info.gc.collectors[i].collectionCount = gcMxBean.getCollectionCount();
            info.gc.collectors[i].collectionTime = gcMxBean.getCollectionTime();
        }

        return info;
    }

    private static final String YOUNG = "young";
    private static final String OLD = "old";
    private static final String SURVIVOR = "survivor";

    /**
     * Resolves the GC type by its memory pool name ({@link java.lang.management.MemoryPoolMXBean#getName()}.
     */
    static String getByMemoryPoolName(String poolName, String defaultName) {
        if ("Eden Space".equals(poolName) || "PS Eden Space".equals(poolName) || "Par Eden Space".equals(poolName) || "G1 Eden Space".equals(poolName)) {
            return YOUNG;
        }
        if ("Survivor Space".equals(poolName) || "PS Survivor Space".equals(poolName) || "Par Survivor Space".equals(poolName) || "G1 Survivor Space".equals(poolName)) {
            return SURVIVOR;
        }
        if ("Tenured Gen".equals(poolName) || "PS Old Gen".equals(poolName) || "CMS Old Gen".equals(poolName) || "G1 Old Gen".equals(poolName)) {
            return OLD;
        }
        return defaultName;
    }

    static String getByGcName(String gcName, String defaultName) {
        if ("Copy".equals(gcName) || "PS Scavenge".equals(gcName) || "ParNew".equals(gcName) || "G1 Young Generation".equals(gcName)) {
            return YOUNG;
        }
        if ("MarkSweepCompact".equals(gcName) || "PS MarkSweep".equals(gcName) || "ConcurrentMarkSweep".equals(gcName) || "G1 Old Generation".equals(gcName)) {
            return OLD;
        }
        return defaultName;
    }

}
