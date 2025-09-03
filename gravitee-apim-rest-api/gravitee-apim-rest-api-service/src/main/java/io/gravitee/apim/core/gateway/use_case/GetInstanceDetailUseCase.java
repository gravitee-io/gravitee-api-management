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
package io.gravitee.apim.core.gateway.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.gateway.model.BaseInstance;
import io.gravitee.apim.core.gateway.query_service.InstanceQueryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetInstanceDetailUseCase {

    private final InstanceQueryService instanceQueryService;

    public Output execute(Input input) {
        var instance = instanceQueryService.findById(input.executionContext(), input.instanceId());
        return new Output(Optional.ofNullable(instance));
    }

    public record Input(ExecutionContext executionContext, String instanceId) {}

    public record Output(Optional<BaseInstance> instance) {
        public Output(BaseInstance instance) {
            this(Optional.ofNullable(instance));
        }
    }
}
