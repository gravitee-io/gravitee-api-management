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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.ConnectorMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(name = "ChannelSelectorV4")
@SuperBuilder(toBuilder = true)
public class ChannelSelector extends Selector {

    private Set<@NotNull Operation> operations;

    @JsonProperty(required = true)
    @NotEmpty
    @Builder.Default
    private String channel = "/";

    @JsonProperty(required = true)
    @NotNull
    @Builder.Default
    private Operator channelOperator = Operator.STARTS_WITH;

    private Set<String> entrypoints;

    public ChannelSelector() {
        super(SelectorType.CHANNEL);
    }

    @RequiredArgsConstructor
    @Getter
    @Schema(name = "ChannelSelectorOperationV4")
    public enum Operation {
        SUBSCRIBE(ConnectorMode.SUBSCRIBE.getLabel(), ConnectorMode.SUBSCRIBE),
        PUBLISH(ConnectorMode.PUBLISH.getLabel(), ConnectorMode.PUBLISH);

        private static final Map<ConnectorMode, Operation> MAP = Map.of(SUBSCRIBE.connectorMode, SUBSCRIBE, PUBLISH.connectorMode, PUBLISH);

        @JsonValue
        private final String label;

        @JsonIgnore
        private final ConnectorMode connectorMode;

        public static Operation fromConnectorMode(final ConnectorMode connectorMode) {
            if (connectorMode != null) {
                return MAP.get(connectorMode);
            }
            return null;
        }
    }

    public abstract static class ChannelSelectorBuilder<C extends ChannelSelector, B extends ChannelSelectorBuilder<C, B>>
        extends SelectorBuilder<C, B> {

        ChannelSelectorBuilder() {
            type(SelectorType.CHANNEL);
        }
    }
}
