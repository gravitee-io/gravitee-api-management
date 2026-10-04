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

import static io.gravitee.repository.management.model.Audit.AuditProperties.PARAMETER;
import static io.gravitee.repository.management.model.Parameter.AuditEvent.PARAMETER_CREATED;
import static io.gravitee.repository.management.model.Parameter.AuditEvent.PARAMETER_DELETED;
import static io.gravitee.repository.management.model.Parameter.AuditEvent.PARAMETER_UPDATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.llm.ContextManagementPolicy;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.OrganizationNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationContextPolicyServiceImplTest {

    private static final String ORGANIZATION_ID = "org-1";

    @InjectMocks
    private OrganizationContextPolicyServiceImpl service;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ParameterRepository parameterRepository;

    @Mock
    private AuditService auditService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws TechnicalException {
        when(organizationRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(new Organization()));
    }

    @Test
    void shouldCreateCanonicalPolicyAndAudit() throws TechnicalException {
        ContextManagementPolicy policy = new ContextManagementPolicy(ContextManagementPolicy.Mode.ENFORCE, 2048, null);
        when(parameterRepository.findById(any(), eq(ORGANIZATION_ID), eq(ParameterReferenceType.ORGANIZATION))).thenReturn(
            Optional.empty()
        );
        when(parameterRepository.create(any(Parameter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContextManagementPolicy result = service.save(new ExecutionContext(ORGANIZATION_ID), ORGANIZATION_ID, policy);

        assertThat(result).isEqualTo(policy);
        verify(parameterRepository).create(
            argThat(
                parameter ->
                    parameter.getKey().equals(OrganizationContextPolicyServiceImpl.PARAMETER_KEY) &&
                    parameter.getReferenceId().equals(ORGANIZATION_ID) &&
                    parameter.getReferenceType() == ParameterReferenceType.ORGANIZATION &&
                    parameter.getValue().equals("{\"mode\":\"ENFORCE\",\"compactThreshold\":2048}")
            )
        );
        verify(auditService).createAuditLog(
            eq(new ExecutionContext(ORGANIZATION_ID)),
            argThat(
                audit ->
                    audit.getProperties().equals(Map.of(PARAMETER, OrganizationContextPolicyServiceImpl.PARAMETER_KEY)) &&
                    audit.getEvent() == PARAMETER_CREATED &&
                    audit.getOldValue() == null
            )
        );
    }

    @Test
    void shouldUpdateExistingPolicyAndAuditPreviousValue() throws TechnicalException {
        Parameter existing = parameter(ContextManagementPolicy.Mode.DEFAULT, 1024);
        when(parameterRepository.findById(any(), eq(ORGANIZATION_ID), eq(ParameterReferenceType.ORGANIZATION))).thenReturn(
            Optional.of(existing)
        );
        when(parameterRepository.update(any(Parameter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContextManagementPolicy policy = new ContextManagementPolicy(ContextManagementPolicy.Mode.PASSTHROUGH, null, null);
        assertThat(service.save(new ExecutionContext(ORGANIZATION_ID), ORGANIZATION_ID, policy)).isEqualTo(policy);

        verify(parameterRepository).update(argThat(parameter -> parameter.getValue().equals("{\"mode\":\"PASSTHROUGH\"}")));
        verify(auditService).createAuditLog(
            eq(new ExecutionContext(ORGANIZATION_ID)),
            argThat(audit -> audit.getEvent() == PARAMETER_UPDATED && audit.getOldValue() == existing)
        );
    }

    @Test
    void shouldReadAndClearOnlyTheOrganizationPolicy() throws TechnicalException {
        Parameter existing = parameter(ContextManagementPolicy.Mode.DEFAULT, 1024);
        when(parameterRepository.findById(any(), eq(ORGANIZATION_ID), eq(ParameterReferenceType.ORGANIZATION))).thenReturn(
            Optional.of(existing)
        );

        assertThat(service.find(ORGANIZATION_ID)).isEqualTo(new ContextManagementPolicy(ContextManagementPolicy.Mode.DEFAULT, 1024, null));

        service.clear(new ExecutionContext(ORGANIZATION_ID), ORGANIZATION_ID);

        verify(parameterRepository).delete(
            OrganizationContextPolicyServiceImpl.PARAMETER_KEY,
            ORGANIZATION_ID,
            ParameterReferenceType.ORGANIZATION
        );
        verify(auditService).createAuditLog(
            eq(new ExecutionContext(ORGANIZATION_ID)),
            argThat(audit -> audit.getEvent() == PARAMETER_DELETED && audit.getOldValue() == existing)
        );
    }

    @Test
    void shouldTreatMissingPolicyAsEmptyAndDeleteIdempotently() throws TechnicalException {
        when(parameterRepository.findById(any(), eq(ORGANIZATION_ID), eq(ParameterReferenceType.ORGANIZATION))).thenReturn(
            Optional.empty()
        );

        assertThat(service.find(ORGANIZATION_ID)).isNull();
        service.clear(new ExecutionContext(ORGANIZATION_ID), ORGANIZATION_ID);

        verify(parameterRepository, never()).delete(any(), any(), any());
        verifyNoInteractions(auditService);
    }

    @Test
    void shouldRejectMissingOrganizationAndInvalidStoredPolicy() throws TechnicalException {
        when(organizationRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.find("missing")).isInstanceOf(OrganizationNotFoundException.class);

        Parameter invalid = Parameter.builder()
            .key(OrganizationContextPolicyServiceImpl.PARAMETER_KEY)
            .referenceId(ORGANIZATION_ID)
            .referenceType(ParameterReferenceType.ORGANIZATION)
            .value("{\"mode\":\"PASSTHROUGH\",\"compactThreshold\":10}")
            .build();
        when(parameterRepository.findById(any(), eq(ORGANIZATION_ID), eq(ParameterReferenceType.ORGANIZATION))).thenReturn(
            Optional.of(invalid)
        );

        assertThatThrownBy(() -> service.find(ORGANIZATION_ID)).isInstanceOf(TechnicalManagementException.class);
    }

    private Parameter parameter(ContextManagementPolicy.Mode mode, Integer threshold) {
        String value = threshold == null
            ? "{\"mode\":\"" + mode + "\"}"
            : "{\"mode\":\"" + mode + "\",\"compactThreshold\":" + threshold + "}";
        return Parameter.builder()
            .key(OrganizationContextPolicyServiceImpl.PARAMETER_KEY)
            .referenceId(ORGANIZATION_ID)
            .referenceType(ParameterReferenceType.ORGANIZATION)
            .value(value)
            .build();
    }
}
