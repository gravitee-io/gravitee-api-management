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
package io.gravitee.apim.core.integration.model;

import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Set;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
public sealed interface Integration {
    String id();

    String name();
    String description();
    String provider();
    String environmentId();
    ZonedDateTime createdAt();
    ZonedDateTime updatedAt();
    Set<String> groups();

    record ApiIntegration(
        String id,
        String name,
        String description,
        String provider,
        String environmentId,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt,
        Set<String> groups
    )
        implements Integration {
        public ApiIntegration update(String name, String description, Set<String> groups) {
            return new ApiIntegration(id, name, description, provider, environmentId, createdAt, TimeProvider.now(), groups);
        }
        public ApiIntegration withId(String id) {
            return new ApiIntegration(id, name, description, provider, environmentId, createdAt, updatedAt, groups);
        }
        public ApiIntegration withEnvironmentId(String environmentId) {
            return new ApiIntegration(id, name, description, provider, environmentId, createdAt, updatedAt, groups);
        }
    }

    record A2aIntegration(
        String id,
        String name,
        String description,
        String provider,
        String environmentId,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt,
        Set<String> groups,
        Collection<WellKnownUrl> wellKnownUrls
    )
        implements Integration {
        public record WellKnownUrl(String url) {}
        public A2aIntegration update(String name, String description, Set<String> groups, Collection<WellKnownUrl> wellKnownUrls) {
            return new A2aIntegration(id, name, description, provider, environmentId, createdAt, TimeProvider.now(), groups, wellKnownUrls);
        }
        public A2aIntegration withEnvironmentId(String environmentId) {
            return new A2aIntegration(id, name, description, provider, environmentId, createdAt, updatedAt, groups, wellKnownUrls);
        }
    }

    static ApiIntegration create(ApiIntegration integration) {
        var now = TimeProvider.now();
        return new ApiIntegration(
            UuidString.generateRandom(),
            integration.name(),
            integration.description(),
            integration.provider(),
            integration.environmentId(),
            now,
            now,
            Set.of()
        );
    }

    static A2aIntegration create(A2aIntegration integration) {
        var now = TimeProvider.now();
        return new A2aIntegration(
            UuidString.generateRandom(),
            integration.name(),
            integration.description(),
            integration.provider(),
            integration.environmentId(),
            now,
            now,
            Set.of(),
            integration.wellKnownUrls()
        );
    }
}
