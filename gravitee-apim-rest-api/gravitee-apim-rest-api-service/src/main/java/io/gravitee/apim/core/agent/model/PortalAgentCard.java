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
package io.gravitee.apim.core.agent.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PortalAgentCard(
    String id,
    String kind,
    String entityId,
    String slug,
    String sourceId,
    String sourceKind,
    String environmentId,
    String organizationId,
    Instant creationDate,
    Instant updateDate,
    Map<String, String> metadata,
    Definition definition
) {
    public static final String KIND_AGENT = "agent";

    public PortalAgentCard {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public record Definition(
        String name,
        String description,
        String url,
        Provider provider,
        String version,
        String documentationUrl,
        Capabilities capabilities,
        List<String> defaultInputModes,
        List<String> defaultOutputModes,
        List<Skill> skills
    ) {
        public Definition {
            defaultInputModes = defaultInputModes == null ? List.of() : List.copyOf(defaultInputModes);
            defaultOutputModes = defaultOutputModes == null ? List.of() : List.copyOf(defaultOutputModes);
            skills = skills == null ? List.of() : List.copyOf(skills);
        }
    }

    public record Provider(String organization, String url) {}

    public record Capabilities(Boolean streaming, Boolean pushNotifications, Boolean stateTransitionHistory) {}

    public record Skill(
        String id,
        String name,
        String description,
        List<String> tags,
        List<String> examples,
        List<String> inputModes,
        List<String> outputModes
    ) {
        public Skill {
            tags = tags == null ? List.of() : List.copyOf(tags);
            examples = examples == null ? List.of() : List.copyOf(examples);
            inputModes = inputModes == null ? List.of() : List.copyOf(inputModes);
            outputModes = outputModes == null ? List.of() : List.copyOf(outputModes);
        }
    }
}
