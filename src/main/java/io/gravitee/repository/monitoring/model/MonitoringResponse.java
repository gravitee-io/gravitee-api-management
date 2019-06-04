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
package io.gravitee.repository.monitoring.model;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MonitoringResponse {

    private String gatewayId;
    private String hostname;
    private ZonedDateTime timestamp;

    // JVM

    private long jvmTimestamp;

    private long jvmHeapCommittedInBytes;
    private int jvmHeapUsedPercent;
    private long jvmHeapMaxInBytes;
    private long jvmHeapUsedInBytes;
    private long jvmNonHeapUsedInBytes;
    private long jvmNonHeapCommittedInBytes;

    private long jvmMemPoolYoungUsedInBytes;
    private long jvmMemPoolYoungPeakUsedInBytes;
    private long jvmMemPoolYoungMaxInBytes;
    private long jvmMemPoolYoungPeakMaxInBytes;

    private long jvmMemPoolOldUsedInBytes;
    private long jvmMemPoolOldPeakUsedInBytes;
    private long jvmMemPoolOldMaxInBytes;
    private long jvmMemPoolOldPeakMaxInBytes;

    private long jvmMemPoolSurvivorUsedInBytes;
    private long jvmMemPoolSurvivorPeakUsedInBytes;
    private long jvmMemPoolSurvivorMaxInBytes;
    private long jvmMemPoolSurvivorPeakMaxInBytes;

    private int jvmThreadCount;
    private int jvmThreadPeakCount;

    private long jvmUptimeInMillis;

    private int jvmGCCollectorsYoungCollectionCount;
    private long jvmGCCollectorsYoungCollectionTimeInMillis;

    private int jvmGCCollectorsOldCollectionCount;
    private long jvmGCCollectorsOldCollectionTimeInMillis;

    // Process
    private int processOpenFileDescriptors;
    private int processMaxFileDescriptors;
    private int processCPUPercent;

    // OS

    private long osMemTotalInBytes;
    private long osMemFreeInBytes;
    private long osMemUsedInBytes;
    private int osMemFreePercent;
    private int osMemUsedPercent;

    private Map<String, ? super Number> osCPULoadAverage;
    private int osCPUPercent;

    public String getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public long getJvmTimestamp() {
        return jvmTimestamp;
    }

    public void setJvmTimestamp(long jvmTimestamp) {
        this.jvmTimestamp = jvmTimestamp;
    }

    public long getJvmHeapCommittedInBytes() {
        return jvmHeapCommittedInBytes;
    }

    public void setJvmHeapCommittedInBytes(long jvmHeapCommittedInBytes) {
        this.jvmHeapCommittedInBytes = jvmHeapCommittedInBytes;
    }

    public int getJvmHeapUsedPercent() {
        return jvmHeapUsedPercent;
    }

    public void setJvmHeapUsedPercent(int jvmHeapUsedPercent) {
        this.jvmHeapUsedPercent = jvmHeapUsedPercent;
    }

    public long getJvmHeapMaxInBytes() {
        return jvmHeapMaxInBytes;
    }

    public void setJvmHeapMaxInBytes(long jvmHeapMaxInBytes) {
        this.jvmHeapMaxInBytes = jvmHeapMaxInBytes;
    }

    public long getJvmHeapUsedInBytes() {
        return jvmHeapUsedInBytes;
    }

    public void setJvmHeapUsedInBytes(long jvmHeapUsedInBytes) {
        this.jvmHeapUsedInBytes = jvmHeapUsedInBytes;
    }

    public long getJvmNonHeapUsedInBytes() {
        return jvmNonHeapUsedInBytes;
    }

    public void setJvmNonHeapUsedInBytes(long jvmNonHeapUsedInBytes) {
        this.jvmNonHeapUsedInBytes = jvmNonHeapUsedInBytes;
    }

    public long getJvmNonHeapCommittedInBytes() {
        return jvmNonHeapCommittedInBytes;
    }

    public void setJvmNonHeapCommittedInBytes(long jvmNonHeapCommittedInBytes) {
        this.jvmNonHeapCommittedInBytes = jvmNonHeapCommittedInBytes;
    }

    public long getJvmMemPoolYoungUsedInBytes() {
        return jvmMemPoolYoungUsedInBytes;
    }

    public void setJvmMemPoolYoungUsedInBytes(long jvmMemPoolYoungUsedInBytes) {
        this.jvmMemPoolYoungUsedInBytes = jvmMemPoolYoungUsedInBytes;
    }

    public long getJvmMemPoolYoungPeakUsedInBytes() {
        return jvmMemPoolYoungPeakUsedInBytes;
    }

    public void setJvmMemPoolYoungPeakUsedInBytes(long jvmMemPoolYoungPeakUsedInBytes) {
        this.jvmMemPoolYoungPeakUsedInBytes = jvmMemPoolYoungPeakUsedInBytes;
    }

    public long getJvmMemPoolYoungMaxInBytes() {
        return jvmMemPoolYoungMaxInBytes;
    }

    public void setJvmMemPoolYoungMaxInBytes(long jvmMemPoolYoungMaxInBytes) {
        this.jvmMemPoolYoungMaxInBytes = jvmMemPoolYoungMaxInBytes;
    }

    public long getJvmMemPoolYoungPeakMaxInBytes() {
        return jvmMemPoolYoungPeakMaxInBytes;
    }

    public void setJvmMemPoolYoungPeakMaxInBytes(long jvmMemPoolYoungPeakMaxInBytes) {
        this.jvmMemPoolYoungPeakMaxInBytes = jvmMemPoolYoungPeakMaxInBytes;
    }

    public long getJvmMemPoolOldUsedInBytes() {
        return jvmMemPoolOldUsedInBytes;
    }

    public void setJvmMemPoolOldUsedInBytes(long jvmMemPoolOldUsedInBytes) {
        this.jvmMemPoolOldUsedInBytes = jvmMemPoolOldUsedInBytes;
    }

    public long getJvmMemPoolOldPeakUsedInBytes() {
        return jvmMemPoolOldPeakUsedInBytes;
    }

    public void setJvmMemPoolOldPeakUsedInBytes(long jvmMemPoolOldPeakUsedInBytes) {
        this.jvmMemPoolOldPeakUsedInBytes = jvmMemPoolOldPeakUsedInBytes;
    }

    public long getJvmMemPoolOldMaxInBytes() {
        return jvmMemPoolOldMaxInBytes;
    }

    public void setJvmMemPoolOldMaxInBytes(long jvmMemPoolOldMaxInBytes) {
        this.jvmMemPoolOldMaxInBytes = jvmMemPoolOldMaxInBytes;
    }

    public long getJvmMemPoolOldPeakMaxInBytes() {
        return jvmMemPoolOldPeakMaxInBytes;
    }

    public void setJvmMemPoolOldPeakMaxInBytes(long jvmMemPoolOldPeakMaxInBytes) {
        this.jvmMemPoolOldPeakMaxInBytes = jvmMemPoolOldPeakMaxInBytes;
    }

    public long getJvmMemPoolSurvivorUsedInBytes() {
        return jvmMemPoolSurvivorUsedInBytes;
    }

    public void setJvmMemPoolSurvivorUsedInBytes(long jvmMemPoolSurvivorUsedInBytes) {
        this.jvmMemPoolSurvivorUsedInBytes = jvmMemPoolSurvivorUsedInBytes;
    }

    public long getJvmMemPoolSurvivorPeakUsedInBytes() {
        return jvmMemPoolSurvivorPeakUsedInBytes;
    }

    public void setJvmMemPoolSurvivorPeakUsedInBytes(long jvmMemPoolSurvivorPeakUsedInBytes) {
        this.jvmMemPoolSurvivorPeakUsedInBytes = jvmMemPoolSurvivorPeakUsedInBytes;
    }

    public long getJvmMemPoolSurvivorMaxInBytes() {
        return jvmMemPoolSurvivorMaxInBytes;
    }

    public void setJvmMemPoolSurvivorMaxInBytes(long jvmMemPoolSurvivorMaxInBytes) {
        this.jvmMemPoolSurvivorMaxInBytes = jvmMemPoolSurvivorMaxInBytes;
    }

    public long getJvmMemPoolSurvivorPeakMaxInBytes() {
        return jvmMemPoolSurvivorPeakMaxInBytes;
    }

    public void setJvmMemPoolSurvivorPeakMaxInBytes(long jvmMemPoolSurvivorPeakMaxInBytes) {
        this.jvmMemPoolSurvivorPeakMaxInBytes = jvmMemPoolSurvivorPeakMaxInBytes;
    }

    public int getJvmThreadCount() {
        return jvmThreadCount;
    }

    public void setJvmThreadCount(int jvmThreadCount) {
        this.jvmThreadCount = jvmThreadCount;
    }

    public int getJvmThreadPeakCount() {
        return jvmThreadPeakCount;
    }

    public void setJvmThreadPeakCount(int jvmThreadPeakCount) {
        this.jvmThreadPeakCount = jvmThreadPeakCount;
    }

    public long getJvmUptimeInMillis() {
        return jvmUptimeInMillis;
    }

    public void setJvmUptimeInMillis(long jvmUptimeInMillis) {
        this.jvmUptimeInMillis = jvmUptimeInMillis;
    }

    public int getJvmGCCollectorsYoungCollectionCount() {
        return jvmGCCollectorsYoungCollectionCount;
    }

    public void setJvmGCCollectorsYoungCollectionCount(int jvmGCCollectorsYoungCollectionCount) {
        this.jvmGCCollectorsYoungCollectionCount = jvmGCCollectorsYoungCollectionCount;
    }

    public long getJvmGCCollectorsYoungCollectionTimeInMillis() {
        return jvmGCCollectorsYoungCollectionTimeInMillis;
    }

    public void setJvmGCCollectorsYoungCollectionTimeInMillis(long jvmGCCollectorsYoungCollectionTimeInMillis) {
        this.jvmGCCollectorsYoungCollectionTimeInMillis = jvmGCCollectorsYoungCollectionTimeInMillis;
    }

    public int getJvmGCCollectorsOldCollectionCount() {
        return jvmGCCollectorsOldCollectionCount;
    }

    public void setJvmGCCollectorsOldCollectionCount(int jvmGCCollectorsOldCollectionCount) {
        this.jvmGCCollectorsOldCollectionCount = jvmGCCollectorsOldCollectionCount;
    }

    public long getJvmGCCollectorsOldCollectionTimeInMillis() {
        return jvmGCCollectorsOldCollectionTimeInMillis;
    }

    public void setJvmGCCollectorsOldCollectionTimeInMillis(long jvmGCCollectorsOldCollectionTimeInMillis) {
        this.jvmGCCollectorsOldCollectionTimeInMillis = jvmGCCollectorsOldCollectionTimeInMillis;
    }

    public int getProcessOpenFileDescriptors() {
        return processOpenFileDescriptors;
    }

    public void setProcessOpenFileDescriptors(int processOpenFileDescriptors) {
        this.processOpenFileDescriptors = processOpenFileDescriptors;
    }

    public int getProcessMaxFileDescriptors() {
        return processMaxFileDescriptors;
    }

    public void setProcessMaxFileDescriptors(int processMaxFileDescriptors) {
        this.processMaxFileDescriptors = processMaxFileDescriptors;
    }

    public long getOsMemTotalInBytes() {
        return osMemTotalInBytes;
    }

    public void setOsMemTotalInBytes(long osMemTotalInBytes) {
        this.osMemTotalInBytes = osMemTotalInBytes;
    }

    public long getOsMemFreeInBytes() {
        return osMemFreeInBytes;
    }

    public void setOsMemFreeInBytes(long osMemFreeInBytes) {
        this.osMemFreeInBytes = osMemFreeInBytes;
    }

    public long getOsMemUsedInBytes() {
        return osMemUsedInBytes;
    }

    public void setOsMemUsedInBytes(long osMemUsedInBytes) {
        this.osMemUsedInBytes = osMemUsedInBytes;
    }

    public int getOsMemFreePercent() {
        return osMemFreePercent;
    }

    public void setOsMemFreePercent(int osMemFreePercent) {
        this.osMemFreePercent = osMemFreePercent;
    }

    public int getOsMemUsedPercent() {
        return osMemUsedPercent;
    }

    public void setOsMemUsedPercent(int osMemUsedPercent) {
        this.osMemUsedPercent = osMemUsedPercent;
    }

    public Map<String, ? super Number> getOsCPULoadAverage() {
        return osCPULoadAverage;
    }

    public void setOsCPULoadAverage(Map<String, ? super Number> osCPULoadAverage) {
        this.osCPULoadAverage = osCPULoadAverage;
    }

    public int getOsCPUPercent() {
        return osCPUPercent;
    }

    public void setOsCPUPercent(int osCPUPercent) {
        this.osCPUPercent = osCPUPercent;
    }

    public int getProcessCPUPercent() {
        return processCPUPercent;
    }

    public void setProcessCPUPercent(int processCPUPercent) {
        this.processCPUPercent = processCPUPercent;
    }
}
