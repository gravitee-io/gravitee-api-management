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
package io.gravitee.plugin.endpoint.internal.fake;

import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.endpoint.async.EndpointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.gateway.jupiter.api.qos.QosCapability;
import io.reactivex.rxjava3.core.Completable;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

/**
 * @author GraviteeSource Team
 */
@Builder
@Getter
public class FakeEndpointConnector extends EndpointAsyncConnector {

    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.REQUEST_RESPONSE);
    static final Set<Qos> SUPPORTED_QOS = Set.of(Qos.values());
    static final Set<QosCapability> SUPPORTED_QOS_CAPABILITIES = Set.of(QosCapability.values());

    private FakeEndpointConnectorConfiguration configuration;

    @Override
    public String id() {
        return "fake";
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return SUPPORTED_MODES;
    }

    @Override
    public Set<Qos> supportedQos() {
        return SUPPORTED_QOS;
    }

    @Override
    public Set<QosCapability> supportedQosCapabilities() {
        return SUPPORTED_QOS_CAPABILITIES;
    }

    @Override
    public Completable subscribe(ExecutionContext ctx) {
        return Completable.complete();
    }

    @Override
    public Completable publish(ExecutionContext ctx) {
        return Completable.complete();
    }
}
