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
package io.gravitee.apim.core.performance_target.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.performance_target.crud_service.PerformanceTargetCrudService;
import io.gravitee.apim.core.performance_target.domain_service.ValidatePerformanceTargetDomainService;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class CreatePerformanceTargetUseCase {

    private final PerformanceTargetCrudService performanceTargetCrudService;
    private final ValidatePerformanceTargetDomainService validatePerformanceTargetDomainService;

    public Output execute(Input input) {
        var now = TimeProvider.now();
        var target = input
            .target()
            .toBuilder()
            .id(UuidString.generateRandom())
            .environmentId(input.auditInfo().environmentId())
            .createdAt(now)
            .updatedAt(now)
            .build();
        validatePerformanceTargetDomainService.validate(target);
        return new Output(performanceTargetCrudService.create(target));
    }

    /**
     * @param target the target to create; its id, environment and timestamps are set by the use case
     */
    public record Input(PerformanceTarget target, AuditInfo auditInfo) {}

    public record Output(PerformanceTarget target) {}
}
