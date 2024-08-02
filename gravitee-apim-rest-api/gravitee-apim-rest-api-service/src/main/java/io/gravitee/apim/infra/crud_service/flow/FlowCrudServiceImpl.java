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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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

    @Override
    public List<Flow> saveApiFlows(String apiId, List<Flow> flows) {
        return save(FlowReferenceType.API, apiId, flows);
    }

    @Override
    public List<Flow> getApiV4Flows(String apiId) {
        return getV4(FlowReferenceType.API, apiId);
    }

    @Override
    public List<Flow> getPlanV4Flows(String planId) {
        return getV4(FlowReferenceType.PLAN, planId);
    }

    @Override
    public List<io.gravitee.definition.model.flow.Flow> getApiV2Flows(String apiId) {
        return getV2(FlowReferenceType.API, apiId);
    }

    @Override
    public List<io.gravitee.definition.model.flow.Flow> getPlanV2Flows(String planId) {
        return getV2(FlowReferenceType.PLAN, planId);
    }

    private List<Flow> save(FlowReferenceType flowReferenceType, String referenceId, List<Flow> flows) {
        try {
            log.debug("Save flows for reference {},{}", flowReferenceType, flowReferenceType);
            if (flows == null || flows.isEmpty()) {
                flowRepository.deleteByReferenceIdAndReferenceType(referenceId, flowReferenceType);
                return List.of();
            }
            Map<String, io.gravitee.repository.management.model.flow.Flow> dbFlowsById = flowRepository
                .findByReference(flowReferenceType, referenceId)
                .stream()
                .collect(Collectors.toMap(io.gravitee.repository.management.model.flow.Flow::getId, Function.identity()));

            Set<String> flowIdsToSave = flows.stream().map(Flow::getId).filter(Objects::nonNull).collect(Collectors.toSet());

            Set<String> flowIdsToDelete = dbFlowsById
                .keySet()
                .stream()
                .filter(Predicate.not(flowIdsToSave::contains))
                .collect(Collectors.toSet());
            if (!flowIdsToDelete.isEmpty()) {
                flowRepository.deleteAllById(flowIdsToDelete);
            }

            List<Flow> savedFlows = new ArrayList<>();
            io.gravitee.repository.management.model.flow.Flow dbFlow;
            for (int order = 0; order < flows.size(); ++order) {
                Flow flow = flows.get(order);
                if (flow.getId() == null || !dbFlowsById.containsKey(flow.getId())) {
                    dbFlow = flowRepository.create(FlowAdapter.INSTANCE.toRepository(flow, flowReferenceType, referenceId, order));
                } else {
                    dbFlow = flowRepository.update(FlowAdapter.INSTANCE.toRepositoryUpdate(dbFlowsById.get(flow.getId()), flow, order));
                }
                savedFlows.add(FlowAdapter.INSTANCE.toFlowV4(dbFlow));
            }
            return savedFlows;
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to save flows for " + flowReferenceType + ": " + referenceId;
            log.error(error, ex);
            throw new TechnicalDomainException(error, ex);
        }
    }

    private List<io.gravitee.definition.model.flow.Flow> getV2(FlowReferenceType flowReferenceType, String referenceId) {
        try {
            log.debug("Get flows for reference {},{}", flowReferenceType, flowReferenceType);
            return flowRepository
                .findByReference(flowReferenceType, referenceId)
                .stream()
                .sorted(Comparator.comparing(io.gravitee.repository.management.model.flow.Flow::getOrder))
                .map(FlowAdapter.INSTANCE::toFlowV2)
                .collect(Collectors.toList());
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to get flows for " + flowReferenceType + ": " + referenceId;
            log.error(error, ex);
            throw new TechnicalDomainException(error, ex);
        }
    }

    private List<Flow> getV4(FlowReferenceType flowReferenceType, String referenceId) {
        try {
            log.debug("Get flows for reference {},{}", flowReferenceType, flowReferenceType);
            return flowRepository
                .findByReference(flowReferenceType, referenceId)
                .stream()
                .sorted(Comparator.comparing(io.gravitee.repository.management.model.flow.Flow::getOrder))
                .map(FlowAdapter.INSTANCE::toFlowV4)
                .collect(Collectors.toList());
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to get flows for " + flowReferenceType + ": " + referenceId;
            log.error(error, ex);
            throw new TechnicalDomainException(error, ex);
        }
    }
}
