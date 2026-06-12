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
package io.gravitee.apim.infra.domain_service.portal_page;

import io.gravitee.apim.core.parameters.model.ParameterContext;
import io.gravitee.apim.core.parameters.query_service.ParametersQueryService;
import io.gravitee.apim.core.portal_page.domain_service.CheckTypoToleranceDomainService;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckTypoToleranceDomainServiceImpl implements CheckTypoToleranceDomainService {

    private final ParametersQueryService parametersQueryService;

    @Override
    public boolean isEnabled(String environmentId, String organizationId) {
        return parametersQueryService.findAsBoolean(
            Key.PORTAL_NEXT_SEARCH_FUZZY,
            new ParameterContext(environmentId, organizationId, ParameterReferenceType.ENVIRONMENT)
        );
    }
}
