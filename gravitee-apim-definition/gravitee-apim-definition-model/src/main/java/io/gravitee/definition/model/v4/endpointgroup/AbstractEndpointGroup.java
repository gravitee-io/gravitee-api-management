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
package io.gravitee.definition.model.v4.endpointgroup;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public abstract class AbstractEndpointGroup<E extends AbstractEndpoint> implements Serializable {

    @NotBlank
    private String name;

    @JsonProperty(required = true)
    @NotBlank
    private String type;

    @Builder.Default
    private LoadBalancer loadBalancer = new LoadBalancer();

    @Schema(implementation = Object.class)
    @JsonRawValue
    private String sharedConfiguration;

    @Valid
    private List<E> endpoints;

    @JsonSetter
    public void setSharedConfiguration(final JsonNode configuration) {
        if (configuration != null) {
            this.sharedConfiguration = configuration.toString();
        }
    }

    public void setSharedConfiguration(final String sharedConfiguration) {
        this.sharedConfiguration = sharedConfiguration;
    }

    @JsonIgnore
    public List<Plugin> getPlugins() {
        return Optional
            .ofNullable(this.endpoints)
            .map(e -> e.stream().map(AbstractEndpoint::getPlugins).flatMap(List::stream).collect(Collectors.toList()))
            .orElse(List.of());
    }
}
