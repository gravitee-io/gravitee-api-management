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
package io.gravitee.repository.mongodb.management.upgrade.initializer.index.roles;

import static io.gravitee.repository.mongodb.management.upgrade.initializer.index.roles.ScopeNameReferenceTypeReferenceIdUniqueIndexInitializer.INDEX_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.StreamSupport;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoOperations;

public class ScopeNameReferenceTypeReferenceIdUniqueIndexInitializerTest extends AbstractManagementRepositoryTest {

    private static final List<String> DUPLICATED_ROLE_IDS = List.of("duplicate-role-1", "duplicate-role-2");

    @Inject
    private ScopeNameReferenceTypeReferenceIdUniqueIndexInitializer initializer;

    @Inject
    private MongoOperations mongoOperations;

    @Inject
    private Environment environment;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Before
    public void dropIndex() {
        if (indexNames().contains(INDEX_NAME)) {
            roles().dropIndex(INDEX_NAME);
        }
    }

    @After
    public void restoreIndex() {
        roles().deleteMany(Filters.in("_id", DUPLICATED_ROLE_IDS));
        initializer.initialize();
    }

    @Test
    public void should_create_unique_index_when_no_role_is_duplicated() {
        assertThat(initializer.initialize()).isTrue();

        assertThat(indexNames()).contains(INDEX_NAME);
    }

    @Test
    public void should_keep_a_single_unique_index_when_run_several_times() {
        assertThat(initializer.initialize()).isTrue();
        final var indexCountAfterFirstRun = indexNames().size();

        assertThat(initializer.initialize()).isTrue();

        assertThat(indexNames()).hasSize(indexCountAfterFirstRun).containsOnlyOnce(INDEX_NAME);
    }

    @Test
    public void should_not_create_index_and_return_false_when_roles_are_duplicated() {
        insertDuplicatedRoles();

        assertThat(initializer.initialize()).isFalse();

        assertThat(indexNames()).doesNotContain(INDEX_NAME);
        assertThat(roles().countDocuments(Filters.in("_id", DUPLICATED_ROLE_IDS))).isEqualTo(DUPLICATED_ROLE_IDS.size());
    }

    @Test
    public void should_create_unique_index_at_next_run_once_duplicated_roles_are_removed() {
        insertDuplicatedRoles();
        initializer.initialize();
        roles().deleteOne(Filters.eq("_id", DUPLICATED_ROLE_IDS.get(1)));

        assertThat(initializer.initialize()).isTrue();

        assertThat(indexNames()).contains(INDEX_NAME);
    }

    @Test
    public void should_reject_a_second_role_with_the_same_scope_name_and_reference() {
        initializer.initialize();
        roles().insertOne(aRole(DUPLICATED_ROLE_IDS.get(0)));

        assertThatThrownBy(() -> roles().insertOne(aRole(DUPLICATED_ROLE_IDS.get(1)))).isInstanceOf(MongoWriteException.class);
    }

    private void insertDuplicatedRoles() {
        DUPLICATED_ROLE_IDS.forEach(id -> roles().insertOne(aRole(id)));
    }

    private MongoCollection<Document> roles() {
        return mongoOperations.getCollection(environment.getProperty("management.mongodb.prefix", "") + "roles");
    }

    private List<String> indexNames() {
        return StreamSupport.stream(roles().listIndexes().spliterator(), false)
            .map(index -> index.getString("name"))
            .toList();
    }

    private Document aRole(String id) {
        return new Document("_id", id)
            .append("scope", "ENVIRONMENT")
            .append("name", "DUPLICATED_ROLE")
            .append("referenceType", "ORGANIZATION")
            .append("referenceId", "DEFAULT");
    }
}
