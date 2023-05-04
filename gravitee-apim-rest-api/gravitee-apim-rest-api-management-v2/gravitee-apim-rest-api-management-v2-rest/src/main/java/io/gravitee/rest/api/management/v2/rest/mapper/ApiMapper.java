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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { DateMapper.class, FlowMapper.class })
public interface ApiMapper {
    ApiMapper INSTANCE = Mappers.getMapper(ApiMapper.class);

    // Api
    default Api convert(GenericApiEntity apiEntity) {
        if (apiEntity == null) {
            return null;
        }
        if (apiEntity.getDefinitionVersion() == io.gravitee.definition.model.DefinitionVersion.V4) {
            return new io.gravitee.rest.api.management.v2.rest.model.Api(this.convert((ApiEntity) apiEntity));
        }
        if (apiEntity.getDefinitionVersion() == io.gravitee.definition.model.DefinitionVersion.V2) {
            return new io.gravitee.rest.api.management.v2.rest.model.Api(this.map((io.gravitee.rest.api.model.api.ApiEntity) apiEntity));
        }
        return null;
    }

    List<Api> convert(List<GenericApiEntity> apiEntities);

    @Mapping(target = "listeners", qualifiedByName = "fromListeners")
    ApiV4 convert(ApiEntity apiEntity);

    @Mapping(target = "listeners", source = "listeners", qualifiedByName = "toListeners")
    NewApiEntity convert(CreateApiV4 api);

    io.gravitee.rest.api.management.v2.rest.model.ApiV2 map(io.gravitee.rest.api.model.api.ApiEntity apiEntity);

    // DefinitionVersion
    io.gravitee.definition.model.DefinitionVersion map(DefinitionVersion definitionVersion);

    // DefinitionContext
    default DefinitionContext map(io.gravitee.definition.model.DefinitionContext definitionContext) {
        if (definitionContext == null) {
            return null;
        }
        DefinitionContext context = new DefinitionContext();
        context.setOrigin(DefinitionContext.OriginEnum.fromValue(definitionContext.getOrigin()));
        context.setMode(DefinitionContext.ModeEnum.fromValue(definitionContext.getMode()));
        return context;
    }

    // ResponseTemplate
    Map<String, ResponseTemplate> convertResponseTemplate(Map<String, io.gravitee.definition.model.ResponseTemplate> responseTemplate);

    List<Property> map(List<io.gravitee.definition.model.Property> properties);

    default List<Property> map(io.gravitee.definition.model.Properties properties) {
        if (properties == null) {
            return null;
        }
        return this.map(properties.getProperties());
    }

    default Map<String, Map<String, ResponseTemplate>> map(Map<String, Map<String, io.gravitee.definition.model.ResponseTemplate>> value) {
        if (Objects.isNull(value)) {
            return null;
        }
        Map<String, Map<String, ResponseTemplate>> convertedMap = new HashMap<>();
        value.forEach((key, map) -> convertedMap.put(key, convertResponseTemplate(map)));
        return convertedMap;
    }

    // Listeners
    io.gravitee.rest.api.management.v2.rest.model.HttpListener map(HttpListener httpListener);
    io.gravitee.rest.api.management.v2.rest.model.SubscriptionListener map(SubscriptionListener subscriptionListener);
    io.gravitee.rest.api.management.v2.rest.model.TcpListener map(TcpListener tcpListener);
    HttpListener map(io.gravitee.rest.api.management.v2.rest.model.HttpListener listener);
    SubscriptionListener map(io.gravitee.rest.api.management.v2.rest.model.SubscriptionListener listener);
    TcpListener map(io.gravitee.rest.api.management.v2.rest.model.TcpListener listener);

    @Named("toListeners")
    default List<Listener> toListeners(List<io.gravitee.rest.api.management.v2.rest.model.Listener> listeners) {
        if (Objects.isNull(listeners)) {
            return new ArrayList<>();
        }
        return listeners
            .stream()
            .map(listener -> {
                if (listener.getActualInstance() instanceof io.gravitee.rest.api.management.v2.rest.model.HttpListener) {
                    return this.map(listener.getHttpListener());
                }
                if (listener.getActualInstance() instanceof io.gravitee.rest.api.management.v2.rest.model.SubscriptionListener) {
                    return this.map(listener.getSubscriptionListener());
                }
                if (listener.getActualInstance() instanceof io.gravitee.rest.api.management.v2.rest.model.TcpListener) {
                    return this.map(listener.getTcpListener());
                }
                return null;
            })
            .collect(Collectors.toList());
    }

    @Named("fromListeners")
    default List<io.gravitee.rest.api.management.v2.rest.model.Listener> fromListeners(List<Listener> listeners) {
        if (Objects.isNull(listeners)) {
            return new ArrayList<>();
        }

        return listeners
            .stream()
            .map(listener -> {
                if (listener.getType() == io.gravitee.definition.model.v4.listener.ListenerType.HTTP) {
                    return new io.gravitee.rest.api.management.v2.rest.model.Listener(this.map((HttpListener) listener));
                }
                if (listener.getType() == io.gravitee.definition.model.v4.listener.ListenerType.SUBSCRIPTION) {
                    return new io.gravitee.rest.api.management.v2.rest.model.Listener(this.map((SubscriptionListener) listener));
                }
                if (listener.getType() == io.gravitee.definition.model.v4.listener.ListenerType.TCP) {
                    return new io.gravitee.rest.api.management.v2.rest.model.Listener(this.map((TcpListener) listener));
                }
                return null;
            })
            .collect(Collectors.toList());
    }
}
