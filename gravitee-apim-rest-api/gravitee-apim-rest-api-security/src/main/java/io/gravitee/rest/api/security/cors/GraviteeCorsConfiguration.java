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

import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.access_point.model.AccessPointEvent;
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

public class GraviteeCorsConfiguration extends CorsConfiguration {

    public static final String UNDEFINED_REFERENCE_ID = "undefined";
    private final Environment environment;
    private final ParameterService parameterService;
    private final InstallationAccessQueryService installationAccessQueryService;
    private final String referenceId;
    private final ParameterReferenceType parameterReferenceType;
    private final ParameterKeyEventListener parameterKeyEventListener;
    private final AccessPointEventListener accessPointEventListener;
    private final EventManager eventManager;

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
        if (!this.referenceId.equals(UNDEFINED_REFERENCE_ID)) {
            parameterKeyEventListener = new ParameterKeyEventListener(this);
            eventManager.subscribeForEvents(parameterKeyEventListener, Key.class);
        } else {
            parameterKeyEventListener = null;
        }
        accessPointEventListener = new AccessPointEventListener(this);
        eventManager.subscribeForEvents(accessPointEventListener, AccessPointEvent.class);
        this.eventManager = eventManager;
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

    private static List<String> semicolonStringToList(final String listStr) {
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

    public void release() {
        if (parameterKeyEventListener != null) {
            eventManager.unsubscribeForEvents(parameterKeyEventListener, Key.class);
        }
        if (accessPointEventListener != null) {
            eventManager.unsubscribeForEvents(accessPointEventListener, AccessPointEvent.class);
        }
    }

    record ParameterKeyEventListener(GraviteeCorsConfiguration graviteeCorsConfiguration) implements EventListener<Key, Parameter> {
        @Override
        public void onEvent(final Event<Key, Parameter> event) {
            if (graviteeCorsConfiguration.referenceId.equals(event.content().getReferenceId())) {
                if (event.type() == graviteeCorsConfiguration.allowOriginKey()) {
                    graviteeCorsConfiguration.setAllowedOriginPatterns(
                        graviteeCorsConfiguration.buildAllowedOriginPatterns(semicolonStringToList(event.content().getValue()))
                    );
                } else if (event.type() == graviteeCorsConfiguration.allowHeadersKey()) {
                    graviteeCorsConfiguration.setAllowedHeaders(semicolonStringToList(event.content().getValue()));
                } else if (event.type() == graviteeCorsConfiguration.allowMethodsKey()) {
                    graviteeCorsConfiguration.setAllowedMethods(semicolonStringToList(event.content().getValue()));
                } else if (event.type() == graviteeCorsConfiguration.exposedHeadersKey()) {
                    graviteeCorsConfiguration.setExposedHeaders(semicolonStringToList(event.content().getValue()));
                } else if (event.type() == graviteeCorsConfiguration.maxAgeKey()) {
                    graviteeCorsConfiguration.setMaxAge(Long.parseLong(event.content().getValue()));
                }
            }
        }
    }

    record AccessPointEventListener(GraviteeCorsConfiguration graviteeCorsConfiguration) implements
        EventListener<AccessPointEvent, AccessPoint> {
        @Override
        public void onEvent(final Event<AccessPointEvent, AccessPoint> event) {
            if (isReferenced(event) && (isConsoleTarget(event) || isPortalTarget(event))) {
                List<String> newAllowedOriginPatterns = new ArrayList<>();
                List<String> allowedOriginPatterns = graviteeCorsConfiguration.getAllowedOriginPatterns();
                if (allowedOriginPatterns != null) {
                    newAllowedOriginPatterns.addAll(allowedOriginPatterns);
                }
                if (event.type() == AccessPointEvent.CREATED) {
                    newAllowedOriginPatterns.add(event.content().buildInstallationAccess());
                } else if (event.type() == AccessPointEvent.DELETED) {
                    newAllowedOriginPatterns.remove(event.content().buildInstallationAccess());
                }
                graviteeCorsConfiguration.setAllowedOriginPatterns(newAllowedOriginPatterns);
            }
        }

        private boolean isReferenced(final Event<AccessPointEvent, AccessPoint> event) {
            return (
                (graviteeCorsConfiguration.referenceId.equals(UNDEFINED_REFERENCE_ID) ||
                    graviteeCorsConfiguration.referenceId.equals(event.content().getReferenceId())) &&
                graviteeCorsConfiguration.parameterReferenceType.name().equals(event.content().getReferenceType().name())
            );
        }

        private boolean isPortalTarget(final Event<AccessPointEvent, AccessPoint> event) {
            return (
                graviteeCorsConfiguration.parameterReferenceType == ParameterReferenceType.ENVIRONMENT &&
                event.content().getTarget() == AccessPoint.Target.PORTAL
            );
        }

        private boolean isConsoleTarget(final Event<AccessPointEvent, AccessPoint> event) {
            return (
                graviteeCorsConfiguration.parameterReferenceType == ParameterReferenceType.ORGANIZATION &&
                event.content().getTarget() == AccessPoint.Target.CONSOLE
            );
        }
    }
}
