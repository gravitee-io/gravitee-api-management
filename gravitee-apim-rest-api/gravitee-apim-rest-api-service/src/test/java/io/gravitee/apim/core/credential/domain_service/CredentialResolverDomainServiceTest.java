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
package io.gravitee.apim.core.credential.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.CredentialCrudServiceInMemory;
import io.gravitee.apim.core.credential.CredentialSecretsInMemory;
import io.gravitee.apim.core.credential.exception.CredentialExpiredException;
import io.gravitee.apim.core.credential.exception.CredentialNotFoundException;
import io.gravitee.apim.core.credential.exception.CredentialNotResolvableException;
import io.gravitee.apim.core.credential.exception.CredentialSecretMissingException;
import io.gravitee.apim.core.credential.exception.CredentialSecretNotOwnedException;
import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.CredentialStrategy;
import io.gravitee.apim.core.credential.model.SecretReference;
import io.gravitee.apim.core.secret.exception.SecretNotFoundException;
import io.gravitee.apim.core.secret.model.Secret;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class CredentialResolverDomainServiceTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";
    private static final String CREDENTIAL_ID = "cred-1";
    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");

    private final CredentialCrudServiceInMemory credentialCrudService = new CredentialCrudServiceInMemory();
    private final CredentialSecretsInMemory secrets = new CredentialSecretsInMemory();

    private CredentialResolverDomainService service;

    @BeforeEach
    void setUp() {
        service = new CredentialResolverDomainService(credentialCrudService, secrets, secrets.encryptor);
    }

    @ParameterizedTest
    @EnumSource(CredentialStrategy.class)
    void should_resolve_the_values_of_a_managed_credential(CredentialStrategy strategy) {
        Map<String, Object> configuration = Map.of("tokenUrl", "https://login.example.com/token", "scopes", List.of("a", "b"));
        Map<String, String> values = strategy.secretFields().stream().collect(Collectors.toMap(field -> field, field -> "plain-" + field));
        credentialCrudService.initWith(List.of(managedCredential(strategy, configuration, values)));

        var resolved = service.resolve(ORG_ID, ENV_ID, CREDENTIAL_ID);

        assertThat(resolved.strategy()).isEqualTo(strategy);
        assertThat(resolved.configuration()).isEqualTo(configuration);
        assertThat(resolved.secrets()).isEqualTo(values);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "/vault/secret/azure:client-secret", "{#secrets.get('/vault/secret/azure:client-secret')}", "/gravitee/azure-secret:value",
        }
    )
    void should_refuse_a_secret_that_only_the_gateway_can_resolve(String reference) {
        var references = new LinkedHashMap<String, SecretReference>();
        references.put("clientId", storedReference("clientId", "client-id"));
        references.put("clientSecret", new SecretReference(reference, null));
        credentialCrudService.initWith(List.of(credential(CredentialStrategy.OAUTH2, Map.of(), references)));

        assertThatThrownBy(() -> service.resolve(ORG_ID, ENV_ID, CREDENTIAL_ID))
            .isInstanceOf(CredentialNotResolvableException.class)
            .hasMessageContaining("clientSecret");
    }

    @Test
    void should_not_find_a_credential_of_another_environment() {
        credentialCrudService.initWith(List.of(managedCredential(CredentialStrategy.BEARER, Map.of(), Map.of("token", "plain-token"))));

        assertThatThrownBy(() -> service.resolve(ORG_ID, "env-2", CREDENTIAL_ID)).isInstanceOf(CredentialNotFoundException.class);
        assertThatThrownBy(() -> service.resolve("org-2", ENV_ID, CREDENTIAL_ID)).isInstanceOf(CredentialNotFoundException.class);
    }

    @Test
    void should_name_the_field_whose_secret_is_gone() {
        var credential = managedCredential(CredentialStrategy.BEARER, Map.of(), Map.of("token", "plain-token"));
        credentialCrudService.initWith(List.of(credential));
        secrets.removeSecret(credential.secretReferences().get("token").secretId());

        assertThatThrownBy(() -> service.resolve(ORG_ID, ENV_ID, CREDENTIAL_ID))
            .isInstanceOf(CredentialSecretMissingException.class)
            .hasMessageContaining("token")
            .hasMessageNotContaining("rotated");
    }

    @Test
    void should_not_read_a_secret_that_another_credential_holds() {
        var references = new LinkedHashMap<String, SecretReference>();
        references.put("token", storedSecret("credential.other.token", "other-token"));
        credentialCrudService.initWith(List.of(credential(CredentialStrategy.BEARER, Map.of(), references)));

        assertThatThrownBy(() -> service.resolve(ORG_ID, ENV_ID, CREDENTIAL_ID))
            .isInstanceOf(CredentialSecretNotOwnedException.class)
            .hasMessageContaining("token")
            .hasMessageContaining("another credential")
            .hasMessageNotContaining("other-token");
    }

    @Test
    void should_refuse_a_credential_that_has_expired() {
        var credential = managedCredential(CredentialStrategy.BEARER, Map.of(), Map.of("token", "plain-token"));
        credentialCrudService.initWith(List.of(credential.toBuilder().expiresAt(Instant.parse("2023-01-01T00:00:00Z")).build()));

        assertThatThrownBy(() -> service.resolve(ORG_ID, ENV_ID, CREDENTIAL_ID))
            .isInstanceOf(CredentialExpiredException.class)
            .hasMessageContaining(CREDENTIAL_ID);
    }

    @Test
    void should_resolve_a_credential_that_has_not_expired_yet() {
        var credential = managedCredential(CredentialStrategy.BEARER, Map.of(), Map.of("token", "plain-token"));
        credentialCrudService.initWith(List.of(credential.toBuilder().expiresAt(Instant.now().plus(1, ChronoUnit.DAYS)).build()));

        assertThat(service.resolve(ORG_ID, ENV_ID, CREDENTIAL_ID).secrets()).containsEntry("token", "plain-token");
    }

    @Test
    void should_name_the_field_whose_secret_goes_while_it_is_read() {
        var vanishing = new CredentialSecretsInMemory() {
            @Override
            public String getEncryptedValue(Secret secret) {
                throw new SecretNotFoundException(secret.id());
            }
        };
        service = new CredentialResolverDomainService(credentialCrudService, vanishing, vanishing.encryptor);
        var secret = Secret.create(null, ORG_ID, ENV_ID, "credential." + CREDENTIAL_ID + ".token", NOW);
        var stored = vanishing.create(secret, vanishing.encryptor.encrypt(secret, "plain-token"));
        credentialCrudService.initWith(
            List.of(credential(CredentialStrategy.BEARER, Map.of(), Map.of("token", SecretReference.managed(stored.path(), stored.id()))))
        );

        assertThatThrownBy(() -> service.resolve(ORG_ID, ENV_ID, CREDENTIAL_ID))
            .isInstanceOf(CredentialSecretMissingException.class)
            .hasMessageContaining("token");
    }

    @Test
    void should_not_let_the_caller_change_the_values() {
        Map<String, Object> configuration = new HashMap<>(
            Map.of("tokenUrl", "https://login.example.com/token", "scopes", new ArrayList<>(List.of("a")))
        );
        credentialCrudService.initWith(
            List.of(managedCredential(CredentialStrategy.OAUTH2, configuration, Map.of("clientId", "id", "clientSecret", "secret")))
        );

        var resolved = service.resolve(ORG_ID, ENV_ID, CREDENTIAL_ID);

        assertThatThrownBy(() -> resolved.secrets().put("clientSecret", "changed")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> resolved.configuration().put("tokenUrl", "changed")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> ((List<String>) resolved.configuration().get("scopes")).add("b")).isInstanceOf(
            UnsupportedOperationException.class
        );
    }

    @Test
    void should_not_print_the_values() {
        credentialCrudService.initWith(List.of(managedCredential(CredentialStrategy.BEARER, Map.of(), Map.of("token", "plain-token"))));

        assertThat(service.resolve(ORG_ID, ENV_ID, CREDENTIAL_ID).toString()).doesNotContain("plain-token").contains("token");
    }

    private Credential managedCredential(CredentialStrategy strategy, Map<String, Object> configuration, Map<String, String> values) {
        var references = new LinkedHashMap<String, SecretReference>();
        values.forEach((field, value) -> references.put(field, storedReference(field, value)));
        return credential(strategy, configuration, references);
    }

    private SecretReference storedReference(String field, String value) {
        return storedSecret(Secret.CREDENTIAL_KEY_PREFIX + CREDENTIAL_ID + "." + field, value);
    }

    private SecretReference storedSecret(String key, String value) {
        var secret = Secret.create(null, ORG_ID, ENV_ID, key, NOW);
        var stored = secrets.create(secret, secrets.encryptor.encrypt(secret, value));
        return SecretReference.managed(stored.path(), stored.id());
    }

    private static Credential credential(
        CredentialStrategy strategy,
        Map<String, Object> configuration,
        Map<String, SecretReference> references
    ) {
        return Credential.builder()
            .id(CREDENTIAL_ID)
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .name("Azure discovery")
            .strategy(strategy)
            .configuration(configuration)
            .secretReferences(references)
            .createdAt(NOW)
            .updatedAt(NOW)
            .build();
    }
}
