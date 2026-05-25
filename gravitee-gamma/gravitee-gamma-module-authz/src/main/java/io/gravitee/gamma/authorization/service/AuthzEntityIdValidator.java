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

import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import io.gravitee.gamma.authorization.domain.AuthzPolicyKind;
import io.gravitee.gamma.authorization.service.exception.AuthzEntityIdValidationCode;
import io.gravitee.gamma.authorization.service.exception.AuthzInvalidEntityIdException;
import io.gravitee.gamma.definition.authz.AuthzEntityIdConstants;
import java.util.Objects;
import java.util.regex.Pattern;

public final class AuthzEntityIdValidator {

    private static final Pattern FORMAT = Pattern.compile(AuthzEntityIdConstants.FORMAT_REGEX);

    public AuthzEntityIdValidator() {}

    public void validate(AuthzPolicyKind kind, String entityId) {
        Objects.requireNonNull(kind, "kind must not be null");
        switch (kind) {
            case GLOBAL -> {
                if (entityId != null) {
                    throw new AuthzInvalidEntityIdException(
                        AuthzEntityIdValidationCode.ENTITY_ID_FORBIDDEN_ON_GLOBAL,
                        "entityId must be null when kind is GLOBAL"
                    );
                }
            }
            case RESOURCE -> {
                if (entityId == null || entityId.isBlank()) {
                    throw new AuthzInvalidEntityIdException(
                        AuthzEntityIdValidationCode.ENTITY_ID_REQUIRED_FOR_RESOURCE,
                        "entityId must not be null or blank when kind is RESOURCE"
                    );
                }
            }
        }
    }

    public void validate(AuthzEntityKind kind, String entityId) {
        Objects.requireNonNull(kind, "kind must not be null");
        if (entityId == null || entityId.isBlank()) {
            throw new AuthzInvalidEntityIdException(
                AuthzEntityIdValidationCode.ENTITY_ID_REQUIRED_FOR_RESOURCE,
                "entityId must not be null or blank when kind is " + kind.name()
            );
        }
        if (entityId.length() > AuthzEntityIdConstants.MAX_ENTITY_ID_LENGTH) {
            throw new AuthzInvalidEntityIdException(
                AuthzEntityIdValidationCode.ENTITY_ID_MALFORMED,
                "entityId must be at most " + AuthzEntityIdConstants.MAX_ENTITY_ID_LENGTH + " characters (got " + entityId.length() + ")"
            );
        }
        if (!FORMAT.matcher(entityId).matches()) {
            throw new AuthzInvalidEntityIdException(
                AuthzEntityIdValidationCode.ENTITY_ID_MALFORMED,
                "entityId must match " + AuthzEntityIdConstants.FORMAT_REGEX + " (got: '" + entityId + "')"
            );
        }
    }
}
