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

import static io.gravitee.apim.core.utils.CollectionUtils.isEmpty;
import static io.gravitee.apim.core.utils.CollectionUtils.stream;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.infra.adapter.FlowAdapter;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
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
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
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
        return FlowAdapter.INSTANCE.toFlowV4(save(FlowReferenceType.PLAN, planId, flows));
    }

    public void savePlanFlowsV2(String planId, List<io.gravitee.definition.model.flow.Flow> flows) {
        saveV2(FlowReferenceType.PLAN, planId, flows);
    }

    public void saveApiFlowsV2(String apiId, List<io.gravitee.definition.model.flow.Flow> flows) {
        saveV2(FlowReferenceType.API, apiId, flows);
    }

    @Override
    public List<Flow> saveApiFlows(String apiId, List<Flow> flows) {
        return FlowAdapter.INSTANCE.toFlowV4(save(FlowReferenceType.API, apiId, flows));
    }

    @Override
    public List<Flow> getApiV4Flows(String apiId) {
        return getHttpV4(FlowReferenceType.API, apiId);
    }

    @Override
    public List<Flow> getPlanV4Flows(String planId) {
        return getHttpV4(FlowReferenceType.PLAN, planId);
    }

    @Override
    public List<io.gravitee.definition.model.flow.Flow> getApiV2Flows(String apiId) {
        return getV2(FlowReferenceType.API, apiId);
    }

    @Override
    public List<io.gravitee.definition.model.flow.Flow> getPlanV2Flows(String planId) {
        return getV2(FlowReferenceType.PLAN, planId);
    }

    @Override
    public List<NativeFlow> saveNativeApiFlows(String apiId, List<NativeFlow> flows) {
        return FlowAdapter.INSTANCE.toNativeFlow(save(FlowReferenceType.API, apiId, flows));
    }

    @Override
    public List<NativeFlow> saveNativePlanFlows(String planId, List<NativeFlow> flows) {
        return FlowAdapter.INSTANCE.toNativeFlow(save(FlowReferenceType.PLAN, planId, flows));
    }

    @Override
    public List<NativeFlow> getNativeApiFlows(String apiId) {
        return getNativeV4(FlowReferenceType.API, apiId);
    }

    @Override
    public List<NativeFlow> getNativePlanFlows(String planId) {
        return getNativeV4(FlowReferenceType.PLAN, planId);
    }

    private List<io.gravitee.repository.management.model.flow.Flow> save(
        FlowReferenceType flowReferenceType,
        String referenceId,
        List<? extends AbstractFlow> flows
    ) {
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

            Set<String> flowIdsToSave = flows.stream().map(AbstractFlow::getId).filter(Objects::nonNull).collect(Collectors.toSet());

            Set<String> flowIdsToDelete = dbFlowsById
                .keySet()
                .stream()
                .filter(Predicate.not(flowIdsToSave::contains))
                .collect(Collectors.toSet());
            if (!flowIdsToDelete.isEmpty()) {
                flowRepository.deleteAllById(flowIdsToDelete);
            }

            List<io.gravitee.repository.management.model.flow.Flow> savedFlows = new ArrayList<>();
            io.gravitee.repository.management.model.flow.Flow dbFlow;
            for (int order = 0; order < flows.size(); ++order) {
                var flow = flows.get(order);
                if (flow.getId() == null || !dbFlowsById.containsKey(flow.getId())) {
                    dbFlow =
                        flowRepository.create(FlowAdapter.INSTANCE.toRepositoryFromAbstract(flow, flowReferenceType, referenceId, order));
                } else {
                    dbFlow =
                        flowRepository.update(
                            FlowAdapter.INSTANCE.toRepositoryUpdateFromAbstract(dbFlowsById.get(flow.getId()), flow, order)
                        );
                }
                savedFlows.add(dbFlow);
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

    private List<Flow> getHttpV4(FlowReferenceType flowReferenceType, String referenceId) {
        return getRepositoryV4(flowReferenceType, referenceId).map(FlowAdapter.INSTANCE::toFlowV4).collect(Collectors.toList());
    }

    private List<NativeFlow> getNativeV4(FlowReferenceType flowReferenceType, String referenceId) {
        return getRepositoryV4(flowReferenceType, referenceId).map(FlowAdapter.INSTANCE::toNativeFlow).collect(Collectors.toList());
    }

    private Stream<io.gravitee.repository.management.model.flow.Flow> getRepositoryV4(
        FlowReferenceType flowReferenceType,
        String referenceId
    ) {
        try {
            log.debug("Get flows for reference {},{}", flowReferenceType, flowReferenceType);
            return flowRepository
                .findByReference(flowReferenceType, referenceId)
                .stream()
                .sorted(Comparator.comparing(io.gravitee.repository.management.model.flow.Flow::getOrder));
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to get flows for " + flowReferenceType + ": " + referenceId;
            log.error(error, ex);
            throw new TechnicalDomainException(error, ex);
        }
    }

    private void saveV2(
        FlowReferenceType flowReferenceType,
        String referenceId,
        @Nullable List<io.gravitee.definition.model.flow.Flow> flows
    ) {
        try {
            log.debug("Save flows for reference {},{}", flowReferenceType, flowReferenceType);
            if (isEmpty(flows)) {
                flowRepository.deleteByReferenceIdAndReferenceType(referenceId, flowReferenceType);
                return;
            }
            var dbFlowsById = stream(flowRepository.findByReference(flowReferenceType, referenceId))
                .collect(Collectors.toMap(io.gravitee.repository.management.model.flow.Flow::getId, Function.identity()));

            var flowIdsToSave = flows.stream().flatMap(f -> Stream.ofNullable(f.getId())).collect(Collectors.toSet());

            var flowIdsToDelete = dbFlowsById.keySet().stream().filter(Predicate.not(flowIdsToSave::contains)).collect(Collectors.toSet());
            if (!flowIdsToDelete.isEmpty()) {
                flowRepository.deleteAllById(flowIdsToDelete);
            }

            for (int order = 0; order < flows.size(); ++order) {
                var flow = flows.get(order);
                if (flow.getId() == null || !dbFlowsById.containsKey(flow.getId())) {
                    flowRepository.create(FlowAdapter.INSTANCE.toRepository(flow, flowReferenceType, referenceId, order));
                } else {
                    flowRepository.update(FlowAdapter.INSTANCE.toRepositoryUpdate(dbFlowsById.get(flow.getId()), flow, order));
                }
            }
        } catch (TechnicalException ex) {
            final String error = "An error occurs while trying to save flows for " + flowReferenceType + ": " + referenceId;
            throw new TechnicalDomainException(error, ex);
        }
    }
}
