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
package io.gravitee.definition.model.v4.flow.selector;

import static io.gravitee.definition.model.v4.flow.selector.Selector.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = HttpSelector.class, name = HTTP_LABEL),
        @JsonSubTypes.Type(value = ChannelSelector.class, name = CHANNEL_LABEL),
        @JsonSubTypes.Type(value = ConditionSelector.class, name = CONDITION_LABEL),
    }
)
@SuperBuilder(toBuilder = true)
public abstract class Selector extends AbstractSelector {

    public static final String HTTP_LABEL = "http";
    public static final String CHANNEL_LABEL = "channel";
    public static final String CONDITION_LABEL = "condition";

    public Selector(SelectorType type) {
        super(type);
    }
}
