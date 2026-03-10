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
package io.gravitee.rest.api.management.v2.rest.resource.api_product;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.use_case.GetApiProductApisUseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.model.ApisResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiProductApisResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";
    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";

    @Inject
    private GetApiProductApisUseCase getApiProductApisUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/api-products/" + API_PRODUCT_ID + "/apis";
    }

    @BeforeEach
    void init() {
        super.setUp();
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
        reset(getApiProductApisUseCase);
    }

    @Nested
    class GetApiProductApisTest {

        @Test
        void should_return_empty_list_when_product_has_no_apis() {
            var apiProduct = ApiProduct.builder().id(API_PRODUCT_ID).environmentId(ENV_ID).name("My Product").apiIds(Set.of()).build();
            when(getApiProductApisUseCase.execute(any())).thenReturn(
                new GetApiProductApisUseCase.Output(Optional.of(apiProduct), new Page<>(List.of(), 1, 10, 0))
            );

            Response response = rootTarget().queryParam("page", 1).queryParam("perPage", 10).request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            ApisResponse result = response.readEntity(ApisResponse.class);
            assertAll(
                () -> assertThat(result.getData()).isEmpty(),
                () -> assertThat(result.getPagination()).isNotNull(),
                () -> assertThat(result.getPagination().getTotalCount()).isEqualTo(0),
                () -> assertThat(result.getLinks()).isNotNull()
            );
        }

        @Test
        void should_return_apis_page_when_product_has_apis() {
            var apiProduct = ApiProduct.builder()
                .id(API_PRODUCT_ID)
                .environmentId(ENV_ID)
                .name("My Product")
                .apiIds(Set.of("api-1", "api-2"))
                .build();

            var genericApi = new ApiEntity();
            genericApi.setId("api-1");
            genericApi.setName("My API");
            genericApi.setDefinitionVersion(DefinitionVersion.V4);

            when(getApiProductApisUseCase.execute(any())).thenReturn(
                new GetApiProductApisUseCase.Output(Optional.of(apiProduct), new Page<>(List.of(genericApi), 1, 10, 2))
            );

            Response response = rootTarget().queryParam("page", 1).queryParam("perPage", 10).request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            ApisResponse result = response.readEntity(ApisResponse.class);
            assertAll(
                () -> assertThat(result.getData()).hasSize(1),
                () -> assertThat(result.getData().get(0).getApiV4().getId()).isEqualTo("api-1"),
                () -> assertThat(result.getData().get(0).getApiV4().getName()).isEqualTo("My API"),
                () -> assertThat(result.getPagination()).isNotNull(),
                () -> assertThat(result.getPagination().getTotalCount()).isEqualTo(2),
                () -> assertThat(result.getLinks()).isNotNull()
            );
        }

        @Test
        void should_pass_pagination_and_query_to_use_case() {
            var apiProduct = ApiProduct.builder()
                .id(API_PRODUCT_ID)
                .environmentId(ENV_ID)
                .name("My Product")
                .apiIds(Set.of("api-1"))
                .build();
            when(getApiProductApisUseCase.execute(any())).thenReturn(
                new GetApiProductApisUseCase.Output(Optional.of(apiProduct), new Page<>(List.of(), 2, 20, 0))
            );

            rootTarget().queryParam("page", 2).queryParam("perPage", 20).queryParam("query", "search-term").request().get();

            verify(getApiProductApisUseCase).execute(
                argThat(
                    input ->
                        API_PRODUCT_ID.equals(input.apiProductId()) &&
                        "search-term".equals(input.query()) &&
                        input.pageable().getPageNumber() == 2 &&
                        input.pageable().getPageSize() == 20
                )
            );
        }
    }
}
