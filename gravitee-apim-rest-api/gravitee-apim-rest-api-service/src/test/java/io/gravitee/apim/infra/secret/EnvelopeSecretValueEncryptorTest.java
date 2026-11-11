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
package io.gravitee.apim.infra.secret;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.gravitee.apim.common.secret.DataKey;
import io.gravitee.apim.common.secret.StaticKeyEncryptionKeyProvider;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.secret.exception.SecretEncryptionUnavailableException;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.repository.management.model.OrganizationKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

class EnvelopeSecretValueEncryptorTest {

    private static final String KEK = base64Key(1);
    private static final String OTHER_KEK = base64Key(2);
    private static final Instant AT = Instant.parse("2026-11-06T10:00:00Z");
    private static final Secret SECRET = new Secret("secret-1", "org-1", "env-1", "anthropic-key", AT, AT);

    private final InMemoryOrganizationKeyRepository keys = new InMemoryOrganizationKeyRepository();
    private final SecretRepository secrets = mock(SecretRepository.class);
    private final EnvelopeSecretValueEncryptor encryptor = node(KEK);

    @Test
    void should_decrypt_what_it_encrypted() {
        String ciphertext = encryptor.encrypt(SECRET, "sk-ant-123");

        assertThat(encryptor.decrypt(SECRET, ciphertext)).isEqualTo("sk-ant-123");
    }

    @Test
    void should_prefix_the_ciphertext_with_the_format_version_and_the_organization_key_id() {
        String ciphertext = encryptor.encrypt(SECRET, "sk-ant-123");

        assertThat(ciphertext).startsWith("v1:" + keys.of("org-1").getId() + ":").doesNotContain("sk-ant-123");
    }

    @Test
    void should_create_one_data_key_on_the_first_write_of_an_organization_and_reuse_it() {
        encryptor.encrypt(SECRET, "sk-ant-123");
        encryptor.encrypt(SECRET.toBuilder().id("secret-2").build(), "sk-ant-456");

        assertThat(keys.all()).hasSize(1);
        assertThat(keys.of("org-1").getKekId()).isEqualTo(StaticKeyEncryptionKeyProvider.fromBase64(KEK).id());
    }

    @Test
    void should_reuse_the_stored_data_key_after_a_restart() {
        String ciphertext = encryptor.encrypt(SECRET, "sk-ant-123");

        var restarted = node(KEK);

        assertThat(restarted.decrypt(SECRET, ciphertext)).isEqualTo("sk-ant-123");
        assertThat(keys.all()).hasSize(1);
    }

    @Test
    void should_store_only_a_wrapped_data_key() {
        String ciphertext = encryptor.encrypt(SECRET, "sk-ant-123");
        var dataKey = encryptor.dataKeyFor("org-1");

        assertThat(keys.of("org-1").getWrappedKey()).isNotEqualTo(Base64.getEncoder().encodeToString(dataKey.material()));
        assertThat(keys.of("org-1").getId()).isEqualTo(dataKey.id());
        assertThat(ciphertext).doesNotContain(Base64.getEncoder().encodeToString(dataKey.material()));
    }

    @Test
    void should_give_each_organization_its_own_data_key() {
        encryptor.encrypt(SECRET, "sk-ant-123");
        encryptor.encrypt(SECRET.toBuilder().organizationId("org-2").build(), "sk-ant-123");

        assertThat(keys.all()).hasSize(2);
        assertThat(encryptor.dataKeyFor("org-1").id()).isNotEqualTo(encryptor.dataKeyFor("org-2").id());
    }

    @Test
    void should_create_a_new_data_key_when_the_organization_key_was_deleted_and_it_has_no_secrets() throws Exception {
        encryptor.encrypt(SECRET, "sk-ant-123");
        String deletedKeyId = keys.of("org-1").getId();

        keys.deleteByOrganizationId("org-1");
        String ciphertext = encryptor.encrypt(SECRET, "sk-ant-456");

        assertThat(keys.of("org-1").getId()).isNotEqualTo(deletedKeyId);
        assertThat(ciphertext).startsWith("v1:" + keys.of("org-1").getId() + ":");
        assertThat(node(KEK).decrypt(SECRET, ciphertext)).isEqualTo("sk-ant-456");
    }

    @Test
    void should_refuse_to_replace_a_deleted_data_key_while_the_organization_has_secrets() throws Exception {
        encryptor.encrypt(SECRET, "sk-ant-123");
        keys.deleteByOrganizationId("org-1");
        when(secrets.existsByOrganizationId("org-1")).thenReturn(true);

        assertThatThrownBy(() -> encryptor.encrypt(SECRET.toBuilder().id("secret-2").build(), "sk-ant-456"))
            .isInstanceOf(TechnicalDomainException.class)
            .hasMessageContaining("Organization org-1 has secrets but no data key");
        assertThat(keys.all()).isEmpty();
    }

    @Test
    void should_refuse_to_decrypt_the_value_of_another_organization() {
        String ciphertext = encryptor.encrypt(SECRET, "sk-ant-123");

        var other = SECRET.toBuilder().organizationId("org-2").build();
        encryptor.encrypt(other, "sk-ant-456");
        assertThatThrownBy(() -> encryptor.decrypt(other, ciphertext)).isInstanceOf(TechnicalDomainException.class);
    }

    @Test
    void should_refuse_to_decrypt_a_ciphertext_copied_onto_another_secret() {
        String ciphertext = encryptor.encrypt(SECRET, "sk-ant-123");

        var other = SECRET.toBuilder().id("secret-2").key("prod-db-password").build();
        assertThatThrownBy(() -> encryptor.decrypt(other, ciphertext)).isInstanceOf(TechnicalDomainException.class);
    }

    @Test
    void should_refuse_to_decrypt_a_ciphertext_copied_onto_another_environment() {
        String ciphertext = encryptor.encrypt(SECRET, "sk-ant-123");

        var other = SECRET.toBuilder().environmentId("env-2").build();
        assertThatThrownBy(() -> encryptor.decrypt(other, ciphertext)).isInstanceOf(TechnicalDomainException.class);
    }

    @Test
    void should_refuse_to_decrypt_a_malformed_ciphertext_and_say_why() {
        encryptor.encrypt(SECRET, "sk-ant-123");

        assertThatThrownBy(() -> encryptor.decrypt(SECRET, "AAAAAAAAAAAAAAAAAAAAAAAA"))
            .isInstanceOf(TechnicalDomainException.class)
            .hasMessage("The secret value is not a v1 envelope");
    }

    @Test
    void should_use_the_data_key_of_the_node_that_won_a_concurrent_first_write() {
        var otherNode = node(KEK);
        keys.beforeNextCreate(() -> otherNode.encrypt(SECRET, "sk-ant-other-node"));

        String ciphertext = encryptor.encrypt(SECRET, "sk-ant-123");

        assertThat(keys.all()).hasSize(1);
        assertThat(otherNode.decrypt(SECRET, ciphertext)).isEqualTo("sk-ant-123");
    }

    @Test
    void should_give_one_data_key_when_many_first_writes_of_one_organization_run_at_once() throws Exception {
        int writers = 8;
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(writers);
        try {
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < writers; i++) {
                var node = node(KEK);
                results.add(
                    pool.submit(() -> {
                        start.await();
                        return node.encrypt(SECRET, "sk-ant-123");
                    })
                );
            }
            start.countDown();

            Set<String> keyIds = new HashSet<>();
            for (var result : results) {
                String ciphertext = result.get(10, TimeUnit.SECONDS);
                assertThat(encryptor.decrypt(SECRET, ciphertext)).isEqualTo("sk-ant-123");
                keyIds.add(ciphertext.split(":")[1]);
            }
            assertThat(keys.all()).hasSize(1);
            assertThat(keyIds).containsExactly(keys.of("org-1").getId());
        } finally {
            pool.shutdownNow();
        }
    }

    @Nested
    class StorageFailure {

        @Test
        void should_fail_when_the_data_key_cannot_be_read_and_recover_once_it_can() {
            keys.failOn("findByOrganizationId");

            assertThatThrownBy(() -> encryptor.encrypt(SECRET, "sk-ant-123"))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("Failed to read the data key of organization org-1");

            keys.failOn();
            assertThat(encryptor.decrypt(SECRET, encryptor.encrypt(SECRET, "sk-ant-123"))).isEqualTo("sk-ant-123");
        }

        @Test
        void should_fail_when_the_data_key_cannot_be_stored() {
            keys.failOn("create");

            assertThatThrownBy(() -> encryptor.encrypt(SECRET, "sk-ant-123"))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("Failed to store the data key of organization org-1");
        }

        @Test
        void should_fail_when_the_stored_key_encryption_key_ids_cannot_be_read_and_check_again_later() {
            keys.failOn("findKekIds");

            assertThatThrownBy(encryptor::verifyStoredKeys)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("Failed to read the key encryption key ids of the stored secrets");
            assertThatThrownBy(() -> encryptor.encrypt(SECRET, "sk-ant-123")).isInstanceOf(TechnicalDomainException.class);
            assertThat(keys.all()).isEmpty();

            keys.failOn();
            assertThat(encryptor.decrypt(SECRET, encryptor.encrypt(SECRET, "sk-ant-123"))).isEqualTo("sk-ant-123");
        }

        @Test
        void should_fail_when_the_secrets_of_the_organization_cannot_be_checked() throws Exception {
            when(secrets.existsByOrganizationId("org-1")).thenThrow(new TechnicalException("down"));

            assertThatThrownBy(() -> encryptor.encrypt(SECRET, "sk-ant-123"))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("Failed to check for the secrets of organization org-1");
            assertThat(keys.all()).isEmpty();
        }
    }

    @Nested
    class Unavailable {

        private final Logger logger = (Logger) LoggerFactory.getLogger(EnvelopeSecretValueEncryptor.class);
        private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

        Unavailable() {
            appender.start();
            logger.addAppender(appender);
        }

        @AfterEach
        void detachAppender() {
            logger.detachAppender(appender);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { " ", "not base64!", "c2hvcnQ=" })
        void should_start_with_a_missing_or_invalid_key_and_refuse_to_encrypt_or_decrypt(String kek) {
            var unavailable = node(kek);

            assertThatThrownBy(() -> unavailable.encrypt(SECRET, "sk-ant-123"))
                .isInstanceOf(SecretEncryptionUnavailableException.class)
                .hasMessageContaining(EnvelopeSecretValueEncryptor.KEY_PROPERTY);
            assertThatThrownBy(() -> unavailable.decrypt(SECRET, "v1:key:AAAA")).isInstanceOf(SecretEncryptionUnavailableException.class);
            assertThat(keys.all()).isEmpty();
        }

        @Test
        void should_warn_once_at_startup_when_the_key_is_not_set() {
            node(null).verifyStoredKeys();

            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.getFirst().getLevel()).isEqualTo(Level.WARN);
            assertThat(appender.list.getFirst().getFormattedMessage()).contains(
                EnvelopeSecretValueEncryptor.KEY_PROPERTY,
                "is not set",
                "Set it to a base64-encoded 256-bit key."
            );
        }

        @Test
        void should_log_an_invalid_key_once_at_startup_as_an_error_without_the_key() {
            String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

            node(shortKey);

            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.getFirst().getLevel()).isEqualTo(Level.ERROR);
            assertThat(appender.list.getFirst().getFormattedMessage())
                .contains(
                    EnvelopeSecretValueEncryptor.KEY_PROPERTY,
                    "must decode to 32 bytes, not 16",
                    "Set it to a base64-encoded 256-bit key."
                )
                .doesNotContain(shortKey);
        }

        @Test
        void should_report_at_startup_a_key_that_does_not_match_the_stored_keys() {
            encryptor.encrypt(SECRET, "sk-ant-123");

            node(OTHER_KEK).verifyStoredKeys();

            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.getFirst().getLevel()).isEqualTo(Level.ERROR);
            assertThat(appender.list.getFirst().getFormattedMessage())
                .contains(EnvelopeSecretValueEncryptor.KEY_PROPERTY, "does not match")
                .doesNotContain(OTHER_KEK, KEK);
        }

        @Test
        void should_refuse_to_create_a_data_key_under_another_key_than_the_stored_ones_even_after_its_startup_check() {
            var otherNode = node(OTHER_KEK);
            encryptor.verifyStoredKeys();
            otherNode.verifyStoredKeys();
            encryptor.encrypt(SECRET, "sk-ant-123");

            assertThatThrownBy(() -> otherNode.encrypt(SECRET.toBuilder().organizationId("org-2").build(), "sk-ant-456"))
                .isInstanceOf(SecretEncryptionUnavailableException.class)
                .hasMessageContaining("does not match");
            assertThat(keys.all()).extracting(OrganizationKey::getOrganizationId).containsExactly("org-1");
        }

        @Test
        void should_fail_only_the_organization_whose_data_key_another_key_wrapped() {
            String ciphertext = encryptor.encrypt(SECRET, "sk-ant-123");
            keys.store(wrappedUnder(OTHER_KEK, "org-2"));
            var otherOrg = SECRET.toBuilder().organizationId("org-2").build();

            assertThatThrownBy(() -> encryptor.encrypt(otherOrg, "sk-ant-456"))
                .isInstanceOf(SecretEncryptionUnavailableException.class)
                .hasMessageContaining("does not match the key that wrapped the data key of organization org-2");
            assertThat(encryptor.decrypt(SECRET, ciphertext)).isEqualTo("sk-ant-123");
            assertThat(encryptor.encrypt(SECRET.toBuilder().id("secret-2").build(), "sk-ant-789")).isNotBlank();
        }

        @Test
        void should_recover_without_a_restart_once_the_data_key_of_another_key_is_removed() throws Exception {
            keys.store(wrappedUnder(OTHER_KEK, "org-2"));
            var otherOrg = SECRET.toBuilder().organizationId("org-2").build();
            assertThatThrownBy(() -> encryptor.encrypt(otherOrg, "sk-ant-456")).isInstanceOf(SecretEncryptionUnavailableException.class);

            keys.deleteByOrganizationId("org-2");

            assertThat(encryptor.decrypt(otherOrg, encryptor.encrypt(otherOrg, "sk-ant-456"))).isEqualTo("sk-ant-456");
        }

        @Test
        void should_treat_a_data_key_stored_without_a_key_encryption_key_id_as_wrapped_by_another_key() {
            String ciphertext = encryptor.encrypt(SECRET, "sk-ant-123");
            keys.store(wrappedUnder(KEK, "org-2").toBuilder().kekId(null).build());

            encryptor.verifyStoredKeys();

            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage).singleElement().asString().contains("does not match");
            assertThatThrownBy(() -> encryptor.encrypt(SECRET.toBuilder().organizationId("org-2").build(), "sk-ant-456")).isInstanceOf(
                SecretEncryptionUnavailableException.class
            );
            assertThat(encryptor.decrypt(SECRET, ciphertext)).isEqualTo("sk-ant-123");
        }

        @Test
        void should_pass_the_startup_check_with_the_key_that_encrypted_the_stored_keys() {
            String ciphertext = encryptor.encrypt(SECRET, "sk-ant-123");
            var restarted = node(KEK);

            restarted.verifyStoredKeys();

            assertThat(appender.list).isEmpty();
            assertThat(restarted.decrypt(SECRET, ciphertext)).isEqualTo("sk-ant-123");
        }

        @Test
        void should_pass_the_startup_check_when_no_key_is_stored_yet() {
            encryptor.verifyStoredKeys();

            assertThat(appender.list).isEmpty();
            assertThat(encryptor.decrypt(SECRET, encryptor.encrypt(SECRET, "sk-ant-123"))).isEqualTo("sk-ant-123");
        }

        @Test
        void should_refuse_to_decrypt_with_a_key_that_does_not_match_the_stored_keys() {
            String ciphertext = encryptor.encrypt(SECRET, "sk-ant-123");

            var wrong = node(OTHER_KEK);

            assertThatThrownBy(() -> wrong.decrypt(SECRET, ciphertext))
                .isInstanceOf(SecretEncryptionUnavailableException.class)
                .hasMessageContaining("does not match");
            assertThat(keys.all()).hasSize(1);
            assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .allSatisfy(message -> assertThat(message).doesNotContain(OTHER_KEK, KEK, "sk-ant"));
        }
    }

    private EnvelopeSecretValueEncryptor node(String kek) {
        return new EnvelopeSecretValueEncryptor(kek, keys, secrets);
    }

    private static OrganizationKey wrappedUnder(String kek, String organizationId) {
        var provider = StaticKeyEncryptionKeyProvider.fromBase64(kek);
        var dataKey = DataKey.generate();
        return OrganizationKey.builder()
            .id(dataKey.id())
            .organizationId(organizationId)
            .wrappedKey(Base64.getEncoder().encodeToString(provider.wrap(organizationId, dataKey.material())))
            .kekId(provider.id())
            .createdAt(Date.from(AT))
            .build();
    }

    private static String base64Key(int seed) {
        var key = new byte[32];
        key[0] = (byte) seed;
        return Base64.getEncoder().encodeToString(key);
    }

    private static class InMemoryOrganizationKeyRepository implements OrganizationKeyRepository {

        private final Map<String, OrganizationKey> byOrganization = new ConcurrentHashMap<>();
        private volatile Runnable beforeNextCreate;
        private volatile Set<String> failingOperations = Set.of();

        void beforeNextCreate(Runnable action) {
            this.beforeNextCreate = action;
        }

        void failOn(String... operations) {
            this.failingOperations = Set.of(operations);
        }

        void store(OrganizationKey organizationKey) {
            byOrganization.put(organizationKey.getOrganizationId(), organizationKey);
        }

        OrganizationKey of(String organizationId) {
            return byOrganization.get(organizationId);
        }

        List<OrganizationKey> all() {
            return List.copyOf(byOrganization.values());
        }

        @Override
        public OrganizationKey create(OrganizationKey organizationKey) throws TechnicalException {
            failIfAsked("create");
            var action = beforeNextCreate;
            if (action != null) {
                beforeNextCreate = null;
                action.run();
            }
            if (byOrganization.putIfAbsent(organizationKey.getOrganizationId(), organizationKey) != null) {
                throw new DuplicateKeyException("Organization [" + organizationKey.getOrganizationId() + "] already has a key");
            }
            return organizationKey;
        }

        @Override
        public Optional<OrganizationKey> findByOrganizationId(String organizationId) throws TechnicalException {
            failIfAsked("findByOrganizationId");
            return Optional.ofNullable(byOrganization.get(organizationId));
        }

        @Override
        public Set<String> findKekIds() throws TechnicalException {
            failIfAsked("findKekIds");
            return byOrganization.values().stream().map(OrganizationKey::getKekId).collect(Collectors.toCollection(HashSet::new));
        }

        @Override
        public void deleteByOrganizationId(String organizationId) {
            byOrganization.remove(organizationId);
        }

        private void failIfAsked(String operation) throws TechnicalException {
            if (failingOperations.contains(operation)) {
                throw new TechnicalException(operation + " failed");
            }
        }
    }
}
