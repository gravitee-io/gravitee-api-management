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
package io.gravitee.gamma.rest.core.guardian.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.model.ApiReference;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import io.gravitee.gamma.rest.core.guardian.port.service_provider.GuardedApiPort;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Lists the APIs of the environment a Guardian is enabled on, by name: those whose current definition runs it,
 * deployed or not.
 *
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class ListGuardianApisUseCase {

    private final GuardianRepository guardianRepository;
    private final GuardedApiPort apis;

    public Output execute(Input input) {
        var guardian = guardianRepository
            .findByEnvironmentId(input.environmentId(), input.guardianId())
            .orElseThrow(() -> new GuardianNotFoundException(input.guardianId()));
        var guardianApis = apis
            .findByGuardian(input.environmentId(), guardian.getId())
            .stream()
            .sorted(Comparator.comparing(ApiReference::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .toList();
        return new Output(guardianApis);
    }

    public record Input(String environmentId, String guardianId) {}

    public record Output(List<ApiReference> apis) {}
}
