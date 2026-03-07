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

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.repository.ConnectionLogFixtures;
import inmemory.ConnectionLogsCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.log.use_case.SearchEnvironmentConnectionLogErrorKeysUseCase.Input;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchEnvironmentConnectionLogErrorKeysUseCaseTest {

    private static final Long FROM = Instant.parse("2020-02-01T00:01:00.00Z").toEpochMilli();
    private static final Long TO = Instant.parse("2020-02-02T23:59:59.00Z").toEpochMilli();

    ConnectionLogFixtures connectionLogFixtures = new ConnectionLogFixtures("api-id", "app-id", "plan-id");
    ConnectionLogsCrudServiceInMemory logStorageService = new ConnectionLogsCrudServiceInMemory();

    SearchEnvironmentConnectionLogErrorKeysUseCase usecase;

    @BeforeEach
    void setUp() {
        usecase = new SearchEnvironmentConnectionLogErrorKeysUseCase(logStorageService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(logStorageService).forEach(InMemoryAlternative::reset);
        GraviteeContext.cleanContext();
    }

    @Test
    void should_return_error_keys_of_an_environment() {
        logStorageService.initWithConnectionLogs(
            List.of(
                connectionLogFixtures.aConnectionLog("req1").toBuilder().errorKey("ERROR_1").timestamp("2020-02-01T20:00:00.00Z").build(),
                connectionLogFixtures
                    .aConnectionLog("req2")
                    .toBuilder()
                    .apiId("api2")
                    .errorKey("ERROR_2")
                    .timestamp("2020-02-01T21:00:00.00Z")
                    .build(),
                connectionLogFixtures.aConnectionLog("req3").toBuilder().errorKey("ERROR_1").timestamp("2020-02-01T22:00:00.00Z").build(),
                connectionLogFixtures.aConnectionLog("req4").toBuilder().errorKey("ERROR_3").timestamp("2020-02-03T20:00:00.00Z").build() // Out of range
            )
        );

        var result = usecase.execute(GraviteeContext.getExecutionContext(), new Input(FROM, TO));

        assertThat(result.errorKeys()).containsExactly("ERROR_1", "ERROR_2");
    }

    @Test
    void should_return_empty_list_if_no_error_keys() {
        logStorageService.initWithConnectionLogs(
            List.of(connectionLogFixtures.aConnectionLog("req1").toBuilder().errorKey(null).timestamp("2020-02-01T20:00:00.00Z").build())
        );

        var result = usecase.execute(GraviteeContext.getExecutionContext(), new Input(FROM, TO));

        assertThat(result.errorKeys()).isEmpty();
    }
}
