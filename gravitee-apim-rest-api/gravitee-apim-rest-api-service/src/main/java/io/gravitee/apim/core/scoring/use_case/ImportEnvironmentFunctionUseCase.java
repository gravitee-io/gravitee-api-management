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
package io.gravitee.apim.core.scoring.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.scoring.crud_service.ScoringFunctionCrudService;
import io.gravitee.apim.core.scoring.model.ScoringFunction;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class ImportEnvironmentFunctionUseCase {

    private final ScoringFunctionCrudService scoringFunctionCrudService;

    public Output execute(Input input) {
        var created = scoringFunctionCrudService.create(
            new ScoringFunction(
                UuidString.generateRandom(),
                input.newFunction.name(),
                input.auditInfo.environmentId(),
                ScoringFunction.ReferenceType.ENVIRONMENT,
                input.newFunction.payload(),
                TimeProvider.now()
            )
        );
        return new Output(created.id());
    }

    public record Input(NewFunction newFunction, AuditInfo auditInfo) {}

    public record NewFunction(String name, String payload) {}

    public record Output(String functionId) {}
}
