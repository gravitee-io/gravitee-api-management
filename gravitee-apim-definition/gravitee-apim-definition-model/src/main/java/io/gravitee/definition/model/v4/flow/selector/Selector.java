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
package io.gravitee.definition.model.v4.flow.selector;

import static io.gravitee.definition.model.v4.flow.selector.Selector.CHANNEL_LABEL;
import static io.gravitee.definition.model.v4.flow.selector.Selector.CONDITION_LABEL;
import static io.gravitee.definition.model.v4.flow.selector.Selector.HTTP_LABEL;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import javax.validation.constraints.NotNull;
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
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@FieldNameConstants
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = Selector.Fields.type)
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = SelectorHttp.class, name = HTTP_LABEL),
        @JsonSubTypes.Type(value = SelectorChannel.class, name = CHANNEL_LABEL),
        @JsonSubTypes.Type(value = SelectorCondition.class, name = CONDITION_LABEL),
    }
)
public abstract class Selector implements Serializable {

    public static final String HTTP_LABEL = "http";
    public static final String CHANNEL_LABEL = "channel";
    public static final String CONDITION_LABEL = "condition";

    @JsonProperty(required = true)
    @NotNull
    private SelectorType type;
}
