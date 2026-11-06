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

import static io.gravitee.rest.api.security.cors.GraviteeCorsConfiguration.UNDEFINED_REFERENCE_ID;

import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.event.EventManager;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.security.cors.AbstractGraviteeUrlBasedCorsConfigurationSource;
import io.gravitee.rest.api.security.cors.GraviteeCorsConfiguration;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeUrlBasedCorsConfigurationSource extends AbstractGraviteeUrlBasedCorsConfigurationSource {

    private static final String COCKPIT_AUTH_PATH = "/auth/cockpit";

    public GraviteeUrlBasedCorsConfigurationSource(
        final Environment environment,
        final ParameterService parameterService,
        final InstallationAccessQueryService installationAccessQueryService,
        final EventManager eventManager,
        final ParameterReferenceType parameterReferenceType
    ) {
        super(environment, parameterService, installationAccessQueryService, eventManager, parameterReferenceType);
    }

    @Override
    public CorsConfiguration getCorsConfiguration(final @NonNull HttpServletRequest request) {
        // Cockpit posts its login form from its own origin. It is a top-level navigation, which CORS does not protect,
        // and an organization's allowed origins must not block it.
        if (COCKPIT_AUTH_PATH.equals(request.getPathInfo())) {
            return null;
        }
        return super.getCorsConfiguration(request);
    }

    @Override
    protected String getReferenceId() {
        return GraviteeContext.getCurrentOrganization();
    }
}
