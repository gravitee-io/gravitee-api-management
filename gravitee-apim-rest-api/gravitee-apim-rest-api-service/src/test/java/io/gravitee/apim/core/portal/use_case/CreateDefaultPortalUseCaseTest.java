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
package io.gravitee.apim.core.portal.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.portal.model.Portal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateDefaultPortalUseCaseTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENVIRONMENT_ID = "environment-id";

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private CreateDefaultPortalUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateDefaultPortalUseCase(portalCrudService);
    }

    @AfterEach
    void tearDown() {
        portalCrudService.reset();
    }

    @Test
    void should_create_default_portal_when_absent() {
        useCase.execute(ORGANIZATION_ID, ENVIRONMENT_ID);

        assertThat(portalCrudService.storage()).hasSize(1);
        Portal seeded = portalCrudService.storage().get(0);
        assertThat(seeded.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(seeded.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
        assertThat(seeded.getName()).isEqualTo(CreateDefaultPortalUseCase.DEFAULT_PORTAL_NAME);
        assertThat(seeded.getId()).isNotNull();
    }

    @Test
    void should_be_idempotent_when_already_seeded() {
        useCase.execute(ORGANIZATION_ID, ENVIRONMENT_ID);
        useCase.execute(ORGANIZATION_ID, ENVIRONMENT_ID);

        assertThat(portalCrudService.storage()).hasSize(1);
    }

    @Test
    void should_derive_a_stable_id_per_environment() {
        useCase.execute(ORGANIZATION_ID, ENVIRONMENT_ID);
        var firstId = portalCrudService.storage().get(0).getId();
        portalCrudService.reset();

        useCase.execute(ORGANIZATION_ID, ENVIRONMENT_ID);
        var secondId = portalCrudService.storage().get(0).getId();

        assertThat(firstId).isEqualTo(secondId);
    }

    @Test
    void should_derive_different_ids_for_different_environments() {
        useCase.execute(ORGANIZATION_ID, ENVIRONMENT_ID);
        useCase.execute(ORGANIZATION_ID, "other-env");

        assertThat(portalCrudService.storage()).hasSize(2);
        assertThat(portalCrudService.storage().get(0).getId()).isNotEqualTo(portalCrudService.storage().get(1).getId());
    }
}
