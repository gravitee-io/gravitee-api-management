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
package io.gravitee.gateway.jupiter.debug.policy.steps;

import com.google.common.base.Stopwatch;
import io.gravitee.definition.model.debug.DebugStepError;
import io.gravitee.definition.model.debug.DebugStepStatus;
import io.gravitee.gateway.debug.reactor.handler.context.AttributeHelper;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class PolicyStep<T> {

    public static final String DIFF_KEY_HEADERS = "headers";
    public static final String DIFF_KEY_PARAMETERS = "parameters";
    public static final String DIFF_KEY_PATH = "path";
    public static final String DIFF_KEY_PATH_PARAMETERS = "pathParameters";
    public static final String DIFF_KEY_METHOD = "method";
    public static final String DIFF_KEY_CONTEXT_PATH = "contextPath";
    public static final String DIFF_KEY_ATTRIBUTES = "attributes";
    public static final String DIFF_KEY_BODY_BUFFER = "bodyBuffer";
    public static final String DIFF_KEY_BODY = "body";
    public static final String DIFF_KEY_STATUS_CODE = "statusCode";
    public static final String DIFF_KEY_REASON = "reason";

    protected final String id;
    protected final String policyId;
    protected final ExecutionPhase executionPhase;
    protected final String flowPhase;
    protected final Stopwatch stopwatch;
    protected DebugStepStatus status;
    protected String condition;
    protected DebugStepError error;
    protected boolean ended = false;
    private PolicyStepState inputState;
    private Map<String, Object> diffMap;

    protected PolicyStep(final String policyId, final ExecutionPhase executionPhase, final String flowPhase) {
        this.id = UUID.randomUUID().toString();
        this.policyId = policyId;
        this.executionPhase = executionPhase;
        this.flowPhase = flowPhase;
        this.stopwatch = Stopwatch.createUnstarted();
    }

    public Completable pre(final T source, final Map<String, Object> attributes) {
        return saveInputState(source, AttributeHelper.filterAndSerializeAttributes(attributes))
            .doOnSuccess(
                policyStepState -> {
                    this.inputState = policyStepState;
                    starTimeWatch();
                }
            )
            .ignoreElement();
    }

    protected abstract Single<PolicyStepState> saveInputState(final T source, final Map<String, Serializable> inputAttributes);

    public Completable post(final T source, final Map<String, Object> attributes) {
        return Completable
            .fromRunnable(this::stopTimeWatch)
            .andThen(computeDiff(source, this.inputState, AttributeHelper.filterAndSerializeAttributes(attributes)))
            .doOnSuccess(
                computedDiffMap -> {
                    this.diffMap = computedDiffMap;
                    this.inputState = null;
                    this.status = this.status == null ? DebugStepStatus.COMPLETED : this.status;
                }
            )
            .ignoreElement();
    }

    protected abstract Single<Map<String, Object>> computeDiff(
        final T source,
        final PolicyStepState inputState,
        final Map<String, Serializable> outputAttributes
    );

    public Map<String, Object> getDiff() {
        if (diffMap == null) {
            diffMap = new HashMap<>();
        }
        return diffMap;
    }

    protected PolicyStepState getInputState() {
        return inputState;
    }

    protected void starTimeWatch() {
        if (!stopwatch.isRunning()) {
            this.stopwatch.start();
        }
    }

    protected void stopTimeWatch() {
        if (stopwatch.isRunning()) {
            this.stopwatch.stop();
        }
    }

    public Completable error(Throwable ex) {
        return Completable.fromRunnable(
            () -> {
                stopTimeWatch();
                this.status = DebugStepStatus.ERROR;
                this.error = new DebugStepError();
                this.error.setMessage(ex.getMessage());
            }
        );
    }

    public Completable error(ExecutionFailure executionFailure) {
        return Completable.fromRunnable(
            () -> {
                stopTimeWatch();
                this.status = DebugStepStatus.ERROR;
                this.error = new DebugStepError();
                this.error.setMessage(executionFailure.message());
                this.error.setKey(executionFailure.key());
                this.error.setStatus(executionFailure.statusCode());
                this.error.setContentType(executionFailure.contentType());
            }
        );
    }

    public void onConditionFilter(String condition, boolean isConditionTruthy) {
        this.condition = condition;
        if (!isConditionTruthy) {
            this.status = DebugStepStatus.SKIPPED;
        }
    }

    public String getId() {
        return id;
    }

    public String getPolicyId() {
        return policyId;
    }

    public ExecutionPhase getExecutionPhase() {
        return executionPhase;
    }

    public String getFlowPhase() {
        return flowPhase;
    }

    public Duration elapsedTime() {
        return this.stopwatch.elapsed();
    }

    public DebugStepStatus getStatus() {
        return status;
    }

    public String getCondition() {
        return condition;
    }

    public DebugStepError getError() {
        return error;
    }

    public boolean isEnded() {
        return ended;
    }

    public void end() {
        this.ended = true;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolicyStep<?> that = (PolicyStep<?>) o;
        return (
            ended == that.ended &&
            Objects.equals(id, that.id) &&
            Objects.equals(policyId, that.policyId) &&
            executionPhase == that.executionPhase &&
            Objects.equals(flowPhase, that.flowPhase) &&
            Objects.equals(stopwatch, that.stopwatch) &&
            status == that.status &&
            Objects.equals(condition, that.condition) &&
            Objects.equals(error, that.error) &&
            Objects.equals(inputState, that.inputState) &&
            Objects.equals(diffMap, that.diffMap)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, policyId, executionPhase, flowPhase, stopwatch, status, condition, error, ended, inputState, diffMap);
    }

    @Override
    public String toString() {
        return (
            getClass().getSimpleName() +
            "{" +
            "id='" +
            id +
            '\'' +
            "policyId='" +
            policyId +
            '\'' +
            ", executionPhase=" +
            executionPhase +
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
