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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.guardrails;

import static io.gravitee.repository.mongodb.management.upgrade.upgrader.guardrails.GuardrailVersionMongoUpgrader.VERSION;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class GuardrailVersionMongoUpgraderTest extends AbstractManagementRepositoryTest {

    @Inject
    private MongoTemplate mongoTemplate;

    @Inject
    private Environment environment;

    private GuardrailVersionMongoUpgrader upgrader;
    private String guardrails;

    @Override
    protected String getTestCasesPath() {
        // No fixtures: the upgrader manipulates the collection directly.
        return null;
    }

    @BeforeEach
    public void initUpgrader() {
        upgrader = new GuardrailVersionMongoUpgrader();
        upgrader.setMongoTemplate(mongoTemplate);
        upgrader.setEnvironment(environment);
        guardrails = environment.getProperty("management.mongodb.prefix", "") + "guardrails";
        // deleteMany, not drop: drop() also removes the indexes created once at context bootstrap
        mongoTemplate.getCollection(guardrails).deleteMany(new Document());
    }

    @AfterEach
    public void cleanUp() {
        mongoTemplate.getCollection(guardrails).deleteMany(new Document());
    }

    private Object versionOf(String id) {
        return mongoTemplate.getCollection(guardrails).find(new Document("_id", id)).first().get(VERSION);
    }

    private void givenAGuardrail(Document guardrail) {
        // the unique (environmentId, name) index needs each of them to carry its own name
        mongoTemplate
            .getCollection(guardrails)
            .insertOne(guardrail.append("environmentId", "DEFAULT").append("name", guardrail.getString("_id")));
    }

    @Test
    public void should_give_version_1_to_a_guardrail_stored_without_one() {
        givenAGuardrail(new Document("_id", "stored-before"));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(versionOf("stored-before")).isEqualTo(1);
    }

    @Test
    public void should_give_version_1_to_a_guardrail_whose_version_is_null() {
        givenAGuardrail(new Document("_id", "null-version").append(VERSION, null));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(versionOf("null-version")).isEqualTo(1);
    }

    /** A second node running the upgrader, or a Guardrail saved since, must not be sent back to 1. */
    @Test
    public void should_leave_a_guardrail_that_already_has_a_version() {
        givenAGuardrail(new Document("_id", "written-since").append(VERSION, 7));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(versionOf("written-since")).isEqualTo(7);
    }

    @Test
    public void should_succeed_when_there_is_no_guardrail() {
        assertThat(upgrader.upgrade()).isTrue();
    }
}
