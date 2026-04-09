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

import static io.gravitee.apim.core.utils.CollectionUtils.stream;
import static io.gravitee.rest.api.service.v4.exception.EndpointGroupLlmProxyInvalidException.Validation.ALIASES_MISMATCH;
import static java.util.function.Predicate.not;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.AbstractEndpoint;
import io.gravitee.definition.model.v4.endpointgroup.AbstractEndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.exceptions.EndpointConfigurationValidationException;
import io.gravitee.rest.api.service.exceptions.EndpointGroupNameAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.EndpointMissingException;
import io.gravitee.rest.api.service.exceptions.EndpointNameAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.EndpointNameInvalidException;
import io.gravitee.rest.api.service.exceptions.HealthcheckInheritanceException;
import io.gravitee.rest.api.service.exceptions.HealthcheckInvalidException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.ApiServicePluginService;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import io.gravitee.rest.api.service.v4.exception.EndpointGroupLlmProxyInvalidException;
import io.gravitee.rest.api.service.v4.exception.EndpointGroupTypeInvalidException;
import io.gravitee.rest.api.service.v4.exception.EndpointGroupTypeMismatchInvalidException;
import io.gravitee.rest.api.service.v4.exception.EndpointTypeInvalidException;
import io.gravitee.rest.api.service.v4.validation.EndpointGroupsValidationService;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class EndpointGroupsValidationServiceImpl extends TransactionalService implements EndpointGroupsValidationService {

    private static final String LLM_PROXY_TYPE = "llm-proxy";

    private final EndpointConnectorPluginService endpointService;
    private final ApiServicePluginService apiServicePluginService;
    private final ObjectMapper objectMapper;

    public EndpointGroupsValidationServiceImpl(
        final EndpointConnectorPluginService endpointService,
        final ApiServicePluginService apiServicePluginService,
        final ObjectMapper objectMapper
    ) {
        this.endpointService = endpointService;
        this.apiServicePluginService = apiServicePluginService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<EndpointGroup> validateAndSanitizeHttpV4(ApiType apiType, List<EndpointGroup> endpointGroups) {
        return validateAndSanitize(apiType, endpointGroups);
    }

    @Override
    public List<NativeEndpointGroup> validateAndSanitizeNativeV4(List<NativeEndpointGroup> endpointGroups) {
        return validateAndSanitize(ApiType.NATIVE, endpointGroups);
    }

    public <G extends AbstractEndpointGroup<? extends AbstractEndpoint>> List<G> validateAndSanitize(
        ApiType apiType,
        List<G> endpointGroups
    ) {
        if (endpointGroups == null || endpointGroups.isEmpty()) {
            throw new EndpointMissingException();
        }

        final Set<String> names = new HashSet<>();

        endpointGroups.forEach(endpointGroup -> {
            validateUniqueEndpointGroupName(endpointGroup.getName(), names);

            final ConnectorPluginEntity endpointConnector = endpointService.findById(endpointGroup.getType());
            validateEndpointGroupType(apiType, endpointGroup.getType(), endpointConnector);
            validateEndpointsExistence(endpointGroup);
            validateLlmProxyAliasesConsistency(endpointGroup);
            validateServices(apiType, endpointGroup);

            if (endpointGroup.getSharedConfiguration() != null) {
                endpointGroup.setSharedConfiguration(
                    endpointService.validateSharedConfiguration(endpointConnector, endpointGroup.getSharedConfiguration())
                );
            }
            if (endpointGroup.getEndpoints() != null && !endpointGroups.isEmpty()) {
                endpointGroup
                    .getEndpoints()
                    .forEach(endpoint -> {
                        validateUniqueEndpointName(endpoint.getName(), names);
                        validateEndpointWeight(endpoint);
                        validateEndpointType(endpoint.getType());
                        validateEndpointMatchType(endpointGroup, endpoint);
                        validateEndpointConfiguration(endpointConnector, endpoint);
                        validateSharedConfigurationInheritance(endpointGroup, endpoint);
                        validateSharedConfigurationOverride(endpointConnector, endpoint);
                        validateServices(apiType, endpointGroup, endpoint);
                    });
            }
        });

        return endpointGroups;
    }

    private void validateEndpointWeight(final AbstractEndpoint endpoint) {
        if (endpoint.getWeight() <= 0) {
            endpoint.setWeight(AbstractEndpoint.DEFAULT_WEIGHT);
        }
    }

    private void validateEndpointConfiguration(ConnectorPluginEntity endpointConnector, AbstractEndpoint endpoint) {
        endpoint.setConfiguration(endpointService.validateConnectorConfiguration(endpointConnector, endpoint.getConfiguration()));
    }

    private void validateSharedConfigurationOverride(ConnectorPluginEntity endpointConnector, AbstractEndpoint endpoint) {
        if (!endpoint.isInheritConfiguration()) {
            if (endpoint.getSharedConfigurationOverride() == null) {
                // If no endpoint group provided, validate with an empty object to verify required fields
                endpointService.validateSharedConfiguration(endpointConnector, "{}");
            } else {
                endpoint.setSharedConfigurationOverride(
                    endpointService.validateSharedConfiguration(endpointConnector, endpoint.getSharedConfigurationOverride())
                );
            }
        }
    }

    private void validateSharedConfigurationInheritance(AbstractEndpointGroup<?> endpointGroup, AbstractEndpoint endpoint) {
        if (endpoint.isInheritConfiguration() && endpointGroup.getSharedConfiguration() == null) {
            // If we try to inherit shared configuration that is null
            // Shared configuration has already been validated so no need to do it again
            throw new EndpointConfigurationValidationException(
                "Impossible to inherit from a null shared configuration for endpoint: " + endpoint.getName()
            );
        }
    }

    private void validateEndpointsExistence(AbstractEndpointGroup<?> endpointGroup) {
        if (endpointGroup instanceof EndpointGroup asHttpEndpointGroup) {
            validateHttpEndpointsExistence(asHttpEndpointGroup);
        } else if (endpointGroup instanceof NativeEndpointGroup asNativeEndpointGroup) {
            validateNativeEndpointsExistence(asNativeEndpointGroup);
        }
    }

    private void validateHttpEndpointsExistence(EndpointGroup endpointGroup) {
        //Is service discovery enabled ?
        Service endpointDiscoveryService = endpointGroup.getServices() == null ? null : endpointGroup.getServices().getDiscovery();
        if (
            (endpointDiscoveryService == null || !endpointDiscoveryService.isEnabled()) &&
            (endpointGroup.getEndpoints() == null || endpointGroup.getEndpoints().isEmpty())
        ) {
            throw new EndpointMissingException();
        }
    }

    private void validateNativeEndpointsExistence(NativeEndpointGroup endpointGroup) {
        if (endpointGroup.getEndpoints() == null || endpointGroup.getEndpoints().isEmpty()) {
            throw new EndpointMissingException();
        }
    }

    private void validateDiscovery(Service discovery) {
        // TODO FCY: Nothing is done today to validate discovery validation. Could be handled by the connector (with a JSON Schema for instance).
    }

    private void validateEndpointType(final String type) {
        if (isBlank(type)) {
            throw new EndpointTypeInvalidException(type);
        }
    }

    private void validateEndpointGroupType(final ApiType apiType, final String type, final ConnectorPluginEntity connectorPluginEntity) {
        if (isBlank(type) || !connectorPluginEntity.getSupportedApiType().equals(apiType)) {
            throw new EndpointGroupTypeInvalidException(type);
        }
    }

    private void validateEndpointMatchType(final AbstractEndpointGroup<?> endpointGroup, final AbstractEndpoint endpoint) {
        if (!endpointGroup.getType().equals(endpoint.getType())) {
            throw new EndpointGroupTypeMismatchInvalidException(endpointGroup.getType());
        }
    }

    private void validateLlmProxyAliasesConsistency(final AbstractEndpointGroup<? extends AbstractEndpoint> endpointGroup) {
        if (!LLM_PROXY_TYPE.equals(endpointGroup.getType())) {
            return;
        }
        var aliasesList = stream(endpointGroup.getEndpoints())
            .flatMap(endpoint -> extractLlmProxyAliases(endpoint.getConfiguration()))
            .toList();

        // all endpoints must have the same aliases
        long count = aliasesList.stream().distinct().limit(2).count();
        if (count > 1) {
            throw new EndpointGroupLlmProxyInvalidException(endpointGroup.getName(), EnumSet.of(ALIASES_MISMATCH));
        }
    }

    private Stream<Set<String>> extractLlmProxyAliases(final String configuration) {
        if (configuration == null) {
            return Stream.empty();
        }
        try {
            JsonNode config = objectMapper.readTree(configuration);

            var aliases = stream(config.at("/models"))
                .flatMap(model -> stream(model.at("/aliases")))
                .map(JsonNode::textValue)
                .filter(not(StringUtils::isBlank))
                .collect(Collectors.toCollection(HashSet::new));
            return Stream.of(aliases);
        } catch (JsonProcessingException e) {
            throw new TechnicalManagementException("Failed to parse llm-proxy endpoint configuration", e);
        }
    }

    private void validateAndSetHealthCheckConfiguration(Service healthCheck) {
        if (isBlank(healthCheck.getType())) {
            log.debug("HealthCheck requires a type");
            throw new HealthcheckInvalidException(healthCheck.getType());
        }

        healthCheck.setConfiguration(
            this.apiServicePluginService.validateApiServiceConfiguration(healthCheck.getType(), healthCheck.getConfiguration())
        );
    }

    private void validateName(final String name) {
        if (name != null && name.contains(":")) {
            throw new EndpointNameInvalidException(name);
        }
    }

    private void validateUniqueEndpointGroupName(final String name, final Set<String> names) {
        validateName(name);

        if (names.contains(name)) {
            throw new EndpointGroupNameAlreadyExistsException(name);
        }

        names.add(name);
    }

    private void validateUniqueEndpointName(final String name, final Set<String> names) {
        validateName(name);

        if (names.contains(name)) {
            throw new EndpointNameAlreadyExistsException(name);
        }

        names.add(name);
    }

    private void validateServices(ApiType apiType, AbstractEndpointGroup<?> endpointGroup) {
        if (!ApiType.NATIVE.equals(apiType)) {
            validateHttpServices(((EndpointGroup) endpointGroup).getServices());
        }
    }

    private void validateServices(ApiType apiType, AbstractEndpointGroup<?> endpointGroup, AbstractEndpoint endpoint) {
        if (!ApiType.NATIVE.equals(apiType)) {
            validateHttpServices(((EndpointGroup) endpointGroup).getServices(), ((Endpoint) endpoint).getServices());
        }
    }

    private void validateHttpServices(EndpointGroupServices services) {
        if (services != null) {
            if (services.getDiscovery() != null) {
                validateDiscovery(services.getDiscovery());
            }
            if (services.getHealthCheck() != null) {
                validateAndSetHealthCheckConfiguration(services.getHealthCheck());
            }
        }
    }

    private void validateHttpServices(EndpointGroupServices groupServices, EndpointServices services) {
        if (services != null) {
            if (services.getHealthCheck() != null) {
                final var serviceHealthCheck = services.getHealthCheck();

                if (serviceHealthCheck.isEnabled()) {
                    validateAndSetHealthCheckConfiguration(serviceHealthCheck);
                }

                final var hcGroupWithoutConfig = (groupServices == null ||
                    groupServices.getHealthCheck() == null ||
                    isBlank(groupServices.getHealthCheck().getConfiguration()));
                if (!serviceHealthCheck.isOverrideConfiguration() && hcGroupWithoutConfig) {
                    log.debug("HealthCheck inherit from a missing configuration");
                    throw new HealthcheckInheritanceException();
                }

                if (serviceHealthCheck.isOverrideConfiguration() && isBlank(serviceHealthCheck.getConfiguration())) {
                    log.debug("HealthCheck requires a configuration when overrideConfiguration is enabled");
                    throw new HealthcheckInheritanceException();
                }

                if (
                    groupServices != null &&
                    groupServices.getHealthCheck() != null &&
                    !serviceHealthCheck.getType().equals(groupServices.getHealthCheck().getType())
                ) {
                    log.debug(
                        "HealthCheck with type [{}] inherit configuration from another HealthCheck type [{}]",
                        serviceHealthCheck.getType(),
                        groupServices.getHealthCheck().getType()
                    );
                    throw new HealthcheckInheritanceException();
                }
            }
        }
    }
}
