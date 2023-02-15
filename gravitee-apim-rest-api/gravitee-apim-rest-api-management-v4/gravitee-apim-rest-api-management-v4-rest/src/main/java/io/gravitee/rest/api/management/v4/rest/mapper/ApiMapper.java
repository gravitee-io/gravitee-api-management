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

import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.rest.api.management.v4.rest.model.Api;
import io.gravitee.rest.api.management.v4.rest.model.ApiListenersInner;
import io.gravitee.rest.api.management.v4.rest.model.ResponseTemplate;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import java.util.*;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApiMapper {
    ApiMapper INSTANCE = Mappers.getMapper(ApiMapper.class);

    Api convert(ApiEntity apiEntity);

    Map<String, ResponseTemplate> convertResponseTemplate(Map<String, io.gravitee.definition.model.ResponseTemplate> responseTemplate);

    io.gravitee.rest.api.management.v4.rest.model.HttpListener map(HttpListener httpListener);
    io.gravitee.rest.api.management.v4.rest.model.SubscriptionListener map(SubscriptionListener subscriptionListener);
    io.gravitee.rest.api.management.v4.rest.model.TcpListener map(TcpListener tcpListener);

    default Map<String, Map<String, ResponseTemplate>> map(Map<String, Map<String, io.gravitee.definition.model.ResponseTemplate>> value) {
        if (Objects.isNull(value)) {
            return null;
        }
        Map<String, Map<String, ResponseTemplate>> convertedMap = new HashMap<>();
        value.forEach((key, map) -> convertedMap.put(key, convertResponseTemplate(map)));
        return convertedMap;
    }

    default List<ApiListenersInner> map(List<Listener> listeners) {
        if (Objects.isNull(listeners)) {
            return new ArrayList<>();
        }
        return listeners
            .stream()
            .map(
                listener -> {
                    if (listener instanceof HttpListener) {
                        var httpListener = this.map((HttpListener) listener);
                        return new ApiListenersInner(httpListener);
                    }
                    if (listener instanceof SubscriptionListener) {
                        var subscriptionListener = this.map((SubscriptionListener) listener);
                        return new ApiListenersInner(subscriptionListener);
                    }
                    if (listener instanceof TcpListener) {
                        var tcpListener = this.map((TcpListener) listener);
                        return new ApiListenersInner(tcpListener);
                    }
                    return null;
                }
            )
            .collect(Collectors.toList());
    }
}
