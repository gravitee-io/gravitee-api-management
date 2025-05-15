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
package io.gravitee.repository.jdbc.management.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.common.base.Strings;
import io.gravitee.repository.management.model.Integration;
import io.gravitee.repository.management.model.integration.A2aWellKnownUrl;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JdbcIntegration {

    private static final JsonMapper JSON_MAPPER = new JsonMapper();
    private static final TypeReference<Collection<A2aWellKnownUrl>> WELL_KNOWN_URLS_TYPE_REFERENCE = new TypeReference<>() {};
    private String id;
    private String name;
    private String description;
    private String provider;
    private String environmentId;
    private Date createdAt;
    private Date updatedAt;
    private String wellKnownUrls;

    public Integration toIntegration(Collection<String> groups) throws JsonProcessingException {
        Collection<A2aWellKnownUrl> a2aWellKnownUrls = Strings.isNullOrEmpty(wellKnownUrls)
            ? null
            : JSON_MAPPER.readValue(wellKnownUrls, WELL_KNOWN_URLS_TYPE_REFERENCE);

        return new Integration(id, name, description, provider, environmentId, createdAt, updatedAt, Set.copyOf(groups), a2aWellKnownUrls);
    }

    public static JdbcIntegration fromIntegration(Integration integration) throws JsonProcessingException {
        String wellKnownUrls = integration.getWellKnownUrls() != null && !integration.getWellKnownUrls().isEmpty()
            ? JSON_MAPPER.writeValueAsString(integration.getWellKnownUrls())
            : null;
        return new JdbcIntegration(
            integration.getId(),
            integration.getName(),
            integration.getDescription(),
            integration.getProvider(),
            integration.getEnvironmentId(),
            integration.getCreatedAt(),
            integration.getUpdatedAt(),
            wellKnownUrls
        );
    }
}
