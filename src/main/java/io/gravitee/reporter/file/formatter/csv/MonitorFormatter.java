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
package io.gravitee.reporter.file.formatter.csv;

import io.gravitee.node.api.monitor.JvmInfo;
import io.gravitee.node.api.monitor.Monitor;
import io.vertx.core.buffer.Buffer;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MonitorFormatter extends SingleValueFormatter<Monitor> {

    @Override
    public Buffer format0(Monitor monitor) {
        final Buffer buffer = Buffer.buffer();

        // Write info about OS
        appendShort(buffer, monitor.getOs().cpu.percent);
        //appendString(buffer, monitor.getOs().cpu.getLoadAverage());

        // Write info about process
        appendLong(buffer, monitor.getProcess().timestamp);
        appendLong(buffer, monitor.getProcess().openFileDescriptors);
        appendLong(buffer, monitor.getProcess().maxFileDescriptors);

        // Write info about JVM
        appendLong(buffer, monitor.getJvm().timestamp);
        appendLong(buffer, monitor.getJvm().uptime);

        appendLong(buffer, monitor.getJvm().mem.heapCommitted);
        appendLong(buffer, monitor.getJvm().mem.nonHeapCommitted);
        appendLong(buffer, monitor.getJvm().mem.heapUsed);
        appendLong(buffer, monitor.getJvm().mem.nonHeapUsed);
        appendLong(buffer, monitor.getJvm().mem.heapMax);

        appendInt(buffer, monitor.getJvm().threads.getCount());
        appendInt(buffer, monitor.getJvm().threads.getPeakCount());

        for (JvmInfo.GarbageCollector collector : monitor.getJvm().gc.collectors) {
            appendString(buffer, collector.getName());
            appendLong(buffer, collector.getCollectionCount());
            appendLong(buffer, collector.getCollectionTime());
        }

        return buffer;
    }
}
