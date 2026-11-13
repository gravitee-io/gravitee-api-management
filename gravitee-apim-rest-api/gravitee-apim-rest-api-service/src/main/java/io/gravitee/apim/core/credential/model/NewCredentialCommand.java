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
package io.gravitee.apim.core.credential.model;

import io.gravitee.apim.core.credential.exception.InvalidCredentialException;
import io.gravitee.apim.core.secret.model.SecretValue;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;

/**
 * A credential to create. Give {@code references} to point at secrets the user already has, or {@code values} for
 * Gravitee to store as secrets: one or the other, with one entry per secret field of the strategy.
 *
 * @param ownerId the user who may change the credential, the creator when {@code null}
 */
@Builder
public record NewCredentialCommand(
    String name,
    CredentialStrategy strategy,
    String serviceId,
    String ownerId,
    Instant expiresAt,
    Map<String, Object> configuration,
    Map<String, String> references,
    Map<String, String> values
) {
    public boolean isManaged() {
        return values != null;
    }

    /** @return the references of a credential that points at the user's own secrets, none for one Gravitee stores */
    Map<String, SecretReference> validatedReferences() {
        if (strategy == null) {
            throw new InvalidCredentialException("strategy is required.");
        }
        if (references == null && values == null) {
            throw new InvalidCredentialException("Give references to existing secrets or values to store.");
        }
        if (references != null && values != null) {
            throw new InvalidCredentialException("Give either references to existing secrets or values to store, not both.");
        }
        if (isManaged()) {
            strategy.requireAllSecretFields(values.keySet());
            requireValues(values);
            return Map.of();
        }
        return strategy.externalReferences(references);
    }

    static void requireValues(Map<String, String> values) {
        values.forEach((field, value) -> {
            if (value == null || value.isBlank()) {
                throw new InvalidCredentialException("The value of " + field + " must not be blank.");
            }
            if (value.getBytes(StandardCharsets.UTF_8).length > SecretValue.MAX_BYTES) {
                throw new InvalidCredentialException(
                    "The value of " + field + " must not be larger than " + SecretValue.MAX_BYTES + " bytes."
                );
            }
        });
    }

    @Override
    public String toString() {
        return (
            "NewCredentialCommand[name=" +
            name +
            ", strategy=" +
            strategy +
            ", serviceId=" +
            serviceId +
            ", ownerId=" +
            ownerId +
            ", expiresAt=" +
            expiresAt +
            ", configuration=" +
            configuration +
            ", references=" +
            references +
            ", values=" +
            (values == null ? null : values.keySet()) +
            "]"
        );
    }
}
