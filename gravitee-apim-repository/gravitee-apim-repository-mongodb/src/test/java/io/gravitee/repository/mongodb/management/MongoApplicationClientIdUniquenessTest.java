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

import static io.gravitee.repository.management.model.Application.METADATA_CLIENT_ID;
import static io.gravitee.repository.utils.DateUtils.parse;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.management.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The client_id uniqueness is enforced by a unique index only created on MongoDB, hence a MongoDB specific test
 * instead of a shared repository test.
 *
 * @author GraviteeSource Team
 */
public class MongoApplicationClientIdUniquenessTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/application-tests/";
    }

    @Test
    public void should_not_create_two_active_applications_with_same_client_id_in_same_environment() {
        assertThatThrownBy(() -> applicationRepository.create(anApplicationWithClientId("duplicate", "PROD", "my-client-id"))).isInstanceOf(
            DuplicateKeyException.class
        );
    }

    @Test
    public void should_create_application_with_same_client_id_in_another_environment() throws Exception {
        applicationRepository.create(anApplicationWithClientId("other-env", "DEFAULT", "my-client-id"));

        assertTrue(applicationRepository.findById("other-env").isPresent());
    }

    @Test
    public void should_create_application_with_client_id_of_an_archived_application() throws Exception {
        applicationRepository.create(anApplicationWithClientId("reusing-archived", "PROD", "my-client-id-old"));

        assertTrue(applicationRepository.findById("reusing-archived").isPresent());
    }

    @Test
    public void should_not_update_an_application_to_use_an_existing_client_id() throws Exception {
        Application application = applicationRepository.create(anApplicationWithClientId("to-be-updated", "PROD", "its-own-client-id"));
        application.getMetadata().put(METADATA_CLIENT_ID, "my-client-id");

        assertThatThrownBy(() -> applicationRepository.update(application)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    public void should_update_an_application_keeping_its_own_client_id() throws Exception {
        Application application = applicationRepository.create(anApplicationWithClientId("keeping-client-id", "PROD", "kept-client-id"));
        application.setName("renamed");

        Application updated = applicationRepository.update(application);

        assertEquals("renamed", updated.getName());
        assertEquals("kept-client-id", updated.getMetadata().get(METADATA_CLIENT_ID));
    }

    @Test
    public void should_release_client_id_when_application_is_archived() throws Exception {
        Application application = applicationRepository.findById("app-with-client-id").orElseThrow();
        application.setStatus(ApplicationStatus.ARCHIVED);
        applicationRepository.update(application);

        applicationRepository.create(anApplicationWithClientId("taking-over", "PROD", "my-client-id"));

        assertTrue(applicationRepository.findById("taking-over").isPresent());
    }

    private Application anApplicationWithClientId(String id, String environmentId, String clientId) {
        Application application = new Application();
        application.setId(id);
        application.setName(id);
        application.setEnvironmentId(environmentId);
        application.setType(ApplicationType.SIMPLE);
        application.setStatus(ApplicationStatus.ACTIVE);
        application.setCreatedAt(parse("11/02/2016"));
        application.setUpdatedAt(parse("11/02/2016"));
        application.setMetadata(new HashMap<>(Map.of(METADATA_CLIENT_ID, clientId)));
        return application;
    }
}
