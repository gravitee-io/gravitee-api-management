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
import io.gravitee.gamma.rest.model.CreateResourceRequest;
import io.gravitee.gamma.rest.model.ResourceResponse;
import io.gravitee.gamma.rest.model.ResourcesResponse;
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

class EnvironmentResourcesResourceTest extends AbstractResourceTest {

    private static final String ORG_ID = ResourceFixture.DEFAULT_ORGANIZATION_ID;
    private static final String ENV_ID = ResourceFixture.DEFAULT_ENVIRONMENT_ID;

    @Inject
    private ResourceCrudServiceInMemory resourceCrudService;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORG_ID + "/environments/" + ENV_ID + "/resources";
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
    class Create {

        @Test
        void should_return_201_and_persist_resource() {
            var request = new CreateResourceRequest(ResourceFixture.DEFAULT_NEW_ID, "my-cache", "cache", "{\"ttl\":30}", true);

            try (Response response = rootTarget().request().post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE))) {
                assertThat(response.getStatus()).isEqualTo(HttpStatusCode.CREATED_201);

                var body = response.readEntity(ResourceResponse.class);
                assertThat(body.id()).isEqualTo(ResourceFixture.DEFAULT_NEW_ID);
                assertThat(body.referenceId()).isEqualTo(ENV_ID);
                assertThat(body.referenceType()).isEqualTo("ENVIRONMENT");
                assertThat(body.definition().getName()).isEqualTo("my-cache");
                assertThat(response.getLocation()).isNotNull();
                assertThat(response.getLocation().toString()).endsWith(body.id());

                assertThat(resourceCrudService.storage()).hasSize(1);
            }
        }

        @Test
        void should_return_400_when_payload_is_empty() {
            try (Response response = rootTarget().request().post(Entity.entity("{}", MediaType.APPLICATION_JSON_TYPE))) {
                assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                assertThat(resourceCrudService.storage()).isEmpty();
            }
        }

        @Test
        void should_return_400_when_name_is_missing() {
            var request = new CreateResourceRequest(ResourceFixture.DEFAULT_NEW_ID, null, "cache", "{\"ttl\":30}", true);

            try (Response response = rootTarget().request().post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE))) {
                assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                assertThat(resourceCrudService.storage()).isEmpty();
            }
        }
    }

    @Nested
    class Search {

        @Test
        void should_return_empty_page_when_no_resources() {
            try (Response response = rootTarget().request().get()) {
                assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);

                var body = response.readEntity(ResourcesResponse.class);
                assertThat(body.data()).isEmpty();
                assertThat(body.pagination().totalCount()).isZero();
                assertThat(body.pagination().page()).isOne();
                assertThat(body.pagination().perPage()).isEqualTo(10);
            }
        }

        @Test
        void should_return_resources_for_environment() {
            resourceCrudService.initWith(
                java.util.List.of(
                    ResourceFixture.aResource(r -> r.id("a").definition(ResourceFixture.aDefinition(d -> d.name("a-cache")))),
                    ResourceFixture.aResource(r -> r.id("b").definition(ResourceFixture.aDefinition(d -> d.name("b-cache"))))
                )
            );

            try (Response response = rootTarget().request().get()) {
                assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);

                var body = response.readEntity(ResourcesResponse.class);
                assertThat(body.data()).hasSize(2);
                assertThat(body.pagination().totalCount()).isEqualTo(2);
                assertThat(body.pagination().pageItemsCount()).isEqualTo(2);
            }
        }

        @Test
        void should_filter_by_query_string() {
            resourceCrudService.initWith(
                java.util.List.of(
                    ResourceFixture.aResource(r -> r.id("a").definition(ResourceFixture.aDefinition(d -> d.name("alpha-cache")))),
                    ResourceFixture.aResource(r -> r.id("b").definition(ResourceFixture.aDefinition(d -> d.name("beta-cache"))))
                )
            );

            try (Response response = rootTarget().queryParam("q", "alpha").request().get()) {
                assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);

                var body = response.readEntity(ResourcesResponse.class);
                assertThat(body.data()).hasSize(1);
                assertThat(body.data().getFirst().definition().getName()).isEqualTo("alpha-cache");
                assertThat(body.pagination().totalCount()).isOne();
            }
        }

        @Test
        void should_ignore_resources_from_other_environments() {
            resourceCrudService.initWith(
                java.util.List.of(
                    ResourceFixture.aResource(r -> r.id("a")),
                    ResourceFixture.aResource(r -> r.id("b").referenceId("OTHER_ENV"))
                )
            );

            try (Response response = rootTarget().request().get()) {
                var body = response.readEntity(ResourcesResponse.class);
                assertThat(body.data()).hasSize(1);
                assertThat(body.data().getFirst().id()).isEqualTo("a");
            }
        }
    }
}
