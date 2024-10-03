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
package io.gravitee.definition.model.v4.nativeapi;

import static io.gravitee.definition.model.v4.nativeapi.NativeListener.KAFKA_LABEL;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.gravitee.definition.model.v4.listener.AbstractListener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
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
@JsonSubTypes({ @JsonSubTypes.Type(value = KafkaListener.class, name = KAFKA_LABEL) })
@Schema(
    name = "NativeListenerV4",
    discriminatorProperty = "type",
    discriminatorMapping = { @DiscriminatorMapping(value = KAFKA_LABEL, schema = KafkaListener.class) },
    oneOf = { KafkaListener.class }
)
@SuperBuilder(toBuilder = true)
public abstract class NativeListener extends AbstractListener<NativeEntrypoint> {

    public static final String KAFKA_LABEL = "kafka";

    protected NativeListener(ListenerType type) {
        this.type = type;
    }

    public NativeListener(ListenerType type, NativeListenerBuilder<?, ?> b) {
        super(type, b);
    }
}
