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
package io.gravitee.apim.core.api_product.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@DomainService
@RequiredArgsConstructor
@Slf4j
public class ValidateApiProductService {

    private final ApiQueryService apiQueryService;

    public void validate(ApiProduct apiProduct) {
        if (StringUtils.isEmpty(apiProduct.getName())) {
            throw new InvalidDataException("API Product name is required.");
        }
        if (StringUtils.isEmpty(apiProduct.getVersion())) {
            throw new InvalidDataException("API Product version is required.");
        }
    }

    public Set<String> filterApiIdsAllowedInProduct(String environmentId, @Nonnull List<String> apiIds) {
        if (apiIds.isEmpty()) {
            log.debug("Filtered API IDs for environment {}: input={}, output=0 (empty list)", environmentId, apiIds.size());
            return Set.of();
        }

        ApiSearchCriteria criteria = ApiSearchCriteria.builder().ids(apiIds).environmentId(environmentId).build();
        ApiFieldFilter fieldFilter = ApiFieldFilter.builder().definitionExcluded(false).pictureExcluded(true).build();

        Set<String> allowedApiIds = apiQueryService
            .search(criteria, null, fieldFilter)
            .filter(
                api ->
                    api.getDefinitionVersion() == DefinitionVersion.V4 &&
                    api.getApiDefinitionValue() instanceof io.gravitee.definition.model.v4.Api v4Api &&
                    Boolean.TRUE.equals(v4Api.getAllowedInApiProducts())
            )
            .map(Api::getId)
            .collect(Collectors.toSet());

        log.debug("Filtered API IDs for environment {}: input={}, output={}", environmentId, apiIds.size(), allowedApiIds.size());

        return allowedApiIds;
    }
}
