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
package io.gravitee.repository.management.model.flow.selector;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * @author GraviteeSource Team
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder(toBuilder = true)
public class FlowMcpSelector extends FlowSelector {

    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<String> methods;

    public FlowMcpSelector() {
        super(FlowSelectorType.MCP);
    }

    public abstract static class FlowMcpSelectorBuilder<C extends FlowMcpSelector, B extends FlowMcpSelector.FlowMcpSelectorBuilder<C, B>>
        extends FlowSelectorBuilder<C, B> {

        FlowMcpSelectorBuilder() {
            type(FlowSelectorType.MCP);
        }
    }
}
