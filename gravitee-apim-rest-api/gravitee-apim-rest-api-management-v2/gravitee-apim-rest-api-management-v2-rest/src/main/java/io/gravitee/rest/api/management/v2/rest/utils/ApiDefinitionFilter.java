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
package io.gravitee.rest.api.management.v2.rest.utils;

import static io.gravitee.apim.core.utils.CollectionUtils.isNotEmpty;
import static io.gravitee.apim.core.utils.CollectionUtils.stream;
import static java.util.Collections.singletonList;

import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiDefinitionFilter {

    /**
     * Removes the gateway definition details (endpoints, properties, services, resources...) from an API
     * returned to a caller who can see the API but cannot read its definition.
     */
    public static void filterSensitiveData(GenericApiEntity apiEntity) {
        if (apiEntity instanceof ApiEntity asApiEntity) {
            filterSensitiveData(asApiEntity);
        } else if (apiEntity instanceof NativeApiEntity asNativeApiEntity) {
            filterSensitiveData(asNativeApiEntity);
        } else if (apiEntity instanceof io.gravitee.rest.api.model.api.ApiEntity asApiEntityV2) {
            filterSensitiveData(asApiEntityV2);
        }
    }

    private static void filterSensitiveData(ApiEntity apiEntity) {
        List<Listener> listeners = apiEntity.getListeners();

        stream(listeners)
            .filter(listener -> ListenerType.HTTP == listener.getType())
            .findFirst()
            .ifPresent(first -> {
                if (first instanceof HttpListener httpListener) {
                    if (isNotEmpty(httpListener.getPaths())) {
                        var path = httpListener.getPaths().getFirst();
                        var filteredPath = new io.gravitee.definition.model.v4.listener.http.Path(path.getPath());
                        httpListener.setPaths(List.of(filteredPath));
                    }
                    httpListener.setPathMappings(null);
                }
            });
        apiEntity.setEndpointGroups(null);
        apiEntity.setProperties(null);
        apiEntity.setServices(null);
        apiEntity.setResources(null);
        apiEntity.setResponseTemplates(null);
    }

    private static void filterSensitiveData(NativeApiEntity apiEntity) {
        apiEntity.setEndpointGroups(null);
        apiEntity.setProperties(null);
        apiEntity.setServices(null);
        apiEntity.setResources(null);
    }

    private static void filterSensitiveData(io.gravitee.rest.api.model.api.ApiEntity apiEntity) {
        final Proxy filteredProxy = new Proxy();
        Optional.ofNullable(apiEntity.getProxy())
            .map(Proxy::getVirtualHosts)
            .filter(virtualHosts -> !virtualHosts.isEmpty())
            .map(List::getFirst)
            .ifPresent(virtualHost -> {
                virtualHost.setHost(null);
                filteredProxy.setVirtualHosts(singletonList(virtualHost));
            });

        apiEntity.setProxy(filteredProxy);
        apiEntity.setPaths(null);
        apiEntity.setProperties(null);
        apiEntity.setServices(null);
        apiEntity.setResources(null);
        apiEntity.setPathMappings(null);
        apiEntity.setResponseTemplates(null);
    }
}
