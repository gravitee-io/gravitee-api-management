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
package io.gravitee.definition.model.v4.listener;

import static io.gravitee.definition.model.v4.listener.Listener.HTTP_LABEL;
import static io.gravitee.definition.model.v4.listener.Listener.SUBSCRIPTION_LABEL;
import static io.gravitee.definition.model.v4.listener.Listener.TCP_LABEL;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.gravitee.definition.model.v4.listener.http.ListenerHttp;
import io.gravitee.definition.model.v4.listener.subscription.ListenerSubscription;
import io.gravitee.definition.model.v4.listener.tcp.ListenerTcp;
import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@FieldNameConstants
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = Listener.Fields.type)
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = ListenerHttp.class, name = HTTP_LABEL),
        @JsonSubTypes.Type(value = ListenerSubscription.class, name = SUBSCRIPTION_LABEL),
        @JsonSubTypes.Type(value = ListenerTcp.class, name = TCP_LABEL),
    }
)
public abstract class Listener implements Serializable {

    public static final String HTTP_LABEL = "http";
    public static final String SUBSCRIPTION_LABEL = "subscription";
    public static final String TCP_LABEL = "tcp";

    @JsonProperty(required = true)
    @NotNull
    private ListenerType type;
}
