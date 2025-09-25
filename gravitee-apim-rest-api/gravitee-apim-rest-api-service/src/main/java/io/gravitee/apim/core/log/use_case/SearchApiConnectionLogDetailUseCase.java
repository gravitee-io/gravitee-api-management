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
import io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Optional;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
public class SearchApiConnectionLogDetailUseCase {

    private final ConnectionLogsCrudService connectionLogsCrudService;

    public SearchApiConnectionLogDetailUseCase(ConnectionLogsCrudService connectionLogsCrudService) {
        this.connectionLogsCrudService = connectionLogsCrudService;
    }

    public Output execute(ExecutionContext executionContext, Input input) {
        return connectionLogsCrudService
            .searchApiConnectionLog(executionContext, input.apiId(), input.requestId())
            .map(Output::new)
            .orElse(new Output());
    }

    public record Input(String apiId, String requestId) {}

    public record Output(Optional<ConnectionLogDetail> connectionLogDetail) {
        Output(ConnectionLogDetail connectionLogDetail) {
            this(Optional.of(connectionLogDetail));
        }

        Output() {
            this(Optional.empty());
        }
    }
}
