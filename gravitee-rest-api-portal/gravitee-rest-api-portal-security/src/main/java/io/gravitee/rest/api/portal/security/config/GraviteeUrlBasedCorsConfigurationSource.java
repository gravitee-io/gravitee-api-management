/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.portal.security.config;

import io.gravitee.common.event.EventManager;
import io.gravitee.rest.api.service.ParameterService;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeUrlBasedCorsConfigurationSource extends UrlBasedCorsConfigurationSource {

    private Map<String, GraviteeCorsConfiguration> corsConfigurationByEnvironment = new HashMap<>();

    private ParameterService parameterService;
    private EventManager eventManager;

    public GraviteeUrlBasedCorsConfigurationSource(ParameterService parameterService, EventManager eventManager) {
        this.parameterService = parameterService;
        this.eventManager = eventManager;
    }

    private String computeEnvironmentId(HttpServletRequest request) {
        String path = request.getPathInfo();
        final String environmentsResourcePath = "/environments/";
        final int environmentPathIndex = path.indexOf(environmentsResourcePath);

        if (environmentPathIndex > -1) {
            int envIdStartIndex = environmentPathIndex + environmentsResourcePath.length();
            return path.substring(envIdStartIndex, path.indexOf('/', envIdStartIndex));
        }
        return null;
    }

    @Override
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        String environmentId = computeEnvironmentId(request);
        if (environmentId != null) {
            GraviteeCorsConfiguration corsConfiguration = corsConfigurationByEnvironment.get(environmentId);
            if (corsConfiguration == null) {
                corsConfiguration = new GraviteeCorsConfiguration(parameterService, eventManager, environmentId);
                this.corsConfigurationByEnvironment.put(environmentId, corsConfiguration);
            }
            return corsConfiguration;
        }
        return super.getCorsConfiguration(request);
    }
}
