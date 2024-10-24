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
package io.gravitee.rest.api.management.v2.rest.resource.category;

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import inmemory.ApiAuthorizationDomainServiceInMemory;
import inmemory.ApiCategoryOrderQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.CategoryQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.category.model.ApiCategoryOrder;
import io.gravitee.apim.core.category.model.Category;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.model.CategoryApisResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CategoryApisResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "env-id";
    private static final String CAT_ID = "cat-id";

    @Autowired
    ApiAuthorizationDomainServiceInMemory apiAuthorizationDomainService;

    @Autowired
    ApiCategoryOrderQueryServiceInMemory categoryApiQueryServiceInMemory;

    @Autowired
    ApiQueryServiceInMemory apiQueryServiceInMemory;

    @Autowired
    CategoryQueryServiceInMemory categoryQueryServiceInMemory;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/categories/" + CAT_ID + "/apis";
    }

    @BeforeEach
    void init() {
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENV_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENV_ID)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENV_ID)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENV_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        Stream
            .of(apiAuthorizationDomainService, categoryApiQueryServiceInMemory, categoryQueryServiceInMemory, apiQueryServiceInMemory)
            .forEach(InMemoryAlternative::reset);
    }

    @Nested
    class GetCategoryApis {

        @Test
        void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.ENVIRONMENT_CATEGORY,
                    ENV_ID,
                    RolePermissionAction.READ
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
        void should_throw_error_when_category_does_not_exist() {
            final Response response = rootTarget().request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Category not found.");
        }

        @Test
        void should_return_empty_list_when_no_apis_in_category() {
            categoryQueryServiceInMemory.initWith(List.of(Category.builder().id(CAT_ID).build()));

            final Response response = rootTarget().request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(CategoryApisResponse.class)
                .hasFieldOrPropertyWithValue("data", List.of());
        }

        @Test
        void should_return_page_of_results() {
            categoryQueryServiceInMemory.initWith(List.of(Category.builder().id(CAT_ID).build()));
            categoryApiQueryServiceInMemory.initWith(
                List.of(
                    ApiCategoryOrder.builder().apiId("api-1").categoryId(CAT_ID).order(0).build(),
                    ApiCategoryOrder.builder().apiId("api-2").categoryId(CAT_ID).order(1).build()
                )
            );

            apiQueryServiceInMemory.initWith(
                List.of(
                    Api.builder().id("api-1").definitionVersion(DefinitionVersion.V4).categories(Set.of(CAT_ID)).build(),
                    Api.builder().id("api-2").definitionVersion(DefinitionVersion.V4).categories(Set.of(CAT_ID)).build()
                )
            );
            final Response response = rootTarget().queryParam("page", 2).queryParam("perPage", 1).request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(CategoryApisResponse.class)
                .extracting(CategoryApisResponse::getData)
                .satisfies(list -> {
                    assertThat(list).hasSize(1);
                    assertThat(list.get(0)).hasFieldOrPropertyWithValue("id", "api-2").hasFieldOrPropertyWithValue("order", 1);
                });
        }

        @Test
        void should_return_all_results() {
            categoryQueryServiceInMemory.initWith(List.of(Category.builder().id(CAT_ID).build()));
            categoryApiQueryServiceInMemory.initWith(
                List.of(
                    ApiCategoryOrder.builder().apiId("api-1").categoryId(CAT_ID).order(0).build(),
                    ApiCategoryOrder.builder().apiId("api-2").categoryId(CAT_ID).order(1).build()
                )
            );

            apiQueryServiceInMemory.initWith(
                List.of(
                    Api.builder().id("api-1").definitionVersion(DefinitionVersion.V4).categories(Set.of(CAT_ID)).build(),
                    Api.builder().id("api-2").definitionVersion(DefinitionVersion.V4).categories(Set.of(CAT_ID)).build()
                )
            );
            final Response response = rootTarget().request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(CategoryApisResponse.class)
                .extracting(CategoryApisResponse::getData)
                .satisfies(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list.get(0)).hasFieldOrPropertyWithValue("id", "api-1").hasFieldOrPropertyWithValue("order", 0);
                    assertThat(list.get(1)).hasFieldOrPropertyWithValue("id", "api-2").hasFieldOrPropertyWithValue("order", 1);
                });
        }

        @Test
        void should_order_results_by_category_api_order() {
            categoryQueryServiceInMemory.initWith(List.of(Category.builder().id(CAT_ID).build()));
            categoryApiQueryServiceInMemory.initWith(
                List.of(
                    ApiCategoryOrder.builder().apiId("api-1").categoryId(CAT_ID).order(1).build(),
                    ApiCategoryOrder.builder().apiId("api-2").categoryId(CAT_ID).order(0).build()
                )
            );

            apiQueryServiceInMemory.initWith(
                List.of(
                    Api.builder().id("api-1").definitionVersion(DefinitionVersion.V4).categories(Set.of(CAT_ID)).build(),
                    Api.builder().id("api-2").definitionVersion(DefinitionVersion.V4).categories(Set.of(CAT_ID)).build()
                )
            );
            final Response response = rootTarget().request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(CategoryApisResponse.class)
                .extracting(CategoryApisResponse::getData)
                .satisfies(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list.get(0)).hasFieldOrPropertyWithValue("id", "api-2").hasFieldOrPropertyWithValue("order", 0);
                    assertThat(list.get(1)).hasFieldOrPropertyWithValue("id", "api-1").hasFieldOrPropertyWithValue("order", 1);
                });
        }
    }
}
