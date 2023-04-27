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
package io.gravitee.rest.api.management.v4.rest.mapper;

import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.rest.api.management.v4.rest.model.*;
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

@Mapper
public interface ApiMapper {
    ApiMapper INSTANCE = Mappers.getMapper(ApiMapper.class);

    // Api
    default Api convert(GenericApiEntity apiEntity) {
        if (apiEntity == null) {
            return null;
        }
        if (apiEntity.getDefinitionVersion() == io.gravitee.definition.model.DefinitionVersion.V4) {
            return new io.gravitee.rest.api.management.v4.rest.model.Api(this.convert((ApiEntity) apiEntity));
        }
        if (apiEntity.getDefinitionVersion() == io.gravitee.definition.model.DefinitionVersion.V2) {
            return new io.gravitee.rest.api.management.v4.rest.model.Api(this.map((io.gravitee.rest.api.model.api.ApiEntity) apiEntity));
        }
        return null;
    }

    List<Api> convert(List<GenericApiEntity> apiEntities);

    @Mapping(target = "listeners", qualifiedByName = "fromListeners")
    ApiV4 convert(ApiEntity apiEntity);

    @Mapping(target = "listeners", source = "listeners", qualifiedByName = "toListeners")
    NewApiEntity convert(CreateApiV4 api);

    io.gravitee.rest.api.management.v4.rest.model.ApiV2 map(io.gravitee.rest.api.model.api.ApiEntity apiEntity);

    // Flow
    @Mapping(target = "selectors", qualifiedByName = "fromSelectors")
    FlowV4 map(io.gravitee.definition.model.v4.flow.Flow flow);

    @Mapping(target = "selectors", qualifiedByName = "toSelectors")
    io.gravitee.definition.model.v4.flow.Flow map(FlowV4 flow);

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
    io.gravitee.rest.api.management.v4.rest.model.HttpListener map(HttpListener httpListener);
    io.gravitee.rest.api.management.v4.rest.model.SubscriptionListener map(SubscriptionListener subscriptionListener);
    io.gravitee.rest.api.management.v4.rest.model.TcpListener map(TcpListener tcpListener);
    HttpListener map(io.gravitee.rest.api.management.v4.rest.model.HttpListener listener);
    SubscriptionListener map(io.gravitee.rest.api.management.v4.rest.model.SubscriptionListener listener);
    TcpListener map(io.gravitee.rest.api.management.v4.rest.model.TcpListener listener);

    @Named("toListeners")
    default List<Listener> toListeners(List<io.gravitee.rest.api.management.v4.rest.model.Listener> listeners) {
        if (Objects.isNull(listeners)) {
            return new ArrayList<>();
        }
        return listeners
            .stream()
            .map(listener -> {
                if (listener.getActualInstance() instanceof io.gravitee.rest.api.management.v4.rest.model.HttpListener) {
                    return this.map(listener.getHttpListener());
                }
                if (listener.getActualInstance() instanceof io.gravitee.rest.api.management.v4.rest.model.SubscriptionListener) {
                    return this.map(listener.getSubscriptionListener());
                }
                if (listener.getActualInstance() instanceof io.gravitee.rest.api.management.v4.rest.model.TcpListener) {
                    return this.map(listener.getTcpListener());
                }
                return null;
            })
            .collect(Collectors.toList());
    }

    @Named("fromListeners")
    default List<io.gravitee.rest.api.management.v4.rest.model.Listener> fromListeners(List<Listener> listeners) {
        if (Objects.isNull(listeners)) {
            return new ArrayList<>();
        }

        return listeners
            .stream()
            .map(listener -> {
                if (listener.getType() == io.gravitee.definition.model.v4.listener.ListenerType.HTTP) {
                    return new io.gravitee.rest.api.management.v4.rest.model.Listener(this.map((HttpListener) listener));
                }
                if (listener.getType() == io.gravitee.definition.model.v4.listener.ListenerType.SUBSCRIPTION) {
                    return new io.gravitee.rest.api.management.v4.rest.model.Listener(this.map((SubscriptionListener) listener));
                }
                if (listener.getType() == io.gravitee.definition.model.v4.listener.ListenerType.TCP) {
                    return new io.gravitee.rest.api.management.v4.rest.model.Listener(this.map((TcpListener) listener));
                }
                return null;
            })
            .collect(Collectors.toList());
    }

    // Selectors
    HttpSelector map(io.gravitee.definition.model.v4.flow.selector.HttpSelector selector);
    ConditionSelector map(io.gravitee.definition.model.v4.flow.selector.ConditionSelector selector);
    ChannelSelector map(io.gravitee.definition.model.v4.flow.selector.ChannelSelector selector);

    io.gravitee.definition.model.v4.flow.selector.HttpSelector map(HttpSelector selector);
    io.gravitee.definition.model.v4.flow.selector.ConditionSelector map(ConditionSelector selector);
    io.gravitee.definition.model.v4.flow.selector.ChannelSelector map(ChannelSelector selector);

    @Named("toSelectors")
    default List<io.gravitee.definition.model.v4.flow.selector.Selector> toSelectors(List<Selector> selectors) {
        if (Objects.isNull(selectors)) {
            return new ArrayList<>();
        }
        return selectors
            .stream()
            .map(selector -> {
                if (selector == null) {
                    return null;
                }
                if (selector.getActualInstance() instanceof io.gravitee.rest.api.management.v4.rest.model.HttpSelector) {
                    return this.map(selector.getHttpSelector());
                }
                if (selector.getActualInstance() instanceof io.gravitee.rest.api.management.v4.rest.model.ConditionSelector) {
                    return this.map(selector.getConditionSelector());
                }
                if (selector.getActualInstance() instanceof io.gravitee.rest.api.management.v4.rest.model.ChannelSelector) {
                    return this.map(selector.getChannelSelector());
                }
                return null;
            })
            .collect(Collectors.toList());
    }

    @Named("fromSelectors")
    default List<Selector> fromSelectors(List<io.gravitee.definition.model.v4.flow.selector.Selector> selectors) {
        if (Objects.isNull(selectors)) {
            return new ArrayList<>();
        }
        return selectors
            .stream()
            .map(selector -> {
                if (selector == null) {
                    return null;
                }
                if (selector.getType() == SelectorType.HTTP) {
                    return new io.gravitee.rest.api.management.v4.rest.model.Selector(
                        this.map((io.gravitee.definition.model.v4.flow.selector.HttpSelector) selector)
                    );
                }
                if (selector.getType() == SelectorType.CONDITION) {
                    return new io.gravitee.rest.api.management.v4.rest.model.Selector(
                        this.map((io.gravitee.definition.model.v4.flow.selector.ConditionSelector) selector)
                    );
                }
                if (selector.getType() == SelectorType.CHANNEL) {
                    return new io.gravitee.rest.api.management.v4.rest.model.Selector(
                        this.map((io.gravitee.definition.model.v4.flow.selector.ChannelSelector) selector)
                    );
                }
                return null;
            })
            .collect(Collectors.toList());
    }

    // EndpointGroups
    //    @Mapping(target = "sharedConfiguration", qualifiedByName = "toConfiguration")
    //    EndpointGroup map(EndpointGroupV4 endpointGroup);
    //
    //
    //    // Configuration
    //    @Named("toConfiguration")
    //    default String toConfiguration(Object value) {
    //        if (Objects.isNull(value)) {
    //            return null;
    //        }
    //        return value.toString();
    //    }

    // DateTime
    default OffsetDateTime map(Date value) {
        if (Objects.isNull(value)) {
            return null;
        }
        return value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
