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
package io.gravitee.gamma.module.platform.infra.service_provider;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.AbstractUseCaseTest;
import inmemory.SecretCrudServiceInMemory;
import inmemory.SecretValueEncryptorInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.use_case.CreateSecretUseCase;
import io.gravitee.apim.core.secret.use_case.DeleteSecretUseCase;
import io.gravitee.apim.core.secret.use_case.UpdateSecretUseCase;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.gamma.module.platform.core.credential.model.Credential;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialStrategy;
import io.gravitee.gamma.module.platform.core.credential.model.NewCredentialCommand;
import io.gravitee.gamma.module.platform.core.credential.use_case.CreateCredentialUseCase;
import io.gravitee.gamma.module.platform.core.credential.use_case.DeleteCredentialUseCase;
import io.gravitee.gamma.module.platform.core.credential.use_case.RotateCredentialUseCase;
import io.gravitee.gamma.module.platform.fixture.CredentialFixture;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/** Runs the credential use cases on the host's own secret use cases, so a value is stored exactly as a user secret is. */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApimCredentialSecretStoreTest extends AbstractUseCaseTest {

    private final SecretCrudServiceInMemory secretCrudService = new SecretCrudServiceInMemory();
    private final SecretValueEncryptorInMemory secretValueEncryptor = new SecretValueEncryptorInMemory();
    private CredentialFixture fixture;

    @BeforeEach
    void setUp() {
        // The base class gives every id the same value; each secret needs its own.
        var sequence = new AtomicInteger();
        UuidString.overrideGenerator(() -> "secret-" + sequence.incrementAndGet());
        fixture = new CredentialFixture();
        var auditDomainService = new AuditDomainService(fixture.auditCrudService, fixture.userCrudService, new JacksonJsonDiffProcessor());
        fixture.build(
            new ApimCredentialSecretStore(
                new CreateSecretUseCase(secretCrudService, secretValueEncryptor, auditDomainService),
                new UpdateSecretUseCase(secretCrudService, secretValueEncryptor, auditDomainService),
                new DeleteSecretUseCase(secretCrudService, auditDomainService)
            )
        );
    }

    private Credential givenAnOauthCredential() {
        return fixture.createCredentialUseCase
            .execute(
                new CreateCredentialUseCase.Input(
                    "credential-1",
                    fixture.owner(),
                    NewCredentialCommand.builder()
                        .name("Azure OpenAI")
                        .strategy(CredentialStrategy.OAUTH2)
                        .configuration(Map.of("tokenUrl", "https://login.example.com/token"))
                        .values(Map.of("clientId", "my-client", "clientSecret", "my-client-secret"))
                        .build()
                )
            )
            .credential();
    }

    private String storedValue(String key) {
        var secret = secretCrudService.findByKey(CredentialFixture.ORG_ID, CredentialFixture.ENV_ID, key).orElseThrow();
        return secretValueEncryptor.decrypt(secret, secretCrudService.encryptedValueOf(secret.id()).orElseThrow());
    }

    @Test
    void should_store_each_value_as_a_gravitee_secret_the_gateway_reloads() {
        var credential = givenAnOauthCredential();

        assertThat(storedValue("credential.credential-1.clientId")).isEqualTo("my-client");
        assertThat(storedValue("credential.credential-1.clientSecret")).isEqualTo("my-client-secret");
        assertThat(credential.secretReferences().get("clientSecret").reference()).isEqualTo(
            "/gravitee/credential.credential-1.clientSecret:value?renewable=true&reloadOnChange=true"
        );
        assertThat(credential.toString()).doesNotContain("my-client-secret");
        assertThat(fixture.credentialRepository.storage().getFirst().toString()).doesNotContain("my-client-secret");
    }

    @Test
    void should_write_the_rotated_value_into_the_same_secret() {
        var credential = givenAnOauthCredential();
        var secretId = credential.secretReferences().get("clientSecret").secretId();

        fixture.rotateCredentialUseCase.execute(
            new RotateCredentialUseCase.Input(fixture.owner(), credential.id(), Map.of("clientSecret", "rotated-secret"))
        );

        assertThat(storedValue("credential.credential-1.clientSecret")).isEqualTo("rotated-secret");
        assertThat(secretCrudService.findByKey(CredentialFixture.ORG_ID, CredentialFixture.ENV_ID, "credential.credential-1.clientSecret"))
            .map(Secret::id)
            .contains(secretId);
    }

    @Test
    void should_delete_the_secrets_with_the_credential() {
        var credential = givenAnOauthCredential();

        fixture.deleteCredentialUseCase.execute(new DeleteCredentialUseCase.Input(fixture.owner(), credential.id()));

        assertThat(secretCrudService.storage()).isEmpty();
    }

    @Test
    void should_delete_the_credential_when_a_user_already_deleted_its_secret() {
        var credential = givenAnOauthCredential();
        secretCrudService.delete(
            secretCrudService
                .findByKey(CredentialFixture.ORG_ID, CredentialFixture.ENV_ID, "credential.credential-1.clientId")
                .orElseThrow()
        );

        fixture.deleteCredentialUseCase.execute(new DeleteCredentialUseCase.Input(fixture.owner(), credential.id()));

        assertThat(fixture.credentialRepository.storage()).isEmpty();
        assertThat(secretCrudService.storage()).isEmpty();
    }
}
