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
package inmemory;

import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ApiProductQueryServiceInMemory extends AbstractQueryServiceInMemory<ApiProduct> implements ApiProductQueryService {

    @Override
    public Optional<ApiProduct> findByEnvironmentIdAndName(String environmentId, String name) {
        return storage
            .stream()
            .filter(
                apiProduct -> Objects.equals(environmentId, apiProduct.getEnvironmentId()) && Objects.equals(name, apiProduct.getName())
            )
            .findFirst();
    }

    @Override
    public Set<ApiProduct> findByEnvironmentId(String environmentId) {
        return new HashSet<>(storage);
    }

    @Override
    public Set<ApiProduct> findByEnvironmentIdAndIdIn(String environmentId, Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        Set<ApiProduct> result = new HashSet<>();
        for (ApiProduct apiProduct : storage) {
            if (Objects.equals(environmentId, apiProduct.getEnvironmentId()) && ids.contains(apiProduct.getId())) {
                result.add(apiProduct);
            }
        }
        return result;
    }

    @Override
    public Optional<ApiProduct> findById(String apiProductId) {
        return storage
            .stream()
            .filter(apiProduct -> apiProduct.getId().equals(apiProductId))
            .findFirst();
    }

    @Override
    public Set<ApiProduct> findByApiId(String apiId) {
        Set<ApiProduct> result = new HashSet<>();
        for (ApiProduct apiProduct : storage) {
            if (apiProduct.getApiIds() != null && apiProduct.getApiIds().contains(apiId)) {
                result.add(apiProduct);
            }
        }
        return result;
    }

    @Override
    public Map<String, Set<ApiProduct>> findProductsByApiIds(Set<String> apiIds) {
        Map<String, Set<ApiProduct>> result = new HashMap<>();
        if (apiIds == null || apiIds.isEmpty()) {
            return result;
        }
        for (String apiId : apiIds) {
            result.put(apiId, findByApiId(apiId));
        }
        return result;
    }
}
