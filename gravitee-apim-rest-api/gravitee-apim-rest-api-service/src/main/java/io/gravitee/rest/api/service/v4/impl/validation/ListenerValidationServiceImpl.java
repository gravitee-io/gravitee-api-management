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
package io.gravitee.rest.api.service.v4.impl.validation;

import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.ListenerHttp;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.EntrypointService;
import io.gravitee.rest.api.service.v4.exception.ListenerHttpEntrypointMissingException;
import io.gravitee.rest.api.service.v4.exception.ListenerHttpEntrypointMissingTypeException;
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
    private final EntrypointService entrypointService;
    private final CorsValidationService corsValidationService;
    private final LoggingValidationService loggingValidationService;

    public ListenerValidationServiceImpl(
        final PathValidationService pathValidationService,
        final EntrypointService entrypointService,
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
            listeners.forEach(
                listener -> {
                    switch (listener.getType()) {
                        case HTTP:
                            // TODO this need to be improved when entrypoint connector are implemented in order to check the configuration schema
                            validateAndSanitizeHttpListener(executionContext, apiId, (ListenerHttp) listener);
                        case TCP:
                        case SUBSCRIPTION:
                        default:
                            break;
                    }
                }
            );
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
        final ListenerHttp listenerHttp
    ) {
        listenerHttp.setPaths(pathValidationService.validateAndSanitizePaths(executionContext, apiId, listenerHttp.getPaths()));
        validatePathMappings(listenerHttp.getPathMappings());
        // Validate and clean entrypoints
        validateEntrypointsConfiguration(listenerHttp.getEntrypoints());
        // Validate and clean cors configuration
        listenerHttp.setCors(corsValidationService.validateAndSanitize(listenerHttp.getCors()));
        // Validate and clean logging configuration
        listenerHttp.setLogging(loggingValidationService.validateAndSanitize(executionContext, listenerHttp.getLogging()));
    }

    private void validateEntrypointsConfiguration(final List<Entrypoint> entrypoints) {
        if (entrypoints == null || entrypoints.isEmpty()) {
            throw new ListenerHttpEntrypointMissingException();
        }
        entrypoints.forEach(
            entrypoint -> {
                if (entrypoint.getType() == null) {
                    throw new ListenerHttpEntrypointMissingTypeException();
                }
                String entrypointConfiguration = null;
                if (entrypoint.getConfiguration() != null) {
                    entrypointConfiguration = entrypoint.getConfiguration().toString();
                }
                entrypoint.setConfiguration(
                    entrypointService.validateEntrypointConfiguration(entrypoint.getType(), entrypointConfiguration)
                );
            }
        );
    }

    private void validatePathMappings(final Set<String> pathMappings) {
        // validate regex on pathMappings
        if (pathMappings != null) {
            pathMappings.forEach(
                pathMapping -> {
                    try {
                        Pattern.compile(pathMapping);
                    } catch (java.util.regex.PatternSyntaxException pse) {
                        String errorMsg = String.format("An error occurs while trying to parse the path mapping '%s'", pathMapping);
                        log.error(errorMsg, pse);
                        throw new TechnicalManagementException(errorMsg, pse);
                    }
                }
            );
        }
    }
}
