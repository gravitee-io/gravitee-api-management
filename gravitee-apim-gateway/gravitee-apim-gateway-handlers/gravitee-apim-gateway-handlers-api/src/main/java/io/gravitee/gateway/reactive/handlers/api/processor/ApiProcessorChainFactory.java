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
package io.gravitee.gateway.reactive.handlers.api.processor;

import static io.gravitee.gateway.handlers.api.ApiReactorHandlerFactory.HANDLERS_REQUEST_HEADERS_X_FORWARDED_PREFIX_PROPERTY;
import static io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessor.DEFAULT_CLIENT_IDENTIFIER_HEADER;

import io.gravitee.definition.model.Cors;
import io.gravitee.gateway.core.logging.utils.LoggingUtils;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.processor.pathparameters.PathParametersExtractor;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactive.api.hook.ProcessorHook;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactive.core.processor.ProcessorChain;
import io.gravitee.gateway.reactive.core.tracing.TracingHook;
import io.gravitee.gateway.reactive.handlers.api.processor.cors.CorsPreflightRequestProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.cors.CorsSimpleRequestProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.error.SimpleFailureProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.error.template.ResponseTemplateBasedFailureProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.forward.XForwardedPrefixProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.pathmapping.PathMappingProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.pathparameters.PathParametersProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.shutdown.ShutdownProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.subscription.SubscriptionProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.transaction.TransactionPostProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.transaction.TransactionPostProcessorConfiguration;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.logging.LogInitProcessor;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.logging.LogRequestProcessor;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.logging.LogResponseProcessor;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiProcessorChainFactory {

    private static final String HANDLERS_REQUEST_CLIENT_HEADER = "handlers.request.client.header";
    private final boolean overrideXForwardedPrefix;
    private final String clientIdentifierHeader;
    private final Node node;
    private final Configuration configuration;
    private TracingHook tracingHook;

    public ApiProcessorChainFactory(final Configuration configuration, Node node) {
        this.configuration = configuration;
        this.overrideXForwardedPrefix =
            configuration.getProperty(HANDLERS_REQUEST_HEADERS_X_FORWARDED_PREFIX_PROPERTY, Boolean.class, false);
        this.clientIdentifierHeader =
            configuration.getProperty(HANDLERS_REQUEST_CLIENT_HEADER, String.class, DEFAULT_CLIENT_IDENTIFIER_HEADER);

        this.node = node;

        tracingHook = new TracingHook("processor");
    }

    public ProcessorChain beforeHandle(final Api api, final TracingContext tracingContext) {
        final List<Processor> processors = new ArrayList<>();

        if (LoggingUtils.getLoggingContext(api.getDefinition()) != null) {
            processors.add(LogInitProcessor.instance());
            processors.add(LogRequestProcessor.instance());
        }

        return new ProcessorChain("before-api-handle", processors, processorHooks(tracingContext));
    }

    /**
     * Return the chain of processors to execute before executing security chain.
     *
     * @param api the api to create the processor chain for.
     *
     * @return the chain of processors.
     */
    public ProcessorChain beforeSecurityChain(final Api api, final TracingContext tracingContext) {
        List<Processor> preProcessorList = new ArrayList<>();

        Cors cors = api.getDefinition().getProxy().getCors();
        if (cors != null && cors.isEnabled()) {
            preProcessorList.add(CorsPreflightRequestProcessor.instance());
        }

        ProcessorChain processorChain = new ProcessorChain("before-security-chain", preProcessorList);
        processorChain.addHooks(processorHooks(tracingContext));
        return processorChain;
    }

    public ProcessorChain beforeApiExecution(final Api api, final TracingContext tracingContext) {
        List<Processor> preProcessorList = new ArrayList<>();

        if (overrideXForwardedPrefix) {
            preProcessorList.add(XForwardedPrefixProcessor.instance());
        }

        final PathParametersExtractor extractor = new PathParametersExtractor(api.getDefinition());
        if (extractor.canExtractPathParams()) {
            preProcessorList.add(new PathParametersProcessor(extractor));
        }

        preProcessorList.add(SubscriptionProcessor.instance(clientIdentifierHeader));

        Map<String, Pattern> pathMappings = api.getDefinition().getPathMappings();
        if (pathMappings != null && !pathMappings.isEmpty()) {
            preProcessorList.add(PathMappingProcessor.instance());
        }

        ProcessorChain processorChain = new ProcessorChain("before-api-execution", preProcessorList);
        processorChain.addHooks(processorHooks(tracingContext));
        return processorChain;
    }

    public ProcessorChain afterApiExecution(final Api api, final TracingContext tracingContext) {
        List<Processor> postProcessorList = getAfterApiExecutionProcessors(api);

        ProcessorChain processorChain = new ProcessorChain("after-api-execution", postProcessorList);
        processorChain.addHooks(processorHooks(tracingContext));
        return processorChain;
    }

    private List<Processor> getAfterApiExecutionProcessors(Api api) {
        List<Processor> postProcessorList = new ArrayList<>();
        postProcessorList.add(new ShutdownProcessor(node));
        postProcessorList.add(new TransactionPostProcessor(new TransactionPostProcessorConfiguration(configuration)));

        Cors cors = api.getDefinition().getProxy().getCors();
        if (cors != null && cors.isEnabled()) {
            postProcessorList.add(CorsSimpleRequestProcessor.instance());
        }

        return postProcessorList;
    }

    public ProcessorChain onError(final Api api, final TracingContext tracingContext) {
        List<Processor> errorProcessorList = new ArrayList<>(getAfterApiExecutionProcessors(api));

        if (api.getDefinition().getResponseTemplates() != null && !api.getDefinition().getResponseTemplates().isEmpty()) {
            errorProcessorList.add(ResponseTemplateBasedFailureProcessor.instance());
        } else {
            errorProcessorList.add(SimpleFailureProcessor.instance());
        }

        return new ProcessorChain("api-error", errorProcessorList, processorHooks(tracingContext));
    }

    public ProcessorChain afterHandle(final Api api, final TracingContext tracingContext) {
        final List<Processor> processors = new ArrayList<>();

        if (LoggingUtils.getLoggingContext(api.getDefinition()) != null) {
            processors.add(LogResponseProcessor.instance());
        }

        return new ProcessorChain("after-api-handle", processors, processorHooks(tracingContext));
    }

    protected List<ProcessorHook> processorHooks(final TracingContext tracingContext) {
        if (tracingContext.isVerbose()) {
            return List.of(tracingHook);
        }
        return List.of();
    }
}
