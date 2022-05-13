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
package io.gravitee.gateway.reactive.handlers.api.processor;

import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactive.core.processor.ProcessorChain;
import io.gravitee.gateway.reactive.handlers.api.processor.cors.CorsPreflightRequestProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.cors.CorsSimpleRequestProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.forward.XForwardedPrefixProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.pathmapping.PathMappingProcessor;
import io.gravitee.gateway.reactive.handlers.api.processor.shutdown.ShutdownProcessor;
import io.gravitee.node.api.Node;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiProcessorChainFactory {

    private final Options options;
    private final Node node;
    private ProcessorChain preProcessorChain;
    private ProcessorChain postProcessorChain;

    public ApiProcessorChainFactory(final Options options, Node node) {
        this.options = options;
        this.node = node;
    }

    public ProcessorChain preProcessorChain() {
        if (preProcessorChain == null) {
            initPreProcessorChain();
        }
        return preProcessorChain;
    }

    private void initPreProcessorChain() {
        List<Processor> preProcessorList = new ArrayList<>();

        preProcessorList.add(new CorsPreflightRequestProcessor());
        if (options.overrideXForwardedPrefix()) {
            preProcessorList.add(new XForwardedPrefixProcessor());
        }
        preProcessorChain = new ProcessorChain("pre-api-processor-chain", preProcessorList);
    }

    public ProcessorChain postProcessorChain() {
        if (postProcessorChain == null) {
            initPostProcessorChain();
        }
        return postProcessorChain;
    }

    private void initPostProcessorChain() {
        List<Processor> postProcessorList = new ArrayList<>();
        postProcessorList.add(new ShutdownProcessor(node));
        postProcessorList.add(new CorsSimpleRequestProcessor());
        postProcessorList.add(new PathMappingProcessor());
        postProcessorChain = new ProcessorChain("post-api-processor-chain", postProcessorList);
    }

    public static class Options {

        private boolean overrideXForwardedPrefix = false;

        public boolean overrideXForwardedPrefix() {
            return overrideXForwardedPrefix;
        }

        public Options overrideXForwardedPrefix(boolean overrideXForwardedPrefix) {
            this.overrideXForwardedPrefix = overrideXForwardedPrefix;
            return this;
        }
    }
}
