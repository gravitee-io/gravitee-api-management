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
package io.gravitee.gamma.authorization.service;

import io.gravitee.gamma.authorization.api.EntityIdConstants;
import io.gravitee.gamma.authorization.domain.EntityKind;
import io.gravitee.gamma.authorization.domain.PolicyKind;
import io.gravitee.gamma.authorization.service.exception.EntityIdValidationCode;
import io.gravitee.gamma.authorization.service.exception.InvalidEntityIdException;
import java.util.Objects;
import java.util.regex.Pattern;

public final class EntityIdValidator {

    private static final Pattern FORMAT = Pattern.compile(EntityIdConstants.FORMAT_REGEX);

    public EntityIdValidator() {}

    public void validate(PolicyKind kind, String entityId) {
        Objects.requireNonNull(kind, "kind must not be null");
        switch (kind) {
            case GLOBAL -> {
                if (entityId != null) {
                    throw new InvalidEntityIdException(
                        EntityIdValidationCode.ENTITY_ID_FORBIDDEN_ON_GLOBAL,
                        "entityId must be null when kind is GLOBAL"
                    );
                }
            }
            case RESOURCE -> {
                if (entityId == null || entityId.isBlank()) {
                    throw new InvalidEntityIdException(
                        EntityIdValidationCode.ENTITY_ID_REQUIRED_FOR_RESOURCE,
                        "entityId must not be null or blank when kind is RESOURCE"
                    );
                }
            }
        }
    }

    public void validate(EntityKind kind, String entityId) {
        Objects.requireNonNull(kind, "kind must not be null");
        if (entityId == null || entityId.isBlank()) {
            throw new InvalidEntityIdException(
                EntityIdValidationCode.ENTITY_ID_REQUIRED_FOR_RESOURCE,
                "entityId must not be null or blank when kind is " + kind.name()
            );
        }
        if (entityId.length() > EntityIdConstants.MAX_ENTITY_ID_LENGTH) {
            throw new InvalidEntityIdException(
                EntityIdValidationCode.ENTITY_ID_MALFORMED,
                "entityId must be at most " + EntityIdConstants.MAX_ENTITY_ID_LENGTH + " characters (got " + entityId.length() + ")"
            );
        }
        if (!FORMAT.matcher(entityId).matches()) {
            throw new InvalidEntityIdException(
                EntityIdValidationCode.ENTITY_ID_MALFORMED,
                "entityId must match " + EntityIdConstants.FORMAT_REGEX + " (got: '" + entityId + "')"
            );
        }
    }
}
