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
package io.gravitee.rest.api.service.v4.impl.validation;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.entrypoint.Qos;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.gravitee.rest.api.service.v4.exception.ListenerEntrypointDuplicatedException;
import io.gravitee.rest.api.service.v4.exception.ListenerEntrypointInvalidQosException;
import io.gravitee.rest.api.service.v4.exception.ListenerEntrypointMissingException;
import io.gravitee.rest.api.service.v4.exception.ListenerEntrypointMissingTypeException;
import io.gravitee.rest.api.service.v4.exception.ListenerEntrypointUnsupportedQosException;
import io.gravitee.rest.api.service.v4.exception.ListenersDuplicatedException;
import io.gravitee.rest.api.service.v4.validation.CorsValidationService;
import io.gravitee.rest.api.service.v4.validation.ListenerValidationService;
import io.gravitee.rest.api.service.v4.validation.LoggingValidationService;
import io.gravitee.rest.api.service.v4.validation.PathValidationService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class ListenerValidationServiceImpl extends TransactionalService implements ListenerValidationService {

    private final PathValidationService pathValidationService;
    private final EntrypointConnectorPluginService entrypointService;
    private final CorsValidationService corsValidationService;
    private final LoggingValidationService loggingValidationService;

    public ListenerValidationServiceImpl(
        final PathValidationService pathValidationService,
        final EntrypointConnectorPluginService entrypointService,
        final CorsValidationService corsValidationService,
        final LoggingValidationService loggingValidationService
    ) {
        this.pathValidationService = pathValidationService;
        this.entrypointService = entrypointService;
        this.corsValidationService = corsValidationService;
        this.loggingValidationService = loggingValidationService;
    }

    @Override
    public List<Listener> validateAndSanitize(final ExecutionContext executionContext, final String apiId, final List<Listener> listeners) {
        if (listeners != null && !listeners.isEmpty()) {
            checkDuplicatedListeners(listeners);
            listeners.forEach(listener -> {
                switch (listener.getType()) {
                    case HTTP:
                        validateAndSanitizeHttpListener(executionContext, apiId, (HttpListener) listener);
                        break;
                    case SUBSCRIPTION:
                        validateAndSanitizeSubscriptionListener((SubscriptionListener) listener);
                        break;
                    case TCP:
                    default:
                        break;
                }
            });
        }
        return listeners;
    }

    private void checkDuplicatedListeners(final List<Listener> listeners) {
        Set<Listener> seenListeners = new HashSet<>();
        Set<String> duplicatedListeners = listeners
            .stream()
            .filter(e -> !seenListeners.add(e))
            .map(selector -> selector.getType().getLabel())
            .collect(Collectors.toSet());
        if (!duplicatedListeners.isEmpty()) {
            throw new ListenersDuplicatedException(duplicatedListeners);
        }
    }

    private void validateAndSanitizeHttpListener(
        final ExecutionContext executionContext,
        final String apiId,
        final HttpListener httpListener
    ) {
        httpListener.setPaths(pathValidationService.validateAndSanitizePaths(executionContext, apiId, httpListener.getPaths()));
        validatePathMappings(httpListener.getPathMappings());
        // Validate and clean entrypoints
        validateEntrypoints(httpListener.getType(), httpListener.getEntrypoints());
        // Validate and clean cors configuration
        httpListener.setCors(corsValidationService.validateAndSanitize(httpListener.getCors()));
        // Validate and clean logging configuration
        httpListener.setLogging(loggingValidationService.validateAndSanitize(executionContext, httpListener.getLogging()));
    }

    private void validateAndSanitizeSubscriptionListener(final SubscriptionListener subscriptionListener) {
        // Validate and clean entrypoints
        validateEntrypoints(subscriptionListener.getType(), subscriptionListener.getEntrypoints());
    }

    private void validateEntrypoints(final ListenerType type, final List<Entrypoint> entrypoints) {
        if (entrypoints == null || entrypoints.isEmpty()) {
            throw new ListenerEntrypointMissingException(type);
        }
        checkDuplicatedEntrypoints(type, entrypoints);
        entrypoints.forEach(entrypoint -> {
            if (entrypoint.getType() == null) {
                throw new ListenerEntrypointMissingTypeException();
            }
            checkEntrypointQos(entrypoint);
            checkEntrypointConfiguration(entrypoint);
        });
    }

    private void checkEntrypointQos(final Entrypoint entrypoint) {
        ConnectorPluginEntity connectorPlugin = entrypointService.findById(entrypoint.getType());
        if (connectorPlugin.getSupportedApiType() == ApiType.ASYNC) {
            if (entrypoint.getQos() == null) {
                throw new ListenerEntrypointInvalidQosException(entrypoint.getType());
            }
            if (
                connectorPlugin.getSupportedApiType() == ApiType.ASYNC &&
                (connectorPlugin.getSupportedQos() == null || !connectorPlugin.getSupportedQos().contains(entrypoint.getQos()))
            ) {
                throw new ListenerEntrypointUnsupportedQosException(entrypoint.getType(), entrypoint.getQos().getLabel());
            }
        }
    }

    private void checkEntrypointConfiguration(final Entrypoint entrypoint) {
        String entrypointConfiguration = null;
        if (entrypoint.getConfiguration() != null) {
            entrypointConfiguration = entrypoint.getConfiguration();
        }
        entrypoint.setConfiguration(entrypointService.validateConnectorConfiguration(entrypoint.getType(), entrypointConfiguration));
    }

    private void checkDuplicatedEntrypoints(final ListenerType type, final List<Entrypoint> entrypoints) {
        if (entrypoints != null) {
            Set<Entrypoint> seenEntrypoints = new HashSet<>();
            Set<String> duplicatedEntrypoints = entrypoints
                .stream()
                .filter(e -> !seenEntrypoints.add(e))
                .map(Entrypoint::getType)
                .collect(Collectors.toSet());
            if (!duplicatedEntrypoints.isEmpty()) {
                throw new ListenerEntrypointDuplicatedException(type, duplicatedEntrypoints);
            }
        }
    }

    private void validatePathMappings(final Set<String> pathMappings) {
        // validate regex on pathMappings
        if (pathMappings != null) {
            pathMappings.forEach(pathMapping -> {
                try {
                    Pattern.compile(pathMapping);
                } catch (java.util.regex.PatternSyntaxException pse) {
                    String errorMsg = String.format("An error occurs while trying to parse the path mapping '%s'", pathMapping);
                    log.error(errorMsg, pse);
                    throw new TechnicalManagementException(errorMsg, pse);
                }
            });
        }
    }
}
