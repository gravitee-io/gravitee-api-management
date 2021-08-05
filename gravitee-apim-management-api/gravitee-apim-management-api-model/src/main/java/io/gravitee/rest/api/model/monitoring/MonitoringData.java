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
package io.gravitee.rest.api.model.monitoring;

import java.util.Objects;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 * @author GraviteeSource Team
 */
public class MonitoringData {

    private MonitoringCPU cpu;
    private MonitoringProcess process;
    private MonitoringJVM jvm;
    private MonitoringThread thread;
    private MonitoringGC gc;

    public MonitoringCPU getCpu() {
        return cpu;
    }

    public void setCpu(MonitoringCPU cpu) {
        this.cpu = cpu;
    }

    public MonitoringProcess getProcess() {
        return process;
    }

    public void setProcess(MonitoringProcess process) {
        this.process = process;
    }

    public MonitoringJVM getJvm() {
        return jvm;
    }

    public void setJvm(MonitoringJVM jvm) {
        this.jvm = jvm;
    }

    public MonitoringThread getThread() {
        return thread;
    }

    public void setThread(MonitoringThread thread) {
        this.thread = thread;
    }

    public MonitoringGC getGc() {
        return gc;
    }

    public void setGc(MonitoringGC gc) {
        this.gc = gc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MonitoringData)) return false;
        MonitoringData that = (MonitoringData) o;
        return (
            Objects.equals(cpu, that.cpu) &&
            Objects.equals(process, that.process) &&
            Objects.equals(jvm, that.jvm) &&
            Objects.equals(thread, that.thread) &&
            Objects.equals(gc, that.gc)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpu, process, jvm, thread, gc);
    }

    @Override
    public String toString() {
        return "MonitoringData{" + "cpu=" + cpu + ", process=" + process + ", jvm=" + jvm + ", thread=" + thread + ", gc=" + gc + '}';
    }
}
