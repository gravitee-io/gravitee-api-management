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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.category.model.ApiCategoryOrder;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.rest.api.management.v2.rest.model.CategoryApi;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CategoryApiMapper {
    CategoryApiMapper INSTANCE = Mappers.getMapper(CategoryApiMapper.class);

    @Mapping(source = "api.id", target = "id")
    @Mapping(source = "api.name", target = "name")
    @Mapping(source = "api.description", target = "description")
    @Mapping(source = "api.version", target = "apiVersion")
    @Mapping(source = "api.definitionVersion", target = "definitionVersion")
    @Mapping(source = "apiCategoryOrder.order", target = "order")
    @Mapping(target = "accessPaths", expression = "java(computeAccessPaths(api))")
    CategoryApi map(ApiCategoryOrder apiCategoryOrder, Api api);

    @Named("computeAccessPaths")
    default List<String> computeAccessPaths(Api api) {
        if (Objects.isNull(api)) {
            return List.of();
        }

        switch (api.getDefinitionVersion()) {
            case V4 -> {
                if (Objects.nonNull(api.getApiDefinitionHttpV4()) && Objects.nonNull(api.getApiDefinitionHttpV4().getListeners())) {
                    Stream<String> tcpListenerHostsStream = api
                        .getApiDefinitionHttpV4()
                        .getListeners()
                        .stream()
                        .filter(listener -> ListenerType.TCP.equals(listener.getType()))
                        .map(TcpListener.class::cast)
                        .flatMap(listener -> listener.getHosts().stream());

                    Stream<String> httpListenerHostsStream = api
                        .getApiDefinitionHttpV4()
                        .getListeners()
                        .stream()
                        .filter(listener -> ListenerType.HTTP.equals(listener.getType()))
                        .map(HttpListener.class::cast)
                        .filter(listener -> Objects.nonNull(listener.getPaths()))
                        .flatMap(listener -> listener.getPaths().stream())
                        .map(pathV4 -> Optional.ofNullable(pathV4.getHost()).orElse("") + Optional.ofNullable(pathV4.getPath()).orElse(""));

                    return Stream.concat(tcpListenerHostsStream, httpListenerHostsStream).toList();
                }
            }
            case V2 -> {
                if (
                    Objects.nonNull(api.getApiDefinition()) &&
                    Objects.nonNull(api.getApiDefinition().getProxy()) &&
                    Objects.nonNull(api.getApiDefinition().getProxy().getVirtualHosts())
                ) {
                    var virtualHosts = api.getApiDefinition().getProxy().getVirtualHosts();
                    if (Objects.nonNull(virtualHosts)) {
                        return virtualHosts
                            .stream()
                            .map(virtualHost ->
                                Optional.ofNullable(virtualHost.getHost()).orElse("") +
                                Optional.ofNullable(virtualHost.getPath()).orElse("")
                            )
                            .toList();
                    }
                }
            }
        }
        return List.of();
    }
}
