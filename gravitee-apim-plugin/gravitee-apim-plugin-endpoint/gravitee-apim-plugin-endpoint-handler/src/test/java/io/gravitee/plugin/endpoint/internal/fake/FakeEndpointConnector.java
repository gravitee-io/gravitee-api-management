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
package io.gravitee.plugin.endpoint.internal.fake;

import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.endpoint.EndpointConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.reactivex.Completable;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

/**
 * @author GraviteeSource Team
 */
@Builder
@Getter
public class FakeEndpointConnector implements EndpointConnector {

    static final ApiType SUPPORTED_API = ApiType.SYNC;
    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.REQUEST_RESPONSE);

    private FakeEndpointConnectorConfiguration configuration;

    @Override
    public ApiType supportedApi() {
        return ApiType.SYNC;
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return Set.of(ConnectorMode.REQUEST_RESPONSE);
    }

    @Override
    public Completable connect(ExecutionContext executionContext) {
        return Completable.complete();
    }
}
