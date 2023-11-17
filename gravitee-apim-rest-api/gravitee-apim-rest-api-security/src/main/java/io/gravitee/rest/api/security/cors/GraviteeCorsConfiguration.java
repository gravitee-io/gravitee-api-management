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

import static java.util.Arrays.asList;

import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;

public class GraviteeCorsConfiguration extends CorsConfiguration implements EventListener<Key, Parameter> {

    public static final String UNDEFINED_REFERENCE_ID = "undefined";
    private final Environment environment;
    private final ParameterService parameterService;
    private final InstallationAccessQueryService installationAccessQueryService;
    private final String referenceId;
    private final ParameterReferenceType parameterReferenceType;

    public GraviteeCorsConfiguration(
        final Environment environment,
        final ParameterService parameterService,
        final InstallationAccessQueryService installationAccessQueryService,
        final EventManager eventManager,
        final String referenceId,
        final ParameterReferenceType parameterReferenceType
    ) {
        this.environment = environment;
        this.parameterService = parameterService;
        this.installationAccessQueryService = installationAccessQueryService;
        this.referenceId = referenceId;
        this.parameterReferenceType = parameterReferenceType;
        eventManager.subscribeForEvents(this, Key.class);

        this.setAllowCredentials(true);
        this.setAllowedOriginPatterns(buildAllowedOriginPatterns(getPropertiesAsList(allowOriginKey())));
        this.setAllowedHeaders(getPropertiesAsList(allowHeadersKey()));
        this.setAllowedMethods(getPropertiesAsList(allowMethodsKey()));
        this.setExposedHeaders(getPropertiesAsList(exposedHeadersKey()));
        this.setMaxAge(Long.valueOf(getProperty(maxAgeKey())));
    }

    protected Key allowOriginKey() {
        if (parameterReferenceType == ParameterReferenceType.ORGANIZATION) {
            return Key.CONSOLE_HTTP_CORS_ALLOW_ORIGIN;
        } else if (parameterReferenceType == ParameterReferenceType.ENVIRONMENT) {
            return Key.PORTAL_HTTP_CORS_ALLOW_ORIGIN;
        }
        return null;
    }

    protected Key allowHeadersKey() {
        if (parameterReferenceType == ParameterReferenceType.ORGANIZATION) {
            return Key.CONSOLE_HTTP_CORS_ALLOW_HEADERS;
        } else if (parameterReferenceType == ParameterReferenceType.ENVIRONMENT) {
            return Key.PORTAL_HTTP_CORS_ALLOW_HEADERS;
        }
        return null;
    }

    protected Key allowMethodsKey() {
        if (parameterReferenceType == ParameterReferenceType.ORGANIZATION) {
            return Key.CONSOLE_HTTP_CORS_ALLOW_METHODS;
        } else if (parameterReferenceType == ParameterReferenceType.ENVIRONMENT) {
            return Key.PORTAL_HTTP_CORS_ALLOW_METHODS;
        }
        return null;
    }

    protected Key exposedHeadersKey() {
        if (parameterReferenceType == ParameterReferenceType.ORGANIZATION) {
            return Key.CONSOLE_HTTP_CORS_EXPOSED_HEADERS;
        } else if (parameterReferenceType == ParameterReferenceType.ENVIRONMENT) {
            return Key.PORTAL_HTTP_CORS_EXPOSED_HEADERS;
        }
        return null;
    }

    protected Key maxAgeKey() {
        if (parameterReferenceType == ParameterReferenceType.ORGANIZATION) {
            return Key.CONSOLE_HTTP_CORS_MAX_AGE;
        } else if (parameterReferenceType == ParameterReferenceType.ENVIRONMENT) {
            return Key.PORTAL_HTTP_CORS_MAX_AGE;
        }
        return null;
    }

    @Override
    public void onEvent(Event<Key, Parameter> event) {
        if (referenceId.equals(event.content().getReferenceId())) {
            if (event.type() == allowOriginKey()) {
                this.setAllowedOriginPatterns(buildAllowedOriginPatterns(semicolonStringToList(event.content().getValue())));
            } else if (event.type() == allowHeadersKey()) {
                this.setAllowedHeaders(semicolonStringToList(event.content().getValue()));
            } else if (event.type() == allowMethodsKey()) {
                this.setAllowedMethods(semicolonStringToList(event.content().getValue()));
            } else if (event.type() == exposedHeadersKey()) {
                this.setExposedHeaders(semicolonStringToList(event.content().getValue()));
            } else if (event.type() == maxAgeKey()) {
                this.setMaxAge(Long.parseLong(event.content().getValue()));
            }
        }
    }

    protected String getProperty(final Key propertyKey) {
        if (propertyKey != null) {
            if (referenceId.equals(UNDEFINED_REFERENCE_ID)) {
                return environment.getProperty(propertyKey.key(), String.class, propertyKey.defaultValue());
            } else {
                String value = parameterService.find(
                    GraviteeContext.getExecutionContext(),
                    propertyKey,
                    referenceId,
                    parameterReferenceType
                );
                if (value == null) {
                    value = propertyKey.defaultValue();
                }
                return value;
            }
        }
        return null;
    }

    protected List<String> getPropertiesAsList(final Key propertyKey) {
        String property = getProperty(propertyKey);
        return semicolonStringToList(property);
    }

    private List<String> semicolonStringToList(final String listStr) {
        return asList(listStr.replaceAll("\\s+", "").split(";"));
    }

    private List<String> buildAllowedOriginPatterns(final List<String> allowedOriginPatterns) {
        List<String> builtAllowedOrigins = new ArrayList<>();
        if (allowedOriginPatterns != null) {
            builtAllowedOrigins.addAll(allowedOriginPatterns);
        }
        List<String> urls = null;
        if (parameterReferenceType == ParameterReferenceType.ORGANIZATION) {
            urls = getConsoleUrls();
        } else if (parameterReferenceType == ParameterReferenceType.ENVIRONMENT) {
            urls = getPortalUrls();
        }
        if (urls != null) {
            builtAllowedOrigins.addAll(urls);
        }
        return builtAllowedOrigins;
    }

    private List<String> getConsoleUrls() {
        if (referenceId.equals(UNDEFINED_REFERENCE_ID)) {
            return installationAccessQueryService.getConsoleUrls();
        } else {
            return installationAccessQueryService.getConsoleUrls(referenceId);
        }
    }

    private List<String> getPortalUrls() {
        if (referenceId.equals(UNDEFINED_REFERENCE_ID)) {
            return installationAccessQueryService.getPortalUrls();
        } else {
            List<String> urls = new ArrayList<>();
            List<String> portalUrls = installationAccessQueryService.getPortalUrls(referenceId);
            if (portalUrls != null) {
                urls.addAll(portalUrls);
                if (GraviteeContext.getCurrentEnvironment().equals(referenceId) && GraviteeContext.getCurrentOrganization() != null) {
                    List<String> consoleUrls = installationAccessQueryService.getConsoleUrls(GraviteeContext.getCurrentOrganization());
                    if (consoleUrls != null) {
                        urls.addAll(consoleUrls);
                    }
                }
            }
            return urls;
        }
    }
}
