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

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.ApiFederated;
import io.gravitee.rest.api.management.v2.rest.model.ApiLinks;
import io.gravitee.rest.api.management.v2.rest.model.ApiSearchQuery;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.ApisResponse;
import io.gravitee.rest.api.management.v2.rest.model.BaseOriginContext;
import io.gravitee.rest.api.management.v2.rest.model.GenericApi;
import io.gravitee.rest.api.management.v2.rest.model.IntegrationOriginContext;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.federation.FederatedApiEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ApisResource_SearchApisTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/_search";
    }

    @BeforeEach
    public void init() {
        GraviteeContext.cleanContext();
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENVIRONMENT);
        environment.setOrganizationId(ORGANIZATION);

        doReturn(environment).when(environmentService).findById(ENVIRONMENT);
        doReturn(environment).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT);
    }

    @Test
    public void should_not_search_if_no_params() {
        final Response response = rootTarget().request().post(null);
        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400);
    }

    @Test
    public void should_search_with_empty_query() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setQuery("");

        var apiEntity = new ApiEntity();
        apiEntity.setId("api-id");
        apiEntity.setState(Lifecycle.State.INITIALIZED);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);

        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10)),
                eq(false),
                eq(true)
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        final Response response = rootTarget().request().post(Entity.json(apiSearchQuery));
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApisResponse.class)
            .extracting(ApisResponse::getData)
            .satisfies(list -> {
                assertThat(list).hasSize(1);
                assertThat(list.get(0).getApiV4().getId()).isEqualTo("api-id");
            });

        var apiQueryBuilder = apiQueryBuilderCaptor.getValue();
        var apiQuery = apiQueryBuilder.build();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(apiQuery.getQuery()).isNull();
            soft.assertThat(apiQuery.getFilters()).isEmpty();
        });
    }

    @Test
    public void should_search_with_valid_query() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setQuery("api-name");

        var apiEntity = new ApiEntity();
        apiEntity.setId("api-id");
        apiEntity.setState(Lifecycle.State.INITIALIZED);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);

        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10)),
                eq(false),
                eq(true)
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        final Response response = rootTarget().request().post(Entity.json(apiSearchQuery));
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApisResponse.class)
            .extracting(ApisResponse::getData)
            .satisfies(list -> {
                assertThat(list).hasSize(1);
                // Check no expands fields are set
                assertThat(list.get(0).getApiV4().getDeploymentState()).isNull();
            });

        var apiQueryBuilder = apiQueryBuilderCaptor.getValue();
        var apiQuery = apiQueryBuilder.build();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(apiQuery.getQuery()).isEqualTo("api-name");
            soft.assertThat(apiQuery.getFilters()).isEmpty();
        });
    }

    @Test
    public void should_not_search_with_empty_ids() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setIds(List.of());

        var apiEntity = new ApiEntity();
        apiEntity.setId("api-id");
        apiEntity.setState(Lifecycle.State.INITIALIZED);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);

        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10)),
                eq(false),
                eq(true)
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        final Response response = rootTarget().request().post(Entity.json(apiSearchQuery));
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApisResponse.class)
            .extracting(ApisResponse::getData)
            .satisfies(list -> {
                assertThat(list).hasSize(1);
            });

        var apiQueryBuilder = apiQueryBuilderCaptor.getValue();
        var apiQuery = apiQueryBuilder.build();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(apiQuery.getQuery()).isNull();
            soft.assertThat(apiQuery.getFilters()).isEmpty();
        });
    }

    @Test
    public void should_search_with_ids() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setIds(List.of("id-1", "id-2"));

        var apiEntity = new ApiEntity();
        apiEntity.setId("id-1");
        apiEntity.setState(Lifecycle.State.INITIALIZED);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);

        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10)),
                eq(false),
                eq(true)
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        final Response response = rootTarget().request().post(Entity.json(apiSearchQuery));
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApisResponse.class)
            .extracting(ApisResponse::getData)
            .satisfies(list -> {
                assertThat(list).hasSize(1);
                assertThat(list.get(0).getApiV4().getId()).isEqualTo("id-1");
            });

        var apiQueryBuilder = apiQueryBuilderCaptor.getValue();
        var apiQuery = apiQueryBuilder.build();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(apiQuery.getFilters()).containsOnly(Map.entry("api", List.of("id-1", "id-2")));
        });
    }

    @Test
    public void should_return_error_with_wrong_value_for_sort_by_param() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setIds(List.of("id-1", "id-2"));

        final Response response = rootTarget().queryParam("sortBy", "wrong_value").request().post(Entity.json(apiSearchQuery));
        assertThat(response)
            .hasStatus(BAD_REQUEST_400)
            .asError()
            .hasHttpStatus(BAD_REQUEST_400)
            .hasMessage("Invalid sortBy parameter: wrong_value");
    }

    @Test
    public void should_sort_results_with_sort_by_param_asc() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setQuery("api-name");

        var apiEntity = new ApiEntity();
        apiEntity.setId("api-id");
        apiEntity.setState(Lifecycle.State.INITIALIZED);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);

        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10)),
                eq(false),
                eq(true)
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        final Response response = rootTarget().queryParam("sortBy", "name").request().post(Entity.json(apiSearchQuery));
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApisResponse.class)
            .extracting(ApisResponse::getData)
            .satisfies(list -> {
                assertThat(list).hasSize(1);
            });

        var apiQueryBuilder = apiQueryBuilderCaptor.getValue();
        var apiQuery = apiQueryBuilder.build();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(apiQuery.getQuery()).isEqualTo("api-name");
            soft.assertThat(apiQuery.getFilters()).isEmpty();
        });
    }

    @Test
    public void should_sort_results_with_sort_by_param_desc() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setQuery("api-name");

        var apiEntity = new ApiEntity();
        apiEntity.setId("api-id");
        apiEntity.setState(Lifecycle.State.INITIALIZED);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);

        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10)),
                eq(false),
                eq(true)
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        final Response response = rootTarget().queryParam("sortBy", "-paths").request().post(Entity.json(apiSearchQuery));
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApisResponse.class)
            .extracting(ApisResponse::getData)
            .satisfies(list -> {
                assertThat(list).hasSize(1);
            });

        var apiQueryBuilder = apiQueryBuilderCaptor.getValue();
        var apiQuery = apiQueryBuilder.build();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(apiQuery.getQuery()).isEqualTo("api-name");
            soft.assertThat(apiQuery.getFilters()).isEmpty();
            soft.assertThat(apiQuery.getSort()).isEqualTo(new SortableImpl("paths", false));
        });
    }

    @Test
    public void should_search_with_definition_version_param() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setDefinitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.V4);

        var apiEntity = new ApiEntity();
        apiEntity.setId("id-1");
        apiEntity.setState(Lifecycle.State.INITIALIZED);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);

        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10)),
                eq(false),
                eq(true)
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        final Response response = rootTarget().request().post(Entity.json(apiSearchQuery));
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApisResponse.class)
            .extracting(ApisResponse::getData)
            .satisfies(list -> {
                assertThat(list).hasSize(1);
            });

        var apiQueryBuilder = apiQueryBuilderCaptor.getValue();
        var apiQuery = apiQueryBuilder.build();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(apiQuery.getQuery()).isNull();
            soft.assertThat(apiQuery.getFilters()).containsOnly(Map.entry("definition_version", "4.0.0"));
            soft.assertThat(apiQuery.getSort()).isNull();
        });
    }

    @Test
    public void should_sort_by_paths() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setDefinitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.V4);

        var apiEntity = new ApiEntity();
        apiEntity.setId("id-1");
        apiEntity.setState(Lifecycle.State.INITIALIZED);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);

        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10)),
                eq(false),
                eq(true)
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        final Response response = rootTarget().queryParam("sortBy", "paths").request().post(Entity.json(apiSearchQuery));
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApisResponse.class)
            .extracting(ApisResponse::getData)
            .satisfies(list -> {
                assertThat(list).hasSize(1);
            });

        var apiQueryBuilder = apiQueryBuilderCaptor.getValue();
        var apiQuery = apiQueryBuilder.build();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(apiQuery.getQuery()).isNull();
            soft.assertThat(apiQuery.getFilters()).containsOnly(Map.entry("definition_version", "4.0.0"));
            soft.assertThat(apiQuery.getSort()).isEqualTo(new SortableImpl("paths", true));
        });
    }

    @Test
    public void should_search_with_all_expands_params() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setQuery("api-name");

        var apiEntity = new ApiEntity();
        apiEntity.setId("api-id");
        apiEntity.setState(Lifecycle.State.INITIALIZED);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);

        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10)),
                eq(true),
                eq(true)
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        when(apiStateServiceV4.isSynchronized(eq(GraviteeContext.getExecutionContext()), eq(apiEntity))).thenReturn(true);

        final Response response = rootTarget().queryParam("expands", "deploymentState").request().post(Entity.json(apiSearchQuery));
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApisResponse.class)
            .extracting(ApisResponse::getData)
            .satisfies(list -> {
                assertThat(list).hasSize(1);
                assertThat(list.get(0).getApiV4())
                    .extracting(ApiV4::getId, ApiV4::getDeploymentState)
                    .containsExactly("api-id", GenericApi.DeploymentStateEnum.DEPLOYED);
            });
    }

    @Test
    public void should_return_federated_apis() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setQuery("api-name");

        var apiEntity = FederatedApiEntity
            .builder()
            .id("api-id")
            .name("api-name")
            .originContext(new OriginContext.Integration("integration-id"))
            .build();

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);

        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10)),
                eq(false),
                eq(true)
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        when(apiStateServiceV4.isSynchronized(eq(GraviteeContext.getExecutionContext()), eq(apiEntity))).thenReturn(true);

        final Response response = rootTarget().request().post(Entity.json(apiSearchQuery));
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApisResponse.class)
            .extracting(ApisResponse::getData)
            .satisfies(list -> {
                assertThat(list)
                    .extracting(Api::getApiFederated)
                    .isEqualTo(
                        List.of(
                            ApiFederated
                                .builder()
                                .id("api-id")
                                .name("api-name")
                                .definitionVersion(io.gravitee.rest.api.management.v2.rest.model.DefinitionVersion.FEDERATED)
                                .originContext(
                                    IntegrationOriginContext
                                        .builder()
                                        .origin(BaseOriginContext.OriginEnum.INTEGRATION)
                                        .integrationId("integration-id")
                                        .build()
                                )
                                .disableMembershipNotifications(false)
                                .responseTemplates(Collections.emptyMap())
                                .links(
                                    ApiLinks
                                        .builder()
                                        .pictureUrl(
                                            rootTarget()
                                                .getUriBuilder()
                                                .replacePath("/environments/" + ENVIRONMENT + "/apis/api-id/picture")
                                                .toTemplate()
                                        )
                                        .backgroundUrl(
                                            rootTarget()
                                                .getUriBuilder()
                                                .replacePath("/environments/" + ENVIRONMENT + "/apis/api-id/background")
                                                .toTemplate()
                                        )
                                        .build()
                                )
                                .build()
                        )
                    );
            });
    }

    @Test
    public void should_not_search_managed_only_apis() {
        var apiSearchQuery = new ApiSearchQuery();
        apiSearchQuery.setQuery("");

        var apiEntity = new ApiEntity();
        apiEntity.setId("api-id");
        apiEntity.setState(Lifecycle.State.INITIALIZED);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        ArgumentCaptor<QueryBuilder<ApiEntity>> apiQueryBuilderCaptor = ArgumentCaptor.forClass(QueryBuilder.class);
        when(
            apiSearchServiceV4.search(
                eq(GraviteeContext.getExecutionContext()),
                eq("UnitTests"),
                eq(true),
                apiQueryBuilderCaptor.capture(),
                eq(new PageableImpl(1, 10)),
                eq(false),
                eq(false)
            )
        )
            .thenReturn(new Page<>(List.of(apiEntity), 1, 1, 1));

        final Response response = rootTarget().queryParam("manageOnly", false).request().post(Entity.json(apiSearchQuery));
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApisResponse.class)
            .extracting(ApisResponse::getData)
            .satisfies(list -> {
                assertThat(list).hasSize(1);
                assertThat(list.get(0).getApiV4().getId()).isEqualTo("api-id");
            });
    }
}
