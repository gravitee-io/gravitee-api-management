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

import io.gravitee.definition.model.LoggingMode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.processor.StreamableProcessor;
import io.gravitee.gateway.core.processor.StreamableProcessorDecorator;
import io.gravitee.gateway.core.processor.chain.StreamableProcessorChain;
import io.gravitee.gateway.core.processor.provider.ProcessorProvider;
import io.gravitee.gateway.core.processor.provider.ProcessorSupplier;
import io.gravitee.gateway.core.processor.provider.StreamableProcessorProviderChain;
import io.gravitee.gateway.handlers.api.path.PathResolver;
import io.gravitee.gateway.handlers.api.policy.api.ApiPolicyChainProvider;
import io.gravitee.gateway.handlers.api.policy.api.ApiPolicyResolver;
import io.gravitee.gateway.handlers.api.policy.plan.PlanPolicyChainProvider;
import io.gravitee.gateway.handlers.api.policy.plan.PlanPolicyResolver;
import io.gravitee.gateway.handlers.api.processor.cors.CorsPreflightRequestProcessor;
import io.gravitee.gateway.handlers.api.processor.forward.XForwardedPrefixProcessor;
import io.gravitee.gateway.handlers.api.processor.logging.ApiLoggableRequestProcessor;
import io.gravitee.gateway.handlers.api.processor.pathparameters.PathParametersIndexProcessor;
import io.gravitee.gateway.policy.PolicyChainProvider;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.security.core.SecurityPolicyChainProvider;
import io.gravitee.gateway.security.core.SecurityPolicyResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RequestProcessorChainFactory extends ApiProcessorChainFactory {

    private final List<ProcessorProvider<ExecutionContext, StreamableProcessor<ExecutionContext, Buffer>>> providers = new ArrayList<>();

    @Value("${reporters.logging.max_size:-1}")
    private int maxSizeLogMessage;
    @Value("${reporters.logging.excluded_response_types:#{null}}")
    private String excludedResponseTypes;
    @Value("${handlers.request.headers.x-forwarded-prefix:false}")
    private boolean overrideXForwardedPrefix;

    @Override
    public void afterPropertiesSet() {
        ApiPolicyResolver apiPolicyResolver = new ApiPolicyResolver();
        applicationContext.getAutowireCapableBeanFactory().autowireBean(apiPolicyResolver);
        PolicyChainProvider apiPolicyChainProvider = new ApiPolicyChainProvider(StreamType.ON_REQUEST, apiPolicyResolver);


        SecurityPolicyResolver securityPolicyResolver = new SecurityPolicyResolver();
        applicationContext.getAutowireCapableBeanFactory().autowireBean(securityPolicyResolver);
        PolicyChainProvider securityPolicyChainProvider = new SecurityPolicyChainProvider(securityPolicyResolver);

        PlanPolicyResolver planPolicyResolver = new PlanPolicyResolver();
        applicationContext.getAutowireCapableBeanFactory().autowireBean(planPolicyResolver);
        PolicyChainProvider planPolicyChainProvider = new PlanPolicyChainProvider(StreamType.ON_REQUEST, planPolicyResolver);

        final PathParametersIndexProcessor pathParametersIndexProcessor = new PathParametersIndexProcessor();
        applicationContext.getAutowireCapableBeanFactory().autowireBean(pathParametersIndexProcessor);

        ProcessorSupplier<ExecutionContext, StreamableProcessor<ExecutionContext, Buffer>> loggingDecorator = null;

        if (api.getProxy().getLogging() != null && api.getProxy().getLogging().getMode() != LoggingMode.NONE) {
            loggingDecorator = new ProcessorSupplier<>(() -> {
                ApiLoggableRequestProcessor processor = new ApiLoggableRequestProcessor(api.getProxy().getLogging());
                // log max size limit is in MB format
                // -1 means no limit
                processor.setMaxSizeLogMessage((maxSizeLogMessage <= -1) ? -1 : maxSizeLogMessage * (1024 * 1024));
                processor.setExcludedResponseTypes(excludedResponseTypes);

                return new StreamableProcessorDecorator<>(processor);
            });

            providers.add(loggingDecorator);
        }

        if (api.getProxy().getCors() != null && api.getProxy().getCors().isEnabled()) {
            providers.add(new ProcessorSupplier<>(() ->
                    new StreamableProcessorDecorator<>(new CorsPreflightRequestProcessor(api.getProxy().getCors()))));
        }

        providers.add(securityPolicyChainProvider);

        if (loggingDecorator != null) {
            providers.add(loggingDecorator);
        }

        if (overrideXForwardedPrefix) {
            providers.add(new ProcessorSupplier<>(() ->
                    new StreamableProcessorDecorator<>(new XForwardedPrefixProcessor())));
        }

        providers.add(new ProcessorSupplier<>(() -> new StreamableProcessorDecorator<>(pathParametersIndexProcessor)));

        providers.add(planPolicyChainProvider);
        providers.add(apiPolicyChainProvider);
    }

    @Override
    public StreamableProcessorChain<ExecutionContext, Buffer, StreamableProcessor<ExecutionContext, Buffer>> create() {
        return new StreamableProcessorProviderChain<>(providers);
    }
}
