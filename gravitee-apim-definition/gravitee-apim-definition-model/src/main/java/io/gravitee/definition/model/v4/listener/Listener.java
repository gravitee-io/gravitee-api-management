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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = HttpListener.class, name = HTTP_LABEL),
        @JsonSubTypes.Type(value = SubscriptionListener.class, name = SUBSCRIPTION_LABEL),
        @JsonSubTypes.Type(value = TcpListener.class, name = TCP_LABEL),
    }
)
@Schema(
    name = "ListenerV4",
    discriminatorProperty = "type",
    discriminatorMapping = {
        @DiscriminatorMapping(value = HTTP_LABEL, schema = HttpListener.class),
        @DiscriminatorMapping(value = TCP_LABEL, schema = TcpListener.class),
        @DiscriminatorMapping(value = SUBSCRIPTION_LABEL, schema = SubscriptionListener.class),
    },
    oneOf = { HttpListener.class, TcpListener.class, SubscriptionListener.class }
)
@SuperBuilder(toBuilder = true)
public abstract class Listener extends AbstractListener<Entrypoint> {

    public static final String HTTP_LABEL = "http";
    public static final String SUBSCRIPTION_LABEL = "subscription";
    public static final String TCP_LABEL = "tcp";

    public Listener(ListenerType type) {
        super(type);
    }

    public Listener(ListenerType type, Listener.ListenerBuilder<?, ?> b) {
        super(type, b);
    }
}
