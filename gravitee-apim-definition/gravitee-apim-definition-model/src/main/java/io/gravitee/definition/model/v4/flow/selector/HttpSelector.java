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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.flow.Operator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashSet;
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
@Schema(name = "HttpSelectorV4")
@SuperBuilder(toBuilder = true)
public class HttpSelector extends Selector {

    protected static final String DEFAULT_PATH = "/";
    protected static final Operator DEFAULT_OPERATOR = Operator.STARTS_WITH;

    @JsonProperty(required = true)
    @NotEmpty
    @Builder.Default
    private String path = DEFAULT_PATH;

    @JsonProperty(required = true)
    @NotNull
    @Builder.Default
    private Operator pathOperator = DEFAULT_OPERATOR;

    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<HttpMethod> methods;

    public HttpSelector() {
        super(SelectorType.HTTP);
        this.path = DEFAULT_PATH;
        this.pathOperator = DEFAULT_OPERATOR;
    }

    public abstract static class HttpSelectorBuilder<C extends HttpSelector, B extends HttpSelector.HttpSelectorBuilder<C, B>>
        extends SelectorBuilder<C, B> {

        HttpSelectorBuilder() {
            type(SelectorType.HTTP);
        }
    }
}
