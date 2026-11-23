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
package io.gravitee.apim.core.secret.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.AbstractUseCaseTest;
import inmemory.EventCrudInMemory;
import inmemory.EventLatestCrudInMemory;
import inmemory.SecretCrudServiceInMemory;
import inmemory.SecretValueEncryptorInMemory;
import io.gravitee.apim.common.secret.SecretChangedEvent;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.generic_event.domain_service.GenericEventPublicationDomainService;
import io.gravitee.apim.core.generic_event.domain_service.GenericEventWriterDomainService;
import io.gravitee.apim.core.generic_event.model.GenericEvent;
import io.gravitee.apim.core.secret.exception.SecretKeyAlreadyExistsException;
import io.gravitee.apim.core.secret.model.SecretAuditEvent;
import io.gravitee.apim.core.secret.model.SecretValue;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SecretDomainServiceTest extends AbstractUseCaseTest {

    private final SecretCrudServiceInMemory secretCrudService = new SecretCrudServiceInMemory();
    private final SecretValueEncryptorInMemory secretValueEncryptor = new SecretValueEncryptorInMemory();
    private final EventLatestCrudInMemory eventLatestCrudService = new EventLatestCrudInMemory();

    private SecretDomainService service;

    @BeforeEach
    void setUp() {
        service = new SecretDomainService(
            secretCrudService,
            secretValueEncryptor,
            new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor()),
            new GenericEventPublicationDomainService(
                new GenericEventWriterDomainService(new EventCrudInMemory(), eventLatestCrudService, GraviteeJacksonMapper.getInstance()),
                GraviteeJacksonMapper.getInstance()
            )
        );
    }

    @Test
    void should_store_the_value_of_a_credential() {
        var secret = service.create("credential.cred-1.token", new SecretValue("sk-ant-123"), AUDIT_INFO);

        assertThat(secret.key()).isEqualTo("credential.cred-1.token");
        assertThat(valueOf(secret.id())).isEqualTo("sk-ant-123");
        assertThat(auditCrudService.storage()).extracting(AuditEntity::getEvent).containsExactly(SecretAuditEvent.SECRET_CREATED.name());
    }

    @Test
    void should_refuse_a_key_already_taken() {
        service.create("credential.cred-1.token", new SecretValue("sk-ant-123"), AUDIT_INFO);

        assertThatThrownBy(() -> service.create("credential.cred-1.token", new SecretValue("other"), AUDIT_INFO)).isInstanceOf(
            SecretKeyAlreadyExistsException.class
        );
    }

    @Test
    void should_rotate_the_value_of_a_credential() {
        var secret = service.create("credential.cred-1.token", new SecretValue("sk-ant-123"), AUDIT_INFO);

        service.update(secret, new SecretValue("sk-ant-456"), AUDIT_INFO);

        assertThat(valueOf(secret.id())).isEqualTo("sk-ant-456");
        assertThat(auditCrudService.storage())
            .extracting(AuditEntity::getEvent)
            .containsExactly(SecretAuditEvent.SECRET_CREATED.name(), SecretAuditEvent.SECRET_UPDATED.name());
    }

    @Test
    void should_put_back_the_value_a_snapshot_holds() {
        var secret = service.create("credential.cred-1.token", new SecretValue("sk-ant-123"), AUDIT_INFO);
        var snapshot = service.snapshot(secret);
        service.update(secret, new SecretValue("sk-ant-456"), AUDIT_INFO);

        service.restore(snapshot, AUDIT_INFO);

        assertThat(valueOf(secret.id())).isEqualTo("sk-ant-123");
        assertThat(auditCrudService.storage())
            .extracting(AuditEntity::getEvent)
            .containsExactly(
                SecretAuditEvent.SECRET_CREATED.name(),
                SecretAuditEvent.SECRET_UPDATED.name(),
                SecretAuditEvent.SECRET_UPDATED.name()
            );
    }

    @Test
    void should_delete_the_value_of_a_credential() {
        var secret = service.create("credential.cred-1.token", new SecretValue("sk-ant-123"), AUDIT_INFO);

        service.delete(secret, AUDIT_INFO);

        assertThat(secretCrudService.storage()).isEmpty();
        assertThat(auditCrudService.storage())
            .extracting(AuditEntity::getEvent)
            .containsExactly(SecretAuditEvent.SECRET_CREATED.name(), SecretAuditEvent.SECRET_DELETED.name());
    }

    @Test
    void should_not_tell_the_gateways_about_a_new_secret_that_nothing_references_yet() {
        service.create("db-password", new SecretValue("s3cret"), AUDIT_INFO);

        assertThat(eventLatestCrudService.storage()).isEmpty();
    }

    @Test
    void should_tell_the_gateways_when_the_value_of_a_secret_changes_without_sending_it() {
        var secret = service.create("db-password", new SecretValue("s3cret"), AUDIT_INFO);

        service.update(secret, new SecretValue("n3w-s3cret"), AUDIT_INFO);

        assertSecretChangedEvent("db-password");
    }

    @Test
    void should_tell_the_gateways_when_a_snapshot_puts_back_a_value() {
        var secret = service.create("db-password", new SecretValue("s3cret"), AUDIT_INFO);
        var snapshot = service.snapshot(secret);

        service.restore(snapshot, AUDIT_INFO);

        assertSecretChangedEvent("db-password");
    }

    @Test
    void should_tell_the_gateways_when_a_secret_is_deleted() {
        var secret = service.create("db-password", new SecretValue("s3cret"), AUDIT_INFO);

        service.delete(secret, AUDIT_INFO);

        assertSecretChangedEvent("db-password");
    }

    private void assertSecretChangedEvent(String key) {
        assertThat(eventLatestCrudService.storage())
            .singleElement()
            .satisfies(event -> {
                assertThat(event.getId()).isEqualTo(GenericEvent.latestEventId(ENV_ID, SecretChangedEvent.KIND, key));
                assertThat(event.getExpiresAt()).isNotNull();
                assertThat(event.getPayload()).contains("\"key\":\"" + key + "\"").doesNotContain("s3cret");
            });
    }

    private String valueOf(String secretId) {
        var secret = secretCrudService.get(ORG_ID, ENV_ID, secretId);
        return secretValueEncryptor.decrypt(secret, secretCrudService.encryptedValueOf(secretId).orElseThrow());
    }
}
