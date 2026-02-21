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
package io.gravitee.rest.api.security.cors;

import static io.gravitee.rest.api.security.cors.GraviteeCorsConfiguration.UNDEFINED_REFERENCE_ID;

import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.api.cache.CacheListener;
import io.gravitee.node.plugin.cache.common.InMemoryCache;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractGraviteeUrlBasedCorsConfigurationSource extends UrlBasedCorsConfigurationSource {

    private final Environment environment;
    private final ParameterService parameterService;
    private final InstallationAccessQueryService installationAccessQueryService;
    private final EventManager eventManager;
    private final ParameterReferenceType parameterReferenceType;
    private final Cache<String, CorsConfiguration> corsConfigurationByUrl;

    public AbstractGraviteeUrlBasedCorsConfigurationSource(
        Environment environment,
        ParameterService parameterService,
        InstallationAccessQueryService installationAccessQueryService,
        EventManager eventManager,
        ParameterReferenceType parameterReferenceType
    ) {
        super();
        this.environment = environment;
        this.parameterService = parameterService;
        this.installationAccessQueryService = installationAccessQueryService;
        this.eventManager = eventManager;
        this.parameterReferenceType = parameterReferenceType;

        final CacheConfiguration cacheConfiguration = CacheConfiguration.builder()
            .maxSize(environment.getProperty("cors.cache.max-size", Integer.class, 1000))
            .timeToLiveInMs(environment.getProperty("cors.cache.ttl", Long.class, 60000L))
            .build();

        this.corsConfigurationByUrl = new InMemoryCache<>("cors-config-by-url", cacheConfiguration);
        this.corsConfigurationByUrl.addCacheListener(new CorsConfigurationCacheListener());
    }

    @Override
    public CorsConfiguration getCorsConfiguration(final @NonNull HttpServletRequest request) {
        String referenceId = getReferenceId();

        return corsConfigurationByUrl.computeIfAbsent(extractUrl(request), id ->
            new GraviteeCorsConfiguration(
                environment,
                parameterService,
                installationAccessQueryService,
                eventManager,
                referenceId != null ? referenceId : UNDEFINED_REFERENCE_ID,
                parameterReferenceType
            )
        );
    }

    protected abstract String getReferenceId();

    private String extractUrl(HttpServletRequest request) {
        return extractUrlFromReferer(request)
            .or(() -> extractUrlFromOrigin(request))
            .orElseGet(() -> extractUrlFromServer(request));
    }

    private Optional<String> extractUrlFromReferer(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(HttpHeaderNames.REFERER));
    }

    private Optional<String> extractUrlFromOrigin(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(HttpHeaderNames.ORIGIN));
    }

    private String extractUrlFromServer(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    }

    /**
     * Cache listener to release CorsConfiguration when evicted or expired from cache.
     * This is required to unregister any event listener registered by the CorsConfiguration.
     */
    private static class CorsConfigurationCacheListener implements CacheListener<String, CorsConfiguration> {

        @Override
        public void onEntryEvicted(String key, CorsConfiguration corsConfiguration) {
            release(corsConfiguration);
        }

        @Override
        public void onEntryExpired(String key, CorsConfiguration corsConfiguration) {
            release(corsConfiguration);
        }

        private static void release(CorsConfiguration corsConfiguration) {
            if (corsConfiguration instanceof GraviteeCorsConfiguration graviteeCorsConfiguration) {
                graviteeCorsConfiguration.release();
            }
        }
    }
}
