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
package io.gravitee.apim.core.api_product.query_service;

import io.gravitee.apim.core.api_product.model.ApiProduct;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ApiProductQueryService {
    Optional<ApiProduct> findByEnvironmentIdAndName(String environmentId, String name);
    Set<ApiProduct> findByEnvironmentId(String environmentId);
    Optional<ApiProduct> findById(String apiProductId);
    Set<ApiProduct> findByApiId(String apiId);

    Map<String, Set<ApiProduct>> findProductsByApiIds(Set<String> apiIds);
}
