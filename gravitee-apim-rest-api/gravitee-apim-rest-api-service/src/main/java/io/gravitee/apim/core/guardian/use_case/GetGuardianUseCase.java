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
package io.gravitee.apim.core.guardian.use_case;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.guardian.crud_service.GuardianCrudService;
import io.gravitee.apim.core.guardian.model.Guardian;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@DomainService
public class GetGuardianUseCase {

    private final GuardianCrudService guardianCrudService;

    public Output execute(Input input) {
        return new Output(guardianCrudService.getByEnvironmentId(input.environmentId(), input.guardianId()));
    }

    @Builder
    public record Input(String environmentId, String guardianId) {}

    public record Output(Guardian guardian) {}
}
