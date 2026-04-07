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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.loadbalancer.LoadBalancerType;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpoint;
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
import io.gravitee.rest.api.service.v4.ApiServicePluginService;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import io.gravitee.rest.api.service.v4.exception.*;
import io.gravitee.rest.api.service.v4.validation.EndpointGroupsValidationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
public class EndpointGroupsValidationServiceImplTest {

    public static final String FIXED_HC_CONFIG = "{fixed}";
    public static final String HEALTH_CHECK_TYPE = "http-health-check";
    public static final String NATIVE_ENDPOINT_TYPE = "native-friendly";

    @Mock
    private EndpointConnectorPluginService endpointService;

    @Mock
    private ApiServicePluginService apiServicePluginService;

    private EndpointGroupsValidationService endpointGroupsValidationService;

    @BeforeEach
    public void setUp() throws Exception {
        lenient()
            .when(endpointService.validateConnectorConfiguration(any(String.class), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        lenient()
            .when(endpointService.validateSharedConfiguration(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));

        var httpEndpoint = new ConnectorPluginEntity();
        httpEndpoint.setId("http");
        httpEndpoint.setSupportedApiType(ApiType.PROXY);
        lenient().when(endpointService.findById("http")).thenReturn(httpEndpoint);

        var nativeFriendlyEndpoint = new ConnectorPluginEntity();
        nativeFriendlyEndpoint.setId(NATIVE_ENDPOINT_TYPE);
        nativeFriendlyEndpoint.setSupportedApiType(ApiType.NATIVE);
        lenient().when(endpointService.findById(NATIVE_ENDPOINT_TYPE)).thenReturn(nativeFriendlyEndpoint);

        var llmProxyEndpoint = new ConnectorPluginEntity();
        llmProxyEndpoint.setId("llm-proxy");
        llmProxyEndpoint.setSupportedApiType(ApiType.LLM_PROXY);
        lenient().when(endpointService.findById("llm-proxy")).thenReturn(llmProxyEndpoint);

        endpointGroupsValidationService = new EndpointGroupsValidationServiceImpl(
            endpointService,
            apiServicePluginService,
            new ObjectMapper()
        );
    }

    @Test
    public void shouldReturnValidatedEndpointGroupsWithoutEndpointsAndWithoutDiscoveryService() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType("http");
        endpointGroup.setEndpoints(List.of());

        var exception = catchException(() ->
            endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup))
        );

        assertThat(exception).isInstanceOf(EndpointMissingException.class);
    }

    @Test
    public void shouldReturnValidatedEndpointGroupsWithoutEndpointsAndWithDisabledDiscoveryService() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType("http");
        endpointGroup.setEndpoints(List.of());
        Service discovery = new Service();
        discovery.setEnabled(false);
        EndpointGroupServices services = new EndpointGroupServices();
        services.setDiscovery(discovery);
        endpointGroup.setServices(services);
        var exception = catchException(() ->
            endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup))
        );
        assertThat(exception).isInstanceOf(EndpointMissingException.class);
    }

    @Test
    public void shouldReturnValidatedEndpointGroupsWithoutEndpointsAndWithEnabledDiscoveryService() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType("http");
        endpointGroup.setEndpoints(List.of());
        Service discovery = new Service();
        discovery.setEnabled(true);
        EndpointGroupServices services = new EndpointGroupServices();
        services.setDiscovery(discovery);
        endpointGroup.setServices(services);
        List<EndpointGroup> endpointGroups = endpointGroupsValidationService.validateAndSanitizeHttpV4(
            ApiType.PROXY,
            List.of(endpointGroup)
        );
        assertThat(endpointGroups).hasSize(1);
        EndpointGroup validatedEndpointGroup = endpointGroups.getFirst();
        assertThat(validatedEndpointGroup.getName()).isEqualTo(endpointGroup.getName());
        assertThat(validatedEndpointGroup.getType()).isEqualTo(endpointGroup.getType());
        assertThat(validatedEndpointGroup.getEndpoints()).isEmpty();
        assertThat(validatedEndpointGroup.getServices()).isEqualTo(services);
        assertThat(validatedEndpointGroup.getSharedConfiguration()).isNull();
        assertThat(validatedEndpointGroup.getLoadBalancer()).isNotNull();
        assertThat(validatedEndpointGroup.getLoadBalancer().getType()).isEqualTo(LoadBalancerType.ROUND_ROBIN);
    }

    @Test
    public void shouldReturnValidatedEndpointGroupsWithEndpoints() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType("http");
        Endpoint endpoint = new Endpoint();
        endpoint.setName("endpoint");
        endpoint.setType("http");
        endpoint.setSharedConfigurationOverride("minimalSharedConfiguration");
        endpointGroup.setEndpoints(List.of(endpoint));
        List<EndpointGroup> endpointGroups = endpointGroupsValidationService.validateAndSanitizeHttpV4(
            ApiType.PROXY,
            List.of(endpointGroup)
        );
        assertThat(endpointGroups).hasSize(1);
        EndpointGroup validatedEndpointGroup = endpointGroups.getFirst();
        assertThat(validatedEndpointGroup.getName()).isEqualTo(endpointGroup.getName());
        assertThat(validatedEndpointGroup.getType()).isEqualTo(endpointGroup.getType());
        assertThat(validatedEndpointGroup.getEndpoints()).isNotEmpty();
        List<Endpoint> endpoints = validatedEndpointGroup.getEndpoints();
        Endpoint validatedEndpoint = endpoints.getFirst();
        assertThat(validatedEndpoint.getName()).isEqualTo("endpoint");
        assertThat(validatedEndpoint.getType()).isEqualTo("http");
        assertThat(validatedEndpoint.getWeight()).isEqualTo(1);
        assertThat(validatedEndpointGroup.getServices()).isNotNull();
        assertThat(validatedEndpointGroup.getSharedConfiguration()).isNull();
        assertThat(validatedEndpointGroup.getLoadBalancer()).isNotNull();
        assertThat(validatedEndpointGroup.getLoadBalancer().getType()).isEqualTo(LoadBalancerType.ROUND_ROBIN);
    }

    @Test
    public void shouldReturnValidatedEndpointGroupsWithGroupHealthChecks() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType("http");
        endpointGroup.setSharedConfiguration("sharedConfiguration");
        Endpoint endpoint = new Endpoint();
        endpoint.setName("endpoint");
        endpoint.setType("http");
        endpoint.setInheritConfiguration(true);
        endpointGroup.setEndpoints(List.of(endpoint));

        Service healthCheck = new Service();
        healthCheck.setType(HEALTH_CHECK_TYPE);
        healthCheck.setEnabled(true);
        healthCheck.setConfiguration("{}");
        endpointGroup.getServices().setHealthCheck(healthCheck);

        when(apiServicePluginService.validateApiServiceConfiguration(eq(HEALTH_CHECK_TYPE), any())).thenReturn(FIXED_HC_CONFIG);

        List<EndpointGroup> endpointGroups = endpointGroupsValidationService.validateAndSanitizeHttpV4(
            ApiType.PROXY,
            List.of(endpointGroup)
        );
        assertThat(endpointGroups.size()).isEqualTo(1);
        EndpointGroup validatedEndpointGroup = endpointGroups.getFirst();
        assertThat(validatedEndpointGroup.getName()).isEqualTo(endpointGroup.getName());
        assertThat(validatedEndpointGroup.getType()).isEqualTo(endpointGroup.getType());
        assertThat(validatedEndpointGroup.getEndpoints()).isNotEmpty();
        List<Endpoint> endpoints = validatedEndpointGroup.getEndpoints();
        Endpoint validatedEndpoint = endpoints.getFirst();
        assertThat(validatedEndpoint.getName()).isEqualTo("endpoint");
        assertThat(validatedEndpoint.getType()).isEqualTo("http");
        assertThat(validatedEndpoint.getWeight()).isEqualTo(1);
        assertThat(validatedEndpointGroup.getServices())
            .isNotNull()
            .matches(svc -> svc.getHealthCheck().getConfiguration().equals(FIXED_HC_CONFIG));
        assertThat(validatedEndpointGroup.getSharedConfiguration()).isNotNull();
        assertThat(validatedEndpointGroup.getLoadBalancer()).isNotNull();
        assertThat(validatedEndpointGroup.getLoadBalancer().getType()).isEqualTo(LoadBalancerType.ROUND_ROBIN);
    }

    @Test
    public void shouldRejectEndpointGroupsWithInvalidHealthChecks_MissingType() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType("http");
        Endpoint endpoint = new Endpoint();
        endpoint.setName("endpoint");
        endpoint.setType("http");
        endpointGroup.setEndpoints(List.of(endpoint));

        Service healthCheck = new Service();
        healthCheck.setEnabled(true);
        healthCheck.setConfiguration("{}");
        endpointGroup.getServices().setHealthCheck(healthCheck);

        var exception = catchException(() ->
            endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup))
        );
        assertThat(exception).isInstanceOf(HealthcheckInvalidException.class);
    }

    @Test
    public void shouldRejectEndpointGroupsWithInvalidEndpointHealthChecks_MissingType() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType("http");
        Endpoint endpoint = new Endpoint();
        endpoint.setName("endpoint");
        endpoint.setType("http");
        endpointGroup.setEndpoints(List.of(endpoint));

        Service grpHealthCheck = new Service();
        grpHealthCheck.setEnabled(true);
        grpHealthCheck.setType(HEALTH_CHECK_TYPE);
        grpHealthCheck.setConfiguration("{}");
        endpointGroup.getServices().setHealthCheck(grpHealthCheck);

        Service healthCheck = new Service();
        healthCheck.setEnabled(true);
        healthCheck.setConfiguration("{}");
        endpoint.getServices().setHealthCheck(healthCheck);

        when(apiServicePluginService.validateApiServiceConfiguration(eq(HEALTH_CHECK_TYPE), any())).thenReturn(FIXED_HC_CONFIG);
        var exception = catchException(() ->
            endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup))
        );
        assertThat(exception).isInstanceOf(HealthcheckInvalidException.class);
    }

    @Test
    public void shouldRejectEndpointGroupsWithEndpointHealthChecks_InheritError_noGroup() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType("http");
        Endpoint endpoint = new Endpoint();
        endpoint.setName("endpoint");
        endpoint.setType("http");
        endpointGroup.setEndpoints(List.of(endpoint));

        Service healthCheck = new Service();
        healthCheck.setEnabled(true);
        healthCheck.setConfiguration("{}");
        healthCheck.setType(HEALTH_CHECK_TYPE);
        healthCheck.setOverrideConfiguration(false);
        endpoint.getServices().setHealthCheck(healthCheck);

        when(apiServicePluginService.validateApiServiceConfiguration(eq(HEALTH_CHECK_TYPE), any())).thenReturn(FIXED_HC_CONFIG);
        var exception = catchException(() ->
            endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup))
        );
        assertThat(exception).isInstanceOf(HealthcheckInheritanceException.class);
    }

    @Test
    public void shouldRejectEndpointGroupsWithEndpointHealthChecks_InheritError_noConfig() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType("http");
        Endpoint endpoint = new Endpoint();
        endpoint.setName("endpoint");
        endpoint.setType("http");
        endpointGroup.setEndpoints(List.of(endpoint));

        Service healthCheck = new Service();
        healthCheck.setEnabled(true);

        healthCheck.setType(HEALTH_CHECK_TYPE);
        healthCheck.setOverrideConfiguration(true);
        endpoint.getServices().setHealthCheck(healthCheck);

        when(apiServicePluginService.validateApiServiceConfiguration(eq(HEALTH_CHECK_TYPE), any())).thenReturn(null);
        var exception = catchException(() ->
            endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup))
        );
        assertThat(exception).isInstanceOf(HealthcheckInheritanceException.class);
    }

    @Test
    public void shouldRejectEndpointGroupsWithGroupHealthChecksTypeIssue() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType("http");
        Endpoint endpoint = new Endpoint();
        endpoint.setName("endpoint");
        endpoint.setType("http");
        endpointGroup.setEndpoints(List.of(endpoint));

        Service grpHealthCheck = new Service();
        grpHealthCheck.setType("different-type");
        grpHealthCheck.setEnabled(true);
        grpHealthCheck.setConfiguration("{}");
        endpointGroup.getServices().setHealthCheck(grpHealthCheck);

        Service healthCheck = new Service();
        healthCheck.setType(HEALTH_CHECK_TYPE);
        healthCheck.setEnabled(true);
        healthCheck.setConfiguration("{}");
        endpoint.getServices().setHealthCheck(healthCheck);

        when(apiServicePluginService.validateApiServiceConfiguration(eq(grpHealthCheck.getType()), any())).thenReturn(FIXED_HC_CONFIG);
        when(apiServicePluginService.validateApiServiceConfiguration(eq(HEALTH_CHECK_TYPE), any())).thenReturn(FIXED_HC_CONFIG);

        var exception = catchException(() ->
            endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup))
        );
        assertThat(exception).isInstanceOf(HealthcheckInheritanceException.class);
    }

    @Test
    public void shouldThrowValidationExceptionWithWrongEndpointGroupName() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName(":");
        assertThatExceptionOfType(EndpointNameInvalidException.class).isThrownBy(() ->
            endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup))
        );
    }

    @Test
    public void shouldThrowValidationExceptionWithEndpointNameAlreadyExists() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType("http");
        Endpoint endpoint = new Endpoint();
        endpoint.setName("my name");
        endpoint.setType("http");
        endpointGroup.setEndpoints(List.of(endpoint));

        assertThatExceptionOfType(EndpointNameAlreadyExistsException.class).isThrownBy(() ->
            endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup))
        );
    }

    @Test
    public void shouldThrowValidationExceptionWithEndpointNameAlreadyExistsInAnotherGroup() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("group1");
        endpointGroup.setType("http");
        Endpoint endpoint = new Endpoint();
        endpoint.setName("my name");
        endpoint.setType("http");
        endpoint.setSharedConfigurationOverride("minimalSharedConfiguration");
        endpointGroup.setEndpoints(List.of(endpoint));

        EndpointGroup endpointGroup2 = new EndpointGroup();
        endpointGroup2.setName("group2");
        endpointGroup2.setType("http");
        Endpoint endpoint2 = new Endpoint();
        endpoint2.setName("my name");
        endpoint2.setType("http");
        endpoint2.setSharedConfigurationOverride("minimalSharedConfiguration");
        endpointGroup2.setEndpoints(List.of(endpoint2));

        assertThatExceptionOfType(EndpointNameAlreadyExistsException.class).isThrownBy(() ->
            endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup, endpointGroup2))
        );
    }

    @Test
    public void shouldThrowValidationExceptionWithEndpointGroupNameAlreadyExistsInAnotherGroup() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("group1");
        endpointGroup.setType("http");
        endpointGroup.setSharedConfiguration("sharedConfiguration");
        Endpoint endpoint = new Endpoint();
        endpoint.setName("my name");
        endpoint.setType("http");
        endpoint.setInheritConfiguration(true);
        endpointGroup.setEndpoints(List.of(endpoint));

        EndpointGroup endpointGroup2 = new EndpointGroup();
        endpointGroup2.setName("my name");
        endpointGroup2.setType("http");
        endpointGroup2.setSharedConfiguration("sharedConfiguration");
        Endpoint endpoint2 = new Endpoint();
        endpoint2.setName("endpoint2");
        endpoint2.setType("http");
        endpoint2.setInheritConfiguration(true);
        endpointGroup2.setEndpoints(List.of(endpoint2));

        assertThatExceptionOfType(EndpointGroupNameAlreadyExistsException.class).isThrownBy(() ->
            endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup, endpointGroup2))
        );
    }

    @Test
    public void shouldThrowValidationExceptionWithMissingEndpointGroupType() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("name");
        assertThatExceptionOfType(EndpointGroupTypeInvalidException.class).isThrownBy(() ->
            endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup))
        );
    }

    @Test
    public void shouldThrowValidationExceptionWithInvalidTypeForProxyApi() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("name");
        endpointGroup.setType("http");
        assertThatExceptionOfType(EndpointGroupTypeInvalidException.class).isThrownBy(() ->
            endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.MESSAGE, List.of(endpointGroup))
        );
    }

    @Test
    public void shouldThrowValidationExceptionForMessageApiWithProxyEndpointGroup() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("name");
        endpointGroup.setType("http");
        assertThatExceptionOfType(EndpointGroupTypeInvalidException.class).isThrownBy(() ->
            endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.MESSAGE, List.of(endpointGroup))
        );
    }

    @Test
    public void shouldThrowValidationExceptionWithWrongEndpointName() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("name");
        endpointGroup.setType("http");
        Endpoint endpoint = new Endpoint();
        endpoint.setName(":");
        endpoint.setType("http");
        endpointGroup.setEndpoints(List.of(endpoint));
        assertThatExceptionOfType(EndpointNameInvalidException.class).isThrownBy(() ->
            endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup))
        );
    }

    @Test
    public void shouldThrowValidationExceptionWithWrongEndpointType() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("group");
        endpointGroup.setType("http");
        Endpoint endpoint = new Endpoint();
        endpoint.setName("endpoint");
        endpointGroup.setEndpoints(List.of(endpoint));
        assertThatExceptionOfType(EndpointTypeInvalidException.class).isThrownBy(() ->
            endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup))
        );
    }

    @Test
    public void shouldThrowValidationExceptionWithMismatch() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("group");
        endpointGroup.setType("http");
        Endpoint endpoint = new Endpoint();
        endpoint.setName("endpoint");
        endpoint.setType("wrong");
        endpointGroup.setEndpoints(List.of(endpoint));
        assertThatExceptionOfType(EndpointGroupTypeMismatchInvalidException.class).isThrownBy(() ->
            endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup))
        );
    }

    @Test
    public void shouldReturnDefaultWeightWithWrongEndpointWeight() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("name");
        endpointGroup.setType("http");
        Endpoint endpoint = new Endpoint();
        endpoint.setName("endpoint");
        endpoint.setType("http");
        endpoint.setWeight(0);
        endpointGroup.setEndpoints(List.of(endpoint));
        List<EndpointGroup> endpointGroups = endpointGroupsValidationService.validateAndSanitizeHttpV4(
            ApiType.PROXY,
            List.of(endpointGroup)
        );

        assertThat(endpointGroups.getFirst().getEndpoints().getFirst().getWeight()).isEqualTo(1);
    }

    @Test
    public void shouldThrowExceptionWithNullParameter() {
        var exception = catchException(() -> endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, null));
        assertThat(exception).isInstanceOf(EndpointMissingException.class);
    }

    @Test
    public void shouldValidateSharedConfiguration() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType("http");
        endpointGroup.setSharedConfiguration("sharedConfiguration");
        endpointGroup.setEndpoints(List.of());
        Service discovery = new Service();
        discovery.setEnabled(true);
        EndpointGroupServices services = new EndpointGroupServices();
        services.setDiscovery(discovery);
        endpointGroup.setServices(services);
        List<EndpointGroup> endpointGroups = endpointGroupsValidationService.validateAndSanitizeHttpV4(
            ApiType.PROXY,
            List.of(endpointGroup)
        );
        assertThat(endpointGroups).hasSize(1);
        EndpointGroup validatedEndpointGroup = endpointGroups.getFirst();
        assertThat(validatedEndpointGroup.getName()).isEqualTo(endpointGroup.getName());
        assertThat(validatedEndpointGroup.getType()).isEqualTo(endpointGroup.getType());
        assertThat(validatedEndpointGroup.getEndpoints()).isEmpty();
        assertThat(validatedEndpointGroup.getServices()).isEqualTo(services);
        assertThat(validatedEndpointGroup.getSharedConfiguration()).isNotNull();
        assertThat(validatedEndpointGroup.getLoadBalancer()).isNotNull();
        assertThat(validatedEndpointGroup.getLoadBalancer().getType()).isEqualTo(LoadBalancerType.ROUND_ROBIN);
        verify(endpointService).validateSharedConfiguration(any(), eq(endpointGroup.getSharedConfiguration()));
    }

    @Test
    public void shouldValidateOverriddenSharedConfiguration() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType("http");
        endpointGroup.setSharedConfiguration("sharedConfiguration");
        Service discovery = new Service();
        discovery.setEnabled(true);
        EndpointGroupServices services = new EndpointGroupServices();
        services.setDiscovery(discovery);
        endpointGroup.setServices(services);

        Endpoint endpoint = new Endpoint();
        endpoint.setName("endpoint");
        endpoint.setType("http");
        endpoint.setInheritConfiguration(false);
        endpoint.setSharedConfigurationOverride("overriddenSharedConfiguration");

        endpointGroup.setEndpoints(List.of(endpoint));

        List<EndpointGroup> endpointGroups = endpointGroupsValidationService.validateAndSanitizeHttpV4(
            ApiType.PROXY,
            List.of(endpointGroup)
        );
        assertThat(endpointGroups).hasSize(1);
        EndpointGroup validatedEndpointGroup = endpointGroups.getFirst();
        assertThat(validatedEndpointGroup.getName()).isEqualTo(endpointGroup.getName());
        assertThat(validatedEndpointGroup.getType()).isEqualTo(endpointGroup.getType());
        assertThat(validatedEndpointGroup.getServices()).isEqualTo(services);
        assertThat(validatedEndpointGroup.getSharedConfiguration()).isNotNull();
        assertThat(validatedEndpointGroup.getLoadBalancer()).isNotNull();
        assertThat(validatedEndpointGroup.getLoadBalancer().getType()).isEqualTo(LoadBalancerType.ROUND_ROBIN);
        assertThat(validatedEndpointGroup.getEndpoints())
            .hasSize(1)
            .first()
            .extracting("sharedConfigurationOverride")
            .isEqualTo("overriddenSharedConfiguration");
        verify(endpointService).validateSharedConfiguration(any(), eq(endpointGroup.getSharedConfiguration()));
        verify(endpointService).validateSharedConfiguration(any(), eq(endpoint.getSharedConfigurationOverride()));
    }

    @Test
    public void shouldNotValidateEndpointGroupWhenTryingToInheritANullSharedConfiguration() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType("http");
        endpointGroup.setSharedConfiguration((String) null);
        Service discovery = new Service();
        discovery.setEnabled(true);
        EndpointGroupServices services = new EndpointGroupServices();
        services.setDiscovery(discovery);
        endpointGroup.setServices(services);

        Endpoint endpoint = new Endpoint();
        endpoint.setName("endpoint");
        endpoint.setType("http");
        endpoint.setInheritConfiguration(true);
        endpoint.setSharedConfigurationOverride("overriddenSharedConfiguration");

        endpointGroup.setEndpoints(List.of(endpoint));

        assertThatThrownBy(() -> endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup)))
            .isInstanceOf(EndpointConfigurationValidationException.class)
            .hasMessage("Impossible to inherit from a null shared configuration for endpoint: endpoint");
        verify(endpointService, never()).validateSharedConfiguration(any(), eq(endpointGroup.getSharedConfiguration()));
        verify(endpointService, never()).validateSharedConfiguration(any(), eq(endpoint.getSharedConfigurationOverride()));
    }

    @Test
    public void shouldNotValidateEndpointGroupWhenNotInheritingNorOverriding() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType("http");
        endpointGroup.setSharedConfiguration("minimalSharedConfiguration");
        Service discovery = new Service();
        discovery.setEnabled(true);
        EndpointGroupServices services = new EndpointGroupServices();
        services.setDiscovery(discovery);
        endpointGroup.setServices(services);

        Endpoint endpoint = new Endpoint();
        endpoint.setName("endpoint");
        endpoint.setType("http");
        endpoint.setInheritConfiguration(false);
        endpoint.setSharedConfigurationOverride((String) null);

        endpointGroup.setEndpoints(List.of(endpoint));

        endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup));
        verify(endpointService).validateSharedConfiguration(any(), eq(endpointGroup.getSharedConfiguration()));
        verify(endpointService, never()).validateSharedConfiguration(any(), eq(endpoint.getSharedConfigurationOverride()));
        verify(endpointService).validateSharedConfiguration(any(), eq("{}"));
    }

    /**
     * NativeEndpointGroup tests
     */
    @Nested
    class NativeEndpointGroupValidationTest {

        @Test
        public void should_return_validated_native_endpoint_groups_without_endpoints_and_without_discovery_service() {
            NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
            endpointGroup.setName("my name");
            endpointGroup.setType(NATIVE_ENDPOINT_TYPE);
            endpointGroup.setEndpoints(List.of());
            var exception = catchException(() -> endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup)));
            assertThat(exception).isInstanceOf(EndpointMissingException.class);
        }

        @Test
        public void should_return_validated_native_endpoint_groups_with_endpoints() {
            NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
            endpointGroup.setName("my name");
            endpointGroup.setType(NATIVE_ENDPOINT_TYPE);
            NativeEndpoint endpoint = new NativeEndpoint();
            endpoint.setName("endpoint");
            endpoint.setType(NATIVE_ENDPOINT_TYPE);
            endpoint.setSharedConfigurationOverride("minimalSharedConfiguration");
            endpointGroup.setEndpoints(List.of(endpoint));
            List<NativeEndpointGroup> endpointGroups = endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup));
            assertThat(endpointGroups).hasSize(1);
            NativeEndpointGroup validatedEndpointGroup = endpointGroups.getFirst();
            assertThat(validatedEndpointGroup.getName()).isEqualTo(endpointGroup.getName());
            assertThat(validatedEndpointGroup.getType()).isEqualTo(endpointGroup.getType());
            assertThat(validatedEndpointGroup.getEndpoints()).isNotEmpty();
            List<NativeEndpoint> endpoints = validatedEndpointGroup.getEndpoints();
            NativeEndpoint validatedEndpoint = endpoints.getFirst();
            assertThat(validatedEndpoint.getName()).isEqualTo("endpoint");
            assertThat(validatedEndpoint.getType()).isEqualTo(NATIVE_ENDPOINT_TYPE);
            assertThat(validatedEndpointGroup.getSharedConfiguration()).isNull();
            assertThat(validatedEndpointGroup.getLoadBalancer()).isNotNull();
            assertThat(validatedEndpointGroup.getLoadBalancer().getType()).isEqualTo(LoadBalancerType.ROUND_ROBIN);
        }

        @Test
        public void should_throw_validation_exception_with_wrong_native_endpoint_name() {
            NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
            endpointGroup.setName("name");
            endpointGroup.setType(NATIVE_ENDPOINT_TYPE);
            NativeEndpoint endpoint = new NativeEndpoint();
            endpoint.setName(":");
            endpoint.setType(NATIVE_ENDPOINT_TYPE);
            endpointGroup.setEndpoints(List.of(endpoint));
            var exception = catchException(() -> endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup)));
            assertThat(exception).isInstanceOf(EndpointNameInvalidException.class);
        }

        @Test
        public void should_throw_validation_exception_with_wrong_native_endpoint_group_name() {
            NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
            endpointGroup.setName(":");
            var exception = catchException(() -> endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup)));
            assertThat(exception).isInstanceOf(EndpointNameInvalidException.class);
        }

        @Test
        public void should_throw_validation_exception_with_native_endpoint_name_already_exists() {
            NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
            endpointGroup.setName("my name");
            endpointGroup.setType(NATIVE_ENDPOINT_TYPE);
            NativeEndpoint endpoint = new NativeEndpoint();
            endpoint.setName("my name");
            endpoint.setType(NATIVE_ENDPOINT_TYPE);
            endpointGroup.setEndpoints(List.of(endpoint));

            var exception = catchException(() -> endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup)));
            assertThat(exception).isInstanceOf(EndpointNameAlreadyExistsException.class);
        }

        @Test
        public void should_throw_validation_exception_with_native_endpoint_name_already_exists_in_another_group() {
            NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
            endpointGroup.setName("group1");
            endpointGroup.setType(NATIVE_ENDPOINT_TYPE);
            NativeEndpoint endpoint = new NativeEndpoint();
            endpoint.setName("my name");
            endpoint.setType(NATIVE_ENDPOINT_TYPE);
            endpoint.setSharedConfigurationOverride("minimalSharedConfiguration");
            endpointGroup.setEndpoints(List.of(endpoint));

            NativeEndpointGroup endpointGroup2 = new NativeEndpointGroup();
            endpointGroup2.setName("group2");
            endpointGroup2.setType(NATIVE_ENDPOINT_TYPE);
            NativeEndpoint endpoint2 = new NativeEndpoint();
            endpoint2.setName("my name");
            endpoint2.setType(NATIVE_ENDPOINT_TYPE);
            endpoint2.setSharedConfigurationOverride("minimalSharedConfiguration");
            endpointGroup2.setEndpoints(List.of(endpoint2));

            var exception = catchException(() ->
                endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup, endpointGroup2))
            );
            assertThat(exception).isInstanceOf(EndpointNameAlreadyExistsException.class);
        }

        @Test
        public void should_throw_validation_exception_with_native_endpoint_group_name_already_exists_in_another_group() {
            NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
            endpointGroup.setName("group1");
            endpointGroup.setType(NATIVE_ENDPOINT_TYPE);
            endpointGroup.setSharedConfiguration("sharedConfiguration");
            NativeEndpoint endpoint = new NativeEndpoint();
            endpoint.setName("my name");
            endpoint.setType(NATIVE_ENDPOINT_TYPE);
            endpoint.setInheritConfiguration(true);
            endpointGroup.setEndpoints(List.of(endpoint));

            NativeEndpointGroup endpointGroup2 = new NativeEndpointGroup();
            endpointGroup2.setName("my name");
            endpointGroup2.setType(NATIVE_ENDPOINT_TYPE);
            endpointGroup2.setSharedConfiguration("sharedConfiguration");
            NativeEndpoint endpoint2 = new NativeEndpoint();
            endpoint2.setName("endpoint2");
            endpoint2.setType(NATIVE_ENDPOINT_TYPE);
            endpoint2.setInheritConfiguration(true);
            endpointGroup2.setEndpoints(List.of(endpoint2));

            var exception = catchException(() ->
                endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup, endpointGroup2))
            );
            assertThat(exception).isInstanceOf(EndpointGroupNameAlreadyExistsException.class);
        }

        @Test
        public void should_throw_validation_exception_with_missing_native_endpoint_group_type() {
            NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
            endpointGroup.setName("name");
            var exception = catchException(() -> endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup)));
            assertThat(exception).isInstanceOf(EndpointGroupTypeInvalidException.class);
        }

        @Test
        public void should_throw_validation_exception_with_wrong_native_endpoint_type() {
            NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
            endpointGroup.setName("group");
            endpointGroup.setType(NATIVE_ENDPOINT_TYPE);
            NativeEndpoint endpoint = new NativeEndpoint();
            endpoint.setName("endpoint");
            endpointGroup.setEndpoints(List.of(endpoint));
            var exception = catchException(() -> endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup)));
            assertThat(exception).isInstanceOf(EndpointTypeInvalidException.class);
        }

        @Test
        public void should_throw_validation_exception_with_mismatch_native_endpoint_type() {
            NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
            endpointGroup.setName("group");
            endpointGroup.setType(NATIVE_ENDPOINT_TYPE);
            NativeEndpoint endpoint = new NativeEndpoint();
            endpoint.setName("endpoint");
            endpoint.setType("wrong");
            endpointGroup.setEndpoints(List.of(endpoint));
            var exception = catchException(() -> endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup)));
            assertThat(exception).isInstanceOf(EndpointGroupTypeMismatchInvalidException.class);
        }

        @Test
        public void should_throw_exception_with_null_native_endpoint_groups() {
            var exception = catchException(() -> endpointGroupsValidationService.validateAndSanitizeNativeV4(null));
            assertThat(exception).isInstanceOf(EndpointMissingException.class);
        }

        @Test
        public void should_validate_native_endpoint_group_shared_configuration() {
            NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
            endpointGroup.setName("my name");
            endpointGroup.setType(NATIVE_ENDPOINT_TYPE);
            endpointGroup.setSharedConfiguration("sharedConfiguration");
            endpointGroup.setEndpoints(List.of(NativeEndpoint.builder().type(NATIVE_ENDPOINT_TYPE).build()));
            List<NativeEndpointGroup> endpointGroups = endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup));
            assertThat(endpointGroups).hasSize(1);
            NativeEndpointGroup validatedEndpointGroup = endpointGroups.getFirst();
            assertThat(validatedEndpointGroup.getName()).isEqualTo(endpointGroup.getName());
            assertThat(validatedEndpointGroup.getType()).isEqualTo(endpointGroup.getType());
            assertThat(validatedEndpointGroup.getEndpoints()).hasSize(1);
            assertThat(validatedEndpointGroup.getSharedConfiguration()).isNotNull();
            assertThat(validatedEndpointGroup.getLoadBalancer()).isNotNull();
            assertThat(validatedEndpointGroup.getLoadBalancer().getType()).isEqualTo(LoadBalancerType.ROUND_ROBIN);
            verify(endpointService).validateSharedConfiguration(any(), eq(endpointGroup.getSharedConfiguration()));
        }

        @Test
        public void should_validate_overridden_native_endpoint_group_shared_configuration() {
            NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
            endpointGroup.setName("my name");
            endpointGroup.setType(NATIVE_ENDPOINT_TYPE);
            endpointGroup.setSharedConfiguration("sharedConfiguration");

            NativeEndpoint endpoint = new NativeEndpoint();
            endpoint.setName("endpoint");
            endpoint.setType(NATIVE_ENDPOINT_TYPE);
            endpoint.setInheritConfiguration(false);
            endpoint.setSharedConfigurationOverride("overriddenSharedConfiguration");

            endpointGroup.setEndpoints(List.of(endpoint));

            List<NativeEndpointGroup> endpointGroups = endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup));
            assertThat(endpointGroups).hasSize(1);
            NativeEndpointGroup validatedEndpointGroup = endpointGroups.getFirst();
            assertThat(validatedEndpointGroup.getName()).isEqualTo(endpointGroup.getName());
            assertThat(validatedEndpointGroup.getType()).isEqualTo(endpointGroup.getType());
            assertThat(validatedEndpointGroup.getSharedConfiguration()).isNotNull();
            assertThat(validatedEndpointGroup.getLoadBalancer()).isNotNull();
            assertThat(validatedEndpointGroup.getLoadBalancer().getType()).isEqualTo(LoadBalancerType.ROUND_ROBIN);
            assertThat(validatedEndpointGroup.getEndpoints())
                .hasSize(1)
                .first()
                .extracting("sharedConfigurationOverride")
                .isEqualTo("overriddenSharedConfiguration");
            verify(endpointService).validateSharedConfiguration(any(), eq(endpointGroup.getSharedConfiguration()));
            verify(endpointService).validateSharedConfiguration(any(), eq(endpoint.getSharedConfigurationOverride()));
        }

        @Test
        public void should_not_validate_native_endpoint_group_when_trying_to_inherit_a_null_shared_configuration() {
            NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
            endpointGroup.setName("my name");
            endpointGroup.setType(NATIVE_ENDPOINT_TYPE);
            endpointGroup.setSharedConfiguration((String) null);

            NativeEndpoint endpoint = new NativeEndpoint();
            endpoint.setName("endpoint");
            endpoint.setType(NATIVE_ENDPOINT_TYPE);
            endpoint.setInheritConfiguration(true);
            endpoint.setSharedConfigurationOverride("overriddenSharedConfiguration");

            endpointGroup.setEndpoints(List.of(endpoint));

            assertThatThrownBy(() -> endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup)))
                .isInstanceOf(EndpointConfigurationValidationException.class)
                .hasMessage("Impossible to inherit from a null shared configuration for endpoint: endpoint");
            verify(endpointService, never()).validateSharedConfiguration(any(), eq(endpointGroup.getSharedConfiguration()));
            verify(endpointService, never()).validateSharedConfiguration(any(), eq(endpoint.getSharedConfigurationOverride()));
        }

        @Test
        public void should_not_validate_native_endpoint_group_when_not_inheriting_nor_overriding() {
            NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
            endpointGroup.setName("my name");
            endpointGroup.setType(NATIVE_ENDPOINT_TYPE);
            endpointGroup.setSharedConfiguration("minimalSharedConfiguration");

            NativeEndpoint endpoint = new NativeEndpoint();
            endpoint.setName("endpoint");
            endpoint.setType(NATIVE_ENDPOINT_TYPE);
            endpoint.setInheritConfiguration(false);
            endpoint.setSharedConfigurationOverride((String) null);

            endpointGroup.setEndpoints(List.of(endpoint));

            endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup));
            verify(endpointService).validateSharedConfiguration(any(), eq(endpointGroup.getSharedConfiguration()));
            verify(endpointService, never()).validateSharedConfiguration(any(), eq(endpoint.getSharedConfigurationOverride()));
            verify(endpointService).validateSharedConfiguration(any(), eq("{}"));
        }
    }

    /**
     * LLM Proxy provider consistency tests
     */
    @Nested
    class LLMProxyProviderConsistencyTest {

        @Test
        public void should_throw_when_llm_proxy_endpoints_have_different_providers() {
            EndpointGroup endpointGroup = new EndpointGroup();
            endpointGroup.setName("llm-group-name");
            endpointGroup.setType("llm-proxy");

            Endpoint endpoint1 = new Endpoint();
            endpoint1.setName("openai-endpoint");
            endpoint1.setType("llm-proxy");
            endpoint1.setConfiguration("{\"provider\":\"OPEN_AI\"}");

            Endpoint endpoint2 = new Endpoint();
            endpoint2.setName("azure-endpoint");
            endpoint2.setType("llm-proxy");
            endpoint2.setConfiguration("{\"provider\":\"OPEN_AI_COMPATIBLE\"}");

            endpointGroup.setEndpoints(List.of(endpoint1, endpoint2));

            assertThatExceptionOfType(EndpointGroupLlmProxyInvalidException.class)
                .isThrownBy(() -> endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.LLM_PROXY, List.of(endpointGroup)))
                .withMessageContaining("provider")
                .withMessageNotContaining("aliases")
                .withMessageContaining("llm-group-name");
        }

        @Test
        public void should_not_throw_when_llm_proxy_endpoints_have_same_provider() {
            EndpointGroup endpointGroup = new EndpointGroup();
            endpointGroup.setName("llm-group-name");
            endpointGroup.setType("llm-proxy");

            Endpoint endpoint1 = new Endpoint();
            endpoint1.setName("openai-endpoint-1");
            endpoint1.setType("llm-proxy");
            endpoint1.setConfiguration("{\"provider\":\"OPEN_AI\"}");

            Endpoint endpoint2 = new Endpoint();
            endpoint2.setName("openai-endpoint-2");
            endpoint2.setType("llm-proxy");
            endpoint2.setConfiguration("{\"provider\":\"OPEN_AI\"}");

            endpointGroup.setEndpoints(List.of(endpoint1, endpoint2));

            List<EndpointGroup> result = endpointGroupsValidationService.validateAndSanitizeHttpV4(
                ApiType.LLM_PROXY,
                List.of(endpointGroup)
            );
            assertThat(result).hasSize(1);
        }

        @Test
        public void should_not_validate_provider_for_non_llm_proxy_groups() {
            // HTTP endpoint group with different configurations should pass without provider validation
            EndpointGroup endpointGroup = new EndpointGroup();
            endpointGroup.setName("http-group");
            endpointGroup.setType("http");

            Endpoint endpoint1 = new Endpoint();
            endpoint1.setName("endpoint-1");
            endpoint1.setType("http");
            endpoint1.setConfiguration("{\"provider\":\"A\"}");

            Endpoint endpoint2 = new Endpoint();
            endpoint2.setName("endpoint-2");
            endpoint2.setType("http");
            endpoint2.setConfiguration("{\"provider\":\"B\"}");

            endpointGroup.setEndpoints(List.of(endpoint1, endpoint2));

            List<EndpointGroup> result = endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup));
            assertThat(result).hasSize(1);
        }

        @Test
        public void should_throw_technical_exception_when_fail_to_parse_configuration() {
            EndpointGroup endpointGroup = new EndpointGroup();
            endpointGroup.setName("llm-group-name");
            endpointGroup.setType("llm-proxy");

            Endpoint endpoint1 = new Endpoint();
            endpoint1.setName("openai-endpoint");
            endpoint1.setType("llm-proxy");
            endpoint1.setConfiguration(
                """
                {
                 "models": [
                   {"name": "gemini-2.5-flash-lite"},
                   {"n
                 "provider": "OPEN_AI"
                }
                """
            );

            Endpoint endpoint2 = new Endpoint();
            endpoint2.setName("azure-endpoint");
            endpoint2.setType("llm-proxy");
            endpoint2.setConfiguration(
                """
                {
                 "models": [
                   {"name": "gemini-2.5-flash-lite", "aliases": ["light"]},
                   {"name": "gemini-3"}
                 ],
                 "provider": "OPEN_AI"
                }
                """
            );

            endpointGroup.setEndpoints(List.of(endpoint1, endpoint2));

            var exception = catchException(() ->
                endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.LLM_PROXY, List.of(endpointGroup))
            );

            assertThat(exception).isInstanceOf(TechnicalManagementException.class);
        }

        @Test
        public void should_not_throw_when_llm_proxy_endpoints_have_same_aliases() {
            EndpointGroup endpointGroup = new EndpointGroup();
            endpointGroup.setName("llm-group-name");
            endpointGroup.setType("llm-proxy");

            Endpoint endpoint1 = new Endpoint();
            endpoint1.setName("openai-endpoint");
            endpoint1.setType("llm-proxy");
            endpoint1.setConfiguration(
                """
                {
                 "models": [
                   {"name": "gemini-2.5-flash-lite"},
                   {"name": "gemini-3", "aliases": ["light"]}
                 ],
                 "provider": "OPEN_AI"
                }
                """
            );

            Endpoint endpoint2 = new Endpoint();
            endpoint2.setName("azure-endpoint");
            endpoint2.setType("llm-proxy");
            endpoint2.setConfiguration(
                """
                {
                 "models": [
                   {"name": "gemini-2.5-flash-lite", "aliases": ["light"]},
                   {"name": "gemini-3"}
                 ],
                 "provider": "OPEN_AI"
                }
                """
            );

            endpointGroup.setEndpoints(List.of(endpoint1, endpoint2));

            List<EndpointGroup> endpointGroups = endpointGroupsValidationService.validateAndSanitizeHttpV4(
                ApiType.LLM_PROXY,
                List.of(endpointGroup)
            );

            assertThat(endpointGroups).hasSize(1);
        }

        @Test
        public void should_throw_when_llm_proxy_endpoints_have_different_aliases() {
            EndpointGroup endpointGroup = new EndpointGroup();
            endpointGroup.setName("llm-group-name");
            endpointGroup.setType("llm-proxy");

            Endpoint endpoint1 = new Endpoint();
            endpoint1.setName("openai-endpoint");
            endpoint1.setType("llm-proxy");
            endpoint1.setConfiguration(
                """
                {
                 "models": [
                   {"name": "gemini-2.5-flash-lite", "aliases": ["fast"]},
                   {"name": "gemini-3"}
                 ],
                 "provider": "OPEN_AI"
                }
                """
            );

            Endpoint endpoint2 = new Endpoint();
            endpoint2.setName("azure-endpoint");
            endpoint2.setType("llm-proxy");
            endpoint2.setConfiguration(
                """
                {
                 "models": [
                   {"name": "gemini-2.5-flash-lite", "aliases": ["light"]},
                   {"name": "gemini-3"}
                 ],
                 "provider": "OPEN_AI"
                }
                """
            );

            endpointGroup.setEndpoints(List.of(endpoint1, endpoint2));

            assertThatExceptionOfType(EndpointGroupLlmProxyInvalidException.class)
                .isThrownBy(() -> endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.LLM_PROXY, List.of(endpointGroup)))
                .withMessageContaining("aliases")
                .withMessageNotContaining("provider")
                .withMessageContaining("llm-group-name");
        }
    }
}
