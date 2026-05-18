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

import io.gravitee.gamma.repository.authorization.model.AuthorizationEntity;
import io.gravitee.gamma.repository.authorization.model.AuthorizationEntityKind;
import io.gravitee.gamma.repository.exceptions.TechnicalException;
import java.time.Instant;
import java.util.Optional;
import org.junit.Test;

public class AuthorizationEntityRepositoryTest extends AbstractGammaRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/authorization-entity-tests/";
    }

    @Override
    protected Class<?> getClassFromFileName(String baseName) {
        return AuthorizationEntity.class;
    }

    @Override
    protected void createModel(Object object) throws TechnicalException {
        authorizationEntityRepository.create((AuthorizationEntity) object);
    }

    @Test
    public void should_find_by_environment_id_and_id() throws TechnicalException {
        Optional<AuthorizationEntity> found = authorizationEntityRepository.findByEnvironmentIdAndId("DEFAULT", "entity-1");
        assertThat(found).isPresent();
        assertThat(found.get().entityId()).isEqualTo("user-alice");
    }

    @Test
    public void should_find_by_environment_id_and_entity_id() throws TechnicalException {
        Optional<AuthorizationEntity> found = authorizationEntityRepository.findByEnvironmentIdAndEntityId("DEFAULT", "user-alice");
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo("entity-1");
    }

    @Test
    public void should_find_all_by_environment_id() throws TechnicalException {
        assertThat(authorizationEntityRepository.findAllByEnvironmentId("DEFAULT")).hasSize(3);
        assertThat(authorizationEntityRepository.findAllByEnvironmentId("OTHER")).hasSize(1);
    }

    @Test
    public void should_find_all_by_environment_id_and_kind() throws TechnicalException {
        assertThat(authorizationEntityRepository.findAllByEnvironmentIdAndKind("DEFAULT", AuthorizationEntityKind.PRINCIPAL)).hasSize(2);
        assertThat(authorizationEntityRepository.findAllByEnvironmentIdAndKind("DEFAULT", AuthorizationEntityKind.RESOURCE)).hasSize(1);
    }

    @Test
    public void should_find_all_by_environment_id_and_entity_id_starting_with() throws TechnicalException {
        assertThat(authorizationEntityRepository.findAllByEnvironmentIdAndEntityIdStartingWith("DEFAULT", "user-")).hasSize(2);
        assertThat(authorizationEntityRepository.findAllByEnvironmentIdAndEntityIdStartingWith("DEFAULT", "doc-")).hasSize(1);
    }

    @Test
    public void should_create_entity() throws TechnicalException {
        AuthorizationEntity entity = AuthorizationEntity.builder()
            .id("created-1")
            .entityId("new-user")
            .kind(AuthorizationEntityKind.PRINCIPAL)
            .environmentId("DEFAULT")
            .createdAt(Instant.parse("2026-05-01T10:00:00Z"))
            .updatedAt(Instant.parse("2026-05-01T10:00:00Z"))
            .build();

        AuthorizationEntity created = authorizationEntityRepository.create(entity);

        assertThat(created.id()).isEqualTo("created-1");
        assertThat(authorizationEntityRepository.findById("created-1")).isPresent();
    }

    @Test
    public void should_update_entity() throws TechnicalException {
        AuthorizationEntity existing = authorizationEntityRepository.findById("entity-1").orElseThrow();
        existing.source("updated-source");

        AuthorizationEntity updated = authorizationEntityRepository.update(existing);

        assertThat(updated.source()).isEqualTo("updated-source");
        assertThat(authorizationEntityRepository.findById("entity-1").orElseThrow().source()).isEqualTo("updated-source");
    }

    @Test
    public void should_throw_when_updating_missing_entity() {
        AuthorizationEntity entity = AuthorizationEntity.builder().id("missing").build();
        assertThatThrownBy(() -> authorizationEntityRepository.update(entity)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void should_delete_by_environment_id_and_id() throws TechnicalException {
        long deleted = authorizationEntityRepository.deleteByEnvironmentIdAndId("DEFAULT", "entity-2");

        assertThat(deleted).isEqualTo(1L);
        assertThat(authorizationEntityRepository.findByEnvironmentIdAndId("DEFAULT", "entity-2")).isEmpty();
    }

    @Test
    public void should_delete_by_environment_id_and_entity_id() throws TechnicalException {
        long deleted = authorizationEntityRepository.deleteByEnvironmentIdAndEntityId("DEFAULT", "doc-readme");

        assertThat(deleted).isEqualTo(1L);
        assertThat(authorizationEntityRepository.findByEnvironmentIdAndEntityId("DEFAULT", "doc-readme")).isEmpty();
    }
}
