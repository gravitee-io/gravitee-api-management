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
package io.gravitee.apim.infra.crud_service.plan;

import io.gravitee.apim.core.plan.crud_service.KafkaPortRangeCrudService;
import io.gravitee.apim.core.plan.model.KafkaPortRange;
import io.gravitee.apim.infra.adapter.KafkaPortRangeAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.KafkaPortRangeRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class KafkaPortRangeCrudServiceImpl implements KafkaPortRangeCrudService {

    private final KafkaPortRangeRepository repository;

    public KafkaPortRangeCrudServiceImpl(@Lazy KafkaPortRangeRepository repository) {
        this.repository = repository;
    }

    @Override
    public KafkaPortRange create(KafkaPortRange portRange) {
        try {
            var saved = repository.create(KafkaPortRangeAdapter.INSTANCE.toRepository(portRange));
            return KafkaPortRangeAdapter.INSTANCE.fromRepository(saved);
        } catch (TechnicalException e) {
            throw TechnicalManagementException.ofTryingToCreateWithId(KafkaPortRange.class, portRange.getPlanId(), e);
        }
    }

    @Override
    public KafkaPortRange update(KafkaPortRange portRange) {
        try {
            var saved = repository.update(KafkaPortRangeAdapter.INSTANCE.toRepository(portRange));
            return KafkaPortRangeAdapter.INSTANCE.fromRepository(saved);
        } catch (TechnicalException e) {
            throw TechnicalManagementException.ofTryingToUpdateWithId(KafkaPortRange.class, portRange.getPlanId(), e);
        }
    }

    @Override
    public Optional<KafkaPortRange> findByPlanId(String planId) {
        try {
            return repository.findById(planId).map(KafkaPortRangeAdapter.INSTANCE::fromRepository);
        } catch (TechnicalException e) {
            throw TechnicalManagementException.ofTryingToFindById(KafkaPortRange.class, planId, e);
        }
    }

    @Override
    public void delete(String planId) {
        try {
            repository.delete(planId);
        } catch (TechnicalException e) {
            throw TechnicalManagementException.ofTryingToDeleteWithId(KafkaPortRange.class, planId, e);
        }
    }

    @Override
    public void deleteByApiId(String apiId) {
        try {
            repository.deleteByApiId(apiId);
        } catch (TechnicalException e) {
            throw TechnicalManagementException.ofTryingToDeleteWithId(KafkaPortRange.class, apiId, e);
        }
    }

    @Override
    public List<KafkaPortRange> findConflicting(
        String environmentId,
        String shardingTag,
        int bootstrapPort,
        int rangeStart,
        int rangeEnd,
        String excludePlanId
    ) {
        try {
            return repository
                .findConflicting(environmentId, shardingTag, bootstrapPort, rangeStart, rangeEnd, excludePlanId)
                .stream()
                .map(KafkaPortRangeAdapter.INSTANCE::fromRepository)
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Failed to query conflicting kafka port ranges", e);
        }
    }

    @Override
    public List<KafkaPortRange> findConflictingForUpdate(
        String environmentId,
        String shardingTag,
        int bootstrapPort,
        int rangeStart,
        int rangeEnd,
        String excludePlanId
    ) {
        try {
            return repository
                .findConflictingForUpdate(environmentId, shardingTag, bootstrapPort, rangeStart, rangeEnd, excludePlanId)
                .stream()
                .map(KafkaPortRangeAdapter.INSTANCE::fromRepository)
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Failed to query conflicting kafka port ranges for update", e);
        }
    }
}
