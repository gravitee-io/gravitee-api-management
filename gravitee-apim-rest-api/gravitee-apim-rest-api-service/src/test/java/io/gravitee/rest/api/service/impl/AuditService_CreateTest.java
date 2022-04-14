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

import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Collections;
import java.util.Date;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AuditService_CreateTest {

    @InjectMocks
    private AuditServiceImpl auditService = new AuditServiceImpl();

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private AuditRepository auditRepository;

    @Test
    public void should_createApiAuditLog() throws TechnicalException {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        String apiId = "apiId";
        Date createdAt = new Date(1486771200000L);

        when(auditRepository.create(any())).thenReturn(new Audit());

        auditService.createApiAuditLog(
            executionContext,
            apiId,
            Collections.singletonMap(Audit.AuditProperties.PLAN, "123"),
            Plan.AuditEvent.PLAN_CREATED,
            createdAt,
            singletonMap("name", "Joe"),
            singletonMap("name", "Bar")
        );

        verify(auditRepository, times(1))
            .create(
                argThat(
                    arg ->
                        arg != null &&
                        !arg.getId().isEmpty() &&
                        arg.getOrganizationId().equals("DEFAULT") &&
                        arg.getEnvironmentId().equals("DEFAULT") &&
                        arg.getReferenceType().equals(Audit.AuditReferenceType.API) &&
                        arg.getReferenceId().equals(apiId) &&
                        arg.getCreatedAt().equals(createdAt) &&
                        arg.getProperties().equals(Collections.singletonMap(Audit.AuditProperties.PLAN.name(), "123")) &&
                        arg.getPatch().toString().equals("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Bar\"}]")
                )
            );
    }

    @Test
    public void should_createApplicationAuditLog() throws TechnicalException {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        String applicationId = "applicationId";
        Date createdAt = new Date(1486771200000L);

        when(auditRepository.create(any())).thenReturn(new Audit());

        auditService.createApplicationAuditLog(
            executionContext,
            applicationId,
            Collections.singletonMap(Audit.AuditProperties.PLAN, "123"),
            Plan.AuditEvent.PLAN_CREATED,
            createdAt,
            singletonMap("name", "Joe"),
            singletonMap("name", "Bar")
        );

        verify(auditRepository, times(1))
            .create(
                argThat(
                    arg ->
                        arg != null &&
                        !arg.getId().isEmpty() &&
                        arg.getOrganizationId().equals("DEFAULT") &&
                        arg.getEnvironmentId().equals("DEFAULT") &&
                        arg.getReferenceType().equals(Audit.AuditReferenceType.APPLICATION) &&
                        arg.getReferenceId().equals(applicationId) &&
                        arg.getCreatedAt().equals(createdAt) &&
                        arg.getProperties().equals(Collections.singletonMap(Audit.AuditProperties.PLAN.name(), "123")) &&
                        arg.getPatch().toString().equals("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Bar\"}]")
                )
            );
    }

    @Test
    public void should_createAuditLog_with_AuditReferenceType_ENVIRONMENT() throws TechnicalException {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        Date createdAt = new Date(1486771200000L);

        when(auditRepository.create(any())).thenReturn(new Audit());

        auditService.createAuditLog(
            executionContext,
            Collections.singletonMap(Audit.AuditProperties.PLAN, "123"),
            Plan.AuditEvent.PLAN_CREATED,
            createdAt,
            singletonMap("name", "Joe"),
            singletonMap("name", "Bar")
        );

        verify(auditRepository, times(1))
            .create(
                argThat(
                    arg ->
                        arg != null &&
                        !arg.getId().isEmpty() &&
                        arg.getOrganizationId().equals("DEFAULT") &&
                        arg.getEnvironmentId().equals("DEFAULT") &&
                        arg.getReferenceType().equals(Audit.AuditReferenceType.ENVIRONMENT) &&
                        arg.getReferenceId().equals(executionContext.getEnvironmentId()) &&
                        arg.getCreatedAt().equals(createdAt) &&
                        arg.getProperties().equals(Collections.singletonMap(Audit.AuditProperties.PLAN.name(), "123")) &&
                        arg.getPatch().toString().equals("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Bar\"}]")
                )
            );
    }

    @Test
    public void should_createAuditLog_with_AuditReferenceType_ORGANIZATION() throws TechnicalException {
        ExecutionContext executionContext = new ExecutionContext("organizationId", null);
        Date createdAt = new Date(1486771200000L);

        when(auditRepository.create(any())).thenReturn(new Audit());

        auditService.createAuditLog(
            executionContext,
            Collections.singletonMap(Audit.AuditProperties.PLAN, "123"),
            Plan.AuditEvent.PLAN_CREATED,
            createdAt,
            singletonMap("name", "Joe"),
            singletonMap("name", "Bar")
        );

        verify(auditRepository, times(1))
            .create(
                argThat(
                    arg ->
                        arg != null &&
                        !arg.getId().isEmpty() &&
                        arg.getOrganizationId().equals(executionContext.getOrganizationId()) &&
                        arg.getEnvironmentId() == null &&
                        arg.getReferenceType().equals(Audit.AuditReferenceType.ORGANIZATION) &&
                        arg.getReferenceId().equals(executionContext.getOrganizationId()) &&
                        arg.getCreatedAt().equals(createdAt) &&
                        arg.getProperties().equals(Collections.singletonMap(Audit.AuditProperties.PLAN.name(), "123")) &&
                        arg.getPatch().toString().equals("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Bar\"}]")
                )
            );
    }
}
