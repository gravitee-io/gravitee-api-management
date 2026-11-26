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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.portals;

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
public class RemovePortalUrlUpgraderTest extends AbstractManagementRepositoryTest {

    @Inject
    private MongoTemplate mongoTemplate;

    @Inject
    private Environment environment;

    private RemovePortalUrlUpgrader upgrader;
    private String portals;

    @Override
    protected String getTestCasesPath() {
        // No fixtures: the upgrader manipulates the collection directly.
        return null;
    }

    @BeforeEach
    public void initUpgrader() {
        upgrader = new RemovePortalUrlUpgrader();
        upgrader.setMongoTemplate(mongoTemplate);
        upgrader.setEnvironment(environment);
        portals = environment.getProperty("management.mongodb.prefix", "") + "portals";
        // deleteMany, not drop: drop() also removes the indexes created once at context bootstrap
        mongoTemplate.getCollection(portals).deleteMany(new Document());
    }

    @AfterEach
    public void cleanUp() {
        mongoTemplate.getCollection(portals).deleteMany(new Document());
    }

    private Document portal(String id) {
        return mongoTemplate.getCollection(portals).find(new Document("_id", id)).first();
    }

    @Test
    public void should_remove_the_stored_url_and_keep_the_other_fields() {
        mongoTemplate
            .getCollection(portals)
            .insertOne(
                new Document("_id", "portal-1")
                    .append("environmentId", "DEFAULT")
                    .append("name", "Developer Portal")
                    .append("url", "https://portal.acme.io/next")
                    .append("accessPointId", "access-point-1")
            );

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(portal("portal-1"))
            .doesNotContainKey("url")
            .containsEntry("name", "Developer Portal")
            .containsEntry("accessPointId", "access-point-1");
    }

    @Test
    public void should_leave_a_portal_stored_without_url_unchanged() {
        mongoTemplate
            .getCollection(portals)
            .insertOne(new Document("_id", "portal-2").append("environmentId", "DEFAULT").append("name", "Partners"));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(portal("portal-2")).doesNotContainKey("url").containsEntry("name", "Partners");
    }

    @Test
    public void should_succeed_when_there_is_no_portal() {
        assertThat(upgrader.upgrade()).isTrue();
    }
}
