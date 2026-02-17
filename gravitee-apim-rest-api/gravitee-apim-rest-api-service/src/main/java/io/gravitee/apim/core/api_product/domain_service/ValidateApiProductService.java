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
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
@CustomLog
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

    public void validateApiIdsForProduct(String environmentId, @Nonnull List<String> apiIds) {
        if (apiIds.isEmpty()) {
            return;
        }

        ApiSearchCriteria criteria = ApiSearchCriteria.builder().ids(apiIds).environmentId(environmentId).build();
        ApiFieldFilter fieldFilter = ApiFieldFilter.builder().definitionExcluded(false).pictureExcluded(true).build();

        Map<String, Api> foundApis = apiQueryService.search(criteria, null, fieldFilter).collect(Collectors.toMap(Api::getId, api -> api));

        List<String> nonExistentApiIds = apiIds
            .stream()
            .filter(id -> !foundApis.containsKey(id))
            .collect(Collectors.toList());
        List<String> invalidApiIds = new ArrayList<>();
        List<String> notAllowedApiIds = new ArrayList<>();

        for (Api api : foundApis.values()) {
            if (api.getDefinitionVersion() != DefinitionVersion.V4) {
                invalidApiIds.add(api.getId());
            } else if (
                api.getApiDefinitionValue() instanceof io.gravitee.definition.model.v4.Api v4Api &&
                !Boolean.TRUE.equals(v4Api.getAllowedInApiProducts())
            ) {
                notAllowedApiIds.add(api.getId());
            }
        }

        if (nonExistentApiIds.isEmpty() && invalidApiIds.isEmpty() && notAllowedApiIds.isEmpty()) {
            return;
        }

        var messages = new ArrayList<String>();
        var parameters = new HashMap<String, String>();

        addError(nonExistentApiIds, "These APIs [%s] do not exist", "nonExistentApiIds", messages, parameters);
        addError(invalidApiIds, "Only V4 API definition is supported. These APIs [%s] are not V4", "invalidApiIds", messages, parameters);
        addError(notAllowedApiIds, "These APIs [%s] are not allowed in API Products", "notAllowedApiIds", messages, parameters);

        throw new ValidationDomainException(String.join(". ", messages), parameters);
    }

    private void addError(
        List<String> apiIds,
        String messageFormat,
        String parameterKey,
        List<String> messages,
        Map<String, String> parameters
    ) {
        if (!apiIds.isEmpty()) {
            String joined = String.join(", ", apiIds);
            messages.add(String.format(messageFormat, joined));
            parameters.put(parameterKey, joined);
        }
    }
}
