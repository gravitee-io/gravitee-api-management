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
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductKindFilter;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import java.util.Optional;
import lombok.AllArgsConstructor;

@UseCase
@AllArgsConstructor
public class VerifyApiProductExistsUseCase {

    private final ApiProductQueryService apiProductQueryService;

    public void execute(Input input) {
        Optional<ApiProduct> product = apiProductQueryService.findById(input.apiProductId());
        if (
            product.isEmpty() ||
            !input.environmentId().equals(product.get().getEnvironmentId()) ||
            !input.kindFilter().matches(product.get())
        ) {
            throw new ApiProductNotFoundException(input.apiProductId());
        }
    }

    /**
     * {@code kindFilter} is what the asking surface manages. A product of another kind is reported as absent
     * rather than forbidden, like one of another environment: a surface that cannot manage a product should not
     * confirm that it exists either.
     */
    public record Input(String environmentId, String apiProductId, ApiProductKindFilter kindFilter) {
        /** Any kind, for a caller that manages all of them. */
        public Input(String environmentId, String apiProductId) {
            this(environmentId, apiProductId, ApiProductKindFilter.any());
        }
    }
}
