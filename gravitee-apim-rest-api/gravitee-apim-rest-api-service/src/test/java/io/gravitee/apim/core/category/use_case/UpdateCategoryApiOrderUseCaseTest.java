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

import static assertions.CoreAssertions.assertThat;

import inmemory.ApiAuthorizationDomainServiceInMemory;
import inmemory.ApiCrudServiceInMemory;
import inmemory.CategoryApiCrudServiceInMemory;
import inmemory.CategoryQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.UpdateCategoryApiDomainServiceInMemory;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.category.domain_service.ValidateCategoryDomainService;
import io.gravitee.apim.core.category.exception.ApiAndCategoryNotAssociatedException;
import io.gravitee.apim.core.category.exception.CategoryNotFoundException;
import io.gravitee.apim.core.category.model.ApiCategoryOrder;
import io.gravitee.apim.core.category.model.Category;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdateCategoryApiOrderUseCaseTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("ORG_ID", "ENV_ID");
    private static final String CAT_ID = "cat-id";
    private static final String USER_ID = "user-id";
    private static final String API_ID = "api-id";

    ValidateCategoryDomainService validateCategoryDomainService;
    ApiAuthorizationDomainServiceInMemory apiAuthorizationDomainService = new ApiAuthorizationDomainServiceInMemory();
    UpdateCategoryApiDomainServiceInMemory updateCategoryApiDomainService;
    CategoryQueryServiceInMemory categoryQueryService = new CategoryQueryServiceInMemory();
    ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    CategoryApiCrudServiceInMemory categoryApiCrudService = new CategoryApiCrudServiceInMemory();

    UpdateCategoryApiOrderUseCase useCase;

    @BeforeEach
    void setUp() {
        updateCategoryApiDomainService = new UpdateCategoryApiDomainServiceInMemory(categoryApiCrudService);
        validateCategoryDomainService = new ValidateCategoryDomainService(categoryQueryService);
        useCase = new UpdateCategoryApiOrderUseCase(
            validateCategoryDomainService,
            apiAuthorizationDomainService,
            updateCategoryApiDomainService,
            apiCrudServiceInMemory,
            categoryApiCrudService
        );
    }

    @AfterEach
    void tearDown() {
        Stream.of(
            apiAuthorizationDomainService,
            updateCategoryApiDomainService,
            categoryQueryService,
            apiCrudServiceInMemory,
            categoryApiCrudService
        ).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_throw_error_when_category_id_is_null() {
        var throwable = Assertions.catchThrowable(() ->
            useCase.execute(new UpdateCategoryApiOrderUseCase.Input(EXECUTION_CONTEXT, null, API_ID, USER_ID, false, 99))
        );

        Assertions.assertThat(throwable).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void should_throw_error_when_category_id_not_found() {
        var throwable = Assertions.catchThrowable(() ->
            useCase.execute(new UpdateCategoryApiOrderUseCase.Input(EXECUTION_CONTEXT, CAT_ID, API_ID, USER_ID, false, 99))
        );

        Assertions.assertThat(throwable).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void should_throw_error_if_api_not_in_user_scope() {
        categoryQueryService.initWith(List.of(Category.builder().id(CAT_ID).build()));

        var throwable = Assertions.catchThrowable(() ->
            useCase.execute(new UpdateCategoryApiOrderUseCase.Input(EXECUTION_CONTEXT, CAT_ID, API_ID, USER_ID, false, 99))
        );

        Assertions.assertThat(throwable).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_error_if_admin_and_api_not_found() {
        categoryQueryService.initWith(List.of(Category.builder().id(CAT_ID).build()));

        var throwable = Assertions.catchThrowable(() ->
            useCase.execute(new UpdateCategoryApiOrderUseCase.Input(EXECUTION_CONTEXT, CAT_ID, API_ID, USER_ID, true, 99))
        );

        Assertions.assertThat(throwable).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_error_if_api_category_order_not_found() {
        categoryQueryService.initWith(List.of(Category.builder().id(CAT_ID).build()));
        apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));
        var throwable = Assertions.catchThrowable(() ->
            useCase.execute(new UpdateCategoryApiOrderUseCase.Input(EXECUTION_CONTEXT, CAT_ID, API_ID, USER_ID, true, 99))
        );

        Assertions.assertThat(throwable).isInstanceOf(ApiAndCategoryNotAssociatedException.class);
    }

    @Test
    void should_return_changed_api_category_order() {
        categoryQueryService.initWith(List.of(Category.builder().id(CAT_ID).build()));
        apiCrudServiceInMemory.initWith(List.of(Api.builder().id(API_ID).build()));
        categoryApiCrudService.initWith(List.of(ApiCategoryOrder.builder().apiId(API_ID).categoryId(CAT_ID).order(0).build()));

        var result = useCase.execute(new UpdateCategoryApiOrderUseCase.Input(EXECUTION_CONTEXT, CAT_ID, API_ID, USER_ID, true, 99));

        assertThat(result)
            .isNotNull()
            .extracting(UpdateCategoryApiOrderUseCase.Output::result)
            .extracting(UpdateCategoryApiOrderUseCase.Result::apiCategoryOrder)
            .extracting(ApiCategoryOrder::getOrder)
            .isEqualTo(99);
    }
}
