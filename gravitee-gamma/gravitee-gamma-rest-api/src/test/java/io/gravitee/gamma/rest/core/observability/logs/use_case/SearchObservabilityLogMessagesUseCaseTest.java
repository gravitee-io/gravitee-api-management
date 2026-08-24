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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gamma.rest.core.observability.logs.model.MessageLog;
import io.gravitee.gamma.rest.core.observability.logs.model.MessageLogsPage;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchObservabilityLogMessagesUseCaseTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";
    private static final String API_ID = "api-1";
    private static final String REQUEST_ID = "req-1";

    private final ObservabilityLogsDataPort logsDataPort = mock(ObservabilityLogsDataPort.class);
    private final SearchObservabilityLogMessagesUseCase useCase = new SearchObservabilityLogMessagesUseCase(logsDataPort);

    private SearchObservabilityLogMessagesUseCase.Output execute(Integer page, Integer perPage) {
        return useCase.execute(new SearchObservabilityLogMessagesUseCase.Input(ORG_ID, ENV_ID, API_ID, REQUEST_ID, page, perPage));
    }

    @Test
    void should_return_the_messages_of_the_connection() {
        var message = MessageLog.builder()
            .requestId(REQUEST_ID)
            .operation("PUBLISH")
            .entrypoint(MessageLog.Message.builder().connectorId("http-post").payload("{}").build())
            .endpoint(MessageLog.Message.builder().connectorId("kafka").payload("{}").build())
            .build();
        when(logsDataPort.searchMessages(eq(ORG_ID), eq(ENV_ID), eq(API_ID), eq(REQUEST_ID), anyInt(), anyInt())).thenReturn(
            new MessageLogsPage(List.of(message), 1)
        );

        var output = execute(1, 20);

        assertThat(output.data().data()).containsExactly(message);
        assertThat(output.data().totalCount()).isEqualTo(1);
    }

    @Test
    void should_report_an_api_recording_no_message_logs_as_an_empty_page() {
        when(logsDataPort.searchMessages(eq(ORG_ID), eq(ENV_ID), eq(API_ID), eq(REQUEST_ID), anyInt(), anyInt())).thenReturn(
            MessageLogsPage.EMPTY
        );

        var output = execute(1, 20);

        assertThat(output.data()).isEqualTo(MessageLogsPage.EMPTY);
    }

    @Test
    void should_fall_back_to_the_defaults_on_absent_or_meaningless_paging() {
        when(logsDataPort.searchMessages(eq(ORG_ID), eq(ENV_ID), eq(API_ID), eq(REQUEST_ID), anyInt(), anyInt())).thenReturn(
            MessageLogsPage.EMPTY
        );

        var output = execute(null, 0);

        assertThat(output.page()).isEqualTo(1);
        assertThat(output.perPage()).isEqualTo(20);
        verify(logsDataPort).searchMessages(ORG_ID, ENV_ID, API_ID, REQUEST_ID, 1, 20);
    }

    @Test
    void should_cap_the_page_size_so_one_call_cannot_pull_a_whole_connection() {
        when(logsDataPort.searchMessages(eq(ORG_ID), eq(ENV_ID), eq(API_ID), eq(REQUEST_ID), anyInt(), anyInt())).thenReturn(
            MessageLogsPage.EMPTY
        );

        var output = execute(3, 5_000);

        assertThat(output.perPage()).isEqualTo(100);
        verify(logsDataPort).searchMessages(ORG_ID, ENV_ID, API_ID, REQUEST_ID, 3, 100);
    }
}
