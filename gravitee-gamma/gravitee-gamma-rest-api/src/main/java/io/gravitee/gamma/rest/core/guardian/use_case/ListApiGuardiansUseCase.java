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
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.gamma.rest.core.guardian.domain_service.DescribeApiGuardiansDomainService;
import io.gravitee.gamma.rest.core.guardian.model.ApiGuardian;
import io.gravitee.gamma.rest.core.guardian.port.service_provider.GuardedApiPort;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Lists the Guardians enabled on an API, in the order they run.
 *
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class ListApiGuardiansUseCase {

    private final GuardedApiPort apis;
    private final DescribeApiGuardiansDomainService describeApiGuardians;

    public Output execute(Input input) {
        var api = apis.findById(input.environmentId(), input.apiId()).orElseThrow(() -> new ApiNotFoundException(input.apiId()));
        return new Output(describeApiGuardians.describe(input.environmentId(), api.guardians()));
    }

    public record Input(String environmentId, String apiId) {}

    public record Output(List<ApiGuardian> guardians) {}
}
