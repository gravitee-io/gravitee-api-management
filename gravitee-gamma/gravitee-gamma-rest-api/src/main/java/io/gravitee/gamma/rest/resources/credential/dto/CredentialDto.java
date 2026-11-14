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
package io.gravitee.gamma.rest.resources.credential.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.CredentialActor;
import io.gravitee.apim.core.credential.model.CredentialStrategy;
import io.gravitee.apim.core.credential.model.SecretReference;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A credential as a client reads it: references to its secrets, never their values.
 *
 * <p>The timestamps need {@code @JsonFormat}: the inherited rest-api mapper enables {@code WRITE_DATES_AS_TIMESTAMPS},
 * which would otherwise write them as epoch numbers.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CredentialDto(
    String id,
    String name,
    CredentialStrategy strategy,
    String serviceId,
    String ownerId,
    String ownerName,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant expiresAt,
    Map<String, Object> configuration,
    Map<String, SecretReferenceDto> secretReferences,
    boolean managed,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant createdAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant updatedAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant rotatedAt,
    boolean manageable
) {
    /** @param managed whether Gravitee stores the value behind the reference */
    public record SecretReferenceDto(String reference, boolean managed) {}

    /** Without the owner name, which only the reads look up. */
    public static CredentialDto from(Credential credential, CredentialActor caller) {
        return from(credential, caller, null);
    }

    /** @param ownerName null when the owner is no longer a user */
    public static CredentialDto from(Credential credential, CredentialActor caller, String ownerName) {
        var references = new LinkedHashMap<String, SecretReferenceDto>();
        credential.secretReferences().forEach((field, reference) -> references.put(field, from(reference)));
        return new CredentialDto(
            credential.id(),
            credential.name(),
            credential.strategy(),
            credential.serviceId(),
            credential.ownerId(),
            ownerName,
            credential.expiresAt(),
            credential.configuration(),
            references,
            credential.isManaged(),
            credential.createdAt(),
            credential.updatedAt(),
            credential.rotatedAt(),
            credential.isManageableBy(caller)
        );
    }

    public static List<CredentialDto> from(List<Credential> credentials, CredentialActor caller, Map<String, String> ownerNames) {
        return credentials
            .stream()
            .map(credential -> from(credential, caller, ownerNames.get(credential.ownerId())))
            .toList();
    }

    private static SecretReferenceDto from(SecretReference reference) {
        return new SecretReferenceDto(reference.reference(), reference.isManaged());
    }
}
