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
package io.gravitee.gateway.report.impl;

import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.monitor.Monitor;
import io.gravitee.node.monitoring.monitor.NodeMonitorService;
import io.gravitee.reporter.api.monitor.JvmInfo;
import io.gravitee.reporter.api.monitor.OsInfo;
import io.gravitee.reporter.api.monitor.ProcessInfo;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeMonitoringReporterService extends AbstractService<NodeMonitoringReporterService> {

    @Autowired
    private Vertx vertx;

    @Autowired
    private ReporterService reporterService;

    @Override
    protected String name() {
        return "Node Monitoring service";
    }

    @Override
    protected void doStart() throws Exception {
        vertx.eventBus().localConsumer(NodeMonitorService.GIO_NODE_MONITOR_BUS, this::handleMonitorMessage);
    }

    private void handleMonitorMessage(Message<Monitor> message) {
        final Monitor nodeMonitor = message.body();

        // Convert node monitoring to a reporter monitoring
        reporterService.report(
            io.gravitee.reporter.api.monitor.Monitor
                .on(nodeMonitor.getNodeId())
                .at(nodeMonitor.getTimestamp())
                .os(map(nodeMonitor.getOs()))
                .jvm(map(nodeMonitor.getJvm()))
                .process(map(nodeMonitor.getProcess()))
                .build()
        );
    }

    private JvmInfo map(io.gravitee.node.api.monitor.JvmInfo jvmInfo) {
        final JvmInfo info = new JvmInfo(jvmInfo.timestamp, jvmInfo.uptime);

        info.mem = new JvmInfo.Mem();
        info.mem.heapCommitted = jvmInfo.mem.heapCommitted;
        info.mem.heapMax = jvmInfo.mem.heapMax;
        info.mem.heapUsed = jvmInfo.mem.heapUsed;
        info.mem.nonHeapCommitted = jvmInfo.mem.nonHeapCommitted;
        info.mem.nonHeapUsed = jvmInfo.mem.nonHeapUsed;

        if (jvmInfo.mem.pools != null) {
            info.mem.pools = new JvmInfo.MemoryPool[jvmInfo.mem.pools.length];
            for (int i = 0; i < jvmInfo.mem.pools.length; i++) {
                io.gravitee.node.api.monitor.JvmInfo.MemoryPool nodePool = jvmInfo.mem.pools[i];
                info.mem.pools[i] = new JvmInfo.MemoryPool(nodePool.name, nodePool.used, nodePool.max, nodePool.peakUsed, nodePool.peakMax);
            }
        }

        if (jvmInfo.gc != null && jvmInfo.gc.collectors != null) {
            info.gc = new JvmInfo.GarbageCollectors();

            info.gc.collectors = new JvmInfo.GarbageCollector[jvmInfo.gc.collectors.length];
            for (int i = 0; i < jvmInfo.gc.collectors.length; i++) {
                io.gravitee.node.api.monitor.JvmInfo.GarbageCollector nodeCollector = jvmInfo.gc.collectors[i];
                final JvmInfo.GarbageCollector collector = new JvmInfo.GarbageCollector();
                collector.name = nodeCollector.name;
                collector.collectionCount = nodeCollector.collectionCount;
                collector.collectionTime = nodeCollector.collectionTime;
                info.gc.collectors[i] = collector;
            }
        }

        info.threads = new JvmInfo.Threads();
        info.threads.count = jvmInfo.threads.count;
        info.threads.peakCount = jvmInfo.threads.peakCount;

        return info;
    }

    private ProcessInfo map(io.gravitee.node.api.monitor.ProcessInfo processInfo) {
        final ProcessInfo info = new ProcessInfo();

        info.timestamp = processInfo.timestamp;
        info.openFileDescriptors = processInfo.openFileDescriptors;
        info.maxFileDescriptors = processInfo.maxFileDescriptors;

        info.cpu = new ProcessInfo.Cpu();
        info.cpu.total = processInfo.cpu.total;
        info.cpu.percent = processInfo.cpu.percent;

        info.mem = new ProcessInfo.Mem();
        info.mem.totalVirtual = processInfo.mem.totalVirtual;

        return info;
    }

    private OsInfo map(io.gravitee.node.api.monitor.OsInfo osInfo) {
        final OsInfo info = new OsInfo();

        info.timestamp = osInfo.timestamp;

        info.cpu = new OsInfo.Cpu();
        info.cpu.loadAverage = osInfo.cpu.loadAverage;
        info.cpu.percent = osInfo.cpu.percent;

        info.mem = new OsInfo.Mem();
        info.mem.free = osInfo.mem.free;
        info.mem.total = osInfo.mem.total;

        info.swap = new OsInfo.Swap();
        info.swap.free = osInfo.swap.free;
        info.swap.total = osInfo.swap.total;

        return info;
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }
}
