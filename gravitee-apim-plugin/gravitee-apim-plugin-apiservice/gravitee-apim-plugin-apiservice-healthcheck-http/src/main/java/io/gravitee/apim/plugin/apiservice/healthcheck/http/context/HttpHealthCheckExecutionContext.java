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
package io.gravitee.apim.plugin.apiservice.healthcheck.http.context;

import io.gravitee.apim.plugin.apiservice.healthcheck.http.HttpHealthCheckServiceConfiguration;
import io.gravitee.common.util.URIUtils;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.context.TlsSession;
import io.gravitee.gateway.reactive.core.context.AbstractExecutionContext;
import io.gravitee.gateway.reactive.handlers.api.el.ApiTemplateVariableProvider;
import io.gravitee.gateway.reactive.handlers.api.el.ContentTemplateVariableProvider;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.reporter.api.v4.metric.Metrics;
import java.util.List;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpHealthCheckExecutionContext extends AbstractExecutionContext<HttpHealthCheckRequest, HttpHealthCheckResponse> {

    private final DeploymentContext deploymentContext;

    public HttpHealthCheckExecutionContext(
        final HttpHealthCheckServiceConfiguration configuration,
        final DeploymentContext deploymentContext
    ) {
        super(new HttpHealthCheckRequest(configuration), new HttpHealthCheckResponse());
        setAttribute(ContextAttributes.ATTR_REQUEST_ENDPOINT, configuration.getTarget());
        // Target is a completely different url or overrides the endpoint path.
        setAttribute(
            ContextAttributes.ATTR_REQUEST_ENDPOINT_OVERRIDE,
            URIUtils.isAbsolute(configuration.getTarget()) || configuration.isOverrideEndpointPath()
        );

        this.metrics = Metrics.builder().build();
        this.deploymentContext = deploymentContext;
        var api = deploymentContext.getComponent(Api.class);
        this.templateVariableProviders = List.of(new ContentTemplateVariableProvider(), new ApiTemplateVariableProvider(api));

        if (configuration.getHeaders() != null && !configuration.getHeaders().isEmpty()) {
            // evaluate headers against templateProviders
            configuration
                .getHeaders()
                .forEach(header ->
                    request.headers().set(header.getName(), getTemplateEngine().eval(header.getValue(), String.class).blockingGet())
                );
        }
    }

    @Override
    public <T> T getComponent(Class<T> componentClass) {
        return deploymentContext.getComponent(componentClass);
    }

    @Override
    public String remoteAddress() {
        return this.request.remoteAddress();
    }

    @Override
    public String localAddress() {
        return this.request.localAddress();
    }

    @Override
    public TlsSession tlsSession() {
        return this.request.tlsSession();
    }
}
