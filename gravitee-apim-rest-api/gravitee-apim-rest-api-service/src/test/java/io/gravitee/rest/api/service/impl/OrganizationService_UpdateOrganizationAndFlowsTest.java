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
package io.gravitee.rest.api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.UpdateOrganizationEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class OrganizationService_UpdateOrganizationAndFlowsTest {

    private static final String ORG_ID = "orgid";

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

    @Mock
    private AuditService mockAuditService;

    @BeforeEach
    public void setup() {
        GraviteeContext.setCurrentOrganization("orgid");
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldCreateAuditLogWhenUpdatingOrganizationFlows() throws TechnicalException {
        Organization existingOrg = new Organization();
        existingOrg.setId(ORG_ID);
        existingOrg.setName("My Org");
        when(mockOrganizationRepository.findById(ORG_ID)).thenReturn(Optional.of(existingOrg));

        List<Flow> previousFlows = Collections.emptyList();
        List<Flow> newFlows = List.of(mock(Flow.class));
        when(mockFlowService.findByReference(FlowReferenceType.ORGANIZATION, ORG_ID)).thenReturn(previousFlows).thenReturn(newFlows);

        UpdateOrganizationEntity updateEntity = new UpdateOrganizationEntity();
        updateEntity.setName("My Org");
        updateEntity.setFlows(newFlows);

        Organization updatedOrg = new Organization();
        updatedOrg.setId(ORG_ID);
        updatedOrg.setName("My Org");
        when(mockOrganizationRepository.update(any())).thenReturn(updatedOrg);

        EnvironmentEntity env = new EnvironmentEntity();
        env.setId("env-1");
        when(environmentService.findByOrganization(ORG_ID)).thenReturn(List.of(env));

        OrganizationEntity result = organizationService.updateOrganizationAndFlows(ORG_ID, updateEntity);

        assertNotNull(result);
        verify(mockFlowService).save(FlowReferenceType.ORGANIZATION, ORG_ID, newFlows);
        verify(mockAuditService).createOrganizationAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(auditLogData -> {
                assertEquals(Organization.AuditEvent.ORGANIZATION_FLOWS_UPDATED, auditLogData.getEvent());
                assertEquals(previousFlows, auditLogData.getOldValue());
                assertEquals(newFlows, auditLogData.getNewValue());
                return true;
            })
        );
    }
}
