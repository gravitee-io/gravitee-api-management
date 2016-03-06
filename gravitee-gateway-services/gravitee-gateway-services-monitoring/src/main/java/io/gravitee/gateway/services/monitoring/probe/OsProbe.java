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

import io.gravitee.gateway.services.monitoring.Constants;
import io.gravitee.reporter.api.monitor.OsInfo;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class OsProbe {

    private static final OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();

    private static final Method getFreePhysicalMemorySize;
    private static final Method getTotalPhysicalMemorySize;
    private static final Method getFreeSwapSpaceSize;
    private static final Method getTotalSwapSpaceSize;
    private static final Method getSystemLoadAverage;
    private static final Method getSystemCpuLoad;

    static {
        getFreePhysicalMemorySize = getMethod("getFreePhysicalMemorySize");
        getTotalPhysicalMemorySize = getMethod("getTotalPhysicalMemorySize");
        getFreeSwapSpaceSize = getMethod("getFreeSwapSpaceSize");
        getTotalSwapSpaceSize = getMethod("getTotalSwapSpaceSize");
        getSystemLoadAverage = getMethod("getSystemLoadAverage");
        getSystemCpuLoad = getMethod("getSystemCpuLoad");
    }

    private static class OsProbeHolder {
        private final static OsProbe INSTANCE = new OsProbe();
    }

    public static OsProbe getInstance() {
        return OsProbeHolder.INSTANCE;
    }

    private OsProbe() {
    }

    /**
     * Returns the amount of free physical memory in bytes.
     */
    public long getFreePhysicalMemorySize() {
        if (getFreePhysicalMemorySize == null) {
            return -1;
        }
        try {
            return (long) getFreePhysicalMemorySize.invoke(osMxBean);
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Returns the total amount of physical memory in bytes.
     */
    public long getTotalPhysicalMemorySize() {
        if (getTotalPhysicalMemorySize == null) {
            return -1;
        }
        try {
            return (long) getTotalPhysicalMemorySize.invoke(osMxBean);
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Returns the amount of free swap space in bytes.
     */
    public long getFreeSwapSpaceSize() {
        if (getFreeSwapSpaceSize == null) {
            return -1;
        }
        try {
            return (long) getFreeSwapSpaceSize.invoke(osMxBean);
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Returns the total amount of swap space in bytes.
     */
    public long getTotalSwapSpaceSize() {
        if (getTotalSwapSpaceSize == null) {
            return -1;
        }
        try {
            return (long) getTotalSwapSpaceSize.invoke(osMxBean);
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * Returns the system load averages
     */
    public double[] getSystemLoadAverage() {
        if (Constants.LINUX || Constants.FREE_BSD) {
            final String procLoadAvg = Constants.LINUX ? "/proc/loadavg" : "/compat/linux/proc/loadavg";
            double[] loadAverage = readProcLoadavg(procLoadAvg);
            if (loadAverage != null) {
                return loadAverage;
            }
            // fallback
        }
        if (Constants.WINDOWS) {
            return null;
        }
        if (getSystemLoadAverage == null) {
            return null;
        }
        try {
            double oneMinuteLoadAverage = (double) getSystemLoadAverage.invoke(osMxBean);
            return new double[] { oneMinuteLoadAverage >= 0 ? oneMinuteLoadAverage : -1, -1, -1 };
        } catch (Throwable t) {
            return null;
        }
    }


    private static double[] readProcLoadavg(String procLoadavg) {
        try {
            List<String> lines = Files.readAllLines(FileSystems.getDefault().getPath(procLoadavg));
            if (!lines.isEmpty()) {
                String[] fields = lines.get(0).split("\\s+");
                return new double[] { Double.parseDouble(fields[0]), Double.parseDouble(fields[1]), Double.parseDouble(fields[2]) };
            }
        } catch (IOException e) {
        }
        return null;
    }

    public short getSystemCpuPercent() {
        if (getSystemCpuLoad != null) {
            try {
                double load = (double) getSystemCpuLoad.invoke(osMxBean);
                if (load >= 0) {
                    return (short) (load * 100);
                }
            } catch (Throwable t) {
                return -1;
            }
        }
        return -1;
    }

    public OsInfo osInfo() {
        OsInfo info = new OsInfo();
        info.timestamp = System.currentTimeMillis();
        info.cpu = new OsInfo.Cpu();
        info.cpu.percent = getSystemCpuPercent();
        info.cpu.loadAverage = getSystemLoadAverage();

        info.mem = new OsInfo.Mem();
        info.mem.total = getTotalPhysicalMemorySize();
        info.mem.free = getFreePhysicalMemorySize();

        info.swap = new OsInfo.Swap();
        info.swap.total = getTotalSwapSpaceSize();
        info.swap.free = getFreeSwapSpaceSize();

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
}
