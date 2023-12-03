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
package io.gravitee.apim.infra.crud_service.flow;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.infra.adapter.FlowAdapter;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.service.impl.TransactionalService;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FlowCrudServiceImpl extends TransactionalService implements FlowCrudService {

    private final FlowRepository flowRepository;

    public FlowCrudServiceImpl(@Lazy FlowRepository flowRepository) {
        this.flowRepository = flowRepository;
    }

    @Override
    public List<Flow> savePlanFlows(String planId, List<Flow> flows) {
        return save(FlowReferenceType.PLAN, planId, flows);
    }

    private List<Flow> save(FlowReferenceType referenceType, String referenceId, List<Flow> flows) {
        try {
            log.debug("Save flows for reference {} {}", referenceType, referenceId);
            flowRepository.deleteByReference(referenceType, referenceId);
            if (flows == null) {
                return List.of();
            }

            List<Flow> createdFlows = new ArrayList<>();
            for (int order = 0; order < flows.size(); ++order) {
                io.gravitee.repository.management.model.flow.Flow createdFlow = flowRepository.create(
                    FlowAdapter.INSTANCE.toRepository(flows.get(order), referenceType, referenceId, order)
                );
                createdFlows.add(FlowAdapter.INSTANCE.toFlowV4(createdFlow));
            }
            return createdFlows;
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to save flows for " + referenceType + ": " + referenceId;
            log.error(error, ex);
            throw new TechnicalDomainException(error, ex);
        }
    }
}
