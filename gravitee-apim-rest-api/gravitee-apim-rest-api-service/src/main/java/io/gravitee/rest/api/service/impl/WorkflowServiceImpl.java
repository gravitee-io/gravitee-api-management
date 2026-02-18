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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.WorkflowRepository;
import io.gravitee.repository.management.model.Workflow;
import io.gravitee.rest.api.model.WorkflowReferenceType;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.WorkflowType;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class WorkflowServiceImpl extends TransactionalService implements WorkflowService {

    @Lazy
    @Autowired
    private WorkflowRepository workflowRepository;

    @Override
    public Workflow create(
        final WorkflowReferenceType referenceType,
        final String referenceId,
        final WorkflowType type,
        final String user,
        final WorkflowState state,
        final String comment
    ) {
        final Workflow workflow = new Workflow();
        workflow.setId(UuidString.generateRandom());
        workflow.setReferenceType(referenceType.name());
        workflow.setReferenceId(referenceId);
        workflow.setType(type.name());
        workflow.setUser(user);
        workflow.setState(state.name());
        workflow.setComment(comment);
        workflow.setCreatedAt(new Date());
        try {
            return workflowRepository.create(workflow);
        } catch (TechnicalException ex) {
            final String message = "An error occurs while trying to create workflow of type " + workflow.getType();
            log.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public List<Workflow> findByReferenceAndType(
        final WorkflowReferenceType referenceType,
        final String referenceId,
        final WorkflowType type
    ) {
        try {
            return workflowRepository.findByReferenceAndType(referenceType.name(), referenceId, type.name());
        } catch (TechnicalException ex) {
            final String message =
                "An error occurs while trying to find workflow by ref " + referenceType + "/" + referenceId + " and type " + type;
            log.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }

    @Override
    public List<Workflow> findByReferencesAndType(
        final WorkflowReferenceType referenceType,
        final Collection<String> referenceIds,
        final WorkflowType type
    ) {
        if (referenceIds == null || referenceIds.isEmpty()) {
            return List.of();
        }
        try {
            return workflowRepository.findByReferencesAndType(referenceType.name(), referenceIds, type.name());
        } catch (TechnicalException ex) {
            final String message =
                "An error occurs while trying to find workflows by refs " + referenceType + "/" + referenceIds + " and type " + type;
            log.error(message, ex);
            throw new TechnicalManagementException(message, ex);
        }
    }
}
