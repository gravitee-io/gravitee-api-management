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
package io.gravitee.apim.infra.crud_service.workflow;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.workflow.crud_service.WorkflowCrudService;
import io.gravitee.apim.core.workflow.model.Workflow;
import io.gravitee.apim.infra.adapter.WorkflowAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.WorkflowRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class WorkflowCrudServiceImpl implements WorkflowCrudService {

    private final WorkflowRepository workflowRepository;

    public WorkflowCrudServiceImpl(@Lazy WorkflowRepository metadataRepository) {
        this.workflowRepository = metadataRepository;
    }

    public Workflow create(Workflow workflow) {
        try {
            var result = workflowRepository.create(WorkflowAdapter.INSTANCE.toRepository(workflow));
            return WorkflowAdapter.INSTANCE.toEntity(result);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format(
                    "An error occurs while trying to create the %s workflow of [%sId=%s]",
                    workflow.getType(),
                    workflow.getReferenceType().name().toLowerCase(),
                    workflow.getReferenceId()
                ),
                e
            );
        }
    }
}
