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
import io.gravitee.apim.core.secret.exception.InvalidSecretKeyException;
import io.gravitee.apim.core.secret.exception.InvalidSecretValueException;
import io.gravitee.apim.core.secret.exception.SecretKeyAlreadyExistsException;
import io.gravitee.apim.core.secret.model.SecretAuditEvent;
import io.gravitee.apim.core.secret.model.SecretValue;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CreateSecretUseCaseTest extends AbstractUseCaseTest {

    private final SecretCrudServiceInMemory secretCrudService = new SecretCrudServiceInMemory();
    private final SecretValueEncryptorInMemory secretValueEncryptor = new SecretValueEncryptorInMemory();
    private final AuditDomainService auditService = new AuditDomainService(
        auditCrudService,
        userCrudService,
        new JacksonJsonDiffProcessor()
    );

    private CreateSecretUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateSecretUseCase(secretCrudService, secretValueEncryptor, auditService);
    }

    @Test
    void should_create_a_secret_with_its_path() {
        var secret = useCase.execute(create("anthropic-key", "sk-ant-123")).secret();

        assertThat(secret.id()).isEqualTo(GENERATED_UUID);
        assertThat(secret.organizationId()).isEqualTo(ORG_ID);
        assertThat(secret.environmentId()).isEqualTo(ENV_ID);
        assertThat(secret.key()).isEqualTo("anthropic-key");
        assertThat(secret.path()).isEqualTo("/gravitee/anthropic-key:value");
        assertThat(secret.createdAt()).isEqualTo(INSTANT_NOW);
        assertThat(secret.updatedAt()).isEqualTo(INSTANT_NOW);
    }

    @Test
    void should_store_only_the_encrypted_value() {
        var secret = useCase.execute(create("anthropic-key", "sk-ant-123")).secret();

        var stored = secretCrudService.encryptedValueOf(secret.id()).orElseThrow();
        assertThat(stored).doesNotContain("sk-ant-123");
        assertThat(secretValueEncryptor.decrypt(secret, stored)).isEqualTo("sk-ant-123");
    }

    @Test
    void should_create_an_audit_without_the_value() {
        useCase.execute(create("anthropic-key", "sk-ant-123"));

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
                    .properties(Map.of(AuditProperties.SECRET.name(), GENERATED_UUID))
                    .event(SecretAuditEvent.SECRET_CREATED.name())
                    .createdAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                    .build()
            );
        assertThat(auditCrudService.storage().getFirst().getPatch()).doesNotContain("sk-ant-123");
    }

    @Test
    void should_refuse_a_key_that_the_environment_already_holds() {
        var existing = aSecret().toBuilder().id("existing").build();
        secretCrudService.create(existing, secretValueEncryptor.encrypt(existing, "old-value"));

        assertThatThrownBy(() -> useCase.execute(create(existing.key(), "sk-ant-123"))).isInstanceOf(SecretKeyAlreadyExistsException.class);
        assertThat(auditCrudService.storage()).isEmpty();
    }

    @Test
    void should_accept_a_key_that_only_another_environment_holds() {
        var existing = aSecret().toBuilder().id("existing").environmentId("env-2").build();
        secretCrudService.create(existing, secretValueEncryptor.encrypt(existing, "old-value"));

        var secret = useCase.execute(create(existing.key(), "sk-ant-123")).secret();

        assertThat(secret.environmentId()).isEqualTo(ENV_ID);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "with space", "slash/key", "{#el}", "é", ".", "..", ".env", "-key", "_key" })
    void should_refuse_an_invalid_key(String key) {
        assertThatThrownBy(() -> useCase.execute(create(key, "sk-ant-123"))).isInstanceOf(InvalidSecretKeyException.class);
        assertThat(auditCrudService.storage()).isEmpty();
    }

    @Test
    void should_accept_a_key_of_128_characters_and_refuse_129() {
        assertThat(useCase.execute(create("k".repeat(128), "v")).secret().key()).hasSize(128);
        assertThatThrownBy(() -> useCase.execute(create("k".repeat(129), "v"))).isInstanceOf(InvalidSecretKeyException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void should_refuse_an_empty_value(String value) {
        assertThatThrownBy(() -> new SecretValue(value)).isInstanceOf(InvalidSecretValueException.class);
    }

    @Test
    void should_accept_a_value_of_64_KiB_and_refuse_one_byte_more() {
        assertThat(new SecretValue("v".repeat(65536)).plaintext()).hasSize(65536);
        assertThatThrownBy(() -> new SecretValue("v".repeat(65537))).isInstanceOf(InvalidSecretValueException.class);
        assertThatThrownBy(() -> new SecretValue("é".repeat(32769))).isInstanceOf(InvalidSecretValueException.class);
    }

    @Test
    void should_hide_the_value_in_the_input_description() {
        assertThat(create("anthropic-key", "sk-ant-123").toString()).doesNotContain("sk-ant-123");
    }

    private CreateSecretUseCase.Input create(String key, String value) {
        return new CreateSecretUseCase.Input(key, new SecretValue(value), AUDIT_INFO);
    }
}
