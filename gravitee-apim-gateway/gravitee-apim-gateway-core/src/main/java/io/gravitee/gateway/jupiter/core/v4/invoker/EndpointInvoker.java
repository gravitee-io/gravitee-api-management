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
package io.gravitee.gateway.jupiter.core.v4.invoker;

import static io.gravitee.gateway.jupiter.api.context.ContextAttributes.ATTR_REQUEST_ENDPOINT;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_ENDPOINT_CONNECTOR_ID;
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.util.URIUtils;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.jupiter.api.connector.endpoint.async.EndpointAsyncConnector;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.invoker.Invoker;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.gateway.jupiter.api.qos.QosCapability;
import io.gravitee.gateway.jupiter.api.qos.QosRequirement;
import io.gravitee.gateway.jupiter.core.v4.endpoint.EndpointCriteria;
import io.gravitee.gateway.jupiter.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.jupiter.core.v4.endpoint.ManagedEndpoint;
import io.reactivex.rxjava3.core.Completable;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointInvoker implements Invoker {

    private static final String MATCH_GROUP_ENDPOINT = "endpoint";
    private static final String MATCH_GROUP_PATH = "path";
    private static final Pattern ENDPOINT_PATTERN = Pattern.compile(
        "^(?<" + MATCH_GROUP_ENDPOINT + ">[^:]+):(?<" + MATCH_GROUP_PATH + ">.*)$"
    );

    public static final String NO_ENDPOINT_FOUND_KEY = "NO_ENDPOINT_FOUND";
    public static final String INCOMPATIBLE_QOS_KEY = "INCOMPATIBLE_QOS";
    public static final String INCOMPATIBLE_QOS_CAPABILITIES_KEY = "INCOMPATIBLE_QOS_CAPABILITIES";
    public static final String INVALID_HTTP_METHOD = "INVALID_HTTP_METHOD";

    private final EndpointManager endpointManager;

    public EndpointInvoker(final EndpointManager endpointManager) {
        this.endpointManager = endpointManager;
    }

    @Override
    public String getId() {
        return "endpoint-invoker";
    }

    public Completable invoke(final ExecutionContext ctx) {
        final EndpointConnector endpointConnector = resolveConnector(ctx);

        if (endpointConnector == null) {
            return ctx.interruptWith(
                new ExecutionFailure(HttpStatusCode.NOT_FOUND_404).key(NO_ENDPOINT_FOUND_KEY).message("No endpoint available")
            );
        }

        if (endpointConnector.supportedApi() == ApiType.ASYNC) {
            return validateQoSAndConnect((EndpointAsyncConnector) endpointConnector, ctx);
        } else {
            return connect(endpointConnector, ctx);
        }
    }

    private <T extends EndpointConnector> T resolveConnector(final ExecutionContext ctx) {
        final EntrypointConnector entrypointConnector = ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR);

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

    private Completable validateQoSAndConnect(final EndpointAsyncConnector endpointConnector, final ExecutionContext ctx) {
        final EntrypointAsyncConnector entrypointConnector = ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR);
        final QosRequirement qosRequirement = entrypointConnector.qosRequirement();

        if (qosRequirement == null) {
            return ctx.interruptWith(
                new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
                .message("Invalid entrypoint QoS implementation: qosRequirement cannot be null")
            );
        }

        final Qos requiredQos = qosRequirement.getQos();
        final Set<QosCapability> requiredQosCapabilities = qosRequirement.getCapabilities();
        final Set<QosCapability> qosCapabilities = endpointConnector.supportedQosCapabilities();
        final Set<Qos> supportedQos = endpointConnector.supportedQos();

        if (supportedQos == null || qosCapabilities == null) {
            return ctx.interruptWith(
                new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
                .message("Invalid endpoint QoS implementation: supportedQos or qosCapabilities cannot be null")
            );
        } else if (!supportedQos.contains(requiredQos)) {
            return ctx.interruptWith(
                new ExecutionFailure(HttpStatusCode.BAD_REQUEST_400)
                    .key(INCOMPATIBLE_QOS_KEY)
                    .message("Incompatible Qos between entrypoint and endpoint")
            );
        } else if (!qosCapabilities.containsAll(requiredQosCapabilities)) {
            return ctx.interruptWith(
                new ExecutionFailure(HttpStatusCode.BAD_REQUEST_400)
                    .key(INCOMPATIBLE_QOS_CAPABILITIES_KEY)
                    .message("Incompatible Qos capabilities between entrypoint requirements and endpoint supports")
            );
        }

        return connect(endpointConnector, ctx);
    }

    private Completable connect(final EndpointConnector endpointConnector, final ExecutionContext ctx) {
        return computeRequest(ctx).andThen(endpointConnector.connect(ctx));
    }

    private Completable computeRequest(ExecutionContext ctx) {
        return Completable.defer(
            () -> {
                final Object requestMethodAttribute = ctx.getAttribute(io.gravitee.gateway.api.ExecutionContext.ATTR_REQUEST_METHOD);
                if (requestMethodAttribute != null) {
                    final HttpMethod httpMethod = computeHttpMethodFromAttribute(requestMethodAttribute);
                    if (httpMethod == null) {
                        return ctx.interruptWith(
                            new ExecutionFailure(HttpStatusCode.BAD_REQUEST_400)
                                .key(INVALID_HTTP_METHOD)
                                .message("Http method can not be overridden because ATTR_REQUEST_METHOD attribute is invalid")
                        );
                    } else {
                        ctx.request().method(httpMethod);
                    }
                }
                return Completable.complete();
            }
        );
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
