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
package io.gravitee.plugin.entrypoint.internal.fake;

import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.ListenerType;
import io.gravitee.gateway.reactive.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.gateway.reactive.api.qos.QosRequirement;
import io.reactivex.rxjava3.core.Completable;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@Getter
public class FakeEntrypointConnector extends EntrypointAsyncConnector {

    static final ApiType SUPPORTED_API = ApiType.MESSAGE;
    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.SUBSCRIBE);
    static final Set<Qos> SUPPORTED_QOS = Set.of(Qos.values());
    static final ListenerType SUPPORTED_LISTENER_TYPE = ListenerType.HTTP;

    private FakeEntrypointConnectorConfiguration configuration;

    @Override
    public String id() {
        return "fake";
    }

    @Override
    public ListenerType supportedListenerType() {
        return SUPPORTED_LISTENER_TYPE;
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return SUPPORTED_MODES;
    }

    @Override
    public int matchCriteriaCount() {
        return 0;
    }

    @Override
    public boolean matches(final ExecutionContext executionContext) {
        return true;
    }

    @Override
    public Completable handleRequest(final ExecutionContext executionContext) {
        return Completable.complete();
    }

    @Override
    public Completable handleResponse(final ExecutionContext executionContext) {
        return Completable.complete();
    }

    @Override
    public QosRequirement qosRequirement() {
        return QosRequirement.builder().build();
    }
}
