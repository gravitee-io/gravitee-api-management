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

import io.gravitee.apim.core.api.domain_service.VerifyApiHostsDomainService;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.api.exception.InvalidPathsException;
import io.gravitee.apim.infra.adapter.PathAdapter;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.ConnectorFeature;
import io.gravitee.definition.model.v4.ConnectorMode;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Dlq;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.entrypoint.Qos;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.gravitee.rest.api.service.v4.exception.*;
import io.gravitee.rest.api.service.v4.validation.CorsValidationService;
import io.gravitee.rest.api.service.v4.validation.ListenerValidationService;
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

    private final VerifyApiPathDomainService verifyApiPathDomainService;
    private final EntrypointConnectorPluginService entrypointService;
    private final EndpointConnectorPluginService endpointService;
    private final CorsValidationService corsValidationService;
    private final VerifyApiHostsDomainService verifyApiHostsDomainService;

    public ListenerValidationServiceImpl(
        final VerifyApiPathDomainService verifyApiPathDomainService,
        final EntrypointConnectorPluginService entrypointService,
        EndpointConnectorPluginService endpointService,
        final CorsValidationService corsValidationService,
        final VerifyApiHostsDomainService verifyApiHostsDomainService
    ) {
        this.verifyApiPathDomainService = verifyApiPathDomainService;
        this.entrypointService = entrypointService;
        this.endpointService = endpointService;
        this.corsValidationService = corsValidationService;
        this.verifyApiHostsDomainService = verifyApiHostsDomainService;
    }

    @Override
    public List<Listener> validateAndSanitize(
        final ExecutionContext executionContext,
        final String apiId,
        final List<Listener> listeners,
        final List<EndpointGroup> endpointGroups
    ) {
        if (listeners != null && !listeners.isEmpty()) {
            checkDuplicatedListeners(listeners);
            listeners.forEach(listener -> {
                switch (listener.getType()) {
                    case HTTP:
                        validateAndSanitizeHttpListener(executionContext, apiId, (HttpListener) listener, endpointGroups);
                        break;
                    case SUBSCRIPTION:
                        validateAndSanitizeSubscriptionListener((SubscriptionListener) listener, endpointGroups);
                        break;
                    case TCP:
                        validateAndSanitizeTcpListener(executionContext, apiId, (TcpListener) listener, endpointGroups);
                        break;
                    default:
                        break;
                }
            });
        }
        return listeners;
    }

    private void checkDuplicatedListeners(final List<Listener> listeners) {
        Set<ListenerType> seenListeners = new HashSet<>();
        Set<String> duplicatedListeners = listeners
            .stream()
            .filter(e -> !seenListeners.add(e.getType()))
            .map(selector -> selector.getType().getLabel())
            .collect(Collectors.toSet());
        if (!duplicatedListeners.isEmpty()) {
            throw new ListenersDuplicatedException(duplicatedListeners);
        }
    }

    private void validateAndSanitizeHttpListener(
        final ExecutionContext executionContext,
        final String apiId,
        final HttpListener httpListener,
        final List<EndpointGroup> endpointGroups
    ) {
        var validationResult = verifyApiPathDomainService.validateAndSanitize(
            new VerifyApiPathDomainService.Input(
                executionContext.getEnvironmentId(),
                apiId,
                PathAdapter.INSTANCE.fromV4HttpListenerPathList(httpListener.getPaths())
            )
        );

        validationResult
            .severe()
            .ifPresent(errors -> {
                throw new InvalidPathsException(errors.iterator().next().getMessage());
            });

        var sanitizedPaths = validationResult.map(VerifyApiPathDomainService.Input::paths).value().stream().flatMap(List::stream).toList();

        httpListener.setPaths(PathAdapter.INSTANCE.toV4HttpListenerPathList(sanitizedPaths));

        validatePathMappings(httpListener.getPathMappings());
        // Validate and clean entrypoints
        validateEntrypoints(httpListener.getType(), httpListener.getEntrypoints(), endpointGroups);
        // Validate and clean cors configuration
        httpListener.setCors(corsValidationService.validateAndSanitize(httpListener.getCors()));
    }

    private void validateAndSanitizeTcpListener(
        final ExecutionContext executionContext,
        final String apiId,
        final TcpListener listener,
        final List<EndpointGroup> endpointGroups
    ) {
        verifyApiHostsDomainService.checkApiHosts(executionContext.getEnvironmentId(), apiId, listener.getHosts());

        validateEntrypoints(listener.getType(), listener.getEntrypoints(), endpointGroups);
    }

    private void validateAndSanitizeSubscriptionListener(
        final SubscriptionListener subscriptionListener,
        final List<EndpointGroup> endpointGroups
    ) {
        // Validate and clean entrypoints
        validateEntrypoints(subscriptionListener.getType(), subscriptionListener.getEntrypoints(), endpointGroups);
    }

    private void validateEntrypoints(
        final ListenerType type,
        final List<Entrypoint> entrypoints,
        final List<EndpointGroup> endpointGroups
    ) {
        if (entrypoints == null || entrypoints.isEmpty()) {
            throw new ListenerEntrypointMissingException(type);
        }
        checkDuplicatedEntrypoints(type, entrypoints);
        entrypoints.forEach(entrypoint -> {
            if (entrypoint.getType() == null) {
                throw new ListenerEntrypointMissingTypeException();
            }
            final ConnectorPluginEntity connectorPlugin = entrypointService.findById(entrypoint.getType());

            checkEntrypointListenerType(type, connectorPlugin);
            checkEntrypointQos(entrypoint, endpointGroups, connectorPlugin);
            checkEntrypointDlq(entrypoint, endpointGroups, connectorPlugin);
            checkEntrypointConfiguration(entrypoint);
        });
    }

    private void checkEntrypointQos(
        final Entrypoint entrypoint,
        final List<EndpointGroup> endpointGroups,
        final ConnectorPluginEntity connectorPlugin
    ) {
        if (connectorPlugin.getSupportedApiType() == ApiType.MESSAGE) {
            if (entrypoint.getQos() == null) {
                throw new ListenerEntrypointInvalidQosException(entrypoint.getType());
            }
            if (
                connectorPlugin.getSupportedApiType() == ApiType.MESSAGE &&
                (connectorPlugin.getSupportedQos() == null || !connectorPlugin.getSupportedQos().contains(entrypoint.getQos()))
            ) {
                throw new ListenerEntrypointUnsupportedQosException(entrypoint.getType(), entrypoint.getQos().getLabel());
            }

            if (!checkEntrypointQosEndpoint(endpointGroups, entrypoint.getQos())) {
                throw new ListenerEntrypointInvalidQosException(entrypoint.getType());
            }
        }
    }

    private boolean checkEntrypointQosEndpoint(List<EndpointGroup> endpointGroups, Qos qos) {
        return endpointGroups
            .stream()
            .allMatch(endpointGroup -> {
                final ConnectorPluginEntity endpointConnectorPlugin = endpointService.findById(endpointGroup.getType());
                return (
                    endpointConnectorPlugin != null &&
                    endpointConnectorPlugin.getSupportedApiType() == ApiType.MESSAGE &&
                    endpointConnectorPlugin.getSupportedQos() != null &&
                    endpointConnectorPlugin.getSupportedQos().contains(qos)
                );
            });
    }

    private void checkEntrypointDlq(
        final Entrypoint entrypoint,
        final List<EndpointGroup> endpointGroups,
        final ConnectorPluginEntity connectorPlugin
    ) {
        final Dlq dlq = entrypoint.getDlq();
        if (dlq != null) {
            if (!connectorPlugin.getAvailableFeatures().contains(ConnectorFeature.DLQ)) {
                throw new ListenerEntrypointUnsupportedDlqException(entrypoint.getType());
            }

            if (dlq.getEndpoint() == null || !checkEntrypointDlqEndpoint(endpointGroups, dlq)) {
                throw new ListenerEntrypointInvalidDlqException(entrypoint.getType(), dlq.getEndpoint());
            }
        }
    }

    private boolean checkEntrypointDlqEndpoint(List<EndpointGroup> endpointGroups, Dlq dlq) {
        return endpointGroups
            .stream()
            .anyMatch(endpointGroup -> {
                final ConnectorPluginEntity endpointConnectorPlugin = endpointService.findById(endpointGroup.getType());
                return (
                    endpointConnectorPlugin != null &&
                    endpointConnectorPlugin.getSupportedModes().contains(ConnectorMode.PUBLISH) &&
                    (
                        endpointGroup.getName().equals(dlq.getEndpoint()) ||
                        endpointGroup.getEndpoints().stream().anyMatch(endpoint -> endpoint.getName().equals(dlq.getEndpoint()))
                    )
                );
            });
    }

    private void checkEntrypointConfiguration(final Entrypoint entrypoint) {
        String entrypointConfiguration = null;
        if (entrypoint.getConfiguration() != null) {
            entrypointConfiguration = entrypoint.getConfiguration();
        }
        entrypoint.setConfiguration(entrypointService.validateConnectorConfiguration(entrypoint.getType(), entrypointConfiguration));
    }

    private void checkEntrypointListenerType(final ListenerType type, final ConnectorPluginEntity connectorPlugin) {
        if (connectorPlugin.getSupportedListenerType() != null && type != connectorPlugin.getSupportedListenerType()) {
            throw new ListenerEntrypointUnsupportedListenerTypeException(connectorPlugin.getId(), type.getLabel());
        }
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
