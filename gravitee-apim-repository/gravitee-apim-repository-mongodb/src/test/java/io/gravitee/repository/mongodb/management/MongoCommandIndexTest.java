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
package io.gravitee.repository.mongodb.management;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;
import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoDatabase;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.api.search.CommandCriteria;
import io.gravitee.repository.management.api.search.CommandCursor;
import io.gravitee.repository.management.model.Command;
import jakarta.inject.Inject;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;

public class MongoCommandIndexTest extends AbstractManagementRepositoryTest {

    private static final String EXPECTED_INDEX_NAME = "to1ca1i1";

    @Inject
    private MongoTemplate mongoTemplate;

    @Value("${management.mongodb.prefix:}")
    private String tablePrefix;

    @Override
    protected String getTestCasesPath() {
        return "/data/command-tests/";
    }

    /**
     * Profiles the pages the Management API commands refresher reads through
     * {@link io.gravitee.repository.mongodb.management.MongoCommandRepository#searchAfter}, the first one and one after a
     * cursor: each must come in index order, without sorting every pending command in memory.
     */
    @Test
    public void searchAfter_shouldReadThePageInIndexOrder() throws Exception {
        for (int i = 0; i < 20; i++) {
            Command command = new Command();
            command.setId("pending-" + i);
            command.setFrom("node-0");
            command.setTo("MANAGEMENT_APIS");
            command.setTags(List.of(i % 2 == 0 ? "SUBSCRIPTION_FAILURE" : "EMAIL_TEMPLATE_UPDATE"));
            command.setCreatedAt(new Date(1_000L * (20 - i)));
            commandRepository.create(command);
        }
        CommandCriteria criteria = new CommandCriteria.Builder()
            .to("MANAGEMENT_APIS")
            .tags("SUBSCRIPTION_FAILURE", "EMAIL_TEMPLATE_UPDATE")
            .notAckBy("node-1")
            .build();

        assertReadsInIndexOrder(criteria, null);
        assertReadsInIndexOrder(criteria, new CommandCursor(5_000L, "pending-15"));
    }

    private void assertReadsInIndexOrder(CommandCriteria criteria, CommandCursor after) throws Exception {
        MongoDatabase database = mongoTemplate.getDb();
        Document profiled;
        database.runCommand(new Document("profile", 2));
        try {
            commandRepository.searchAfter(criteria, after, 5);
        } finally {
            database.runCommand(new Document("profile", 0));
        }
        // Dropped here: the repository tear down clears every collection, and the server refuses to clear this one.
        try {
            profiled = database
                .getCollection("system.profile")
                .find(and(eq("ns", database.getName() + "." + tablePrefix + "commands"), eq("op", "query")))
                .sort(descending("ts"))
                .first();
        } finally {
            database.getCollection("system.profile").drop();
        }
        assertThat(profiled).as("the page read should be profiled").isNotNull();
        assertThat(profiled.getString("planSummary"))
            .as("the page read should scan the %s index. Profile: %s", EXPECTED_INDEX_NAME, profiled.toJson())
            .isEqualTo("IXSCAN { to: 1, createdAt: 1, _id: 1 }");
        assertThat(profiled.getBoolean("hasSortStage", false))
            .as("the page read should not sort in memory. Profile: %s", profiled.toJson())
            .isFalse();
    }
}
