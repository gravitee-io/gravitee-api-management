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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.model.Portal;
import jakarta.inject.Inject;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoOperations;

/**
 * Portals stored before the default marker and the URL existed have neither field: a MongoDB specific test writing
 * such a document directly, since the repository always writes both.
 *
 * @author GraviteeSource Team
 */
public class MongoPortalStoredDefaultsTest extends AbstractManagementRepositoryTest {

    @Inject
    private MongoOperations mongoOperations;

    @Inject
    private Environment environment;

    @Override
    protected String getTestCasesPath() {
        return null;
    }

    @Test
    public void should_read_a_portal_stored_without_default_marker_and_url_as_not_default_with_placeholder_url() throws Exception {
        mongoOperations
            .getCollection(environment.getProperty("management.mongodb.prefix", "") + "portals")
            .insertOne(
                new Document("_id", "legacy-portal")
                    .append("environmentId", "environment1")
                    .append("organizationId", "organization1")
                    .append("name", "Legacy Portal")
            );

        assertThat(portalRepository.findById("legacy-portal")).hasValueSatisfying(found -> {
            assertThat(found.isDefaultPortal()).isFalse();
            assertThat(found.getUrl()).isEqualTo(Portal.URL_PLACEHOLDER);
        });
    }
}
