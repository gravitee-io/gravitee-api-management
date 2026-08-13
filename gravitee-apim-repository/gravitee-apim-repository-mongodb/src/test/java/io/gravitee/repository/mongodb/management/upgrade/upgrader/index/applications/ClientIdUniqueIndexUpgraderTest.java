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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.applications;

import static io.gravitee.repository.mongodb.management.upgrade.upgrader.index.applications.ClientIdUniqueIndexUpgrader.INDEX_NAME;
import static org.assertj.core.api.Assertions.assertThat;

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

public class ClientIdUniqueIndexUpgraderTest extends AbstractManagementRepositoryTest {

    private static final List<String> DUPLICATED_APPLICATION_IDS = List.of(
        "duplicate-1",
        "duplicate-2"
    );

    @Inject
    private ClientIdUniqueIndexUpgrader upgrader;

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
            applications().dropIndex(INDEX_NAME);
        }
    }

    /**
     * The index is shared with the other tests running against the same database, it has to be put back in place.
     */
    @After
    public void restoreIndex() {
        applications().deleteMany(Filters.in("_id", DUPLICATED_APPLICATION_IDS));
        upgrader.upgrade();
    }

    @Test
    public void should_create_index_when_no_application_shares_a_client_id() {
        assertThat(upgrader.upgrade()).isTrue();

        assertThat(indexNames()).contains(INDEX_NAME);
    }

    @Test
    public void should_not_create_index_nor_fail_when_applications_share_a_client_id() {
        DUPLICATED_APPLICATION_IDS.forEach(id ->
            applications().insertOne(
                anActiveApplication(id, "DEFAULT", "shared-client-id")
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(indexNames()).doesNotContain(INDEX_NAME);
    }

    @Test
    public void should_ignore_a_client_id_shared_with_an_archived_application() {
        applications().insertOne(
            anActiveApplication("duplicate-1", "DEFAULT", "shared-client-id")
        );
        applications().insertOne(
            anActiveApplication("duplicate-2", "DEFAULT", "shared-client-id").append(
                "status",
                "ARCHIVED"
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(indexNames()).contains(INDEX_NAME);
    }

    private MongoCollection<Document> applications() {
        return mongoOperations.getCollection(
            environment.getProperty("management.mongodb.prefix", "") + "applications"
        );
    }

    private List<String> indexNames() {
        return StreamSupport.stream(applications().listIndexes().spliterator(), false)
            .map(index -> index.getString("name"))
            .toList();
    }

    private Document anActiveApplication(
        String id,
        String environmentId,
        String clientId
    ) {
        return new Document("_id", id)
            .append("name", id)
            .append("environmentId", environmentId)
            .append("status", "ACTIVE")
            .append("metadata", new Document("client_id", clientId));
    }
}
