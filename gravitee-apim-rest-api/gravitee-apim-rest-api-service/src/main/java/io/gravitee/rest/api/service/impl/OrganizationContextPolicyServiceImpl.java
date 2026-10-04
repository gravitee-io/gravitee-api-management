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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.llm.ContextManagementPolicy;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.OrganizationContextPolicyService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.OrganizationNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class OrganizationContextPolicyServiceImpl extends TransactionalService implements OrganizationContextPolicyService {

    public static final String PARAMETER_KEY = "llm.context.policy";

    @Lazy
    @Autowired
    private OrganizationRepository organizationRepository;

    @Lazy
    @Autowired
    private ParameterRepository parameterRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditService auditService;

    @Override
    public ContextManagementPolicy find(final String organizationId) {
        ensureOrganizationExists(organizationId);
        try {
            return parameterRepository
                .findById(PARAMETER_KEY, organizationId, ParameterReferenceType.ORGANIZATION)
                .map(this::deserialize)
                .orElse(null);
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(
                "An error occurs while trying to find the context policy for organization " + organizationId,
                ex
            );
        }
    }

    @Override
    public ContextManagementPolicy save(
        final ExecutionContext executionContext,
        final String organizationId,
        final ContextManagementPolicy policy
    ) {
        ensureOrganizationExists(organizationId);
        if (policy == null) {
            throw new IllegalArgumentException("Context management policy must not be null");
        }

        final String value = serialize(policy);
        try {
            Optional<Parameter> existing = parameterRepository.findById(PARAMETER_KEY, organizationId, ParameterReferenceType.ORGANIZATION);
            Parameter parameter = Parameter.builder()
                .key(PARAMETER_KEY)
                .referenceId(organizationId)
                .referenceType(ParameterReferenceType.ORGANIZATION)
                .value(value)
                .build();
            Parameter persisted;
            if (existing.isPresent()) {
                persisted = parameterRepository.update(parameter);
                auditService.createAuditLog(
                    executionContext,
                    AuditService.AuditLogData.builder()
                        .properties(Map.of(PARAMETER, PARAMETER_KEY))
                        .event(PARAMETER_UPDATED)
                        .createdAt(new Date())
                        .oldValue(existing.get())
                        .newValue(persisted)
                        .build()
                );
            } else {
                persisted = parameterRepository.create(parameter);
                auditService.createAuditLog(
                    executionContext,
                    AuditService.AuditLogData.builder()
                        .properties(Map.of(PARAMETER, PARAMETER_KEY))
                        .event(PARAMETER_CREATED)
                        .createdAt(new Date())
                        .newValue(persisted)
                        .build()
                );
            }
            return deserialize(persisted);
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(
                "An error occurs while trying to save the context policy for organization " + organizationId,
                ex
            );
        }
    }

    @Override
    public void clear(final ExecutionContext executionContext, final String organizationId) {
        ensureOrganizationExists(organizationId);
        try {
            Optional<Parameter> existing = parameterRepository.findById(PARAMETER_KEY, organizationId, ParameterReferenceType.ORGANIZATION);
            if (existing.isEmpty()) {
                return;
            }
            parameterRepository.delete(PARAMETER_KEY, organizationId, ParameterReferenceType.ORGANIZATION);
            auditService.createAuditLog(
                executionContext,
                AuditService.AuditLogData.builder()
                    .properties(Map.of(PARAMETER, PARAMETER_KEY))
                    .event(PARAMETER_DELETED)
                    .createdAt(new Date())
                    .oldValue(existing.get())
                    .build()
            );
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(
                "An error occurs while trying to clear the context policy for organization " + organizationId,
                ex
            );
        }
    }

    private void ensureOrganizationExists(final String organizationId) {
        try {
            if (organizationRepository.findById(organizationId).isEmpty()) {
                throw new OrganizationNotFoundException(organizationId);
            }
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to find organization " + organizationId, ex);
        }
    }

    private String serialize(final ContextManagementPolicy policy) {
        try {
            return objectMapper.writeValueAsString(policy);
        } catch (JsonProcessingException ex) {
            throw new TechnicalManagementException("An error occurs while serializing the context management policy", ex);
        }
    }

    private ContextManagementPolicy deserialize(final Parameter parameter) {
        try {
            return objectMapper.readValue(parameter.getValue(), ContextManagementPolicy.class);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new TechnicalManagementException(
                "The stored context management policy for organization " + parameter.getReferenceId() + " is invalid",
                ex
            );
        }
    }
}
