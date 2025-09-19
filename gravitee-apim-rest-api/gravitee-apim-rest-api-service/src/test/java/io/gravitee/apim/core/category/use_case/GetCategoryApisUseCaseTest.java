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
package io.gravitee.apim.core.category.use_case;

import inmemory.ApiAuthorizationDomainServiceInMemory;
import inmemory.ApiCategoryOrderQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.CategoryQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.category.domain_service.ValidateCategoryDomainService;
import io.gravitee.apim.core.category.exception.CategoryNotFoundException;
import io.gravitee.apim.core.category.model.ApiCategoryOrder;
import io.gravitee.apim.core.category.model.Category;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetCategoryApisUseCaseTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("ORG_ID", "ENV_ID");
    private static final String CAT_ID = "cat-id";
    private static final String USER_ID = "user-id";

    ApiAuthorizationDomainServiceInMemory apiAuthorizationDomainService = new ApiAuthorizationDomainServiceInMemory();
    ApiCategoryOrderQueryServiceInMemory apiCategoryOrderQueryService = new ApiCategoryOrderQueryServiceInMemory();
    ApiQueryServiceInMemory apiQueryServiceInMemory = new ApiQueryServiceInMemory();
    CategoryQueryServiceInMemory categoryQueryService = new CategoryQueryServiceInMemory();
    ValidateCategoryDomainService validateCategoryDomainService;
    GetCategoryApisUseCase useCase;

    @BeforeEach
    void setUp() {
        validateCategoryDomainService = new ValidateCategoryDomainService(categoryQueryService);
        useCase = new GetCategoryApisUseCase(
            validateCategoryDomainService,
            apiAuthorizationDomainService,
            apiCategoryOrderQueryService,
            apiQueryServiceInMemory
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(
            apiAuthorizationDomainService,
            apiCategoryOrderQueryService,
            apiQueryServiceInMemory,
            apiCategoryOrderQueryService
        ).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_throw_error_when_category_id_is_null() {
        var throwable = Assertions.catchThrowable(() ->
            useCase.execute(new GetCategoryApisUseCase.Input(EXECUTION_CONTEXT, null, USER_ID, false))
        );

        Assertions.assertThat(throwable).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void should_throw_error_when_category_id_not_found() {
        var throwable = Assertions.catchThrowable(() ->
            useCase.execute(new GetCategoryApisUseCase.Input(EXECUTION_CONTEXT, CAT_ID, USER_ID, false))
        );

        Assertions.assertThat(throwable).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void should_return_empty_lists_if_no_apis_found_in_category() {
        categoryQueryService.initWith(List.of(Category.builder().id(CAT_ID).build()));
        var result = useCase.execute(new GetCategoryApisUseCase.Input(EXECUTION_CONTEXT, CAT_ID, USER_ID, false));

        Assertions.assertThat(result).extracting("results").isEqualTo(List.of());
    }

    @Test
    void should_search_with_restricted_api_ids_if_not_admin() {
        categoryQueryService.initWith(List.of(Category.builder().id(CAT_ID).build()));
        apiCategoryOrderQueryService.initWith(
            List.of(
                ApiCategoryOrder.builder().apiId("api-1").categoryId(CAT_ID).order(0).build(),
                ApiCategoryOrder.builder().apiId("api-2").categoryId(CAT_ID).order(0).build()
            )
        );

        var apiInUserScope = Api.builder().id("api-1").categories(Set.of(CAT_ID)).build();
        apiAuthorizationDomainService.initWith(List.of(apiInUserScope));

        apiQueryServiceInMemory.initWith(List.of(apiInUserScope, Api.builder().id("api-2").categories(Set.of(CAT_ID)).build()));

        var result = useCase.execute(new GetCategoryApisUseCase.Input(EXECUTION_CONTEXT, CAT_ID, USER_ID, false));

        Assertions.assertThat(result)
            .extracting("results")
            .isEqualTo(
                List.of(
                    new GetCategoryApisUseCase.Result(
                        ApiCategoryOrder.builder().apiId("api-1").categoryId(CAT_ID).order(0).build(),
                        apiInUserScope
                    )
                )
            );
    }

    @Test
    void should_search_with_all_api_ids_in_category_if_admin() {
        categoryQueryService.initWith(List.of(Category.builder().id(CAT_ID).build()));
        apiCategoryOrderQueryService.initWith(
            List.of(
                ApiCategoryOrder.builder().apiId("api-1").categoryId(CAT_ID).order(0).build(),
                ApiCategoryOrder.builder().apiId("api-2").categoryId(CAT_ID).order(0).build()
            )
        );

        apiQueryServiceInMemory.initWith(
            List.of(
                Api.builder().id("api-1").categories(Set.of(CAT_ID)).build(),
                Api.builder().id("api-2").categories(Set.of(CAT_ID)).build()
            )
        );

        var result = useCase.execute(new GetCategoryApisUseCase.Input(EXECUTION_CONTEXT, CAT_ID, USER_ID, true));

        Assertions.assertThat(result)
            .extracting("results")
            .isEqualTo(List.of(resultForApi("api-1", 0, Api.Visibility.PRIVATE), resultForApi("api-2", 0, Api.Visibility.PRIVATE)));
    }

    @Test
    void should_return_empty_list_if_no_apis_in_scope_when_not_admin() {
        categoryQueryService.initWith(List.of(Category.builder().id(CAT_ID).build()));
        apiCategoryOrderQueryService.initWith(
            List.of(
                ApiCategoryOrder.builder().apiId("api-1").categoryId(CAT_ID).order(0).build(),
                ApiCategoryOrder.builder().apiId("api-2").categoryId(CAT_ID).order(0).build()
            )
        );

        apiQueryServiceInMemory.initWith(
            List.of(
                Api.builder().id("api-1").categories(Set.of(CAT_ID)).build(),
                Api.builder().id("api-2").categories(Set.of(CAT_ID)).build()
            )
        );

        var result = useCase.execute(new GetCategoryApisUseCase.Input(EXECUTION_CONTEXT, CAT_ID, USER_ID, false));

        Assertions.assertThat(result).extracting("results").isEqualTo(List.of());
    }

    @Test
    void should_return_empty_lists_if_no_apis_found() {
        categoryQueryService.initWith(List.of(Category.builder().id(CAT_ID).build()));
        apiCategoryOrderQueryService.initWith(
            List.of(
                ApiCategoryOrder.builder().apiId("api-1").categoryId(CAT_ID).order(0).build(),
                ApiCategoryOrder.builder().apiId("api-2").categoryId(CAT_ID).order(0).build()
            )
        );

        var result = useCase.execute(new GetCategoryApisUseCase.Input(EXECUTION_CONTEXT, CAT_ID, USER_ID, true));

        Assertions.assertThat(result).extracting("results").isEqualTo(List.of());
    }

    @Test
    void should_return_category_api_list_and_api_list() {
        categoryQueryService.initWith(List.of(Category.builder().id(CAT_ID).build()));
        apiCategoryOrderQueryService.initWith(
            List.of(
                ApiCategoryOrder.builder().apiId("api-1").categoryId(CAT_ID).order(0).build(),
                ApiCategoryOrder.builder().apiId("api-2").categoryId("another-category").order(0).build(),
                ApiCategoryOrder.builder().apiId("api-3").categoryId(CAT_ID).order(2).build(),
                ApiCategoryOrder.builder().apiId("api-4").categoryId(CAT_ID).order(1).build(),
                ApiCategoryOrder.builder().apiId("api-5").categoryId(CAT_ID).order(3).build()
            )
        );

        apiQueryServiceInMemory.initWith(
            List.of(
                Api.builder().id("api-1").categories(Set.of(CAT_ID)).build(),
                Api.builder().id("api-3").categories(Set.of(CAT_ID)).build(),
                Api.builder().id("api-4").categories(Set.of(CAT_ID)).build(),
                Api.builder().id("api-5").categories(Set.of(CAT_ID)).build()
            )
        );

        var result = useCase.execute(new GetCategoryApisUseCase.Input(EXECUTION_CONTEXT, CAT_ID, USER_ID, true));

        Assertions.assertThat(result)
            .extracting("results")
            .isEqualTo(
                List.of(
                    resultForApi("api-1", 0, Api.Visibility.PRIVATE),
                    resultForApi("api-4", 1, Api.Visibility.PRIVATE),
                    resultForApi("api-3", 2, Api.Visibility.PRIVATE),
                    resultForApi("api-5", 3, Api.Visibility.PRIVATE)
                )
            );
    }

    @Test
    void should_return_category_api_list_and_api_published_list_if_not_admin() {
        categoryQueryService.initWith(List.of(Category.builder().id(CAT_ID).build()));
        apiCategoryOrderQueryService.initWith(
            List.of(
                ApiCategoryOrder.builder().apiId("api-1").categoryId(CAT_ID).order(0).build(),
                ApiCategoryOrder.builder().apiId("api-2").categoryId("another-category").order(0).build(),
                ApiCategoryOrder.builder().apiId("api-3").categoryId(CAT_ID).order(2).build(),
                ApiCategoryOrder.builder().apiId("api-4").categoryId(CAT_ID).order(1).build(),
                ApiCategoryOrder.builder().apiId("api-5").categoryId(CAT_ID).order(3).build()
            )
        );

        apiAuthorizationDomainService.initWith(
            List.of(
                Api.builder().id("api-1").categories(Set.of(CAT_ID)).build(),
                Api.builder().id("api-4").categories(Set.of(CAT_ID)).build()
            )
        );

        apiQueryServiceInMemory.initWith(
            List.of(
                Api.builder().id("api-1").categories(Set.of(CAT_ID)).apiLifecycleState(Api.ApiLifecycleState.PUBLISHED).build(),
                Api.builder().id("api-3").categories(Set.of(CAT_ID)).apiLifecycleState(Api.ApiLifecycleState.PUBLISHED).build(),
                Api.builder().id("api-4").categories(Set.of(CAT_ID)).build(),
                Api.builder().id("api-5").categories(Set.of(CAT_ID)).build()
            )
        );

        var result = useCase.execute(new GetCategoryApisUseCase.Input(EXECUTION_CONTEXT, CAT_ID, USER_ID, false, true));

        Assertions.assertThat(result)
            .extracting("results")
            .isEqualTo(List.of(resultForApi("api-1", 0, Api.ApiLifecycleState.PUBLISHED, Api.Visibility.PRIVATE)));
    }

    @Test
    void should_return_category_api_list_and_api_published_list_if_admin() {
        categoryQueryService.initWith(List.of(Category.builder().id(CAT_ID).build()));
        apiCategoryOrderQueryService.initWith(
            List.of(
                ApiCategoryOrder.builder().apiId("api-1").categoryId(CAT_ID).order(0).build(),
                ApiCategoryOrder.builder().apiId("api-2").categoryId("another-category").order(0).build(),
                ApiCategoryOrder.builder().apiId("api-3").categoryId(CAT_ID).order(2).build(),
                ApiCategoryOrder.builder().apiId("api-4").categoryId(CAT_ID).order(1).build(),
                ApiCategoryOrder.builder().apiId("api-5").categoryId(CAT_ID).order(3).build()
            )
        );

        apiQueryServiceInMemory.initWith(
            List.of(
                Api.builder().id("api-1").categories(Set.of(CAT_ID)).apiLifecycleState(Api.ApiLifecycleState.PUBLISHED).build(),
                Api.builder().id("api-3").categories(Set.of(CAT_ID)).apiLifecycleState(Api.ApiLifecycleState.PUBLISHED).build(),
                Api.builder().id("api-4").categories(Set.of(CAT_ID)).build(),
                Api.builder().id("api-5").categories(Set.of(CAT_ID)).build()
            )
        );

        var result = useCase.execute(new GetCategoryApisUseCase.Input(EXECUTION_CONTEXT, CAT_ID, USER_ID, true, true));

        Assertions.assertThat(result)
            .extracting("results")
            .isEqualTo(
                List.of(
                    resultForApi("api-1", 0, Api.ApiLifecycleState.PUBLISHED, Api.Visibility.PRIVATE),
                    resultForApi("api-3", 2, Api.ApiLifecycleState.PUBLISHED, Api.Visibility.PRIVATE)
                )
            );
    }

    @Test
    void should_return_public_and_published_apis_when_user_id_is_null() {
        categoryQueryService.initWith(List.of(Category.builder().id(CAT_ID).build()));
        apiCategoryOrderQueryService.initWith(
            List.of(
                ApiCategoryOrder.builder().apiId("api-1").categoryId(CAT_ID).order(0).build(),
                ApiCategoryOrder.builder().apiId("api-2").categoryId("another-category").order(0).build(),
                ApiCategoryOrder.builder().apiId("api-3").categoryId(CAT_ID).order(2).build(),
                ApiCategoryOrder.builder().apiId("api-4").categoryId(CAT_ID).order(1).build(),
                ApiCategoryOrder.builder().apiId("api-5").categoryId(CAT_ID).order(3).build()
            )
        );

        apiQueryServiceInMemory.initWith(
            List.of(
                Api.builder().id("api-1").categories(Set.of(CAT_ID)).visibility(Api.Visibility.PUBLIC).build(),
                Api.builder().id("api-3").categories(Set.of(CAT_ID)).visibility(Api.Visibility.PUBLIC).build(),
                Api.builder().id("api-4").categories(Set.of(CAT_ID)).visibility(Api.Visibility.PUBLIC).build()
            )
        );

        var result = useCase.execute(new GetCategoryApisUseCase.Input(EXECUTION_CONTEXT, CAT_ID, null, true));

        Assertions.assertThat(result)
            .extracting("results")
            .isEqualTo(
                List.of(
                    resultForApi("api-1", 0, Api.Visibility.PUBLIC),
                    resultForApi("api-4", 1, Api.Visibility.PUBLIC),
                    resultForApi("api-3", 2, Api.Visibility.PUBLIC)
                )
            );
    }

    private static GetCategoryApisUseCase.@NotNull Result resultForApi(
        String apiId,
        int order,
        Api.ApiLifecycleState apiLifecycleState,
        Api.Visibility visibility
    ) {
        return new GetCategoryApisUseCase.Result(
            ApiCategoryOrder.builder().apiId(apiId).categoryId(CAT_ID).order(order).build(),
            Api.builder().id(apiId).categories(Set.of(CAT_ID)).apiLifecycleState(apiLifecycleState).visibility(visibility).build()
        );
    }

    private static GetCategoryApisUseCase.@NotNull Result resultForApi(String apiId, int order, Api.Visibility visibility) {
        return resultForApi(apiId, order, Api.ApiLifecycleState.CREATED, visibility);
    }
}
