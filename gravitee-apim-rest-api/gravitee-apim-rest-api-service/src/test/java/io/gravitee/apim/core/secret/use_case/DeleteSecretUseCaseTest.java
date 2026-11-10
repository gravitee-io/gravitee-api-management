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
package io.gravitee.apim.core.secret.use_case;

import static fixtures.core.model.SecretFixtures.aSecret;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.AbstractUseCaseTest;
import inmemory.SecretCrudServiceInMemory;
import inmemory.SecretValueEncryptorInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.secret.exception.SecretKeptForCredentialException;
import io.gravitee.apim.core.secret.exception.SecretNotFoundException;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.model.SecretAuditEvent;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteSecretUseCaseTest extends AbstractUseCaseTest {

    private final SecretCrudServiceInMemory secretCrudService = new SecretCrudServiceInMemory();
    private final SecretValueEncryptorInMemory secretValueEncryptor = new SecretValueEncryptorInMemory();
    private final AuditDomainService auditService = new AuditDomainService(
        auditCrudService,
        userCrudService,
        new JacksonJsonDiffProcessor()
    );

    private DeleteSecretUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteSecretUseCase(secretCrudService, auditService);
    }

    @Test
    void should_delete_the_secret_so_its_key_no_longer_resolves() {
        var secret = givenASecret(aSecret());

        useCase.execute(new DeleteSecretUseCase.Input(secret.id(), AUDIT_INFO));

        assertThat(secretCrudService.findByKey(ORG_ID, ENV_ID, secret.key())).isEmpty();
        assertThat(secretCrudService.encryptedValueOf(secret.id())).isEmpty();
    }

    @Test
    void should_create_an_audit() {
        var secret = givenASecret(aSecret());

        useCase.execute(new DeleteSecretUseCase.Input(secret.id(), AUDIT_INFO));

        assertThat(auditCrudService.storage())
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("patch")
            .containsExactly(
                AuditEntity.builder()
                    .id(GENERATED_UUID)
                    .organizationId(ORG_ID)
                    .environmentId(ENV_ID)
                    .referenceType(AuditEntity.AuditReferenceType.ENVIRONMENT)
                    .referenceId(ENV_ID)
                    .user(USER_ID)
                    .properties(Map.of(AuditProperties.SECRET.name(), secret.id()))
                    .event(SecretAuditEvent.SECRET_DELETED.name())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
    }

    @Test
    void should_not_delete_a_secret_of_another_environment() {
        var secret = givenASecret(aSecret().toBuilder().environmentId("env-2").build());

        assertThatThrownBy(() -> useCase.execute(new DeleteSecretUseCase.Input(secret.id(), AUDIT_INFO))).isInstanceOf(
            SecretNotFoundException.class
        );
        assertThat(secretCrudService.encryptedValueOf(secret.id())).isPresent();
        assertThat(auditCrudService.storage()).isEmpty();
    }

    @Test
    void should_not_find_an_unknown_secret() {
        assertThatThrownBy(() -> useCase.execute(new DeleteSecretUseCase.Input("unknown", AUDIT_INFO))).isInstanceOf(
            SecretNotFoundException.class
        );
    }

    @Test
    void should_not_delete_the_value_of_a_credential() {
        var secret = givenASecret(aSecret().toBuilder().key("credential.cred-1.token").build());

        assertThatThrownBy(() -> useCase.execute(new DeleteSecretUseCase.Input(secret.id(), AUDIT_INFO))).isInstanceOf(
            SecretKeptForCredentialException.class
        );
        assertThat(secretCrudService.encryptedValueOf(secret.id())).isPresent();
        assertThat(auditCrudService.storage()).isEmpty();
    }

    @Test
    void should_let_a_credential_delete_its_value() {
        var secret = givenASecret(aSecret().toBuilder().key("credential.cred-1.token").build());

        useCase.execute(new DeleteSecretUseCase.Input(secret.id(), AUDIT_INFO, true));

        assertThat(secretCrudService.encryptedValueOf(secret.id())).isEmpty();
    }

    private Secret givenASecret(Secret secret) {
        return secretCrudService.create(secret, secretValueEncryptor.encrypt(secret, "sk-ant-123"));
    }
}
