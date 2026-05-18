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
package io.gravitee.apim.authorization.domain;

import io.gravitee.apim.authorization.api.Validators;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record Policy(
    @NotNull String id,
    @NotBlank String name,
    @NotNull PolicyKind kind,
    String entityId,
    @NotNull String policyText,
    @NotNull PolicyStatus status,
    @NotBlank String environmentId,
    @NotNull Instant createdAt,
    @NotNull Instant updatedAt
) {
    public Policy {
        Validators.validateCtor(Policy.class, id, name, kind, entityId, policyText, status, environmentId, createdAt, updatedAt);
        switch (kind) {
            case GLOBAL -> {
                if (entityId != null) {
                    throw new IllegalArgumentException("entityId must be null when kind is GLOBAL");
                }
            }
            case RESOURCE -> {
                if (entityId == null || entityId.isBlank()) {
                    throw new IllegalArgumentException("entityId must not be null or blank when kind is RESOURCE");
                }
            }
        }
    }
}
