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
import io.gravitee.apim.core.api_product.query_service.ApiProductSearchQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SearchApiProductsUseCase {

    private final ApiProductSearchQueryService apiProductSearchQueryService;

    public Output execute(Input input) {
        Page<ApiProduct> page = apiProductSearchQueryService.search(
            input.environmentId(),
            input.organizationId(),
            input.query(),
            input.ids(),
            input.pageable(),
            input.sortable()
        );
        return new Output(page);
    }

    public record Input(String environmentId, String organizationId, String query, Set<String> ids, Pageable pageable, Sortable sortable) {
        public static Input of(
            String environmentId,
            String organizationId,
            String query,
            Set<String> ids,
            Pageable pageable,
            Sortable sortable
        ) {
            return new Input(environmentId, organizationId, query != null ? query.trim() : null, ids, pageable, sortable);
        }
    }

    public record Output(Page<ApiProduct> page) {}
}
