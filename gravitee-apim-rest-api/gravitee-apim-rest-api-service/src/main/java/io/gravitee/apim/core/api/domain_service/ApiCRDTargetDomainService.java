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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.exception.ValidationDomainException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Finds the API a CRD import updates: by cross id when the spec has one, else by the spec id in the environment.
 * A null cross id is never looked up, since any number of APIs can have none.
 */
@DomainService
@RequiredArgsConstructor
public class ApiCRDTargetDomainService {

    private final ApiQueryService apiQueryService;

    private final ApiCrudService apiCrudService;

    /**
     * @return the API to update, or empty when the import creates one
     * @throws ValidationDomainException when the spec id names an API of another environment
     */
    public Optional<Api> findExistingApi(String environmentId, ApiCRDSpec spec) {
        var apiWithId = Optional.ofNullable(spec.getId()).flatMap(apiCrudService::findById);
        if (apiWithId.isPresent() && !environmentId.equals(apiWithId.get().getEnvironmentId())) {
            throw new ValidationDomainException(
                String.format(
                    "API id [%s] belongs to another environment: an import only updates an API of its own environment, remove the id to create a new one",
                    spec.getId()
                )
            );
        }
        if (spec.getCrossId() != null) {
            return apiQueryService.findByEnvironmentIdAndCrossId(environmentId, spec.getCrossId());
        }
        return apiWithId;
    }
}
