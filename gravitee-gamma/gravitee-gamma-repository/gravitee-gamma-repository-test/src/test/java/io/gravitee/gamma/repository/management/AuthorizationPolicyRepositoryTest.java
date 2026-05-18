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
package io.gravitee.gamma.repository.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicy;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicyKind;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicyStatus;
import io.gravitee.gamma.repository.exceptions.TechnicalException;
import java.time.Instant;
import java.util.Optional;
import org.junit.Test;

public class AuthorizationPolicyRepositoryTest extends AbstractGammaRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/authorization-policy-tests/";
    }

    @Override
    protected Class<?> getClassFromFileName(String baseName) {
        return AuthorizationPolicy.class;
    }

    @Override
    protected void createModel(Object object) throws TechnicalException {
        authorizationPolicyRepository.create((AuthorizationPolicy) object);
    }

    @Test
    public void should_find_by_environment_id_and_id() throws TechnicalException {
        Optional<AuthorizationPolicy> found = authorizationPolicyRepository.findByEnvironmentIdAndId("DEFAULT", "policy-1");
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Admin policy");
    }

    @Test
    public void should_find_all_by_environment_id() throws TechnicalException {
        assertThat(authorizationPolicyRepository.findAllByEnvironmentId("DEFAULT")).hasSize(2);
        assertThat(authorizationPolicyRepository.findAllByEnvironmentId("OTHER")).isEmpty();
    }

    @Test
    public void should_find_all_by_environment_id_and_kind() throws TechnicalException {
        assertThat(authorizationPolicyRepository.findAllByEnvironmentIdAndKind("DEFAULT", AuthorizationPolicyKind.GLOBAL)).hasSize(1);
        assertThat(authorizationPolicyRepository.findAllByEnvironmentIdAndKind("DEFAULT", AuthorizationPolicyKind.RESOURCE)).hasSize(1);
    }

    @Test
    public void should_find_all_by_environment_id_and_entity_id() throws TechnicalException {
        assertThat(authorizationPolicyRepository.findAllByEnvironmentIdAndEntityId("DEFAULT", "user-alice")).hasSize(1);
        assertThat(authorizationPolicyRepository.findAllByEnvironmentIdAndEntityId("DEFAULT", "doc-readme")).hasSize(1);
    }

    @Test
    public void should_create_policy() throws TechnicalException {
        AuthorizationPolicy policy = AuthorizationPolicy.builder()
            .id("created-1")
            .name("New policy")
            .kind(AuthorizationPolicyKind.GLOBAL)
            .entityId("user-bob")
            .policyText("allow if true")
            .status(AuthorizationPolicyStatus.DRAFT)
            .environmentId("DEFAULT")
            .createdAt(Instant.parse("2026-05-01T10:00:00Z"))
            .updatedAt(Instant.parse("2026-05-01T10:00:00Z"))
            .build();

        AuthorizationPolicy created = authorizationPolicyRepository.create(policy);

        assertThat(created.id()).isEqualTo("created-1");
        assertThat(authorizationPolicyRepository.findById("created-1")).isPresent();
    }

    @Test
    public void should_update_policy() throws TechnicalException {
        AuthorizationPolicy existing = authorizationPolicyRepository.findById("policy-1").orElseThrow();
        existing.status(AuthorizationPolicyStatus.DISABLED);

        AuthorizationPolicy updated = authorizationPolicyRepository.update(existing);

        assertThat(updated.status()).isEqualTo(AuthorizationPolicyStatus.DISABLED);
        assertThat(authorizationPolicyRepository.findById("policy-1").orElseThrow().status()).isEqualTo(AuthorizationPolicyStatus.DISABLED);
    }

    @Test
    public void should_throw_when_updating_missing_policy() {
        AuthorizationPolicy policy = AuthorizationPolicy.builder().id("missing").build();
        assertThatThrownBy(() -> authorizationPolicyRepository.update(policy)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void should_delete_by_environment_id_and_id() throws TechnicalException {
        long deleted = authorizationPolicyRepository.deleteByEnvironmentIdAndId("DEFAULT", "policy-2");

        assertThat(deleted).isEqualTo(1L);
        assertThat(authorizationPolicyRepository.findByEnvironmentIdAndId("DEFAULT", "policy-2")).isEmpty();
    }
}
