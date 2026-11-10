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

import inmemory.ApiProductQueryServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetApiProductFlowsUseCaseTest {

    private static final String ENV_ID = "env-id";
    private static final String API_PRODUCT_ID = "api-product-id";

    private final ApiProductQueryServiceInMemory apiProductQueryService = new ApiProductQueryServiceInMemory();
    private final FlowCrudServiceInMemory flowCrudService = new FlowCrudServiceInMemory();
    private GetApiProductFlowsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetApiProductFlowsUseCase(apiProductQueryService, flowCrudService);
    }

    @Test
    void should_return_the_stored_flows_in_order_and_the_flow_mode() {
        var flowExecution = new FlowExecution();
        flowExecution.setMode(FlowMode.BEST_MATCH);
        flowExecution.setMatchRequired(true);
        apiProductQueryService.initWith(List.of(aProduct(ENV_ID).toBuilder().flowExecution(flowExecution).build()));
        flowCrudService.saveApiProductFlows(API_PRODUCT_ID, List.of(Flow.builder().name("b").build(), Flow.builder().name("a").build()));

        var output = useCase.execute(new GetApiProductFlowsUseCase.Input(ENV_ID, API_PRODUCT_ID));

        assertThat(output.apiProductFlows().flows()).extracting(Flow::getName).containsExactly("b", "a");
        assertThat(output.apiProductFlows().flowExecution()).usingRecursiveComparison().isEqualTo(flowExecution);
    }

    @Test
    void should_return_the_default_mode_and_no_flow_for_a_product_that_never_saved_any() {
        apiProductQueryService.initWith(List.of(aProduct(ENV_ID)));

        var output = useCase.execute(new GetApiProductFlowsUseCase.Input(ENV_ID, API_PRODUCT_ID));

        assertThat(output.apiProductFlows().flows()).isEmpty();
        assertThat(output.apiProductFlows().flowExecution().getMode()).isEqualTo(FlowMode.DEFAULT);
        assertThat(output.apiProductFlows().flowExecution().isMatchRequired()).isFalse();
    }

    @Test
    void should_throw_not_found_for_an_unknown_product() {
        assertThatThrownBy(() -> useCase.execute(new GetApiProductFlowsUseCase.Input(ENV_ID, API_PRODUCT_ID))).isInstanceOf(
            ApiProductNotFoundException.class
        );
    }

    @Test
    void should_throw_not_found_for_a_product_of_another_environment() {
        apiProductQueryService.initWith(List.of(aProduct("other-env")));

        assertThatThrownBy(() -> useCase.execute(new GetApiProductFlowsUseCase.Input(ENV_ID, API_PRODUCT_ID))).isInstanceOf(
            ApiProductNotFoundException.class
        );
    }

    private static ApiProduct aProduct(String environmentId) {
        return ApiProduct.builder().id(API_PRODUCT_ID).environmentId(environmentId).name("Product").build();
    }
}
