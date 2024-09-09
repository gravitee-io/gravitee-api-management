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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static fixtures.MetadataFixtures.aApiMetadata;
import static fixtures.MetadataFixtures.aEnvMetadata;
import static fixtures.MetadataFixtures.aMapiV2Metadata;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiMetadataQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.rest.api.management.v2.rest.model.MetadataResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiMetadataResourceTest extends AbstractResourceTest {

    @Autowired
    private ApiCrudServiceInMemory apiCrudServiceInMemory;

    @Autowired
    private ApiMetadataQueryServiceInMemory apiMetadataQueryServiceInMemory;

    protected static final String ENV_ID = "my-env";
    protected static final String API_ID = "api-id";

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/apis/" + API_ID + "/metadata";
    }

    @BeforeEach
    void init() {
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENV_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        doReturn(environmentEntity).when(environmentService).findById(ENV_ID);
        doReturn(environmentEntity).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENV_ID);

        GraviteeContext.setCurrentEnvironment(ENV_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        Stream.of(apiCrudServiceInMemory, apiMetadataQueryServiceInMemory).forEach(InMemoryAlternative::reset);
    }

    @Nested
    class GetApiMetadata {

        // Initial data to be loaded into in-memory repository
        private final Metadata metadata1 = aApiMetadata(API_ID, "key1", "1", "value1", Metadata.MetadataFormat.STRING);
        private final Metadata metadata2 = aApiMetadata(API_ID, "key2", "2", "value2", Metadata.MetadataFormat.STRING);
        private final Metadata globalMetadata2 = aEnvMetadata(ENV_ID, "key2", "2", "global-value-2", Metadata.MetadataFormat.STRING);
        private final Metadata globalMetadata3 = aEnvMetadata(ENV_ID, "key3", "3", "global-value-3", Metadata.MetadataFormat.STRING);
        private final Metadata metadata4 = aApiMetadata(API_ID, "a-key-4", "4", "value4", Metadata.MetadataFormat.STRING);

        // Simple api metadata
        private final io.gravitee.rest.api.management.v2.rest.model.Metadata apiMetadata1 = aMapiV2Metadata(
            metadata1.getKey(),
            metadata1.getName(),
            metadata1.getValue(),
            null,
            io.gravitee.rest.api.management.v2.rest.model.MetadataFormat.STRING
        );

        // Override global metadata
        private final io.gravitee.rest.api.management.v2.rest.model.Metadata apiMetadata2 = aMapiV2Metadata(
            metadata2.getKey(),
            metadata2.getName(),
            metadata2.getValue(),
            globalMetadata2.getValue(),
            io.gravitee.rest.api.management.v2.rest.model.MetadataFormat.STRING
        );

        // Simple global metadata
        private final io.gravitee.rest.api.management.v2.rest.model.Metadata apiMetadata3 = aMapiV2Metadata(
            globalMetadata3.getKey(),
            globalMetadata3.getName(),
            null,
            globalMetadata3.getValue(),
            io.gravitee.rest.api.management.v2.rest.model.MetadataFormat.STRING
        );

        // Simple api metadata with key starting with "a"
        private final io.gravitee.rest.api.management.v2.rest.model.Metadata apiMetadata4 = aMapiV2Metadata(
            metadata4.getKey(),
            metadata4.getName(),
            metadata4.getValue(),
            null,
            io.gravitee.rest.api.management.v2.rest.model.MetadataFormat.STRING
        );

        @BeforeEach
        void setUp() {
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).environmentId(ENV_ID).build()));
            apiMetadataQueryServiceInMemory.initWith(List.of(metadata1, metadata2, globalMetadata2, globalMetadata3, metadata4));
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_METADATA),
                    eq(API_ID),
                    eq(RolePermissionAction.READ)
                )
            )
                .thenReturn(false);

            final Response response = rootTarget().request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        void should_get_empty_metadata_list() {
            apiMetadataQueryServiceInMemory.reset();

            final Response response = rootTarget().request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(MetadataResponse.class);
            assertThat(body.getData()).isEqualTo(List.of());
            assertThat(body.getPagination())
                .hasFieldOrPropertyWithValue("page", null)
                .hasFieldOrPropertyWithValue("perPage", null)
                .hasFieldOrPropertyWithValue("pageCount", null)
                .hasFieldOrPropertyWithValue("pageItemsCount", null)
                .hasFieldOrPropertyWithValue("totalCount", null);
        }

        @Test
        void should_return_all_results_with_no_query_parameters() {
            final Response response = rootTarget().request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(MetadataResponse.class);
            assertThat(body.getData())
                .hasSize(4)
                .usingRecursiveComparison()
                .isEqualTo(List.of(apiMetadata4, apiMetadata1, apiMetadata2, apiMetadata3));
            assertThat(body.getPagination())
                .hasFieldOrPropertyWithValue("page", 1)
                .hasFieldOrPropertyWithValue("perPage", 10)
                .hasFieldOrPropertyWithValue("pageCount", 1)
                .hasFieldOrPropertyWithValue("pageItemsCount", 4)
                .hasFieldOrPropertyWithValue("totalCount", 4L);
        }

        @Test
        void should_return_page_of_results() {
            final Response response = rootTarget().queryParam("perPage", 1).queryParam("page", 2).request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(MetadataResponse.class);
            assertThat(body.getData()).hasSize(1).usingRecursiveComparison().isEqualTo(List.of(apiMetadata1));
            assertThat(body.getPagination())
                .hasFieldOrPropertyWithValue("page", 2)
                .hasFieldOrPropertyWithValue("perPage", 1)
                .hasFieldOrPropertyWithValue("pageCount", 4)
                .hasFieldOrPropertyWithValue("pageItemsCount", 1)
                .hasFieldOrPropertyWithValue("totalCount", 4L);
        }

        @Test
        void should_filter_by_source_global() {
            final Response response = rootTarget().queryParam("source", "GLOBAL").request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(MetadataResponse.class);
            assertThat(body.getData()).hasSize(2).usingRecursiveComparison().isEqualTo(List.of(apiMetadata2, apiMetadata3));
            assertThat(body.getPagination())
                .hasFieldOrPropertyWithValue("page", 1)
                .hasFieldOrPropertyWithValue("perPage", 10)
                .hasFieldOrPropertyWithValue("pageCount", 1)
                .hasFieldOrPropertyWithValue("pageItemsCount", 2)
                .hasFieldOrPropertyWithValue("totalCount", 2L);
        }

        @Test
        void should_filter_by_source_api() {
            final Response response = rootTarget().queryParam("source", "API").request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(MetadataResponse.class);
            assertThat(body.getData()).hasSize(2).usingRecursiveComparison().isEqualTo(List.of(apiMetadata4, apiMetadata1));
            assertThat(body.getPagination())
                .hasFieldOrPropertyWithValue("page", 1)
                .hasFieldOrPropertyWithValue("perPage", 10)
                .hasFieldOrPropertyWithValue("pageCount", 1)
                .hasFieldOrPropertyWithValue("pageItemsCount", 2)
                .hasFieldOrPropertyWithValue("totalCount", 2L);
        }

        @Test
        void should_not_filter_if_invalid_source_keyword() {
            final Response response = rootTarget().queryParam("source", "something_else").request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(MetadataResponse.class);
            assertThat(body.getData())
                .hasSize(4)
                .usingRecursiveComparison()
                .isEqualTo(List.of(apiMetadata4, apiMetadata1, apiMetadata2, apiMetadata3));
            assertThat(body.getPagination())
                .hasFieldOrPropertyWithValue("page", 1)
                .hasFieldOrPropertyWithValue("perPage", 10)
                .hasFieldOrPropertyWithValue("pageCount", 1)
                .hasFieldOrPropertyWithValue("pageItemsCount", 4)
                .hasFieldOrPropertyWithValue("totalCount", 4L);
        }

        @Test
        void should_sort_by_name_asc() {
            final Response response = rootTarget().queryParam("sortBy", "name").request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(MetadataResponse.class);
            assertThat(body.getData())
                .hasSize(4)
                .usingRecursiveComparison()
                .isEqualTo(List.of(apiMetadata1, apiMetadata2, apiMetadata3, apiMetadata4));
            assertThat(body.getPagination())
                .hasFieldOrPropertyWithValue("page", 1)
                .hasFieldOrPropertyWithValue("perPage", 10)
                .hasFieldOrPropertyWithValue("pageCount", 1)
                .hasFieldOrPropertyWithValue("pageItemsCount", 4)
                .hasFieldOrPropertyWithValue("totalCount", 4L);
        }

        @Test
        void should_sort_by_name_desc() {
            final Response response = rootTarget().queryParam("sortBy", "-name").request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(MetadataResponse.class);
            assertThat(body.getData())
                .hasSize(4)
                .usingRecursiveComparison()
                .isEqualTo(List.of(apiMetadata4, apiMetadata3, apiMetadata2, apiMetadata1));
            assertThat(body.getPagination())
                .hasFieldOrPropertyWithValue("page", 1)
                .hasFieldOrPropertyWithValue("perPage", 10)
                .hasFieldOrPropertyWithValue("pageCount", 1)
                .hasFieldOrPropertyWithValue("pageItemsCount", 4)
                .hasFieldOrPropertyWithValue("totalCount", 4L);
        }

        @Test
        void should_sort_by_key_on_unknown_sort_by_param() {
            final Response response = rootTarget().queryParam("sortBy", "something-else").request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            var body = response.readEntity(MetadataResponse.class);
            assertThat(body.getData())
                .hasSize(4)
                .usingRecursiveComparison()
                .isEqualTo(List.of(apiMetadata4, apiMetadata1, apiMetadata2, apiMetadata3));
            assertThat(body.getPagination())
                .hasFieldOrPropertyWithValue("page", 1)
                .hasFieldOrPropertyWithValue("perPage", 10)
                .hasFieldOrPropertyWithValue("pageCount", 1)
                .hasFieldOrPropertyWithValue("pageItemsCount", 4)
                .hasFieldOrPropertyWithValue("totalCount", 4L);
        }
    }
}
