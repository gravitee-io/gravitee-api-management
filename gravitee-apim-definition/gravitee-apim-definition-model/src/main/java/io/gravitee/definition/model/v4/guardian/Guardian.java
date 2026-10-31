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
package io.gravitee.definition.model.v4.guardian;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.resource.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A Guardian is a named group of policies deployed on its own and executed from an API flow through the
 * {@code guardian-policy}. It follows the Shared Policy Group model, but is executed by the Guardian reactor plugin.
 *
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
public class Guardian implements Serializable {

    @JsonProperty(required = true)
    @NotBlank
    private String id;

    @JsonProperty(required = true)
    @NotBlank
    private String environmentId;

    @JsonProperty(required = true)
    @NotBlank
    private String name;

    @JsonProperty
    @NotBlank
    private String version;

    @JsonProperty(required = true)
    @NotBlank
    private Phase phase;

    @Valid
    private List<Step> policies;

    @Valid
    private List<Resource> resources;

    private Date deployedAt;

    @JsonIgnore
    public List<Plugin> getPlugins() {
        return Stream.concat(
            Optional.ofNullable(this.resources).stream().flatMap(List::stream).filter(Resource::isEnabled).map(Resource::getPlugins),
            Optional.ofNullable(this.policies).stream().flatMap(List::stream).filter(Step::isEnabled).map(Step::getPlugins)
        )
            .flatMap(List::stream)
            .toList();
    }

    public enum Phase {
        REQUEST,
        RESPONSE,
    }
}
