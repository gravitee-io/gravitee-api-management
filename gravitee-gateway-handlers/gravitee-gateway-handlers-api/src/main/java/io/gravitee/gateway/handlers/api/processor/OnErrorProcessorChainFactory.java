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

import io.gravitee.gateway.flow.policy.PolicyChainFactory;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.processor.cors.CorsSimpleRequestProcessor;
import io.gravitee.gateway.handlers.api.processor.error.SimpleFailureProcessor;
import io.gravitee.gateway.handlers.api.processor.error.templates.ResponseTemplateBasedFailureProcessor;
import io.gravitee.gateway.handlers.api.processor.pathmapping.PathMappingProcessor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OnErrorProcessorChainFactory extends ApiProcessorChainFactory {

    public OnErrorProcessorChainFactory(final Api api, final PolicyChainFactory policyChainFactory) {
        super(api, policyChainFactory);
        this.initialize();
    }

    private void initialize() {
        if (api.getProxy().getCors() != null && api.getProxy().getCors().isEnabled()) {
            add(() -> new CorsSimpleRequestProcessor(api.getProxy().getCors()));
        }

        if (api.getPathMappings() != null && !api.getPathMappings().isEmpty()) {
            add(() -> new PathMappingProcessor(api.getPathMappings()));
        }

        if (api.getResponseTemplates() != null && !api.getResponseTemplates().isEmpty()) {
            add(() -> new ResponseTemplateBasedFailureProcessor(api.getResponseTemplates()));
        } else {
            add(SimpleFailureProcessor::new);
        }
    }
}
