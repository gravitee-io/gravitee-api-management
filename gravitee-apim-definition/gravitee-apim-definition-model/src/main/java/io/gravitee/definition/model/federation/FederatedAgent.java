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
package io.gravitee.definition.model.federation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.model.ApiDefinition;
import io.gravitee.definition.model.DefinitionVersion;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// https://google.github.io/A2A/specification/#5-agent-discovery-the-agent-card
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class FederatedAgent implements Serializable, ApiDefinition {

    public static final String STREAMING = "streaming";
    public static final String PUSH_NOTIFICATIONS = "pushNotifications";
    public static final String STATE_TRANSITION_HISTORY = "stateTransitionHistory";

    private String name;
    private String description;
    private String url;
    private String version;

    @Nullable
    private String documentationUrl;

    @Nullable
    private Provider provider;

    private Map<String, Boolean> capabilities;
    private Collection<Skill> skills;
    private Collection<String> defaultInputModes;
    private Collection<String> defaultOutputModes;

    @Nullable
    private JsonNode securitySchemes;

    @Nullable
    private JsonNode security;

    @JsonProperty(required = true)
    @NotNull
    @Builder.Default
    private DefinitionVersion definitionVersion = DefinitionVersion.FEDERATED_AGENT;

    @Override
    public void setId(String id) {
        // nothing to do
    }

    @Override
    public Set<String> getTags() {
        return Set.of();
    }

    @Override
    public void setTags(Set<String> tags) {
        // nothing to do
    }

    public record Provider(String organization, String url) {}

    public record Skill(
        String id,
        String name,
        String description,
        Collection<String> tags,
        @Nullable Collection<String> examples,
        @Nullable Collection<String> inputModes,
        @Nullable Collection<String> outputModes
    ) {}
}
