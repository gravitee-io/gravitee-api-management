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
package io.gravitee.apim.infra.query_service.performance_target;

import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.core.performance_target.query_service.PerformanceTargetQueryService;
import io.gravitee.apim.infra.adapter.PerformanceTargetAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PerformanceTargetRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@CustomLog
public class PerformanceTargetQueryServiceImpl implements PerformanceTargetQueryService {

    private final PerformanceTargetRepository performanceTargetRepository;

    public PerformanceTargetQueryServiceImpl(@Lazy PerformanceTargetRepository performanceTargetRepository) {
        this.performanceTargetRepository = performanceTargetRepository;
    }

    @Override
    public List<PerformanceTarget> findByReference(String environmentId, String reference) {
        try {
            return performanceTargetRepository
                .findByReference(environmentId, reference)
                .stream()
                .map(PerformanceTargetAdapter.INSTANCE::toEntity)
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurred while finding Performance Targets by reference [%s/%s]".formatted(environmentId, reference),
                e
            );
        }
    }
}
