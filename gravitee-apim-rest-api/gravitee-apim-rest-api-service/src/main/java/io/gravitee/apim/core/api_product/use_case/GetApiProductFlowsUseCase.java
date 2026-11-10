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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProductFlows;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetApiProductFlowsUseCase {

    private final ApiProductQueryService apiProductQueryService;
    private final FlowCrudService flowCrudService;

    public Output execute(Input input) {
        var apiProduct = apiProductQueryService
            .findById(input.apiProductId())
            .filter(product -> input.environmentId().equals(product.getEnvironmentId()))
            .orElseThrow(() -> new ApiProductNotFoundException(input.apiProductId()));
        return new Output(new ApiProductFlows(apiProduct.getFlowExecution(), flowCrudService.getApiProductFlows(apiProduct.getId())));
    }

    public record Input(String environmentId, String apiProductId) {}

    public record Output(ApiProductFlows apiProductFlows) {}
}
