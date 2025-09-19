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
package io.gravitee.apim.infra.domain_service.api;

import io.gravitee.apim.core.api.domain_service.ApiAuthorizationDomainService;
import io.gravitee.apim.core.api.model.ApiQueryCriteria;
import io.gravitee.apim.infra.adapter.ApiQueryCriteriaAdapter;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ApiAuthorizationDomainServiceLegacyWrapper implements ApiAuthorizationDomainService {

    private final ApiAuthorizationService delegate;

    @Override
    public Set<String> findIdsByUser(
        ExecutionContext executionContext,
        String userId,
        ApiQueryCriteria apiQueryCriteria,
        Sortable sortable,
        boolean manageOnly
    ) {
        return this.delegate.findIdsByUser(
            executionContext,
            userId,
            ApiQueryCriteriaAdapter.INSTANCE.toRestModel(apiQueryCriteria),
            sortable,
            manageOnly
        );
    }
}
