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
import io.gravitee.definition.model.v4.flow.step.Step;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
public abstract class AbstractFlow implements Serializable {

    protected String id;

    protected String name;

    @Builder.Default
    protected boolean enabled = true;

    protected Set<@NotEmpty String> tags;

    public abstract List<Plugin> getPlugins();

    @JsonIgnore
    protected List<Plugin> computePlugins(List<Step> step) {
        return Stream.ofNullable(step)
            .flatMap(Collection::stream)
            .filter(Step::isEnabled)
            .map(Step::getPlugins)
            .flatMap(List::stream)
            .toList();
    }
}
