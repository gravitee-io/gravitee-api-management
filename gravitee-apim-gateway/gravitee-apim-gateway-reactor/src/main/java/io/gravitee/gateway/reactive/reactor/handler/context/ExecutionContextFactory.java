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
package io.gravitee.gateway.reactive.reactor.handler.context;

import io.gravitee.definition.model.Api;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.reactive.api.context.MessageExecutionContext;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.RequestExecutionContext;
import io.gravitee.gateway.reactive.api.context.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * A factory of {@link DefaultRequestExecutionContext} or {@link MessageExecutionContext}.
 * A single instance is created on per api basis because {@link TemplateVariableProvider} providers list is containing provider specific to the api.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ExecutionContextFactory {

    protected final List<TemplateVariableProvider> templateVariableProviders = new ArrayList<>();
    protected final ComponentProvider componentProvider;

    public ExecutionContextFactory() {
        this(new CustomComponentProvider());
    }

    public ExecutionContextFactory(ComponentProvider componentProvider) {
        this.componentProvider = componentProvider;
    }

    /**
     * Creates a new {@link RequestExecutionContext} for each of the incoming sync request to the gateway.
     *
     * @param request the request to attach to the context.
     * @param response the response to attach to the context.
     *
     * @return the created {@link RequestExecutionContext}.
     */
    public RequestExecutionContext createRequestContext(Request request, Response response) {
        return new DefaultRequestExecutionContext(request, response, componentProvider, templateVariableProviders);
    }

    /**
     * Creates a new {@link MessageExecutionContext} for each of the incoming async request to the gateway.
     *
     *
     * @param request the request to attach to the context.
     * @param response the response to attach to the context.
     *
     * @return the created {@link MessageExecutionContext}.
     */
    public MessageExecutionContext createMessageContext(Request request, Response response) {
        return new DefaultMessageExecutionContext(request, response, componentProvider, templateVariableProviders);
    }

    public void addTemplateVariableProvider(TemplateVariableProvider templateVariableProvider) {
        this.templateVariableProviders.add(templateVariableProvider);
    }
}
