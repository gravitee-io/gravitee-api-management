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
package io.gravitee.definition.model.v4.flow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.*;

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
@Schema(name = "FlowV4")
@Builder(toBuilder = true)
@With
public class Flow implements Serializable {

    private String name;

    @Builder.Default
    private boolean enabled = true;

    @Valid
    private List<Selector> selectors;

    @Valid
    private List<Step> request;

    @Valid
    private List<Step> response;

    @Valid
    private List<Step> subscribe;

    @Valid
    private List<Step> publish;

    private Set<@NotEmpty String> tags;

    @JsonIgnore
    public Optional<Selector> selectorByType(SelectorType type) {
        if (selectors != null) {
            return selectors.stream().filter(selector -> selector.getType() == type).findFirst();
        }

        return Optional.empty();
    }

    public List<Plugin> getPlugins() {
        return Stream
            .of(
                Optional
                    .ofNullable(this.request)
                    .map(r -> r.stream().map(Step::getPlugins).flatMap(List::stream).collect(Collectors.toList()))
                    .orElse(List.of()),
                Optional
                    .ofNullable(this.response)
                    .map(r -> r.stream().map(Step::getPlugins).flatMap(List::stream).collect(Collectors.toList()))
                    .orElse(List.of()),
                Optional
                    .ofNullable(this.publish)
                    .map(p -> p.stream().map(Step::getPlugins).flatMap(List::stream).collect(Collectors.toList()))
                    .orElse(List.of()),
                Optional
                    .ofNullable(this.subscribe)
                    .map(s -> s.stream().map(Step::getPlugins).flatMap(List::stream).collect(Collectors.toList()))
                    .orElse(List.of())
            )
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }
}
