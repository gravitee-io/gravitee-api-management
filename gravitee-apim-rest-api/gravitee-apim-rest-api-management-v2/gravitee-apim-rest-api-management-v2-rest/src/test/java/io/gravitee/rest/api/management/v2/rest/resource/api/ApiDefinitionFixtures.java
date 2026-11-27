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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpoint;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * APIs of each definition type (v4 HTTP, v4 native, v2) carrying every gateway definition detail.
 */
public final class ApiDefinitionFixtures {

    private static final String ENDPOINT = "endpoint";
    private static final String GROUP = "group";
    private static final String PROPERTY_KEY = "key";
    private static final String PROPERTY_VALUE = "value";

    private ApiDefinitionFixtures() {}

    public static ApiEntity httpApi(String id) {
        var api = new ApiEntity();
        api.setId(id);
        api.setName(id);
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.PROXY);
        var listener = new HttpListener();
        listener.setPaths(List.of(new Path("first.host", "/first"), new Path("second.host", "/second")));
        listener.setPathMappings(Set.of("/mapped"));
        api.setListeners(List.of(listener));
        var endpoint = new io.gravitee.definition.model.v4.endpointgroup.Endpoint();
        endpoint.setName(ENDPOINT);
        endpoint.setType("http-proxy");
        endpoint.setConfiguration("{\"target\":\"https://backend.example\"}");
        var group = new io.gravitee.definition.model.v4.endpointgroup.EndpointGroup();
        group.setName(GROUP);
        group.setType("http-proxy");
        group.setEndpoints(List.of(endpoint));
        api.setEndpointGroups(List.of(group));
        api.setProperties(List.of(new Property(PROPERTY_KEY, PROPERTY_VALUE)));
        api.setResources(List.of(new io.gravitee.definition.model.v4.resource.Resource()));
        return api;
    }

    public static NativeApiEntity nativeApi(String id) {
        var api = new NativeApiEntity();
        api.setId(id);
        api.setName(id);
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.NATIVE);
        var listener = new KafkaListener();
        listener.setType(ListenerType.KAFKA);
        listener.setHost("kafka.host");
        listener.setPort(9092);
        api.setListeners(List.of(listener));
        var endpoint = new NativeEndpoint();
        endpoint.setName(ENDPOINT);
        endpoint.setType("native-kafka");
        endpoint.setConfiguration("{\"bootstrapServers\":\"broker.example:9092\"}");
        var group = new NativeEndpointGroup();
        group.setName(GROUP);
        group.setType("native-kafka");
        group.setEndpoints(List.of(endpoint));
        api.setEndpointGroups(List.of(group));
        api.setProperties(List.of(new Property(PROPERTY_KEY, PROPERTY_VALUE)));
        api.setResources(List.of(new io.gravitee.definition.model.v4.resource.Resource()));
        return api;
    }

    public static io.gravitee.rest.api.model.api.ApiEntity v2Api(String id) {
        var api = new io.gravitee.rest.api.model.api.ApiEntity();
        api.setId(id);
        api.setName(id);
        api.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        var endpoint = new Endpoint();
        endpoint.setName(ENDPOINT);
        endpoint.setTarget("https://backend.example");
        var group = new EndpointGroup();
        group.setName(GROUP);
        group.setEndpoints(Set.of(endpoint));
        var proxy = new Proxy();
        proxy.setVirtualHosts(new ArrayList<>(List.of(new VirtualHost("first.host", "/first"), new VirtualHost("second.host", "/second"))));
        proxy.setGroups(Set.of(group));
        api.setProxy(proxy);
        var properties = new Properties();
        properties.setProperties(List.of(new io.gravitee.definition.model.Property(PROPERTY_KEY, PROPERTY_VALUE)));
        api.setProperties(properties);
        return api;
    }
}
