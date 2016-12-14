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

import io.gravitee.reporter.api.monitor.ProcessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ProcessProbe {

    private final Logger logger = LoggerFactory.getLogger(ProcessProbe.class);

    private static final OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();

    private static final Method getMaxFileDescriptorCountField;
    private static final Method getOpenFileDescriptorCountField;

    static {
        getMaxFileDescriptorCountField = getUnixMethod("getMaxFileDescriptorCount");
        getOpenFileDescriptorCountField = getUnixMethod("getOpenFileDescriptorCount");
    }

    private static class ProcessProbeHolder {
        private final static ProcessProbe INSTANCE = new ProcessProbe();
    }

    public static ProcessProbe getInstance() {
        return ProcessProbeHolder.INSTANCE;
    }

    private ProcessProbe() {
    }

    /**
     * Returns the maximum number of file descriptors allowed on the system, or -1 if not supported.
     */
    public long getMaxFileDescriptorCount() {
        if (getMaxFileDescriptorCountField == null) {
            return -1;
        }
        try {
            return (Long) getMaxFileDescriptorCountField.invoke(osMxBean);
        } catch (Exception ex) {
            logger.debug("Unexpected exception", ex);
            return -1;
        }
    }

    /**
     * Returns the number of opened file descriptors associated with the current process, or -1 if not supported.
     */
    public long getOpenFileDescriptorCount() {
        if (getOpenFileDescriptorCountField == null) {
            return -1;
        }
        try {
            return (Long) getOpenFileDescriptorCountField.invoke(osMxBean);
        } catch (Exception ex) {
            logger.debug("Unexpected exception", ex);
            return -1;
        }
    }

    public ProcessInfo processInfo() {
        ProcessInfo info = new ProcessInfo();

        info.timestamp = System.currentTimeMillis();
        info.openFileDescriptors = getOpenFileDescriptorCount();
        info.maxFileDescriptors = getMaxFileDescriptorCount();

        return info;
    }

    /**
     * Returns a given method of the OperatingSystemMXBean,
     * or null if the method is not found or unavailable.
     */
    private static Method getMethod(String methodName) {
        try {
            return Class.forName("com.sun.management.OperatingSystemMXBean").getMethod(methodName);
        } catch (Throwable t) {
            // not available
            return null;
        }
    }

    /**
     * Returns a given method of the UnixOperatingSystemMXBean,
     * or null if the method is not found or unavailable.
     */
    private static Method getUnixMethod(String methodName) {
        try {
            return Class.forName("com.sun.management.UnixOperatingSystemMXBean").getMethod(methodName);
        } catch (Throwable t) {
            // not available
            return null;
        }
    }
}
