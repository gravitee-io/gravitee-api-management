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
package io.gravitee.gateway.handlers.api.processor;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.FlowMode;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.core.condition.CompositeConditionEvaluator;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.flow.BestMatchPolicyResolver;
import io.gravitee.gateway.flow.FlowPolicyResolverFactory;
import io.gravitee.gateway.flow.SimpleFlowPolicyChainProvider;
import io.gravitee.gateway.flow.SimpleFlowProvider;
import io.gravitee.gateway.flow.condition.evaluation.ExpressionLanguageFlowConditionEvaluator;
import io.gravitee.gateway.flow.condition.evaluation.HttpMethodConditionEvaluator;
import io.gravitee.gateway.flow.condition.evaluation.PathBasedConditionEvaluator;
import io.gravitee.gateway.flow.policy.PolicyChainFactory;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.flow.api.ApiFlowResolver;
import io.gravitee.gateway.handlers.api.flow.plan.PlanFlowPolicyChainProvider;
import io.gravitee.gateway.handlers.api.flow.plan.PlanFlowResolver;
import io.gravitee.gateway.handlers.api.policy.api.ApiPolicyChainProvider;
import io.gravitee.gateway.handlers.api.policy.api.ApiPolicyResolver;
import io.gravitee.gateway.handlers.api.policy.plan.PlanPolicyChainProvider;
import io.gravitee.gateway.handlers.api.policy.plan.PlanPolicyResolver;
import io.gravitee.gateway.handlers.api.processor.cors.CorsSimpleRequestProcessor;
import io.gravitee.gateway.handlers.api.processor.pathmapping.PathMappingProcessor;
import io.gravitee.gateway.handlers.api.processor.shutdown.ShutdownProcessor;
import io.gravitee.gateway.policy.PolicyChainOrder;
import io.gravitee.gateway.policy.PolicyChainProviderLoader;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.node.api.Node;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseProcessorChainFactory extends ApiProcessorChainFactory {

    private final PolicyChainProviderLoader policyChainProviderLoader;
    private final FlowPolicyResolverFactory flowPolicyResolverFactory;

    private Node node;

    public ResponseProcessorChainFactory(
        final Api api,
        final PolicyChainFactory policyChainFactory,
        final PolicyChainProviderLoader policyChainProviderLoader,
        final Node node,
        FlowPolicyResolverFactory flowPolicyResolverFactory
    ) {
        super(api, policyChainFactory);
        this.policyChainProviderLoader = policyChainProviderLoader;
        this.node = node;
        this.flowPolicyResolverFactory = flowPolicyResolverFactory;

        this.initialize();
    }

    private void initialize() {
        add(() -> new ShutdownProcessor(node));

        final ConditionEvaluator<Flow> evaluator = new CompositeConditionEvaluator<>(
            new HttpMethodConditionEvaluator(),
            new PathBasedConditionEvaluator(),
            new ExpressionLanguageFlowConditionEvaluator()
        );

        if (api.getDefinitionVersion() == DefinitionVersion.V1) {
            add(new ApiPolicyChainProvider(StreamType.ON_RESPONSE, new ApiPolicyResolver(), policyChainFactory));
            add(new PlanPolicyChainProvider(StreamType.ON_RESPONSE, new PlanPolicyResolver(api), policyChainFactory));
        } else if (api.getDefinitionVersion() == DefinitionVersion.V2) {
            if (api.getFlowMode() == null || api.getFlowMode() == FlowMode.DEFAULT) {
                add(
                    new SimpleFlowPolicyChainProvider(
                        StreamType.ON_RESPONSE,
                        new ApiFlowResolver(api, evaluator),
                        policyChainFactory,
                        flowPolicyResolverFactory
                    )
                );
                add(
                    new PlanFlowPolicyChainProvider(
                        StreamType.ON_RESPONSE,
                        new PlanFlowResolver(api, evaluator),
                        policyChainFactory,
                        flowPolicyResolverFactory
                    )
                );
            } else {
                add(
                    new SimpleFlowPolicyChainProvider(
                        StreamType.ON_RESPONSE,
                        new BestMatchPolicyResolver(new ApiFlowResolver(api, evaluator)),
                        policyChainFactory,
                        flowPolicyResolverFactory
                    )
                );
                add(
                    new PlanFlowPolicyChainProvider(
                        StreamType.ON_RESPONSE,
                        new BestMatchPolicyResolver(new PlanFlowResolver(api, evaluator)),
                        policyChainFactory,
                        flowPolicyResolverFactory
                    )
                );
            }
        }

        if (api.getProxy().getCors() != null && api.getProxy().getCors().isEnabled()) {
            add(() -> new CorsSimpleRequestProcessor(api.getProxy().getCors()));
        }

        if (api.getPathMappings() != null && !api.getPathMappings().isEmpty()) {
            add(() -> new PathMappingProcessor(api.getPathMappings()));
        }

        addAll(policyChainProviderLoader.get(PolicyChainOrder.AFTER_API, StreamType.ON_RESPONSE));
    }
}
