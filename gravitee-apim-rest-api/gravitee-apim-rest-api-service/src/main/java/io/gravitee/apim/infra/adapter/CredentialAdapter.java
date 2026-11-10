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
package io.gravitee.apim.infra.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.CredentialStrategy;
import io.gravitee.apim.core.credential.model.SecretReference;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/** Maps a core credential to the repository model, which keeps the settings and the references as JSON. */
public class CredentialAdapter {

    public static final CredentialAdapter INSTANCE = new CredentialAdapter();

    private static final TypeReference<LinkedHashMap<String, Object>> SETTINGS = new TypeReference<>() {};
    private static final TypeReference<LinkedHashMap<String, StoredReference>> REFERENCES = new TypeReference<>() {};

    /** The stored shape of a reference, kept apart from the core record so a new core method never changes the JSON. */
    private record StoredReference(String reference, String secretId) {}

    private final ObjectMapper mapper = new ObjectMapper();

    private CredentialAdapter() {}

    public io.gravitee.repository.management.model.Credential toRepository(Credential credential) {
        return io.gravitee.repository.management.model.Credential.builder()
            .id(credential.id())
            .organizationId(credential.organizationId())
            .environmentId(credential.environmentId())
            .name(credential.name())
            .strategy(credential.strategy().name())
            .serviceId(credential.serviceId())
            .ownerId(credential.ownerId())
            .expiresAt(toDate(credential.expiresAt()))
            .configuration(write(credential.id(), credential.configuration()))
            .secretReferences(write(credential.id(), toStored(credential.secretReferences())))
            .createdAt(toDate(credential.createdAt()))
            .updatedAt(toDate(credential.updatedAt()))
            .rotatedAt(toDate(credential.rotatedAt()))
            .build();
    }

    public Credential toCoreModel(io.gravitee.repository.management.model.Credential stored) {
        return Credential.builder()
            .id(stored.getId())
            .organizationId(stored.getOrganizationId())
            .environmentId(stored.getEnvironmentId())
            .name(stored.getName())
            .strategy(CredentialStrategy.valueOf(stored.getStrategy()))
            .serviceId(stored.getServiceId())
            .ownerId(stored.getOwnerId())
            .expiresAt(toInstant(stored.getExpiresAt()))
            .configuration(read(stored.getId(), stored.getConfiguration(), SETTINGS))
            .secretReferences(toCore(read(stored.getId(), stored.getSecretReferences(), REFERENCES)))
            .createdAt(toInstant(stored.getCreatedAt()))
            .updatedAt(toInstant(stored.getUpdatedAt()))
            .rotatedAt(toInstant(stored.getRotatedAt()))
            .build();
    }

    private String write(String credentialId, Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new TechnicalDomainException("Could not write the details of credential " + credentialId, e);
        }
    }

    private <T> T read(String credentialId, String json, TypeReference<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new TechnicalDomainException("Could not read the stored details of credential " + credentialId, e);
        }
    }

    private static Map<String, StoredReference> toStored(Map<String, SecretReference> references) {
        var stored = new LinkedHashMap<String, StoredReference>();
        references.forEach((field, reference) -> stored.put(field, new StoredReference(reference.reference(), reference.secretId())));
        return stored;
    }

    private static Map<String, SecretReference> toCore(Map<String, StoredReference> stored) {
        var references = new LinkedHashMap<String, SecretReference>();
        stored.forEach((field, reference) -> references.put(field, new SecretReference(reference.reference(), reference.secretId())));
        return references;
    }

    private static Date toDate(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }

    private static Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
