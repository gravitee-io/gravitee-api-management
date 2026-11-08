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

import static fixtures.core.model.SecretFixtures.CREATED_AT;
import static fixtures.core.model.SecretFixtures.aSecret;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.AbstractUseCaseTest;
import inmemory.SecretCrudServiceInMemory;
import inmemory.SecretValueEncryptorInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.secret.exception.SecretNotFoundException;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.model.SecretAuditEvent;
import io.gravitee.apim.core.secret.model.SecretValue;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateSecretUseCaseTest extends AbstractUseCaseTest {

    private final SecretCrudServiceInMemory secretCrudService = new SecretCrudServiceInMemory();
    private final SecretValueEncryptorInMemory secretValueEncryptor = new SecretValueEncryptorInMemory();
    private final AuditDomainService auditService = new AuditDomainService(
        auditCrudService,
        userCrudService,
        new JacksonJsonDiffProcessor()
    );

    private UpdateSecretUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateSecretUseCase(secretCrudService, secretValueEncryptor, auditService);
    }

    @Test
    void should_replace_the_value_and_keep_the_key() {
        var secret = givenASecret(aSecret());

        var updated = useCase.execute(update(secret.id(), "new-value")).secret();

        var stored = secretCrudService.encryptedValueOf(secret.id()).orElseThrow();
        assertThat(secretValueEncryptor.decrypt(secret, stored)).isEqualTo("new-value");
        assertThat(updated.key()).isEqualTo(secret.key());
        assertThat(updated.path()).isEqualTo(secret.path());
        assertThat(updated.createdAt()).isEqualTo(CREATED_AT);
        assertThat(updated.updatedAt()).isEqualTo(INSTANT_NOW);
    }

    @Test
    void should_create_an_audit_without_the_value() {
        var secret = givenASecret(aSecret());

        useCase.execute(update(secret.id(), "new-value"));

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
                    .event(SecretAuditEvent.SECRET_UPDATED.name())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
        assertThat(auditCrudService.storage().getFirst().getPatch()).doesNotContain("old-value").doesNotContain("new-value");
    }

    @Test
    void should_not_find_a_secret_of_another_environment() {
        var secret = givenASecret(aSecret().toBuilder().environmentId("env-2").build());

        assertThatThrownBy(() -> useCase.execute(update(secret.id(), "new-value"))).isInstanceOf(SecretNotFoundException.class);
        assertThat(auditCrudService.storage()).isEmpty();
    }

    @Test
    void should_not_find_a_secret_of_another_organization() {
        var secret = givenASecret(aSecret().toBuilder().organizationId("org-2").build());

        assertThatThrownBy(() -> useCase.execute(update(secret.id(), "new-value"))).isInstanceOf(SecretNotFoundException.class);
    }

    @Test
    void should_not_find_an_unknown_secret() {
        assertThatThrownBy(() -> useCase.execute(update("unknown", "new-value"))).isInstanceOf(SecretNotFoundException.class);
    }

    private Secret givenASecret(Secret secret) {
        return secretCrudService.create(secret, secretValueEncryptor.encrypt(secret, "old-value"));
    }

    private UpdateSecretUseCase.Input update(String id, String value) {
        return new UpdateSecretUseCase.Input(id, new SecretValue(value), AUDIT_INFO);
    }
}
