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
package io.gravitee.apim.core.log.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.log.crud_service.ConnectionLogsCrudService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;

@UseCase
public class SearchApiConnectionLogErrorKeysUseCase {

    private final ConnectionLogsCrudService connectionLogsCrudService;

    public SearchApiConnectionLogErrorKeysUseCase(ConnectionLogsCrudService connectionLogsCrudService) {
        this.connectionLogsCrudService = connectionLogsCrudService;
    }

    public Output execute(ExecutionContext executionContext, Input input) {
        return new Output(
            connectionLogsCrudService.searchApiConnectionLogErrorKeys(executionContext, input.apiId(), input.from(), input.to())
        );
    }

    public record Input(String apiId, Long from, Long to) {}

    public record Output(List<String> errorKeys) {}
}
