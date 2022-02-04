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
package io.gravitee.gateway.debug.reactor.handler.context.steps;

import com.google.common.base.Stopwatch;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.debug.reactor.handler.context.DebugScope;
import io.gravitee.gateway.policy.StreamType;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class DebugStep<T> {

    protected final String policyId;
    protected final StreamType streamType;
    protected final String uuid;
    protected final DebugScope debugScope;
    protected final Map<String, Object> diffMap = new HashMap<>();
    protected final Stopwatch stopwatch;

    protected DebugStepContent policyInputContent;

    public DebugStep(String policyId, StreamType streamType, String uuid, DebugScope debugScope) {
        this.policyId = policyId;
        this.streamType = streamType;
        this.uuid = uuid;
        this.debugScope = debugScope;
        this.stopwatch = Stopwatch.createUnstarted();
        this.policyInputContent = new DebugStepContent();
    }

    public void before(T source, Map<String, Object> attributes) {
        snapshotInputData(source, attributes);
        this.start();
    }

    protected abstract void snapshotInputData(T source, Map<String, Object> attributes);

    public void after(T source, Map<String, Object> attributes, Buffer inputBuffer, Buffer outputBuffer) {
        this.stop();
        generateDiffMap(source, attributes, inputBuffer, outputBuffer);
        policyInputContent = null;
    }

    protected abstract void generateDiffMap(T source, Map<String, Object> attributes, Buffer inputBuffer, Buffer outputBuffer);

    public Map<String, Object> getDebugDiffContent() {
        return diffMap;
    }

    public void start() {
        if (!stopwatch.isRunning()) {
            this.stopwatch.start();
        }
    }

    public void stop() {
        if (stopwatch.isRunning()) {
            this.stopwatch.stop();
        }
    }

    public String getPolicyId() {
        return policyId;
    }

    public StreamType getStreamType() {
        return streamType;
    }

    public Duration elapsedTime() {
        return this.stopwatch.elapsed();
    }

    public String getUuid() {
        return uuid;
    }

    public DebugScope getDebugScope() {
        return debugScope;
    }

    @Override
    public String toString() {
        return (
            getClass().getSimpleName() +
            "{" +
            "policyId='" +
            policyId +
            '\'' +
            ", streamType=" +
            streamType +
            ", stopwatch=" +
            stopwatch.elapsed(TimeUnit.NANOSECONDS) +
            " ns" +
            ", diffMap='" +
            diffMap +
            '\'' +
            '}'
        );
    }
}
