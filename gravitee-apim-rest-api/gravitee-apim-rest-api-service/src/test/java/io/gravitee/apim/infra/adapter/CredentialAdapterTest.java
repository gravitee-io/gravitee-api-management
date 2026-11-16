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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.CredentialStrategy;
import io.gravitee.apim.core.credential.model.SecretReference;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CredentialAdapterTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void should_keep_every_field_through_a_round_trip() {
        var credential = Credential.builder()
            .id("credential-1")
            .organizationId("org-1")
            .environmentId("env-1")
            .name("Azure OpenAI")
            .strategy(CredentialStrategy.OAUTH2)
            .serviceId("azure-openai")
            .ownerId("user-1")
            .expiresAt(Instant.parse("2027-01-01T00:00:00Z"))
            .configuration(Map.of("tokenUrl", "https://login.example.com/token", "scopes", List.of("read")))
            .secretReferences(
                Map.of(
                    "clientId",
                    new SecretReference("/vault/azure:client-id", null),
                    "clientSecret",
                    new SecretReference("/gravitee/credential.credential-1.clientSecret:value?renewable=true&reloadOnChange=true", "s-2")
                )
            )
            .createdAt(CREATED)
            .updatedAt(CREATED.plusSeconds(60))
            .rotatedAt(CREATED.plusSeconds(120))
            .build();

        var stored = CredentialAdapter.INSTANCE.toRepository(credential);

        assertThat(stored.getStrategy()).isEqualTo("OAUTH2");
        assertThat(CredentialAdapter.INSTANCE.toCoreModel(stored)).isEqualTo(credential);
    }

    @Test
    void should_keep_empty_optional_fields() {
        var credential = Credential.builder()
            .id("credential-1")
            .organizationId("org-1")
            .environmentId("env-1")
            .name("OpenAI")
            .strategy(CredentialStrategy.BEARER)
            .ownerId("user-1")
            .configuration(Map.of())
            .secretReferences(Map.of("token", new SecretReference("/vault/openai:token", null)))
            .createdAt(CREATED)
            .updatedAt(CREATED)
            .build();

        assertThat(CredentialAdapter.INSTANCE.toCoreModel(CredentialAdapter.INSTANCE.toRepository(credential))).isEqualTo(credential);
    }

    @Test
    void should_fail_loudly_on_unreadable_stored_references() {
        var stored = CredentialAdapter.INSTANCE.toRepository(
            Credential.builder()
                .id("credential-1")
                .strategy(CredentialStrategy.BEARER)
                .configuration(Map.of())
                .secretReferences(Map.of())
                .build()
        );
        stored.setSecretReferences("{not json");

        assertThatThrownBy(() -> CredentialAdapter.INSTANCE.toCoreModel(stored))
            .isInstanceOf(TechnicalDomainException.class)
            .hasMessageContaining("credential-1");
    }
}
