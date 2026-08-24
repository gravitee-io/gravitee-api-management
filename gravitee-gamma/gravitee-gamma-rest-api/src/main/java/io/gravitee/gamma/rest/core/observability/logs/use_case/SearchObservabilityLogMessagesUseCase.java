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
package io.gravitee.gamma.rest.core.observability.logs.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.observability.logs.model.MessageLogsPage;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort;
import lombok.AllArgsConstructor;

/**
 * Pages through the messages of a single connection, for the message-level trace of a Message API
 * log detail. The caller is authorized against the API at the resource boundary, as for the log
 * detail itself; this use case only bounds the page.
 */
@UseCase
@AllArgsConstructor
public class SearchObservabilityLogMessagesUseCase {

    static final int DEFAULT_PAGE = 1;
    static final int DEFAULT_PER_PAGE = 20;
    static final int MAX_PER_PAGE = 100;

    private final ObservabilityLogsDataPort logsDataPort;

    public record Input(String organizationId, String environmentId, String apiId, String requestId, Integer page, Integer perPage) {}

    public record Output(MessageLogsPage data, int page, int perPage) {}

    public Output execute(Input input) {
        var page = input.page() == null || input.page() < 1 ? DEFAULT_PAGE : input.page();
        var perPage = input.perPage() == null || input.perPage() < 1 ? DEFAULT_PER_PAGE : Math.min(input.perPage(), MAX_PER_PAGE);

        var messages = logsDataPort.searchMessages(
            input.organizationId(),
            input.environmentId(),
            input.apiId(),
            input.requestId(),
            page,
            perPage
        );

        return new Output(messages, page, perPage);
    }
}
