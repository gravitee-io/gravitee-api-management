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
package io.gravitee.gateway.jupiter.core.v4.invoker;

import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.jupiter.api.connector.endpoint.async.EndpointAsyncConnector;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.invoker.Invoker;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.gateway.jupiter.api.qos.QosCapability;
import io.gravitee.gateway.jupiter.api.qos.QosRequirement;
import io.gravitee.gateway.jupiter.core.v4.endpoint.DefaultEndpointConnectorResolver;
import io.reactivex.rxjava3.core.Completable;
import java.util.Set;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointInvoker extends AbstractService<EndpointInvoker> implements Invoker {

    public static final String NO_ENDPOINT_FOUND_KEY = "NO_ENDPOINT_FOUND";
    public static final String INCOMPATIBLE_QOS_KEY = "INCOMPATIBLE_QOS";
    public static final String INCOMPATIBLE_QOS_CAPABILITIES_KEY = "INCOMPATIBLE_QOS_CAPABILITIES";

    private final DefaultEndpointConnectorResolver endpointResolver;

    public EndpointInvoker(final DefaultEndpointConnectorResolver endpointResolver) {
        this.endpointResolver = endpointResolver;
    }

    @Override
    public String getId() {
        return "endpoint-invoker";
    }

    public Completable invoke(ExecutionContext ctx) {
        final EndpointConnector endpointConnector = endpointResolver.resolve(ctx);

        if (endpointConnector == null) {
            return ctx.interruptWith(
                new ExecutionFailure(HttpStatusCode.NOT_FOUND_404).key(NO_ENDPOINT_FOUND_KEY).message("No endpoint available")
            );
        }
        if (endpointConnector.supportedApi() == ApiType.ASYNC) {
            EndpointAsyncConnector endpointAsyncConnector = (EndpointAsyncConnector) endpointConnector;
            EntrypointAsyncConnector entrypointAsyncConnector = ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR);
            QosRequirement qosRequirement = entrypointAsyncConnector.qosRequirement();

            if (qosRequirement == null) {
                return ctx.interruptWith(
                    new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
                        .message("Invalid entrypoint QoS implementation: qosRequirement cannot be null")
                );
            }

            Qos requiredQos = qosRequirement.getQos();
            Set<QosCapability> requiredQosCapabilities = qosRequirement.getCapabilities();
            Set<QosCapability> qosCapabilities = endpointAsyncConnector.supportedQosCapabilities();
            if (endpointAsyncConnector.supportedQos() == null || qosCapabilities == null) {
                return ctx.interruptWith(
                    new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
                        .message("Invalid endpoint QoS implementation: supportedQos cannot be null")
                );
            } else if (!endpointAsyncConnector.supportedQos().contains(requiredQos)) {
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
        }
        return endpointConnector.connect(ctx);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        endpointResolver.stop();
    }

    @Override
    public EndpointInvoker preStop() throws Exception {
        super.preStop();
        endpointResolver.preStop();
        return this;
    }
}
