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
package io.gravitee.gateway.jupiter.debug.reactor.context;

import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.debug.core.invoker.InvokerResponse;
import io.gravitee.gateway.debug.reactor.handler.context.AttributeHelper;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.gravitee.gateway.jupiter.debug.policy.steps.PolicyRequestStep;
import io.gravitee.gateway.jupiter.debug.policy.steps.PolicyResponseStep;
import io.gravitee.gateway.jupiter.debug.policy.steps.PolicyStep;
import io.gravitee.gateway.jupiter.debug.policy.steps.PolicyStepFactory;
import io.gravitee.gateway.jupiter.reactor.handler.context.DefaultRequestExecutionContext;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugRequestExecutionContext extends DefaultRequestExecutionContext {

    private final LinkedList<PolicyStep<?>> policySteps = new LinkedList<>();
    private final Map<String, Serializable> initialAttributes;
    private final InvokerResponse invokerResponse = new InvokerResponse();
    private final HttpHeaders initialHeaders;

    public DebugRequestExecutionContext(final MutableRequest request, final MutableResponse response) {
        super(request, response);
        this.initialAttributes = AttributeHelper.filterAndSerializeAttributes(getAttributes());
        this.initialHeaders = HttpHeaders.create(request().headers());
    }

    public Completable prePolicyExecution(final String id, final ExecutionPhase executionPhase) {
        return Maybe
            .fromCallable(
                () -> {
                    String flowStage = getInternalAttribute(ATTR_INTERNAL_FLOW_STAGE);
                    PolicyStep<?> policyStep = PolicyStepFactory.createPolicyStep(id, executionPhase, flowStage);
                    if (policyStep != null) {
                        policySteps.add(policyStep);
                        return policyStep;
                    }
                    return null;
                }
            )
            .flatMapCompletable(
                currentPolicyStep -> {
                    if (ExecutionPhase.REQUEST == currentPolicyStep.getExecutionPhase()) {
                        return ((PolicyRequestStep) currentPolicyStep).pre(request(), getAttributes());
                    } else if (ExecutionPhase.RESPONSE == currentPolicyStep.getExecutionPhase()) {
                        return ((PolicyResponseStep) currentPolicyStep).pre(response(), getAttributes());
                    }
                    return Completable.complete();
                }
            );
    }

    public Completable postPolicyExecution() {
        return Completable
            .defer(
                () -> {
                    PolicyStep<?> currentPolicyStep = getCurrentDebugStep();
                    if (currentPolicyStep != null && !currentPolicyStep.isEnded()) {
                        if (ExecutionPhase.REQUEST == currentPolicyStep.getExecutionPhase()) {
                            return ((PolicyRequestStep) currentPolicyStep).post(request(), getAttributes());
                        } else if (ExecutionPhase.RESPONSE == currentPolicyStep.getExecutionPhase()) {
                            return ((PolicyResponseStep) currentPolicyStep).post(response(), getAttributes());
                        }
                    }
                    return Completable.complete();
                }
            )
            .doOnComplete(
                () -> {
                    PolicyStep<?> currentPolicyStep = getCurrentDebugStep();
                    if (currentPolicyStep != null) {
                        currentPolicyStep.end();
                    }
                }
            );
    }

    public Completable postPolicyExecution(final Throwable throwable) {
        return Completable.defer(
            () -> {
                PolicyStep<?> currentPolicyStep = getCurrentDebugStep();
                if (currentPolicyStep != null && !currentPolicyStep.isEnded()) {
                    return currentPolicyStep.error(throwable).doOnComplete(currentPolicyStep::end);
                }
                return Completable.complete();
            }
        );
    }

    public Completable postPolicyExecution(final ExecutionFailure executionFailure) {
        return Completable.defer(
            () -> {
                PolicyStep<?> currentPolicyStep = getCurrentDebugStep();
                if (currentPolicyStep != null && !currentPolicyStep.isEnded()) {
                    return currentPolicyStep.error(executionFailure).doOnComplete(currentPolicyStep::end);
                }
                return Completable.complete();
            }
        );
    }

    public Map<String, Serializable> getInitialAttributes() {
        return initialAttributes;
    }

    public HttpHeaders getInitialHeaders() {
        return initialHeaders;
    }

    public PolicyStep<?> getCurrentDebugStep() {
        return policySteps.getLast();
    }

    public List<PolicyStep<?>> getDebugSteps() {
        return this.policySteps;
    }

    public InvokerResponse getInvokerResponse() {
        return invokerResponse;
    }
}
