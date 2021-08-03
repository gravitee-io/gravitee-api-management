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
package io.gravitee.rest.api.service;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.impl.OrganizationServiceImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
    private ObjectMapper mapper;

    @Test
    public void shouldCreateOrganization() throws TechnicalException {
        when(mockOrganizationRepository.findById(any())).thenReturn(Optional.empty());

        UpdateOrganizationEntity org1 = new UpdateOrganizationEntity();
        org1.setHrids(Arrays.asList("orgid"));
        org1.setName("org_name");
        org1.setDescription("org_desc");
        List<String> domainRestrictions = Arrays.asList("domain", "restriction");
        org1.setDomainRestrictions(domainRestrictions);
        org1.setFlows(Arrays.asList());

        Organization createdOrganization = new Organization();
        createdOrganization.setId("org_id");
        when(mockOrganizationRepository.create(any())).thenReturn(createdOrganization);
        when(mockFlowService.findByReference(FlowReferenceType.ORGANIZATION, "org_id")).thenReturn(new ArrayList<>());

        OrganizationEntity organization = organizationService.createOrUpdate("org_id", org1);

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
        verify(mockRoleService, times(1)).initialize("org_id");
        verify(mockRoleService, times(1)).createOrUpdateSystemRoles("org_id");
        verify(mockFlowService, times(1)).save(FlowReferenceType.ORGANIZATION, "org_id", Arrays.asList());
    }

    @Test
    public void shouldUpdateOrganization() throws TechnicalException {
        when(mockOrganizationRepository.findById(any())).thenReturn(Optional.of(new Organization()));

        UpdateOrganizationEntity org1 = new UpdateOrganizationEntity();
        org1.setHrids(Arrays.asList("orgid"));
        org1.setName("org_name");
        org1.setDescription("org_desc");
        List<String> domainRestrictions = Arrays.asList("domain", "restriction");
        org1.setDomainRestrictions(domainRestrictions);
        org1.setFlows(Arrays.asList(mock(Flow.class)));

        Organization createdOrganization = new Organization();
        createdOrganization.setId("org_id");
        when(mockOrganizationRepository.update(any())).thenReturn(createdOrganization);
        when(mockFlowService.findByReference(FlowReferenceType.ORGANIZATION, "org_id")).thenReturn(org1.getFlows());

        OrganizationEntity organization = organizationService.createOrUpdate("org_id", org1);

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
        verify(mockRoleService, never()).initialize("org_id");
        verify(mockRoleService, never()).createOrUpdateSystemRoles("org_id");
        verify(mockFlowService, times(1)).save(FlowReferenceType.ORGANIZATION, "org_id", org1.getFlows());
    }
}
