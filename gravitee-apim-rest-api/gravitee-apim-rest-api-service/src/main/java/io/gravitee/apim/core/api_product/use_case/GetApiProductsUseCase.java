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
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetApiProductsUseCase {

    private final ApiProductQueryService apiProductQueryService;

    public Output execute(Input input) {
        if (input.apiProductId() != null) {
            Optional<ApiProduct> apiProduct = apiProductQueryService.findById(input.apiProductId());
            return Output.single(apiProduct);
        } else if (input.environmentId() != null) {
            Set<ApiProduct> apiProducts = apiProductQueryService.findByEnvironmentId(input.environmentId());
            return Output.multiple(apiProducts);
        } else {
            throw new IllegalArgumentException("environmentId must be provided for listing API Products");
        }
    }

    public record Input(String environmentId, String apiProductId) {
        public static Input of(String environmentId) {
            return new Input(environmentId, null);
        }

        public static Input of(String environmentId, String apiProductId) {
            return new Input(environmentId, apiProductId);
        }
    }

    public record Output(Set<ApiProduct> apiProducts, Optional<ApiProduct> apiProduct) {
        public static Output multiple(Set<ApiProduct> apiProducts) {
            return new Output(apiProducts, Optional.empty());
        }

        public static Output single(Optional<ApiProduct> apiProduct) {
            return new Output(null, apiProduct);
        }
    }
}
