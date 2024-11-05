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
package io.gravitee.gateway.reactive.core.context;

import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.reactive.api.context.TlsSession;
import io.gravitee.reporter.api.v4.metric.Metrics;
import java.util.Collection;

/**
 * Default implementation of {@link io.gravitee.gateway.reactive.api.context.ExecutionContext} to use when handling requests.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultExecutionContext extends AbstractExecutionContext<MutableRequest, MutableResponse> implements MutableExecutionContext {

    public DefaultExecutionContext(final MutableRequest request, final MutableResponse response) {
        super(request, response);
    }

    @Override
    public DefaultExecutionContext request(Request request) {
        return this;
    }

    @Override
    public DefaultExecutionContext response(Response response) {
        return this;
    }

    @Override
    public DefaultExecutionContext metrics(final Metrics metrics) {
        this.metrics = metrics;
        return this;
    }

    @Override
    public long timestamp() {
        return this.request.timestamp();
    }

    public DefaultExecutionContext componentProvider(final ComponentProvider componentProvider) {
        this.componentProvider = componentProvider;
        return this;
    }

    public DefaultExecutionContext templateVariableProviders(final Collection<TemplateVariableProvider> templateVariableProviders) {
        this.templateVariableProviders = templateVariableProviders;
        return this;
    }

    public Collection<TemplateVariableProvider> templateVariableProviders() {
        return templateVariableProviders;
    }
}
