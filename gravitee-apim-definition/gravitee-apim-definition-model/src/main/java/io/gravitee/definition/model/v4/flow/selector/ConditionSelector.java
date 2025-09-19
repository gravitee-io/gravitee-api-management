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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.ConditionSupplier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
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
@Schema(name = "ConditionSelectorV4")
@SuperBuilder(toBuilder = true)
public class ConditionSelector extends Selector implements ConditionSupplier {

    @JsonProperty(required = true)
    @NotEmpty
    private String condition;

    public ConditionSelector() {
        super(SelectorType.CONDITION);
    }

    public abstract static class ConditionSelectorBuilder<
        C extends ConditionSelector,
        B extends ConditionSelector.ConditionSelectorBuilder<C, B>
    >
        extends SelectorBuilder<C, B> {

        ConditionSelectorBuilder() {
            type(SelectorType.CONDITION);
        }
    }
}
