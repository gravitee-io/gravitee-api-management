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
import io.gravitee.apim.core.exception.ValidationDomainException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class VerifyApiProductNameUseCase {

    private final ApiProductQueryService apiProductQueryService;

    public Output execute(Input input) {
        if (input.name == null || input.name.trim().isEmpty()) {
            throw new ValidationDomainException("API Product name cannot be empty");
        }

        String trimmedName = input.name.trim();
        Optional<ApiProduct> existingApiProduct = apiProductQueryService.findByEnvironmentIdAndName(input.environmentId, trimmedName);

        if (existingApiProduct.isPresent()) {
            throw new ValidationDomainException(String.format("API Product name '%s' already exists", trimmedName));
        }
        return new Output(trimmedName);
    }

    public record Input(String environmentId, String name, String apiProductId) {
        public static Input of(String environmentId, String name) {
            return new Input(environmentId, name, null);
        }

        public static Input of(String environmentId, String name, String apiProductId) {
            return new Input(environmentId, name, apiProductId);
        }
    }

    public record Output(String sanitizedName) {}
}
