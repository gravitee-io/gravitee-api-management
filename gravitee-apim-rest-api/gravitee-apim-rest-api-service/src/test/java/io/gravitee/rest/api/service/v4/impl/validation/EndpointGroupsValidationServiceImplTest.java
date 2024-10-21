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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import io.gravitee.rest.api.service.v4.ApiServicePluginService;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import io.gravitee.rest.api.service.v4.exception.*;
import io.gravitee.rest.api.service.v4.validation.EndpointGroupsValidationService;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class EndpointGroupsValidationServiceImplTest {

    public static final String FIXED_HC_CONFIG = "{fixed}";
    public static final String HEALTH_CHECK_TYPE = "http-health-check";
    public static final String NATIVE_ENDPOINT_TYPE = "native-friendly";

    @Mock
    private EndpointConnectorPluginService endpointService;

    @Mock
    private ApiServicePluginService apiServicePluginService;

    private EndpointGroupsValidationService endpointGroupsValidationService;

    @Before
    public void setUp() throws Exception {
        lenient()
            .when(endpointService.validateConnectorConfiguration(any(String.class), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(endpointService.validateSharedConfiguration(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        var httpEndpoint = new ConnectorPluginEntity();
        httpEndpoint.setId("http");
        httpEndpoint.setSupportedApiType(ApiType.PROXY);
        lenient().when(endpointService.findById("http")).thenReturn(httpEndpoint);

        var nativeFriendlyEndpoint = new ConnectorPluginEntity();
        nativeFriendlyEndpoint.setId(NATIVE_ENDPOINT_TYPE);
        nativeFriendlyEndpoint.setSupportedApiType(ApiType.NATIVE);
        lenient().when(endpointService.findById(NATIVE_ENDPOINT_TYPE)).thenReturn(nativeFriendlyEndpoint);

        endpointGroupsValidationService = new EndpointGroupsValidationServiceImpl(endpointService, apiServicePluginService);
    }

    @Test(expected = EndpointMissingException.class)
    public void shouldReturnValidatedEndpointGroupsWithoutEndpointsAndWithoutDiscoveryService() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType("http");
        endpointGroup.setEndpoints(List.of());
        endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup));
    }

    @Test(expected = EndpointMissingException.class)
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
        endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup));
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
        EndpointGroup validatedEndpointGroup = endpointGroups.get(0);
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
        EndpointGroup validatedEndpointGroup = endpointGroups.get(0);
        assertThat(validatedEndpointGroup.getName()).isEqualTo(endpointGroup.getName());
        assertThat(validatedEndpointGroup.getType()).isEqualTo(endpointGroup.getType());
        assertThat(validatedEndpointGroup.getEndpoints()).isNotEmpty();
        List<Endpoint> endpoints = validatedEndpointGroup.getEndpoints();
        Endpoint validatedEndpoint = endpoints.get(0);
        assertThat(validatedEndpoint.getName()).isEqualTo("endpoint");
        assertThat(validatedEndpoint.getType()).isEqualTo("http");
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
        EndpointGroup validatedEndpointGroup = endpointGroups.get(0);
        assertThat(validatedEndpointGroup.getName()).isEqualTo(endpointGroup.getName());
        assertThat(validatedEndpointGroup.getType()).isEqualTo(endpointGroup.getType());
        assertThat(validatedEndpointGroup.getEndpoints()).isNotEmpty();
        List<Endpoint> endpoints = validatedEndpointGroup.getEndpoints();
        Endpoint validatedEndpoint = endpoints.get(0);
        assertThat(validatedEndpoint.getName()).isEqualTo("endpoint");
        assertThat(validatedEndpoint.getType()).isEqualTo("http");
        assertThat(validatedEndpointGroup.getServices())
            .isNotNull()
            .matches(svc -> svc.getHealthCheck().getConfiguration().equals(FIXED_HC_CONFIG));
        assertThat(validatedEndpointGroup.getSharedConfiguration()).isNotNull();
        assertThat(validatedEndpointGroup.getLoadBalancer()).isNotNull();
        assertThat(validatedEndpointGroup.getLoadBalancer().getType()).isEqualTo(LoadBalancerType.ROUND_ROBIN);
    }

    @Test(expected = HealthcheckInvalidException.class)
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

        endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup));
    }

    @Test(expected = HealthcheckInvalidException.class)
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
        endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup));
    }

    @Test(expected = HealthcheckInheritanceException.class)
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
        endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup));
    }

    @Test(expected = HealthcheckInheritanceException.class)
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
        endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup));
    }

    @Test(expected = HealthcheckInheritanceException.class)
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

        List<EndpointGroup> endpointGroups = endpointGroupsValidationService.validateAndSanitizeHttpV4(
            ApiType.PROXY,
            List.of(endpointGroup)
        );
        assertThat(endpointGroups.size()).isEqualTo(1);
        EndpointGroup validatedEndpointGroup = endpointGroups.get(0);
        assertThat(validatedEndpointGroup.getName()).isEqualTo(endpointGroup.getName());
        assertThat(validatedEndpointGroup.getType()).isEqualTo(endpointGroup.getType());
        assertThat(validatedEndpointGroup.getEndpoints()).isNotEmpty();
        List<Endpoint> endpoints = validatedEndpointGroup.getEndpoints();
        Endpoint validatedEndpoint = endpoints.get(0);
        assertThat(validatedEndpoint.getName()).isEqualTo("endpoint");
        assertThat(validatedEndpoint.getType()).isEqualTo("http");
        assertThat(validatedEndpointGroup.getServices())
            .isNotNull()
            .matches(svc -> svc.getHealthCheck().getConfiguration().equals(FIXED_HC_CONFIG));
        assertThat(validatedEndpointGroup.getSharedConfiguration()).isNull();
        assertThat(validatedEndpointGroup.getLoadBalancer()).isNotNull();
        assertThat(validatedEndpointGroup.getLoadBalancer().getType()).isEqualTo(LoadBalancerType.ROUND_ROBIN);
    }

    @Test
    public void shouldThrowValidationExceptionWithWrongEndpointGroupName() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName(":");
        assertThatExceptionOfType(EndpointNameInvalidException.class)
            .isThrownBy(() -> endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup)));
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

        assertThatExceptionOfType(EndpointNameAlreadyExistsException.class)
            .isThrownBy(() -> endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup)));
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

        assertThatExceptionOfType(EndpointNameAlreadyExistsException.class)
            .isThrownBy(() ->
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

        assertThatExceptionOfType(EndpointGroupNameAlreadyExistsException.class)
            .isThrownBy(() ->
                endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup, endpointGroup2))
            );
    }

    @Test
    public void shouldThrowValidationExceptionWithMissingEndpointGroupType() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("name");
        assertThatExceptionOfType(EndpointGroupTypeInvalidException.class)
            .isThrownBy(() -> endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup)));
    }

    @Test
    public void shouldThrowValidationExceptionWithInvalidTypeForProxyApi() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("name");
        endpointGroup.setType("http");
        assertThatExceptionOfType(EndpointGroupTypeInvalidException.class)
            .isThrownBy(() -> endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.MESSAGE, List.of(endpointGroup)));
    }

    @Test
    public void shouldThrowValidationExceptionForMessageApiWithProxyEndpointGroup() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("name");
        endpointGroup.setType("http");
        assertThatExceptionOfType(EndpointGroupTypeInvalidException.class)
            .isThrownBy(() -> endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.MESSAGE, List.of(endpointGroup)));
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
        assertThatExceptionOfType(EndpointNameInvalidException.class)
            .isThrownBy(() -> endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup)));
    }

    @Test
    public void shouldThrowValidationExceptionWithWrongEndpointType() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("group");
        endpointGroup.setType("http");
        Endpoint endpoint = new Endpoint();
        endpoint.setName("endpoint");
        endpointGroup.setEndpoints(List.of(endpoint));
        assertThatExceptionOfType(EndpointTypeInvalidException.class)
            .isThrownBy(() -> endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup)));
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
        assertThatExceptionOfType(EndpointGroupTypeMismatchInvalidException.class)
            .isThrownBy(() -> endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, List.of(endpointGroup)));
    }

    @Test(expected = EndpointMissingException.class)
    public void shouldThrowExceptionWithNullParameter() {
        assertThat(endpointGroupsValidationService.validateAndSanitizeHttpV4(ApiType.PROXY, null)).isNull();
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
        EndpointGroup validatedEndpointGroup = endpointGroups.get(0);
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
        EndpointGroup validatedEndpointGroup = endpointGroups.get(0);
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

    @Test(expected = EndpointMissingException.class)
    public void shouldReturnValidatedNativeEndpointGroupsWithoutEndpointsAndWithoutDiscoveryService() {
        NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType(NATIVE_ENDPOINT_TYPE);
        endpointGroup.setEndpoints(List.of());
        endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup));
    }

    @Test
    public void shouldReturnValidatedNativeEndpointGroupsWithEndpoints() {
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
        NativeEndpointGroup validatedEndpointGroup = endpointGroups.get(0);
        assertThat(validatedEndpointGroup.getName()).isEqualTo(endpointGroup.getName());
        assertThat(validatedEndpointGroup.getType()).isEqualTo(endpointGroup.getType());
        assertThat(validatedEndpointGroup.getEndpoints()).isNotEmpty();
        List<NativeEndpoint> endpoints = validatedEndpointGroup.getEndpoints();
        NativeEndpoint validatedEndpoint = endpoints.get(0);
        assertThat(validatedEndpoint.getName()).isEqualTo("endpoint");
        assertThat(validatedEndpoint.getType()).isEqualTo(NATIVE_ENDPOINT_TYPE);
        assertThat(validatedEndpointGroup.getSharedConfiguration()).isNull();
        assertThat(validatedEndpointGroup.getLoadBalancer()).isNotNull();
        assertThat(validatedEndpointGroup.getLoadBalancer().getType()).isEqualTo(LoadBalancerType.ROUND_ROBIN);
    }

    @Test(expected = EndpointNameInvalidException.class)
    public void shouldThrowValidationExceptionWithWrongNativeEndpointName() {
        NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
        endpointGroup.setName("name");
        endpointGroup.setType(NATIVE_ENDPOINT_TYPE);
        NativeEndpoint endpoint = new NativeEndpoint();
        endpoint.setName(":");
        endpoint.setType(NATIVE_ENDPOINT_TYPE);
        endpointGroup.setEndpoints(List.of(endpoint));
        endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup));
    }

    @Test(expected = EndpointNameInvalidException.class)
    public void shouldThrowValidationExceptionWithWrongNativeEndpointGroupName() {
        NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
        endpointGroup.setName(":");
        endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup));
    }

    @Test(expected = EndpointNameAlreadyExistsException.class)
    public void shouldThrowValidationExceptionWithNativeEndpointNameAlreadyExists() {
        NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType(NATIVE_ENDPOINT_TYPE);
        NativeEndpoint endpoint = new NativeEndpoint();
        endpoint.setName("my name");
        endpoint.setType(NATIVE_ENDPOINT_TYPE);
        endpointGroup.setEndpoints(List.of(endpoint));

        endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup));
    }

    @Test(expected = EndpointNameAlreadyExistsException.class)
    public void shouldThrowValidationExceptionWithNativeEndpointNameAlreadyExistsInAnotherGroup() {
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

        endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup, endpointGroup2));
    }

    @Test(expected = EndpointGroupNameAlreadyExistsException.class)
    public void shouldThrowValidationExceptionWithNativeEndpointGroupNameAlreadyExistsInAnotherGroup() {
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

        endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup, endpointGroup2));
    }

    @Test(expected = EndpointGroupTypeInvalidException.class)
    public void shouldThrowValidationExceptionWithMissingNativeEndpointGroupType() {
        NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
        endpointGroup.setName("name");
        endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup));
    }

    @Test(expected = EndpointTypeInvalidException.class)
    public void shouldThrowValidationExceptionWithWrongNativeEndpointType() {
        NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
        endpointGroup.setName("group");
        endpointGroup.setType(NATIVE_ENDPOINT_TYPE);
        NativeEndpoint endpoint = new NativeEndpoint();
        endpoint.setName("endpoint");
        endpointGroup.setEndpoints(List.of(endpoint));
        endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup));
    }

    @Test(expected = EndpointGroupTypeMismatchInvalidException.class)
    public void shouldThrowValidationExceptionWithMismatchNativeEndpointType() {
        NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
        endpointGroup.setName("group");
        endpointGroup.setType(NATIVE_ENDPOINT_TYPE);
        NativeEndpoint endpoint = new NativeEndpoint();
        endpoint.setName("endpoint");
        endpoint.setType("wrong");
        endpointGroup.setEndpoints(List.of(endpoint));
        endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup));
    }

    @Test(expected = EndpointMissingException.class)
    public void shouldThrowExceptionWithNullNativeEndpointGroups() {
        assertThat(endpointGroupsValidationService.validateAndSanitizeNativeV4(null)).isNull();
    }

    @Test
    public void shouldValidateNativeEndpointGroupSharedConfiguration() {
        NativeEndpointGroup endpointGroup = new NativeEndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType(NATIVE_ENDPOINT_TYPE);
        endpointGroup.setSharedConfiguration("sharedConfiguration");
        endpointGroup.setEndpoints(List.of(NativeEndpoint.builder().type(NATIVE_ENDPOINT_TYPE).build()));
        List<NativeEndpointGroup> endpointGroups = endpointGroupsValidationService.validateAndSanitizeNativeV4(List.of(endpointGroup));
        assertThat(endpointGroups).hasSize(1);
        NativeEndpointGroup validatedEndpointGroup = endpointGroups.get(0);
        assertThat(validatedEndpointGroup.getName()).isEqualTo(endpointGroup.getName());
        assertThat(validatedEndpointGroup.getType()).isEqualTo(endpointGroup.getType());
        assertThat(validatedEndpointGroup.getEndpoints()).hasSize(1);
        assertThat(validatedEndpointGroup.getSharedConfiguration()).isNotNull();
        assertThat(validatedEndpointGroup.getLoadBalancer()).isNotNull();
        assertThat(validatedEndpointGroup.getLoadBalancer().getType()).isEqualTo(LoadBalancerType.ROUND_ROBIN);
        verify(endpointService).validateSharedConfiguration(any(), eq(endpointGroup.getSharedConfiguration()));
    }

    @Test
    public void shouldValidateOverriddenNativeEndpointGroupSharedConfiguration() {
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
        NativeEndpointGroup validatedEndpointGroup = endpointGroups.get(0);
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
    public void shouldNotValidateNativeEndpointGroupWhenTryingToInheritANullSharedConfiguration() {
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
    public void shouldNotValidateNativeEndpointGroupWhenNotInheritingNorOverriding() {
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
