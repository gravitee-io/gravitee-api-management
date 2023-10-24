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
package io.gravitee.rest.api.management.security.config;

import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.common.event.EventManager;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class GraviteeUrlBasedCorsConfigurationSource extends UrlBasedCorsConfigurationSource {

    private final ParameterService parameterService;
    private final AccessPointQueryService accessPointQueryService;
    private final EventManager eventManager;
    private final Map<String, GraviteeCorsConfiguration> corsConfigurationByOrganization = new ConcurrentHashMap<>();

    @Override
    public CorsConfiguration getCorsConfiguration(final @NonNull HttpServletRequest request) {
        String organizationId = GraviteeContext.getCurrentOrganization();
        if (organizationId != null) {
            return corsConfigurationByOrganization.computeIfAbsent(
                organizationId,
                id -> new GraviteeCorsConfiguration(parameterService, accessPointQueryService, eventManager, organizationId)
            );
        }
        return super.getCorsConfiguration(request);
    }
}
