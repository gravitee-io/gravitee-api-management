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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder(toBuilder = true)
public class FlowConditionSelector extends FlowSelector {

    /**
     * Condition
     */
    private String condition;

    public FlowConditionSelector() {
        super(FlowSelectorType.CONDITION);
    }

    public abstract static class FlowConditionSelectorBuilder<
        C extends FlowConditionSelector,
        B extends FlowConditionSelector.FlowConditionSelectorBuilder<C, B>
    >
        extends FlowSelector.FlowSelectorBuilder<C, B> {

        FlowConditionSelectorBuilder() {
            type(FlowSelectorType.CONDITION);
        }
    }
}
