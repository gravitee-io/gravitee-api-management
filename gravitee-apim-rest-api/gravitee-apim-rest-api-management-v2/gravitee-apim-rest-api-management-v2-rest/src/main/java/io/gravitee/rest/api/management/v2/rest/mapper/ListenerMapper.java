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

import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.definition.model.v4.nativeapi.NativeListener;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { CorsMapper.class, EndpointMapper.class, EntrypointMapper.class })
public interface ListenerMapper {
    ListenerMapper INSTANCE = Mappers.getMapper(ListenerMapper.class);

    io.gravitee.rest.api.management.v2.rest.model.HttpListener map(HttpListener httpListener);
    io.gravitee.rest.api.management.v2.rest.model.SubscriptionListener map(SubscriptionListener subscriptionListener);
    io.gravitee.rest.api.management.v2.rest.model.TcpListener map(TcpListener tcpListener);
    io.gravitee.rest.api.management.v2.rest.model.KafkaListener map(KafkaListener kafkaListener);

    @Mapping(target = "pathMappingsPattern", source = "pathMappings", qualifiedByName = "toPathMappingsPattern")
    HttpListener map(io.gravitee.rest.api.management.v2.rest.model.HttpListener listener);

    SubscriptionListener map(io.gravitee.rest.api.management.v2.rest.model.SubscriptionListener listener);
    TcpListener map(io.gravitee.rest.api.management.v2.rest.model.TcpListener listener);
    KafkaListener map(io.gravitee.rest.api.management.v2.rest.model.KafkaListener listener);

    @Named("toPathMappingsPattern")
    default Map<String, Pattern> toPathMappingsPattern(List<String> pathMappings) {
        if (Objects.isNull(pathMappings)) {
            return null;
        }
        return pathMappings.stream().collect(Collectors.toMap(pathMapping -> pathMapping, pathMapping -> Pattern.compile(pathMapping)));
    }

    @Named("toHttpListeners")
    default List<Listener> mapToListenerEntityV4List(List<io.gravitee.rest.api.management.v2.rest.model.Listener> listeners) {
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

    @Named("fromHttpListeners")
    default List<io.gravitee.rest.api.management.v2.rest.model.Listener> mapFromListenerEntityV4List(List<Listener> listeners) {
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

    @Named("toNativeListeners")
    default List<NativeListener> mapToNativeListenerV4List(List<io.gravitee.rest.api.management.v2.rest.model.Listener> listeners) {
        if (Objects.isNull(listeners)) {
            return new ArrayList<>();
        }
        return listeners
            .stream()
            .map(listener -> {
                if (listener.getActualInstance() instanceof io.gravitee.rest.api.management.v2.rest.model.KafkaListener) {
                    return this.map(listener.getKafkaListener());
                }
                return null;
            })
            .collect(Collectors.toList());
    }

    @Named("fromNativeListeners")
    default List<io.gravitee.rest.api.management.v2.rest.model.Listener> mapFromNativeListenerV4List(List<NativeListener> listeners) {
        if (Objects.isNull(listeners)) {
            return new ArrayList<>();
        }

        return listeners
            .stream()
            .map(listener -> {
                if (listener.getType() == io.gravitee.definition.model.v4.listener.ListenerType.KAFKA) {
                    return new io.gravitee.rest.api.management.v2.rest.model.Listener(this.map((KafkaListener) listener));
                }
                return null;
            })
            .collect(Collectors.toList());
    }
}
