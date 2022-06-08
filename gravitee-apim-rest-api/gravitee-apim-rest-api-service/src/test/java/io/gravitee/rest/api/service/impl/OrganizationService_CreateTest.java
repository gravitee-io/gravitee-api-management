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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.impl.OrganizationServiceImpl;
import java.util.*;
import java.util.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class OrganizationService_CreateTest {

    @InjectMocks
    private OrganizationServiceImpl organizationService = new OrganizationServiceImpl();

    @Mock
    private OrganizationRepository mockOrganizationRepository;

    @Mock
    private RoleService mockRoleService;

    @Mock
    private FlowService mockFlowService;

    @Mock
    private EventService eventService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private ObjectMapper mapper;

    @Before
    public void setup() {
        GraviteeContext.setCurrentOrganization("orgid");
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldCreateOrganization() throws Exception {
        when(mockOrganizationRepository.findById(any())).thenReturn(Optional.empty());

        UpdateOrganizationEntity org1 = new UpdateOrganizationEntity();
        org1.setHrids(List.of("orgid"));
        org1.setName("org_name");
        org1.setDescription("org_desc");
        List<String> domainRestrictions = Arrays.asList("domain", "restriction");
        org1.setDomainRestrictions(domainRestrictions);
        org1.setFlows(List.of());

        Organization createdOrganization = new Organization();
        createdOrganization.setId("org_id");

        EnvironmentEntity env1 = new EnvironmentEntity();
        env1.setId("env_1");

        EnvironmentEntity env2 = new EnvironmentEntity();
        env2.setId("env_2");

        when(environmentService.findByOrganization(createdOrganization.getId())).thenReturn(List.of(env1, env2));

        when(mockOrganizationRepository.create(any())).thenReturn(createdOrganization);
        when(mockFlowService.findByReference(FlowReferenceType.ORGANIZATION, "org_id")).thenReturn(new ArrayList<>());

        GraviteeContext.setCurrentOrganization("org_id");

        OrganizationEntity organization = organizationService.createOrUpdate(GraviteeContext.getExecutionContext(), org1);

        assertNotNull("result is null", organization);
        verify(mockOrganizationRepository, times(1))
            .create(
                argThat(
                    arg ->
                        arg != null &&
                        arg.getName().equals("org_name") &&
                        arg.getDescription().equals("org_desc") &&
                        arg.getDomainRestrictions().equals(domainRestrictions)
                )
            );
        verify(mockOrganizationRepository, never()).update(any());
        verify(mockRoleService, times(1)).initialize(GraviteeContext.getExecutionContext(), "org_id");
        verify(mockRoleService, times(1)).createOrUpdateSystemRoles(GraviteeContext.getExecutionContext(), "org_id");
        verify(mockFlowService, times(1)).save(FlowReferenceType.ORGANIZATION, "org_id", List.of());
        verify(eventService, times(1))
            .createOrganizationEvent(
                eq(GraviteeContext.getExecutionContext()),
                eq(Set.of("env_1", "env_2")),
                eq(EventType.PUBLISH_ORGANIZATION),
                any(),
                eq(Map.of("organization_id", "org_id"))
            );
    }

    @Test
    public void shouldUpdateOrganization() throws Exception {
        when(mockOrganizationRepository.findById(any())).thenReturn(Optional.of(new Organization()));

        UpdateOrganizationEntity org1 = new UpdateOrganizationEntity();
        org1.setHrids(List.of("orgid"));
        org1.setName("org_name");
        org1.setDescription("org_desc");
        List<String> domainRestrictions = Arrays.asList("domain", "restriction");
        org1.setDomainRestrictions(domainRestrictions);
        org1.setFlows(List.of(mock(Flow.class)));

        Organization createdOrganization = new Organization();
        createdOrganization.setId("org_id");
        when(mockOrganizationRepository.update(any())).thenReturn(createdOrganization);
        when(mockFlowService.findByReference(FlowReferenceType.ORGANIZATION, "org_id")).thenReturn(org1.getFlows());

        EnvironmentEntity env1 = new EnvironmentEntity();
        env1.setId("env_1");

        EnvironmentEntity env2 = new EnvironmentEntity();
        env2.setId("env_2");

        when(environmentService.findByOrganization(createdOrganization.getId())).thenReturn(List.of(env1, env2));

        GraviteeContext.setCurrentOrganization("org_id");

        OrganizationEntity organization = organizationService.createOrUpdate(GraviteeContext.getExecutionContext(), org1);

        assertNotNull("result is null", organization);
        verify(mockOrganizationRepository, times(1))
            .update(
                argThat(
                    arg ->
                        arg != null &&
                        arg.getName().equals("org_name") &&
                        arg.getDescription().equals("org_desc") &&
                        arg.getDomainRestrictions().equals(domainRestrictions)
                )
            );
        verify(mockOrganizationRepository, never()).create(any());
        verify(mockRoleService, never()).initialize(GraviteeContext.getExecutionContext(), "org_id");
        verify(mockRoleService, never()).createOrUpdateSystemRoles(GraviteeContext.getExecutionContext(), "org_id");
        verify(mockFlowService, times(1)).save(FlowReferenceType.ORGANIZATION, "org_id", org1.getFlows());
        verify(eventService, times(1))
            .createOrganizationEvent(
                eq(GraviteeContext.getExecutionContext()),
                eq(Set.of("env_1", "env_2")),
                eq(EventType.PUBLISH_ORGANIZATION),
                any(),
                eq(Map.of("organization_id", "org_id"))
            );
    }
}
