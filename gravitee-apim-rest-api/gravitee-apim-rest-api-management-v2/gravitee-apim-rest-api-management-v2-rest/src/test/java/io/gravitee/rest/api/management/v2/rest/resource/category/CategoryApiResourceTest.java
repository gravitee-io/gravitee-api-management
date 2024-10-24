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

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import inmemory.ApiAuthorizationDomainServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.CategoryApiCrudServiceInMemory;
import inmemory.CategoryQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.UpdateCategoryApiDomainServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.category.model.ApiCategoryOrder;
import io.gravitee.apim.core.category.model.Category;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.model.CategoryApi;
import io.gravitee.rest.api.management.v2.rest.model.UpdateCategoryApi;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CategoryApiResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "env-id";
    private static final String CAT_ID = "cat-id";
    private static final String API_ID = "cat-id";

    @Autowired
    ApiAuthorizationDomainServiceInMemory apiAuthorizationDomainService;

    @Autowired
    ApiQueryServiceInMemory apiQueryServiceInMemory;

    @Autowired
    CategoryApiCrudServiceInMemory categoryApiCrudServiceInMemory;

    @Autowired
    UpdateCategoryApiDomainServiceInMemory updateCategoryApiDomainServiceInMemory;

    @Autowired
    CategoryQueryServiceInMemory categoryQueryServiceInMemory;

    @Autowired
    ApiCrudServiceInMemory apiCrudServiceInMemory;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/categories/" + CAT_ID + "/apis/" + API_ID;
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
            .of(
                apiAuthorizationDomainService,
                categoryQueryServiceInMemory,
                apiQueryServiceInMemory,
                categoryApiCrudServiceInMemory,
                updateCategoryApiDomainServiceInMemory,
                apiCrudService
            )
            .forEach(InMemoryAlternative::reset);
    }

    @Nested
    class UpdateCategoryApiOrder {

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.ENVIRONMENT_CATEGORY),
                    eq(ENV_ID),
                    eq(RolePermissionAction.UPDATE)
                )
            )
                .thenReturn(false);

            final Response response = rootTarget().request().post(Entity.json(UpdateCategoryApi.builder().order(99).build()));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_return_400_if_missing_body() {
            final Response response = rootTarget().request().post(Entity.json(null));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("Order cannot be null");
        }

        @Test
        public void should_return_400_if_order_not_specified() {
            final Response response = rootTarget().request().post(Entity.json(UpdateCategoryApi.builder().build()));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("Validation error");
        }

        @Test
        public void should_return_404_if_category_not_found() {
            final Response response = rootTarget().request().post(Entity.json(UpdateCategoryApi.builder().order(99).build()));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Category not found.");
        }

        @Test
        public void should_return_404_if_api_not_found() {
            categoryQueryServiceInMemory.initWith(List.of(Category.builder().id(CAT_ID).build()));

            final Response response = rootTarget().request().post(Entity.json(UpdateCategoryApi.builder().order(99).build()));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Api not found.");
        }

        @Test
        public void should_return_400_if_api_not_associated_to_category() {
            categoryQueryServiceInMemory.initWith(List.of(Category.builder().id(CAT_ID).build()));
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).definitionVersion(DefinitionVersion.V4).build()));

            final Response response = rootTarget().request().post(Entity.json(UpdateCategoryApi.builder().order(99).build()));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Api [cat-id] and Category [cat-id] not associated.");
        }

        @Test
        public void should_return_changed_category_api() {
            categoryQueryServiceInMemory.initWith(List.of(Category.builder().id(CAT_ID).build()));
            apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).definitionVersion(DefinitionVersion.V4).build()));
            var apiCategoryOrder = ApiCategoryOrder.builder().apiId(API_ID).categoryId(CAT_ID).order(0).build();

            categoryApiCrudServiceInMemory.initWith(List.of(apiCategoryOrder));
            updateCategoryApiDomainServiceInMemory.initWith(categoryApiCrudServiceInMemory.storage());

            final Response response = rootTarget().request().post(Entity.json(UpdateCategoryApi.builder().order(99).build()));

            MAPIAssertions.assertThat(response).hasStatus(OK_200).asEntity(CategoryApi.class).isNotNull();
        }
    }
}
