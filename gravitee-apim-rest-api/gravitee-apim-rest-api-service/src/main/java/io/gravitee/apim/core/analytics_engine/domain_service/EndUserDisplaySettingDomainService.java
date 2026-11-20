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
package io.gravitee.apim.core.analytics_engine.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.parameters.model.ParameterContext;
import io.gravitee.apim.core.parameters.query_service.ParametersQueryService;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import lombok.RequiredArgsConstructor;

/**
 * Whether an organization shows the end user of a call, the identity a JWT or OAuth2 plan reports as {@code user}: the
 * organization setting {@code logging.user.displayed}, off by default, that the Console logs already honour.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class EndUserDisplaySettingDomainService {

    private final ParametersQueryService parametersQueryService;

    public boolean isDisplayed(String organizationId) {
        return parametersQueryService.findAsBoolean(
            Key.LOGGING_USER_DISPLAYED,
            new ParameterContext(null, organizationId, ParameterReferenceType.ORGANIZATION)
        );
    }
}
