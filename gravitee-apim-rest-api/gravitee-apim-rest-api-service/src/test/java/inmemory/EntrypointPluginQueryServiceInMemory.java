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
package inmemory;

import static java.util.stream.Collectors.toSet;

import io.gravitee.apim.core.plugin.model.ConnectorPlugin;
import io.gravitee.apim.core.plugin.query_service.EntrypointPluginQueryService;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.ConnectorMode;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Qos;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntrypointPluginQueryServiceInMemory implements EntrypointPluginQueryService, InMemoryAlternative<ConnectorPlugin> {

    public static final String HTTP_PROXY_CONNECTOR_ID = "http-proxy";
    public static final String SSE_CONNECTOR_ID = "sse";
    public static final String MOCK_CONNECTOR_ID = "mock";
    private static final List<ConnectorPlugin> DEFAULT_LIST = List.of(
        ConnectorPlugin.builder()
            .id(HTTP_PROXY_CONNECTOR_ID)
            .name("HTTP Proxy")
            .version("1.0.0")
            .supportedApiType(ApiType.PROXY)
            .supportedListenerType(ListenerType.HTTP)
            .supportedModes(Set.of(ConnectorMode.REQUEST_RESPONSE))
            .feature("apim-proxy")
            .deployed(true)
            .build(),
        ConnectorPlugin.builder()
            .id(SSE_CONNECTOR_ID)
            .name("SSE Entrypoint")
            .version("1.0.0")
            .supportedApiType(ApiType.MESSAGE)
            .supportedListenerType(ListenerType.HTTP)
            .supportedModes(Set.of(ConnectorMode.SUBSCRIBE))
            .supportedQos(Set.of(Qos.AUTO))
            .feature("apim-en-connector-sse")
            .deployed(true)
            .build(),
        ConnectorPlugin.builder()
            .id(MOCK_CONNECTOR_ID)
            .name("Mock Endpoint")
            .version("1.0.0")
            .supportedApiType(ApiType.MESSAGE)
            .supportedListenerType(ListenerType.HTTP)
            .supportedModes(Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE))
            .supportedQos(Set.of(Qos.AUTO))
            .feature("apim-en-connector-mock")
            .deployed(false)
            .build()
    );

    private List<ConnectorPlugin> storage = DEFAULT_LIST;

    @Override
    public Set<ConnectorPlugin> findBySupportedApi(ApiType apiType) {
        return storage
            .stream()
            .filter(connectorPlugin -> connectorPlugin.getSupportedApiType().equals(apiType))
            .collect(toSet());
    }

    @Override
    public Set<ConnectorPlugin> findAll() {
        return new HashSet<>(storage);
    }

    @Override
    public void initWith(List<ConnectorPlugin> items) {
        this.storage = items;
    }

    @Override
    public void reset() {
        this.storage = DEFAULT_LIST;
    }

    @Override
    public List<ConnectorPlugin> storage() {
        return Collections.unmodifiableList(storage);
    }
}
