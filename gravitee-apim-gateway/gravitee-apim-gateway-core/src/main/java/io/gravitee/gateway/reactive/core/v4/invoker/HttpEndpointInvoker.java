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
package io.gravitee.gateway.reactive.core.v4.invoker;

import static io.gravitee.gateway.reactive.api.context.ContextAttributes.ATTR_REQUEST_ENDPOINT;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_ENDPOINT_CONNECTOR_ID;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.util.URIUtils;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.reactive.api.connector.endpoint.HttpEndpointConnector;
import io.gravitee.gateway.reactive.api.connector.entrypoint.HttpEntrypointConnector;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.invoker.HttpInvoker;
import io.gravitee.gateway.reactive.api.invoker.Invoker;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointCriteria;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import io.reactivex.rxjava3.core.Completable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpEndpointInvoker implements HttpInvoker, Invoker {

    private static final String MATCH_GROUP_ENDPOINT = "endpoint";
    private static final String MATCH_GROUP_PATH = "path";
    private static final Pattern ENDPOINT_PATTERN = Pattern.compile(
        "^(?<" + MATCH_GROUP_ENDPOINT + ">[^:]+):(?<" + MATCH_GROUP_PATH + ">.*)$"
    );

    public static final String NO_ENDPOINT_FOUND_KEY = "NO_ENDPOINT_FOUND";
    public static final String INVALID_HTTP_METHOD = "INVALID_HTTP_METHOD";

    private final EndpointManager endpointManager;

    public HttpEndpointInvoker(final EndpointManager endpointManager) {
        this.endpointManager = endpointManager;
    }

    @Override
    public String getId() {
        return "endpoint-invoker";
    }

    @Override
    public Completable invoke(ExecutionContext executionContext) {
        return invoke((HttpExecutionContext) executionContext);
    }

    @Override
    public Completable invoke(final HttpExecutionContext ctx) {
        final HttpEndpointConnector endpointConnector = resolveConnector(ctx);

        if (endpointConnector == null) {
            return ctx.interruptWith(new ExecutionFailure(HttpStatusCode.SERVICE_UNAVAILABLE_503).key(NO_ENDPOINT_FOUND_KEY));
        }

        return connect(((EndpointConnector) endpointConnector), ((ExecutionContext) ctx));
    }

    private <T extends HttpEndpointConnector> T resolveConnector(final HttpExecutionContext ctx) {
        final HttpEntrypointConnector entrypointConnector = ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR);

        final EndpointCriteria endpointCriteria = new EndpointCriteria(
            entrypointConnector.supportedApi(),
            entrypointConnector.supportedModes()
        );

        final String endpointTarget = ctx.getAttribute(ATTR_REQUEST_ENDPOINT);

        if (endpointTarget != null) {
            final String evaluatedTarget = ctx.getTemplateEngine().getValue(endpointTarget, String.class);
            if (URIUtils.isAbsolute(evaluatedTarget)) {
                ctx.setAttribute(ATTR_REQUEST_ENDPOINT, evaluatedTarget);
            } else {
                final Matcher matcher = ENDPOINT_PATTERN.matcher(evaluatedTarget);

                if (matcher.matches()) {
                    // Set endpoint name into the criteria.
                    endpointCriteria.setName(matcher.group(MATCH_GROUP_ENDPOINT));

                    // Replace the attribute to remove the endpoint reference part ("my-endpoint:/foo/bar' -> '/foo/bar').
                    ctx.setAttribute(ATTR_REQUEST_ENDPOINT, matcher.group(MATCH_GROUP_PATH));
                }
            }
        }

        final ManagedEndpoint managedEndpoint = endpointManager.next(endpointCriteria);

        if (managedEndpoint != null) {
            EndpointConnector endpointConnector = managedEndpoint.getConnector();
            ctx.setInternalAttribute(ATTR_INTERNAL_ENDPOINT_CONNECTOR_ID, endpointConnector.id());
            return (T) endpointConnector;
        }

        return null;
    }

    // Do not remove this signature until all connectors are migrated to HttpEndpointConnectors#connect(HttpExecutionContext ctx)
    protected Completable connect(final EndpointConnector endpointConnector, final ExecutionContext ctx) {
        final Object requestMethodAttribute = ctx.getAttribute(io.gravitee.gateway.api.ExecutionContext.ATTR_REQUEST_METHOD);
        if (requestMethodAttribute != null) {
            final HttpMethod httpMethod = computeHttpMethodFromAttribute(requestMethodAttribute);
            if (httpMethod == null) {
                return ctx.interruptWith(
                    new ExecutionFailure(HttpStatusCode.BAD_REQUEST_400)
                        .key(INVALID_HTTP_METHOD)
                        .message("Http method cannot be overridden because ATTR_REQUEST_METHOD attribute is invalid")
                );
            } else {
                ctx.request().method(httpMethod);
            }
        }
        return endpointConnector.connect(ctx);
    }

    @SuppressWarnings("unchecked")
    private HttpMethod computeHttpMethodFromAttribute(Object attributeMethod) {
        if (attributeMethod instanceof HttpMethod) {
            return (HttpMethod) attributeMethod;
        } else if (attributeMethod instanceof io.vertx.core.http.HttpMethod) {
            return HttpMethod.valueOf(((io.vertx.core.http.HttpMethod) attributeMethod).name());
        } else if (attributeMethod instanceof String) {
            return HttpMethod.valueOf((String) attributeMethod);
        } else {
            return null;
        }
    }
}
