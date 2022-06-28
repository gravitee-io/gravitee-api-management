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
package io.gravitee.gateway.jupiter.handlers.api.processor;

import io.gravitee.definition.model.Cors;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.gateway.core.logging.LoggingContext;
import io.gravitee.gateway.core.logging.utils.LoggingUtils;
import io.gravitee.gateway.handlers.api.definition.Api;
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
import io.gravitee.gateway.jupiter.handlers.api.processor.plan.PlanProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.shutdown.ShutdownProcessor;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.reactivex.Completable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiProcessorChainFactory {

    private final boolean overrideXForwardedPrefix;
    private final Node node;
    private final List<ProcessorHook> processorHooks = new ArrayList<>();

    public ApiProcessorChainFactory(final Configuration configuration, Node node) {
        this.overrideXForwardedPrefix = configuration.getProperty("handlers.request.headers.x-forwarded-prefix", Boolean.class, false);
        this.node = node;

        boolean tracing = configuration.getProperty("services.tracing.enabled", Boolean.class, false);
        if (tracing) {
            processorHooks.add(new TracingHook("processor"));
        }
    }

    public ProcessorChain preProcessorChain(final Api api) {
        List<Processor> preProcessorList = new ArrayList<>();

        Cors cors = api.getProxy().getCors();
        if (cors != null && cors.isEnabled()) {
            preProcessorList.add(CorsPreflightRequestProcessor.instance());
        }
        if (overrideXForwardedPrefix) {
            preProcessorList.add(XForwardedPrefixProcessor.instance());
        }

        preProcessorList.add(PlanProcessor.instance());

        if (LoggingUtils.getLoggingContext(api) != null) {
            preProcessorList.add(LogRequestProcessor.instance());
        }

        ProcessorChain processorChain = new ProcessorChain("processor-chain-pre-api", preProcessorList);
        processorChain.addHooks(processorHooks);
        return processorChain;
    }

    public ProcessorChain postProcessorChain(final Api api) {
        List<Processor> postProcessorList = new ArrayList<>();
        postProcessorList.add(new ShutdownProcessor(node));

        Cors cors = api.getProxy().getCors();
        if (cors != null && cors.isEnabled()) {
            postProcessorList.add(CorsSimpleRequestProcessor.instance());
        }
        Map<String, Pattern> pathMappings = api.getPathMappings();
        if (pathMappings != null && !pathMappings.isEmpty()) {
            postProcessorList.add(PathMappingProcessor.instance());
        }

        if (LoggingUtils.getLoggingContext(api) != null) {
            postProcessorList.add(LogResponseProcessor.instance());
        }

        ProcessorChain processorChain = new ProcessorChain("processor-chain-post-api", postProcessorList);
        processorChain.addHooks(processorHooks);
        return processorChain;
    }

    public ProcessorChain errorProcessorChain(final Api api) {
        List<Processor> errorProcessorList = new ArrayList<>();
        Cors cors = api.getProxy().getCors();
        if (cors != null && cors.isEnabled()) {
            errorProcessorList.add(CorsSimpleRequestProcessor.instance());
        }
        Map<String, Pattern> pathMappings = api.getPathMappings();
        if (pathMappings != null && !pathMappings.isEmpty()) {
            errorProcessorList.add(PathMappingProcessor.instance());
        }

        if (api.getResponseTemplates() != null && !api.getResponseTemplates().isEmpty()) {
            errorProcessorList.add(ResponseTemplateBasedFailureProcessor.instance());
        } else {
            errorProcessorList.add(SimpleFailureProcessor.instance());
        }

        if (LoggingUtils.getLoggingContext(api) != null) {
            errorProcessorList.add(LogResponseProcessor.instance());
        }

        ProcessorChain processorChain = new ProcessorChain("processor-chain-error-api", errorProcessorList);
        processorChain.addHooks(processorHooks);
        return processorChain;
    }
}
