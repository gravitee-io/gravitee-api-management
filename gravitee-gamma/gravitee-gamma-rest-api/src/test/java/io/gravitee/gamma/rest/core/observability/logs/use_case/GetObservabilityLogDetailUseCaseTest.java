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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gamma.rest.core.observability.logs.model.HttpPayload;
import io.gravitee.gamma.rest.core.observability.logs.model.LogDetail;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetObservabilityLogDetailUseCaseTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";
    private static final String API_ID = "api-1";
    private static final String REQUEST_ID = "req-123";

    @Mock
    private ObservabilityLogsDataPort logsDataPort;

    @InjectMocks
    private GetObservabilityLogDetailUseCase useCase;

    @Test
    void should_return_detail_when_found() {
        // Given
        var detail = LogDetail.builder()
            .requestId(REQUEST_ID)
            .apiId(API_ID)
            .timestamp(Instant.parse("2026-06-10T14:32:01Z"))
            .status(200)
            .method("GET")
            .uri("/pets/42")
            .planName("Gold")
            .entrypointRequest(HttpPayload.builder().method("GET").uri("/pets/42").build())
            .build();
        when(logsDataPort.getLogDetail(ORG_ID, ENV_ID, API_ID, REQUEST_ID)).thenReturn(Optional.of(detail));

        // When
        var output = useCase.execute(new GetObservabilityLogDetailUseCase.Input(ORG_ID, ENV_ID, API_ID, REQUEST_ID));

        // Then
        assertThat(output.detail()).isPresent();
        assertThat(output.detail().get().requestId()).isEqualTo(REQUEST_ID);
        assertThat(output.detail().get().status()).isEqualTo(200);
        assertThat(output.detail().get().entrypointRequest().method()).isEqualTo("GET");
        verify(logsDataPort).getLogDetail(ORG_ID, ENV_ID, API_ID, REQUEST_ID);
    }

    @Test
    void should_return_empty_when_not_found() {
        // Given
        when(logsDataPort.getLogDetail(ORG_ID, ENV_ID, API_ID, REQUEST_ID)).thenReturn(Optional.empty());

        // When
        var output = useCase.execute(new GetObservabilityLogDetailUseCase.Input(ORG_ID, ENV_ID, API_ID, REQUEST_ID));

        // Then
        assertThat(output.detail()).isEmpty();
    }
}
