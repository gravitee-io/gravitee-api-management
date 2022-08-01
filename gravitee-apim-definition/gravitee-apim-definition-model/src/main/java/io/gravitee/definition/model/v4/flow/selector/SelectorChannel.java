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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.flow.Operator;
import java.util.Set;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SelectorChannel extends Selector {

    private Set<@NotNull Operation> operations;

    @JsonProperty(required = true)
    @NotEmpty
    private String channel;

    @JsonProperty(required = true)
    @NotNull
    private Operator channelOperator = Operator.STARTS_WITH;

    public SelectorChannel() {
        super(SelectorType.CHANNEL);
    }

    public enum Operation {
        SUB,
        PUB,
    }
}
