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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.rest.api.service.ConnectorService;
import io.gravitee.rest.api.service.exceptions.EndpointNameInvalidException;
import io.gravitee.rest.api.service.v4.EndpointGroupsValidationService;
import io.gravitee.rest.api.service.v4.exception.EndpointGroupTypeInvalidException;
import io.gravitee.rest.api.service.v4.exception.EndpointGroupTypeMismatchInvalidException;
import io.gravitee.rest.api.service.v4.exception.EndpointTypeInvalidException;
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

    @Mock
    private ConnectorService connectorService;

    private EndpointGroupsValidationService endpointGroupsValidationService;

    @Before
    public void setUp() throws Exception {
        lenient().when(connectorService.validateConnectorConfiguration(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        endpointGroupsValidationService = new EndpointGroupsValidationServiceImpl(connectorService);
    }

    @Test
    public void shouldReturnValidatedEndpointGroupsWithoutEndpoints() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType("http");
        endpointGroup.setEndpoints(List.of());
        List<EndpointGroup> endpointGroups = endpointGroupsValidationService.validateAndSanitize(List.of(endpointGroup));
        assertThat(endpointGroups.size()).isEqualTo(1);
        EndpointGroup validatedEndpointGroup = endpointGroups.get(0);
        assertThat(validatedEndpointGroup.getName()).isEqualTo(endpointGroup.getName());
        assertThat(validatedEndpointGroup.getType()).isEqualTo(endpointGroup.getType());
        assertThat(validatedEndpointGroup.getEndpoints()).isEmpty();
        assertThat(validatedEndpointGroup.getServices()).isNull();
        assertThat(validatedEndpointGroup.getSharedConfiguration()).isNull();
        assertThat(validatedEndpointGroup.getLoadBalancer()).isNull();
    }

    @Test
    public void shouldReturnValidatedEndpointGroupsWithEndpoints() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("my name");
        endpointGroup.setType("http");
        Endpoint endpoint = new Endpoint();
        endpoint.setName("endpoint");
        endpoint.setType("http");
        endpointGroup.setEndpoints(List.of(endpoint));
        List<EndpointGroup> endpointGroups = endpointGroupsValidationService.validateAndSanitize(List.of(endpointGroup));
        assertThat(endpointGroups.size()).isEqualTo(1);
        EndpointGroup validatedEndpointGroup = endpointGroups.get(0);
        assertThat(validatedEndpointGroup.getName()).isEqualTo(endpointGroup.getName());
        assertThat(validatedEndpointGroup.getType()).isEqualTo(endpointGroup.getType());
        assertThat(validatedEndpointGroup.getEndpoints()).isNotEmpty();
        List<Endpoint> endpoints = validatedEndpointGroup.getEndpoints();
        Endpoint validatedEndpoint = endpoints.get(0);
        assertThat(validatedEndpoint.getName()).isEqualTo("endpoint");
        assertThat(validatedEndpoint.getType()).isEqualTo("http");
        assertThat(validatedEndpointGroup.getServices()).isNull();
        assertThat(validatedEndpointGroup.getSharedConfiguration()).isNull();
        assertThat(validatedEndpointGroup.getLoadBalancer()).isNull();
    }

    @Test
    public void shouldThrowValidationExceptionWithWrongEndpointGroupName() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName(":");
        assertThatExceptionOfType(EndpointNameInvalidException.class)
            .isThrownBy(() -> endpointGroupsValidationService.validateAndSanitize(List.of(endpointGroup)));
    }

    @Test
    public void shouldThrowValidationExceptionWithWrongEndpointGroupType() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("name");
        assertThatExceptionOfType(EndpointGroupTypeInvalidException.class)
            .isThrownBy(() -> endpointGroupsValidationService.validateAndSanitize(List.of(endpointGroup)));
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
            .isThrownBy(() -> endpointGroupsValidationService.validateAndSanitize(List.of(endpointGroup)));
    }

    @Test
    public void shouldThrowValidationExceptionWithWrongEndpointType() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("name");
        endpointGroup.setType("http");
        Endpoint endpoint = new Endpoint();
        endpoint.setName("name");
        endpointGroup.setEndpoints(List.of(endpoint));
        assertThatExceptionOfType(EndpointTypeInvalidException.class)
            .isThrownBy(() -> endpointGroupsValidationService.validateAndSanitize(List.of(endpointGroup)));
    }

    @Test
    public void shouldThrowValidationExceptionWithMismatch() {
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("name");
        endpointGroup.setType("http");
        Endpoint endpoint = new Endpoint();
        endpoint.setName("name");
        endpoint.setType("wrong");
        endpointGroup.setEndpoints(List.of(endpoint));
        assertThatExceptionOfType(EndpointGroupTypeMismatchInvalidException.class)
            .isThrownBy(() -> endpointGroupsValidationService.validateAndSanitize(List.of(endpointGroup)));
    }

    @Test
    public void shouldReturnNullListWithNullParameter() {
        assertThat(endpointGroupsValidationService.validateAndSanitize(null)).isNull();
    }
}
