/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.v4.policy;

import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.hook.HttpHook;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.gateway.reactive.policy.tracing.TracingPolicyHook;
import io.netty.util.internal.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@link HttpPolicyChainFactory} that can be instantiated per-api or per-organization, and optimized to maximize the reuse of created {@link HttpPolicyChain} thanks to a cache.
 *
 * <b>WARNING</b>: this factory must absolutely be created per api to ensure proper cache destruction when deploying / un-deploying the api.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpPolicyChainFactory extends AbstractPolicyChainFactory<HttpPolicy, Flow, HttpPolicyChain> {

    protected final List<HttpHook> policyHooks = new ArrayList<>();

    public HttpPolicyChainFactory(final String id, final PolicyManager policyManager, final boolean tracing) {
        super(id, policyManager);
        initPolicyHooks(tracing);
    }

    protected void initPolicyHooks(final boolean tracing) {
        if (tracing) {
            policyHooks.add(new TracingPolicyHook());
        }
    }

    @Override
    protected String getIdSuffix() {
        return "-policyChainFactory";
    }

    @Override
    protected List<Step> getSteps(Flow flow, ExecutionPhase phase) {
        final List<Step> steps =
            switch (phase) {
                case REQUEST -> flow.getRequest();
                case RESPONSE -> flow.getResponse();
                default -> new ArrayList<>();
            };

        return steps != null ? steps : new ArrayList<>();
    }

    @Override
    protected HttpPolicyChain buildPolicyChain(String flowChainId, Flow flow, ExecutionPhase phase, List<HttpPolicy> policies) {
        String policyChainId = getPolicyChainId(flowChainId, flow);
        HttpPolicyChain httpPolicyChain = new HttpPolicyChain(policyChainId, policies, phase);
        httpPolicyChain.addHooks(policyHooks);
        return httpPolicyChain;
    }

    @Override
    protected PolicyMetadata buildPolicyMetadata(Step step) {
        final PolicyMetadata policyMetadata = new PolicyMetadata(step.getPolicy(), step.getConfiguration(), step.getCondition());
        policyMetadata.metadata().put(PolicyMetadata.MetadataKeys.EXECUTION_MODE, ExecutionMode.V4_EMULATION_ENGINE);

        return policyMetadata;
    }

    private String getPolicyChainId(final String flowChainId, final Flow flow) {
        StringBuilder flowNameBuilder = new StringBuilder(flowChainId).append(ID_SEPARATOR);
        if (StringUtil.isNullOrEmpty(flow.getName())) {
            flow
                .selectorByType(SelectorType.HTTP)
                .map(HttpSelector.class::cast)
                .ifPresent(httpSelector -> {
                    if (httpSelector.getMethods() == null || httpSelector.getMethods().isEmpty()) {
                        flowNameBuilder.append("ALL").append(ID_SEPARATOR);
                    } else {
                        httpSelector.getMethods().forEach(httpMethod -> flowNameBuilder.append(httpMethod).append("-"));
                    }
                    flowNameBuilder.append(httpSelector.getPath());
                });
        } else {
            flowNameBuilder.append(flow.getName());
        }
        return flowNameBuilder.toString().toLowerCase(Locale.ROOT);
    }
}
