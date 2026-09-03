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
package io.gravitee.apim.infra.crud_service.performance_target;

import io.gravitee.apim.core.performance_target.crud_service.PerformanceTargetCrudService;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.infra.adapter.PerformanceTargetAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PerformanceTargetRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@CustomLog
@Service
public class PerformanceTargetCrudServiceImpl extends AbstractService implements PerformanceTargetCrudService {

    private final PerformanceTargetRepository performanceTargetRepository;

    public PerformanceTargetCrudServiceImpl(@Lazy PerformanceTargetRepository performanceTargetRepository) {
        this.performanceTargetRepository = performanceTargetRepository;
    }

    @Override
    public PerformanceTarget create(PerformanceTarget target) {
        try {
            var created = performanceTargetRepository.create(PerformanceTargetAdapter.INSTANCE.toRepository(target));
            return PerformanceTargetAdapter.INSTANCE.toEntity(created);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when creating Performance Target: " + target.id(), e);
        }
    }

    @Override
    public Optional<PerformanceTarget> findById(String id) {
        try {
            return performanceTargetRepository.findById(id).map(PerformanceTargetAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when searching for Performance Target: " + id, e);
        }
    }

    @Override
    public PerformanceTarget update(PerformanceTarget target) {
        try {
            var updated = performanceTargetRepository.update(PerformanceTargetAdapter.INSTANCE.toRepository(target));
            return PerformanceTargetAdapter.INSTANCE.toEntity(updated);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when updating Performance Target: " + target.id(), e);
        }
    }

    @Override
    public void delete(String id) {
        try {
            performanceTargetRepository.delete(id);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when deleting Performance Target: " + id, e);
        }
    }

    @Override
    public List<String> deleteByReference(String environmentId, String reference) {
        try {
            return performanceTargetRepository.deleteByReference(environmentId, reference);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "Error when deleting Performance Targets for reference [%s/%s]".formatted(environmentId, reference),
                e
            );
        }
    }

    @Override
    public List<String> removeApiId(String apiId) {
        try {
            return performanceTargetRepository.removeApiId(apiId);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when removing API from Performance Targets: " + apiId, e);
        }
    }
}
