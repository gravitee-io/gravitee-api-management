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
package io.gravitee.gamma.module.platform.core.credential.model;

import io.gravitee.gamma.module.platform.core.credential.exception.CredentialManagementNotAllowedException;
import io.gravitee.gamma.module.platform.core.credential.exception.InvalidCredentialException;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialActor.SecretAccess;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;

/**
 * An environment-scoped credential a consumer uses to call a service. It holds references to its secret values, never the
 * values.
 */
@Builder(toBuilder = true)
public record Credential(
    String id,
    String organizationId,
    String environmentId,
    String name,
    CredentialStrategy strategy,
    String serviceId,
    String ownerId,
    Instant expiresAt,
    Map<String, Object> configuration,
    Map<String, SecretReference> secretReferences,
    Instant createdAt,
    Instant updatedAt,
    Instant rotatedAt
) {
    private static final int MAX_NAME_LENGTH = 256;
    private static final int MAX_SERVICE_LENGTH = 256;

    /** Validates the command; a credential whose values Gravitee stores gets its references once they are stored. */
    public static Credential create(String id, CredentialActor actor, NewCredentialCommand command, String ownerId, Instant now) {
        var references = command.validatedReferences();
        return Credential.builder()
            .id(id)
            .organizationId(actor.organizationId())
            .environmentId(actor.environmentId())
            .name(validName(command.name()))
            .strategy(command.strategy())
            .serviceId(validServiceId(command.serviceId()))
            .ownerId(ownerId)
            .expiresAt(validExpiry(command.expiresAt(), now))
            .configuration(command.strategy().validateConfiguration(command.configuration()))
            .secretReferences(references)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    public Credential withSecretReferences(Map<String, SecretReference> references) {
        return toBuilder().secretReferences(references).build();
    }

    public Credential update(UpdateCredentialCommand command, Instant now) {
        return toBuilder()
            .name(validName(command.name()))
            .serviceId(validServiceId(command.serviceId()))
            .ownerId(command.ownerId() == null ? ownerId : command.ownerId())
            .expiresAt(Objects.equals(command.expiresAt(), expiresAt) ? expiresAt : validExpiry(command.expiresAt(), now))
            .configuration(strategy.validateConfiguration(command.configuration()))
            .secretReferences(
                command.references() == null || sameReferences(command.references())
                    ? secretReferences
                    : replacedReferences(command.references())
            )
            .updatedAt(now)
            .build();
    }

    public void requireRotatableWith(Map<String, String> values) {
        if (!isManaged()) {
            throw new InvalidCredentialException(
                "This credential references secrets of your own: rotate them where they live, and the references stay the same."
            );
        }
        strategy.requireSomeSecretFields(values.keySet());
        NewCredentialCommand.requireValues(values);
    }

    public Credential rotated(Instant now) {
        return toBuilder().rotatedAt(now).updatedAt(now).build();
    }

    /** Whether Gravitee stores the values, as opposed to the credential pointing at secrets of the user's own. */
    /**
     * A reference into Gravitee's secret store hands the secret to whoever uses the credential, so the user must be
     * allowed to read secrets. The secrets of a managed credential were written for it and need no such check.
     */
    public void requireReferencesUsableBy(CredentialActor actor) {
        if (!isManaged() && secretReferences.values().stream().anyMatch(SecretReference::isInGraviteeStore)) {
            actor.requireSecretAccess(SecretAccess.READ);
        }
    }

    public boolean isManaged() {
        return secretReferences.values().stream().anyMatch(SecretReference::isManaged);
    }

    public void requireManageableBy(CredentialActor actor) {
        if (!actor.admin() && !ownerId.equals(actor.userId())) {
            throw new CredentialManagementNotAllowedException(id);
        }
    }

    /** Lets a client send back the references it read, so a credential round-trips through a form unchanged. */
    private boolean sameReferences(Map<String, String> references) {
        return (
            references.size() == secretReferences.size() &&
            secretReferences
                .entrySet()
                .stream()
                .allMatch(e -> e.getValue().reference().equals(references.get(e.getKey())))
        );
    }

    private Map<String, SecretReference> replacedReferences(Map<String, String> references) {
        if (isManaged()) {
            throw new InvalidCredentialException(
                "Gravitee stores the values of this credential, so its references cannot change. Rotate the values instead."
            );
        }
        strategy.requireAllSecretFields(references.keySet());
        var replaced = new LinkedHashMap<String, SecretReference>();
        strategy.secretFields().forEach(field -> replaced.put(field, SecretReference.external(field, references.get(field))));
        return replaced;
    }

    private static String validName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidCredentialException("name is required.");
        }
        var trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidCredentialException("name must be at most " + MAX_NAME_LENGTH + " characters.");
        }
        return trimmed;
    }

    private static String validServiceId(String serviceId) {
        if (serviceId == null || serviceId.isBlank()) {
            return null;
        }
        var trimmed = serviceId.trim();
        if (trimmed.length() > MAX_SERVICE_LENGTH) {
            throw new InvalidCredentialException("serviceId must be at most " + MAX_SERVICE_LENGTH + " characters.");
        }
        return trimmed;
    }

    private static Instant validExpiry(Instant expiresAt, Instant now) {
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw new InvalidCredentialException("expiresAt must be in the future.");
        }
        return expiresAt;
    }
}
