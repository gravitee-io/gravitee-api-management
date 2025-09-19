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
package io.gravitee.definition.model.v4.listener;

import static io.gravitee.definition.model.v4.listener.Listener.HTTP_LABEL;
import static io.gravitee.definition.model.v4.listener.Listener.SUBSCRIPTION_LABEL;
import static io.gravitee.definition.model.v4.listener.Listener.TCP_LABEL;
import static io.gravitee.definition.model.v4.nativeapi.NativeListener.KAFKA_LABEL;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.listener.entrypoint.AbstractEntrypoint;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.definition.model.v4.nativeapi.NativeEntrypoint;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = HttpListener.class, name = "HTTP"),
        @JsonSubTypes.Type(value = HttpListener.class, name = HTTP_LABEL),
        @JsonSubTypes.Type(value = SubscriptionListener.class, name = "SUBSCRIPTION"),
        @JsonSubTypes.Type(value = SubscriptionListener.class, name = SUBSCRIPTION_LABEL),
        @JsonSubTypes.Type(value = TcpListener.class, name = "TCP"),
        @JsonSubTypes.Type(value = TcpListener.class, name = TCP_LABEL),
        @JsonSubTypes.Type(value = KafkaListener.class, name = "KAFKA"),
        @JsonSubTypes.Type(value = KafkaListener.class, name = KAFKA_LABEL),
    }
)
public class AbstractListener<E extends AbstractEntrypoint> implements Serializable {

    @JsonProperty(required = true)
    @NotNull
    protected ListenerType type;

    @NotEmpty
    protected List<E> entrypoints;

    protected List<String> servers;

    protected AbstractListener(ListenerType type) {
        this.type = type;
    }

    protected AbstractListener(ListenerType type, AbstractListener.AbstractListenerBuilder<E, ?, ?> b) {
        this.type = type;
        this.entrypoints = b.entrypoints;
        this.servers = b.servers;
    }

    @JsonIgnore
    public List<Plugin> getPlugins() {
        return Optional.ofNullable(this.entrypoints)
            .map(e -> e.stream().map(AbstractEntrypoint::getPlugins).flatMap(List::stream).collect(Collectors.toList()))
            .orElse(List.of());
    }
}
