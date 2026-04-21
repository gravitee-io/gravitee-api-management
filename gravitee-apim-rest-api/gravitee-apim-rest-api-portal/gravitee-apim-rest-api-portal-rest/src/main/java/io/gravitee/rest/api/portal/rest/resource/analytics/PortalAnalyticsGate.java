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
package io.gravitee.rest.api.portal.rest.resource.analytics;

import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;

/**
 * Ensures portal analytics REST endpoints honor {@link Key#PORTAL_NEXT_ANALYTICS_ENABLED}.
 */
final class PortalAnalyticsGate {

    private PortalAnalyticsGate() {}

    static void requireAnalyticsEnabled(ParameterService parameterService) {
        if (
            !parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.PORTAL_NEXT_ANALYTICS_ENABLED,
                ParameterReferenceType.ENVIRONMENT
            )
        ) {
            throw new ForbiddenAccessException();
        }
    }
}
