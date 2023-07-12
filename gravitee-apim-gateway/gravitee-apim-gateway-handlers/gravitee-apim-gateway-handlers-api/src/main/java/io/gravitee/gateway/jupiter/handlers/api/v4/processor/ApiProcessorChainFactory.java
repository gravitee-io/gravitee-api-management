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
package io.gravitee.gateway.jupiter.handlers.api.v4.processor;

import static io.gravitee.gateway.jupiter.handlers.api.processor.subscription.SubscriptionProcessor.DEFAULT_CLIENT_IDENTIFIER_HEADER;

import io.gravitee.definition.model.Cors;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.gateway.core.logging.utils.LoggingUtils;
import io.gravitee.gateway.jupiter.api.hook.ProcessorHook;
import io.gravitee.gateway.jupiter.core.processor.Processor;
import io.gravitee.gateway.jupiter.core.processor.ProcessorChain;
import io.gravitee.gateway.jupiter.core.tracing.TracingHook;
import io.gravitee.gateway.jupiter.handlers.api.processor.cors.CorsPreflightRequestProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.cors.CorsSimpleRequestProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.error.SimpleFailureProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.error.template.ResponseTemplateBasedFailureProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.forward.XForwardedPrefixProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.logging.LogRequestProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.logging.LogResponseProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.pathmapping.PathMappingProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.shutdown.ShutdownProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.subscription.SubscriptionProcessor;
import io.gravitee.gateway.jupiter.handlers.api.v4.Api;
import io.gravitee.gateway.jupiter.handlers.api.v4.processor.message.error.SimpleFailureMessageProcessor;
import io.gravitee.gateway.jupiter.handlers.api.v4.processor.message.error.template.ResponseTemplateBasedFailureMessageProcessor;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiProcessorChainFactory {

    private final boolean overrideXForwardedPrefix;
    private final String clientIdentifierHeader;
    private final Node node;
    private final List<ProcessorHook> processorHooks = new ArrayList<>();

    public ApiProcessorChainFactory(final Configuration configuration, Node node) {
        this.overrideXForwardedPrefix = configuration.getProperty("handlers.request.headers.x-forwarded-prefix", Boolean.class, false);
        this.clientIdentifierHeader =
            configuration.getProperty("handlers.request.client.header", String.class, DEFAULT_CLIENT_IDENTIFIER_HEADER);
        this.node = node;

        boolean tracing = configuration.getProperty("services.tracing.enabled", Boolean.class, false);
        if (tracing) {
            processorHooks.add(new TracingHook("processor"));
        }
    }

    /**
     * Return the chain of processors to execute prior the security chain.
     *
     * @param api the api to create the processor chain for.
     *
     * @return the chain of processors.
     */
    public ProcessorChain beforeHandle(final Api api) {
        final List<Processor> processors = new ArrayList<>();

        getHttpListener(api)
            .ifPresent(httpListener -> {
                final Logging logging = httpListener.getLogging();
                if (LoggingUtils.getLoggingContext(logging) != null) {
                    processors.add(LogRequestProcessor.instance());
                }
            });

        return new ProcessorChain("processor-chain-before-api-handle", processors, processorHooks);
    }

    /**
     * Return the chain of processors to execute before executing all the apis flows.
     *
     * @param api the api to create the processor chain for.
     *
     * @return the chain of processors.
     */
    public ProcessorChain beforeApiExecution(final Api api) {
        final List<Processor> processors = new ArrayList<>();
        if (api.getDefinition().getListeners() != null) {
            getHttpListener(api)
                .ifPresent(httpListener -> {
                    final Cors cors = httpListener.getCors();

                    if (cors != null && cors.isEnabled()) {
                        processors.add(CorsPreflightRequestProcessor.instance());
                    }

                    if (overrideXForwardedPrefix) {
                        processors.add(XForwardedPrefixProcessor.instance());
                    }
                });

            processors.add(SubscriptionProcessor.instance(clientIdentifierHeader));
        }

        return new ProcessorChain("processor-chain-before-api-execution", processors, processorHooks);
    }

    /**
     * Return the chain of processors to execute after the execution of the apis flows without error.
     *
     * @param api the api to create the processor chain for.
     *
     * @return the chain of processors.
     */
    public ProcessorChain afterApiExecution(final Api api) {
        final List<Processor> processors = getAfterApiExecutionProcessors(api);

        return new ProcessorChain("processor-chain-after-api-execution", processors, processorHooks);
    }

    private List<Processor> getAfterApiExecutionProcessors(Api api) {
        final List<Processor> processors = new ArrayList<>();

        processors.add(new ShutdownProcessor(node));

        getHttpListener(api)
            .ifPresent(httpListener -> {
                final Cors cors = httpListener.getCors();
                if (cors != null && cors.isEnabled()) {
                    processors.add(CorsSimpleRequestProcessor.instance());
                }

                final Map<String, Pattern> pathMappings = httpListener.getPathMappingsPattern();
                if (pathMappings != null && !pathMappings.isEmpty()) {
                    processors.add(PathMappingProcessor.instance());
                }
            });
        return processors;
    }

    public ProcessorChain onMessage(final Api api) {
        List<Processor> processors = new ArrayList<>();

        if (api.getDefinition().getResponseTemplates() != null && !api.getDefinition().getResponseTemplates().isEmpty()) {
            processors.add(ResponseTemplateBasedFailureMessageProcessor.instance());
        } else {
            processors.add(SimpleFailureMessageProcessor.instance());
        }

        return new ProcessorChain("processor-chain-api-message", processors);
    }

    /**
     * Return the chain of processors to execute in case of error during the api execution.
     *
     * @param api the api to create the processor chain for.
     *
     * @return the chain of processors.
     */
    public ProcessorChain onError(final Api api) {
        // On error processor chain contains the after api execution processors.
        List<Processor> processors = new ArrayList<>(getAfterApiExecutionProcessors(api));

        if (api.getDefinition().getResponseTemplates() != null && !api.getDefinition().getResponseTemplates().isEmpty()) {
            processors.add(ResponseTemplateBasedFailureProcessor.instance());
        } else {
            processors.add(SimpleFailureProcessor.instance());
        }

        return new ProcessorChain("processor-chain-api-error", processors, processorHooks);
    }

    /**
     * Return the chain of processors to execute after request handling.
     *
     * @param api the api to create the processor chain for.
     *
     * @return the chain of processors.
     */
    public ProcessorChain afterHandle(final Api api) {
        final List<Processor> processors = new ArrayList<>();

        getHttpListener(api)
            .ifPresent(httpListener -> {
                final Logging logging = httpListener.getLogging();
                if (LoggingUtils.getLoggingContext(logging) != null) {
                    processors.add(LogResponseProcessor.instance());
                }
            });

        return new ProcessorChain("processor-chain-after-api-handle", processors, processorHooks);
    }

    private Optional<HttpListener> getHttpListener(final Api api) {
        if (api.getDefinition().getListeners() != null) {
            return api
                .getDefinition()
                .getListeners()
                .stream()
                .filter(listener -> listener.getType() == ListenerType.HTTP)
                .map(HttpListener.class::cast)
                .findFirst();
        } else {
            return Optional.empty();
        }
    }
}
