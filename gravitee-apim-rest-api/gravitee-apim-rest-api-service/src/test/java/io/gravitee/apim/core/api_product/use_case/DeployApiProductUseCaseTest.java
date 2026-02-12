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
package io.gravitee.apim.core.api_product.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import inmemory.AbstractUseCaseTest;
import inmemory.ApiProductQueryServiceInMemory;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeployApiProductUseCaseTest extends AbstractUseCaseTest {

    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private final EventCrudService eventCrudService = mock(EventCrudService.class);
    private final EventLatestCrudService eventLatestCrudService = mock(EventLatestCrudService.class);
    private DeployApiProductUseCase deployApiProductUseCase;

    @BeforeEach
    void setUp() {
        deployApiProductUseCase = new DeployApiProductUseCase(apiProductQueryService, eventCrudService, eventLatestCrudService);
    }

    @Test
    void should_deploy_api_product() {
        var productId = "api-product-id";
        var apiProduct = ApiProduct.builder().id(productId).name("Product").environmentId(ENV_ID).version("1.0.0").build();
        apiProductQueryService.initWith(List.of(apiProduct));

        var input = DeployApiProductUseCase.Input.of(productId, AUDIT_INFO);
        var output = deployApiProductUseCase.execute(input);

        assertThat(output.apiProduct())
            .hasFieldOrPropertyWithValue("id", productId)
            .hasFieldOrPropertyWithValue("name", "Product")
            .hasFieldOrPropertyWithValue("environmentId", ENV_ID);

        verify(eventCrudService).createEvent(eq(ORG_ID), eq(ENV_ID), any(), any(), any(), any());
        verify(eventLatestCrudService).createOrPatchLatestEvent(eq(ORG_ID), eq(productId), any());
    }

    @Test
    void should_throw_exception_if_product_does_not_exist() {
        var input = DeployApiProductUseCase.Input.of("missing-id", AUDIT_INFO);

        assertThatThrownBy(() -> deployApiProductUseCase.execute(input))
            .isInstanceOf(ApiProductNotFoundException.class)
            .hasMessage("API Product not found.");
    }
}
