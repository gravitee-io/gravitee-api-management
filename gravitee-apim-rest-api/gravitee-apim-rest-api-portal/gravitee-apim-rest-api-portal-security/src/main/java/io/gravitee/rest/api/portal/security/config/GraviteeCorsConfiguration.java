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

import static io.gravitee.rest.api.security.csrf.CookieCsrfSignedTokenRepository.DEFAULT_CSRF_HEADER_NAME;
import static io.gravitee.rest.api.security.filter.RecaptchaFilter.DEFAULT_RECAPTCHA_HEADER_NAME;
import static java.util.Arrays.asList;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import java.util.List;
import org.springframework.web.cors.CorsConfiguration;

public class GraviteeCorsConfiguration extends CorsConfiguration implements EventListener<Key, Parameter> {

    private ParameterService parameterService;
    private String environmentId;

    public GraviteeCorsConfiguration(ParameterService parameterService, EventManager eventManager, String environmentId) {
        this.parameterService = parameterService;
        this.environmentId = environmentId;

        eventManager.subscribeForEvents(this, Key.class);

        this.setAllowCredentials(true);
        this.setAllowedOrigins(getPropertiesAsList(Key.PORTAL_HTTP_CORS_ALLOW_ORIGIN, "*"));
        this.setAllowedHeaders(
                getPropertiesAsList(
                    Key.PORTAL_HTTP_CORS_ALLOW_HEADERS,
                    "Cache-Control, Pragma, Origin, Authorization, Content-Type, X-Requested-With, " +
                    DEFAULT_CSRF_HEADER_NAME +
                    ", " +
                    DEFAULT_RECAPTCHA_HEADER_NAME
                )
            );
        this.setAllowedMethods(getPropertiesAsList(Key.PORTAL_HTTP_CORS_ALLOW_METHODS, "OPTIONS, GET, POST, PUT, DELETE, PATCH"));
        this.setExposedHeaders(getPropertiesAsList(Key.PORTAL_HTTP_CORS_EXPOSED_HEADERS, DEFAULT_CSRF_HEADER_NAME));
        this.setMaxAge(
                Long.valueOf(parameterService.find(Key.PORTAL_HTTP_CORS_MAX_AGE, environmentId, ParameterReferenceType.ENVIRONMENT))
            );
    }

    @Override
    public void onEvent(Event<Key, Parameter> event) {
        if (environmentId.equals(event.content().getReferenceId())) {
            switch (event.type()) {
                case PORTAL_HTTP_CORS_ALLOW_ORIGIN:
                    this.setAllowedOrigins(semicolonStringToList(event.content().getValue()));
                    break;
                case PORTAL_HTTP_CORS_ALLOW_HEADERS:
                    this.setAllowedHeaders(semicolonStringToList(event.content().getValue()));
                    break;
                case PORTAL_HTTP_CORS_ALLOW_METHODS:
                    this.setAllowedMethods(semicolonStringToList(event.content().getValue()));
                    break;
                case PORTAL_HTTP_CORS_EXPOSED_HEADERS:
                    this.setExposedHeaders(semicolonStringToList(event.content().getValue()));
                    break;
                case PORTAL_HTTP_CORS_MAX_AGE:
                    this.setMaxAge(Long.parseLong(event.content().getValue()));
                    break;
            }
        }
    }

    private List<String> getPropertiesAsList(final Key propertyKey, final String defaultValue) {
        String property = parameterService.find(propertyKey, environmentId, ParameterReferenceType.ENVIRONMENT);
        if (property == null) {
            property = defaultValue;
        }
        return semicolonStringToList(property);
    }

    private List<String> semicolonStringToList(String listStr) {
        return asList(listStr.replaceAll("\\s+", "").split(";"));
    }
}
