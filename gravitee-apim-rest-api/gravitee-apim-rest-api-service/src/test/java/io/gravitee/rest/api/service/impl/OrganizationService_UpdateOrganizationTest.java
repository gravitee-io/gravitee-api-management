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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.exceptions.OrganizationNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
public class OrganizationService_UpdateOrganizationTest {

    private static final String ORG_ID = "orgid";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String COCKPIT_ID = "cockpitId";
    private static final String RANDOM_COCKPIT_ID = "randomCockpitId";
    private static final String FLOW_MODE = "DEFAULT";
    private static final List<String> HRIDS = Collections.singletonList("hrid");
    private static final List<String> DOMAINS = Collections.singletonList("domainRestrictions");

    @InjectMocks
    private OrganizationServiceImpl organizationService = new OrganizationServiceImpl();

    @Mock
    private OrganizationRepository mockOrganizationRepository;

    @Mock
    private EventService eventService;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private FlowService mockFlowService;

    @Mock
    private EnvironmentService environmentService;

    @Before
    public void setup() {
        GraviteeContext.setCurrentOrganization("orgid");
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldThrowOrganizationNotFoundException() throws TechnicalException {
        UpdateOrganizationEntity updateOrganizationEntity = new UpdateOrganizationEntity();
        updateOrganizationEntity.setCockpitId(COCKPIT_ID);

        when(mockOrganizationRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(
            OrganizationNotFoundException.class,
            () -> organizationService.updateOrganization(GraviteeContext.getExecutionContext(), updateOrganizationEntity)
        );
    }

    @Test
    public void shouldThrowTechnicalException() throws TechnicalException {
        UpdateOrganizationEntity updateOrganizationEntity = new UpdateOrganizationEntity();
        updateOrganizationEntity.setName(NAME);
        updateOrganizationEntity.setDescription(DESCRIPTION);
        updateOrganizationEntity.setHrids(HRIDS);
        updateOrganizationEntity.setDomainRestrictions(DOMAINS);
        updateOrganizationEntity.setCockpitId(COCKPIT_ID);

        Organization organization = new Organization();
        organization.setId(ORG_ID);
        organization.setCockpitId(RANDOM_COCKPIT_ID);
        organization.setFlowMode(FLOW_MODE);
        organization.setHrids(HRIDS);
        organization.setDomainRestrictions(DOMAINS);
        organization.setName(NAME);
        organization.setDescription(DESCRIPTION);

        when(mockOrganizationRepository.findById(any())).thenReturn(Optional.of(organization));
        when(mockOrganizationRepository.update(any())).thenThrow(new TechnicalException("Technical error"));

        assertThrows(
            TechnicalManagementException.class,
            () -> organizationService.updateOrganization(GraviteeContext.getExecutionContext(), updateOrganizationEntity)
        );
    }

    @Test
    public void shouldUpdateTechincalException() throws TechnicalException {
        UpdateOrganizationEntity updateOrganizationEntity = new UpdateOrganizationEntity();
        updateOrganizationEntity.setName(NAME);
        updateOrganizationEntity.setDescription(DESCRIPTION);
        updateOrganizationEntity.setHrids(HRIDS);
        updateOrganizationEntity.setDomainRestrictions(DOMAINS);
        updateOrganizationEntity.setCockpitId(COCKPIT_ID);

        Organization organization = new Organization();
        organization.setId(ORG_ID);
        organization.setCockpitId(RANDOM_COCKPIT_ID);
        organization.setFlowMode(FLOW_MODE);
        organization.setHrids(HRIDS);
        organization.setDomainRestrictions(DOMAINS);
        organization.setName(NAME);
        organization.setDescription(DESCRIPTION);
        when(mockOrganizationRepository.findById(any())).thenReturn(Optional.of(organization));

        Organization expectedOrganization = new Organization();
        expectedOrganization.setId(ORG_ID);
        expectedOrganization.setCockpitId(COCKPIT_ID);
        expectedOrganization.setFlowMode(FLOW_MODE);
        expectedOrganization.setHrids(HRIDS);
        expectedOrganization.setDomainRestrictions(DOMAINS);
        expectedOrganization.setName(NAME);
        expectedOrganization.setDescription(DESCRIPTION);

        when(mockFlowService.findByReference(FlowReferenceType.ORGANIZATION, ORG_ID)).thenReturn(Collections.emptyList());

        EnvironmentEntity env1 = new EnvironmentEntity();
        env1.setId("env_1");
        when(environmentService.findByOrganization(ORG_ID)).thenReturn(Collections.singletonList(env1));

        when(mockOrganizationRepository.update(any())).thenReturn(expectedOrganization);

        OrganizationEntity result = organizationService.updateOrganization(GraviteeContext.getExecutionContext(), updateOrganizationEntity);

        assertNotNull(result);
        assertEquals(ORG_ID, result.getId());
        assertEquals(COCKPIT_ID, result.getCockpitId());
        verify(mockOrganizationRepository)
            .update(
                argThat(
                    o -> {
                        assertEquals(COCKPIT_ID, o.getCockpitId());
                        assertEquals(FLOW_MODE, o.getFlowMode());
                        assertEquals(HRIDS, o.getHrids());
                        assertEquals(DOMAINS, o.getDomainRestrictions());
                        assertEquals(NAME, o.getName());
                        assertEquals(DESCRIPTION, o.getDescription());
                        return true;
                    }
                )
            );
    }
}
