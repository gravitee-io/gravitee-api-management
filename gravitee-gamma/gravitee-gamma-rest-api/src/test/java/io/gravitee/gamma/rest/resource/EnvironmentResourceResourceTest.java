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
package io.gravitee.gamma.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import gamma.inmemory.ResourceCrudServiceInMemory;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.core.resource.fixture.ResourceFixture;
import io.gravitee.gamma.rest.model.ResourceResponse;
import io.gravitee.gamma.rest.model.UpdateResourceRequest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EnvironmentResourceResourceTest extends AbstractResourceTest {

    private static final String ORG_ID = ResourceFixture.DEFAULT_ORGANIZATION_ID;
    private static final String ENV_ID = ResourceFixture.DEFAULT_ENVIRONMENT_ID;
    private static final String RESOURCE_ID = ResourceFixture.DEFAULT_ID;

    @Inject
    private ResourceCrudServiceInMemory resourceCrudService;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORG_ID + "/environments/" + ENV_ID + "/resources/" + RESOURCE_ID;
    }

    @BeforeEach
    void initContext() {
        var environment = EnvironmentEntity.builder().id(ENV_ID).organizationId(ORGANIZATION).name(ENV_ID).build();
        when(environmentService.findByOrgAndIdOrHrid(eq(ORGANIZATION), eq(ENV_ID))).thenReturn(environment);
        GraviteeContext.fromExecutionContext(new ExecutionContext(ORGANIZATION, ENV_ID));
    }

    @AfterEach
    void resetStorage() {
        resourceCrudService.reset();
    }

    @Nested
    class Get {

        @Test
        void should_return_200_with_resource() {
            resourceCrudService.initWith(java.util.List.of(ResourceFixture.aResource()));

            try (Response response = rootTarget().request().get()) {
                assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);

                var body = response.readEntity(ResourceResponse.class);
                assertThat(body.id()).isEqualTo(RESOURCE_ID);
                assertThat(body.referenceId()).isEqualTo(ENV_ID);
                assertThat(body.referenceType()).isEqualTo("ENVIRONMENT");
                assertThat(body.definition().getName()).isEqualTo("my-cache");
            }
        }

        @Test
        void should_return_404_when_resource_does_not_exist() {
            try (Response response = rootTarget().request().get()) {
                assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
            }
        }

        @Test
        void should_return_404_when_resource_belongs_to_another_environment() {
            resourceCrudService.initWith(java.util.List.of(ResourceFixture.aResource(r -> r.referenceId("OTHER_ENV"))));

            try (Response response = rootTarget().request().get()) {
                assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
            }
        }
    }

    @Nested
    class Update {

        @Test
        void should_return_200_and_persist_changes() {
            resourceCrudService.initWith(java.util.List.of(ResourceFixture.aResource()));
            var request = new UpdateResourceRequest("renamed-cache", "cache", "{\"ttl\":60}", true);

            try (Response response = rootTarget().request().put(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE))) {
                assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);

                var body = response.readEntity(ResourceResponse.class);
                assertThat(body.id()).isEqualTo(RESOURCE_ID);
                assertThat(body.definition().getName()).isEqualTo("renamed-cache");
                assertThat(body.definition().getConfiguration()).contains("60");

                var stored = resourceCrudService.storage().getFirst();
                assertThat(stored.definition().getName()).isEqualTo("renamed-cache");
            }
        }

        @Test
        void should_return_404_when_resource_does_not_exist() {
            var request = new UpdateResourceRequest("my-cache", "cache", "{\"ttl\":30}", true);

            try (Response response = rootTarget().request().put(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE))) {
                assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
            }
        }
    }

    @Nested
    class Delete {

        @Test
        void should_return_204_and_remove_resource() {
            resourceCrudService.initWith(java.util.List.of(ResourceFixture.aResource()));

            try (Response response = rootTarget().request().delete()) {
                assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
                assertThat(resourceCrudService.storage()).isEmpty();
            }
        }

        @Test
        void should_return_404_when_resource_does_not_exist() {
            try (Response response = rootTarget().request().delete()) {
                assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
            }
        }
    }
}
