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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gamma.rest.core.observability.logs.model.LogEntry;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetAuthzDecisionUseCaseTest {

    private static final String ORG = "org-1";
    private static final String ENV = "env-1";
    private static final String API_ID = "api-1";
    private static final String EVENT_ID = "evt-1";

    private ObservabilityLogsDataPort logsDataPort;
    private GetAuthzDecisionUseCase useCase;

    @BeforeEach
    void setUp() {
        logsDataPort = mock(ObservabilityLogsDataPort.class);
        useCase = new GetAuthzDecisionUseCase(logsDataPort);
    }

    @Test
    void returns_the_decision_the_data_port_resolved() {
        var decision = LogEntry.builder().apiId(API_ID).requestId("req-1").build();
        when(logsDataPort.getDecision(ORG, ENV, API_ID, EVENT_ID)).thenReturn(Optional.of(decision));

        var output = useCase.execute(new GetAuthzDecisionUseCase.Input(ORG, ENV, API_ID, EVENT_ID));

        assertThat(output.decision()).contains(decision);
    }

    @Test
    void identifies_the_decision_by_event_id_within_one_api_and_the_caller_scope() {
        when(logsDataPort.getDecision(ORG, ENV, API_ID, EVENT_ID)).thenReturn(Optional.empty());

        useCase.execute(new GetAuthzDecisionUseCase.Input(ORG, ENV, API_ID, EVENT_ID));

        verify(logsDataPort).getDecision(ORG, ENV, API_ID, EVENT_ID);
    }

    @Test
    void reports_no_decision_rather_than_failing_when_the_event_is_absent_or_out_of_scope() {
        when(logsDataPort.getDecision(ORG, ENV, API_ID, "gone")).thenReturn(Optional.empty());

        var output = useCase.execute(new GetAuthzDecisionUseCase.Input(ORG, ENV, API_ID, "gone"));

        assertThat(output.decision()).isEmpty();
    }
}
