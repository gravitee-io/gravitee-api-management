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
package io.gravitee.gateway.handlers.api.processor;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.FlowMode;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.condition.CompositeConditionEvaluator;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.core.processor.provider.StreamableProcessorSupplier;
import io.gravitee.gateway.flow.BestMatchFlowResolver;
import io.gravitee.gateway.flow.BestMatchFlowSelector;
import io.gravitee.gateway.flow.FlowPolicyResolverFactory;
import io.gravitee.gateway.flow.SimpleFlowPolicyChainProvider;
import io.gravitee.gateway.flow.condition.evaluation.ExpressionLanguageFlowConditionEvaluator;
import io.gravitee.gateway.flow.condition.evaluation.HttpMethodConditionEvaluator;
import io.gravitee.gateway.flow.condition.evaluation.PathBasedConditionEvaluator;
import io.gravitee.gateway.flow.policy.PolicyChainFactory;
import io.gravitee.gateway.handlers.api.definition.Api;
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
import io.gravitee.gateway.handlers.api.processor.pathmapping.PathMappingProcessor;
import io.gravitee.gateway.handlers.api.processor.pathparameters.PathParametersExtractor;
import io.gravitee.gateway.handlers.api.processor.pathparameters.PathParametersIndexProcessor;
import io.gravitee.gateway.handlers.api.processor.pathparameters.PathParametersProcessor;
import io.gravitee.gateway.policy.PolicyChainOrder;
import io.gravitee.gateway.policy.PolicyChainProviderLoader;
import io.gravitee.gateway.policy.PolicyManager;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.security.core.AuthenticationHandlerSelector;
import io.gravitee.gateway.security.core.SecurityPolicyChainProvider;
import io.gravitee.gateway.security.core.SecurityPolicyResolver;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RequestProcessorChainFactory extends ApiProcessorChainFactory {

    private final RequestProcessorChainFactoryOptions requestProcessorChainFactoryOptions;
    private final PolicyChainProviderLoader policyChainProviderLoader;
    private final AuthenticationHandlerSelector authenticationHandlerSelector;
    private final PolicyManager policyManager;
    private final FlowPolicyResolverFactory flowPolicyResolverFactory;
    private final SecurityPolicyResolver securityPolicyResolver;
    private final BestMatchFlowSelector bestMatchFlowSelector;

    public RequestProcessorChainFactory(
        final Api api,
        final PolicyChainFactory policyChainFactory,
        final PolicyManager policyManager,
        final RequestProcessorChainFactoryOptions requestProcessorChainFactoryOptions,
        final PolicyChainProviderLoader policyChainProviderLoader,
        final AuthenticationHandlerSelector authenticationHandlerSelector,
        final FlowPolicyResolverFactory flowPolicyResolverFactory,
        SecurityPolicyResolver securityPolicyResolver,
        BestMatchFlowSelector bestMatchFlowSelector
    ) {
        super(api, policyChainFactory);
        this.policyManager = policyManager;
        this.requestProcessorChainFactoryOptions = requestProcessorChainFactoryOptions;
        this.policyChainProviderLoader = policyChainProviderLoader;
        this.authenticationHandlerSelector = authenticationHandlerSelector;
        this.flowPolicyResolverFactory = flowPolicyResolverFactory;
        this.securityPolicyResolver = securityPolicyResolver;
        this.bestMatchFlowSelector = bestMatchFlowSelector;

        this.initialize();
    }

    private void initialize() {
        addAll(policyChainProviderLoader.get(PolicyChainOrder.BEFORE_API, StreamType.ON_REQUEST));

        StreamableProcessorSupplier<ExecutionContext, Buffer> loggingDecoratorSupplier = null;
        if (
            api.getDefinition().getProxy().getLogging() != null && api.getDefinition().getProxy().getLogging().getMode() != LoggingMode.NONE
        ) {
            loggingDecoratorSupplier = new StreamableProcessorSupplier<>(() -> {
                ApiLoggableRequestProcessor processor = new ApiLoggableRequestProcessor(api.getDefinition().getProxy().getLogging());
                processor.setMaxSizeLogMessage(requestProcessorChainFactoryOptions.getMaxSizeLogMessage());
                processor.setExcludedResponseTypes(requestProcessorChainFactoryOptions.getExcludedResponseTypes());

                return processor;
            });

            add(loggingDecoratorSupplier);
        }

        if (api.getDefinition().getProxy().getCors() != null && api.getDefinition().getProxy().getCors().isEnabled()) {
            add(() -> new CorsPreflightRequestProcessor(api.getDefinition().getProxy().getCors()));
        }

        // Prepare security policy chain
        add(new SecurityPolicyChainProvider(securityPolicyResolver));

        final ConditionEvaluator<Flow> evaluator = new CompositeConditionEvaluator<>(
            new HttpMethodConditionEvaluator(),
            new PathBasedConditionEvaluator(),
            new ExpressionLanguageFlowConditionEvaluator()
        );

        if (loggingDecoratorSupplier != null) {
            add(loggingDecoratorSupplier);
        }

        if (requestProcessorChainFactoryOptions.isOverrideXForwardedPrefix()) {
            add(XForwardedPrefixProcessor::new);
        }

        if (api.getDefinitionVersion() == DefinitionVersion.V1) {
            add(() -> new PathParametersIndexProcessor(new ApiPathResolverImpl(api.getDefinition())));
            add(new PlanPolicyChainProvider(StreamType.ON_REQUEST, new PlanPolicyResolver(api), policyChainFactory));
            add(new ApiPolicyChainProvider(StreamType.ON_REQUEST, new ApiPolicyResolver(), policyChainFactory));
        } else if (api.getDefinitionVersion() == DefinitionVersion.V2) {
            final PathParametersExtractor extractor = new PathParametersExtractor(api.getDefinition());
            if (extractor.canExtractPathParams()) {
                add(() -> new PathParametersProcessor(extractor));
            }

            if (api.getDefinition().getFlowMode() == null || api.getDefinition().getFlowMode() == FlowMode.DEFAULT) {
                add(
                    new PlanFlowPolicyChainProvider(
                        StreamType.ON_REQUEST,
                        new PlanFlowResolver(api.getDefinition(), evaluator),
                        policyChainFactory,
                        flowPolicyResolverFactory
                    )
                );
                add(
                    new SimpleFlowPolicyChainProvider(
                        StreamType.ON_REQUEST,
                        new ApiFlowResolver(api.getDefinition(), evaluator),
                        policyChainFactory,
                        flowPolicyResolverFactory
                    )
                );
            } else {
                add(
                    new PlanFlowPolicyChainProvider(
                        StreamType.ON_REQUEST,
                        new BestMatchFlowResolver(new PlanFlowResolver(api.getDefinition(), evaluator), bestMatchFlowSelector),
                        policyChainFactory,
                        flowPolicyResolverFactory
                    )
                );
                add(
                    new SimpleFlowPolicyChainProvider(
                        StreamType.ON_REQUEST,
                        new BestMatchFlowResolver(new ApiFlowResolver(api.getDefinition(), evaluator), bestMatchFlowSelector),
                        policyChainFactory,
                        flowPolicyResolverFactory
                    )
                );
            }
        }

        if (api.getDefinition().getPathMappings() != null && !api.getDefinition().getPathMappings().isEmpty()) {
            add(() -> new PathMappingProcessor(api.getDefinition().getPathMappings()));
        }
    }

    public static class RequestProcessorChainFactoryOptions {

        private String maxSizeLogMessage;

        private String excludedResponseTypes;

        private boolean overrideXForwardedPrefix = false;

        public String getMaxSizeLogMessage() {
            return maxSizeLogMessage;
        }

        public RequestProcessorChainFactoryOptions setMaxSizeLogMessage(String maxSizeLogMessage) {
            this.maxSizeLogMessage = maxSizeLogMessage;
            return this;
        }

        public String getExcludedResponseTypes() {
            return excludedResponseTypes;
        }

        public RequestProcessorChainFactoryOptions setExcludedResponseTypes(String excludedResponseTypes) {
            this.excludedResponseTypes = excludedResponseTypes;
            return this;
        }

        public boolean isOverrideXForwardedPrefix() {
            return overrideXForwardedPrefix;
        }

        public RequestProcessorChainFactoryOptions setOverrideXForwardedPrefix(boolean overrideXForwardedPrefix) {
            this.overrideXForwardedPrefix = overrideXForwardedPrefix;
            return this;
        }
    }
}
