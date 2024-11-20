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
package io.gravitee.apim.infra.query_service.workflow;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.workflow.model.Workflow;
import io.gravitee.apim.core.workflow.query_service.WorkflowQueryService;
import io.gravitee.apim.infra.adapter.WorkflowAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.WorkflowRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WorkflowQueryServiceImpl implements WorkflowQueryService {

    private final WorkflowRepository workflowRepository;

    public WorkflowQueryServiceImpl(@Lazy WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @Override
    public List<Workflow> findAllByApiIdAndType(String apiId, Workflow.Type type) {
        try {
            return WorkflowAdapter.INSTANCE.toEntities(workflowRepository.findByReferenceAndType("API", apiId, type.name()));
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occurred while finding [API] workflows with apiId [%s] and status [%s]", apiId, type.name()),
                e
            );
        }
    }
}
