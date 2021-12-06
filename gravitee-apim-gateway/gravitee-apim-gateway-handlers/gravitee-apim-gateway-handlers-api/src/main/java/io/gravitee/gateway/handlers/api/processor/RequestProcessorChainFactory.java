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
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.core.processor.provider.StreamableProcessorSupplier;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.flow.BestMatchPolicyResolver;
import io.gravitee.gateway.flow.SimpleFlowPolicyChainProvider;
import io.gravitee.gateway.flow.SimpleFlowProvider;
import io.gravitee.gateway.core.condition.CompositeConditionEvaluator;
import io.gravitee.gateway.flow.condition.evaluation.HttpMethodConditionEvaluator;
import io.gravitee.gateway.flow.condition.evaluation.PathBasedConditionEvaluator;
import io.gravitee.gateway.core.condition.ExpressionLanguageStringConditionEvaluator;
import io.gravitee.gateway.handlers.api.flow.api.ApiFlowResolver;
import io.gravitee.gateway.handlers.api.flow.plan.PlanFlowPolicyChainProvider;
import io.gravitee.gateway.handlers.api.flow.plan.PlanFlowResolver;
import io.gravitee.gateway.handlers.api.path.impl.ApiPathResolverImpl;
import io.gravitee.gateway.handlers.api.policy.api.ApiPolicyChainProvider;
import io.gravitee.gateway.handlers.api.policy.api.ApiPolicyResolver;
import io.gravitee.gateway.handlers.api.policy.plan.PlanPolicyChainProvider;
import io.gravitee.gateway.handlers.api.policy.plan.PlanPolicyResolver;
import io.gravitee.gateway.handlers.api.processor.cors.CorsPreflightRequestProcessor;
import io.gravitee.gateway.handlers.api.processor.forward.XForwardedPrefixProcessor;
import io.gravitee.gateway.handlers.api.processor.logging.ApiLoggableRequestProcessor;
import io.gravitee.gateway.handlers.api.processor.pathparameters.PathParametersIndexProcessor;
import io.gravitee.gateway.policy.PolicyChainOrder;
import io.gravitee.gateway.policy.PolicyChainProviderLoader;
import io.gravitee.gateway.policy.PolicyResolver;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.security.core.SecurityPolicyChainProvider;
import io.gravitee.gateway.security.core.SecurityPolicyResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RequestProcessorChainFactory extends ApiProcessorChainFactory {

    @Value("${reporters.logging.max_size:-1}")
    private int maxSizeLogMessage;

    @Value("${reporters.logging.excluded_response_types:#{null}}")
    private String excludedResponseTypes;

    @Value("${handlers.request.headers.x-forwarded-prefix:false}")
    private boolean overrideXForwardedPrefix;

    @Autowired
    GatewayConfiguration gatewayConfiguration;

    @Autowired
    PolicyChainProviderLoader policyChainProviderLoader;

    @Override
    public void afterPropertiesSet() {
        addAll(policyChainProviderLoader.get(PolicyChainOrder.BEFORE_API, StreamType.ON_REQUEST));

        StreamableProcessorSupplier<ExecutionContext, Buffer> loggingDecoratorSupplier = null;
        if (api.getProxy().getLogging() != null && api.getProxy().getLogging().getMode() != LoggingMode.NONE) {
            loggingDecoratorSupplier =
                new StreamableProcessorSupplier<>(
                    () -> {
                        ApiLoggableRequestProcessor processor = new ApiLoggableRequestProcessor(api.getProxy().getLogging());
                        processor.setMaxSizeLogMessage(maxSizeLogMessage);
                        processor.setExcludedResponseTypes(excludedResponseTypes);

                        return processor;
                    }
                );

            add(loggingDecoratorSupplier);
        }

        if (api.getProxy().getCors() != null && api.getProxy().getCors().isEnabled()) {
            add(() -> new CorsPreflightRequestProcessor(api.getProxy().getCors()));
        }

        // Prepare security policy chain
        final PolicyResolver securityPolicyResolver = new SecurityPolicyResolver();
        applicationContext.getAutowireCapableBeanFactory().autowireBean(securityPolicyResolver);
        add(new SecurityPolicyChainProvider(securityPolicyResolver));

        final ConditionEvaluator<Flow> evaluator = new CompositeConditionEvaluator<>(
            new HttpMethodConditionEvaluator(),
            new PathBasedConditionEvaluator(),
            new ExpressionLanguageStringConditionEvaluator()
        );

        if (loggingDecoratorSupplier != null) {
            add(loggingDecoratorSupplier);
        }

        if (overrideXForwardedPrefix) {
            add(XForwardedPrefixProcessor::new);
        }

        if (api.getDefinitionVersion() == DefinitionVersion.V1) {
            add(() -> new PathParametersIndexProcessor(new ApiPathResolverImpl(api)));
            add(new PlanPolicyChainProvider(StreamType.ON_REQUEST, new PlanPolicyResolver(api), chainFactory));
            add(new ApiPolicyChainProvider(StreamType.ON_REQUEST, new ApiPolicyResolver(), chainFactory));
        } else if (api.getDefinitionVersion() == DefinitionVersion.V2) {
            if (api.getFlowMode() == null || api.getFlowMode() == FlowMode.DEFAULT) {
                add(
                    new PlanFlowPolicyChainProvider(
                        new SimpleFlowProvider(StreamType.ON_REQUEST, new PlanFlowResolver(api, evaluator), chainFactory)
                    )
                );
                add(
                    new SimpleFlowPolicyChainProvider(
                        new SimpleFlowProvider(StreamType.ON_REQUEST, new ApiFlowResolver(api, evaluator), chainFactory)
                    )
                );
            } else {
                add(
                    new PlanFlowPolicyChainProvider(
                        new SimpleFlowProvider(
                            StreamType.ON_REQUEST,
                            new BestMatchPolicyResolver(new PlanFlowResolver(api, evaluator)),
                            chainFactory
                        )
                    )
                );
                add(
                    new SimpleFlowPolicyChainProvider(
                        new SimpleFlowProvider(
                            StreamType.ON_REQUEST,
                            new BestMatchPolicyResolver(new ApiFlowResolver(api, evaluator)),
                            chainFactory
                        )
                    )
                );
            }
        }
    }
}
