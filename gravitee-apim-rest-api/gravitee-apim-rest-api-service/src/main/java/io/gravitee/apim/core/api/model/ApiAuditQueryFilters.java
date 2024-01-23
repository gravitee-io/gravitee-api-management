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
package io.gravitee.apim.core.api.model;

import java.util.Optional;
import java.util.Set;

public record ApiAuditQueryFilters(
    String apiId,
    String organizationId,
    String environmentId,
    Optional<Long> from,
    Optional<Long> to,
    Set<String> events
) {
    public ApiAuditQueryFilters {
        if (apiId == null) {
            throw new IllegalArgumentException("apiId must not be null");
        }
        if (organizationId == null) {
            throw new IllegalArgumentException("organizationId must not be null");
        }
        if (environmentId == null) {
            throw new IllegalArgumentException("environmentId must not be null");
        }
        if (events == null) {
            events = Set.of();
        }
    }
}
