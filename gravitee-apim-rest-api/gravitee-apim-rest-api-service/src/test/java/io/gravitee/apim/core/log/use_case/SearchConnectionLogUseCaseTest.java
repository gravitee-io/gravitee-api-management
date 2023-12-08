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

import fixtures.repository.ConnectionLogDetailFixtures;
import inmemory.ConnectionLogsCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.Storage;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchConnectionLogUseCaseTest {

    private static final String API_ID = "f1608475-dd77-4603-a084-75dd775603e9";
    private static final String REQUEST_ID = "c5608475-dd77-4603-a084-75dd77560310";
    private static final Long FIRST_FEBRUARY_2020 = Instant.parse("2020-02-01T00:01:00.00Z").toEpochMilli();
    private static final Long SECOND_FEBRUARY_2020 = Instant.parse("2020-02-02T23:59:59.00Z").toEpochMilli();
    private static final Long FOURTH_FEBRUARY_2020 = Instant.parse("2020-02-04T00:01:00.00Z").toEpochMilli();
    private static final Long FIFTH_FEBRUARY_2020 = Instant.parse("2020-02-05T00:01:00.00Z").toEpochMilli();

    ConnectionLogsCrudServiceInMemory logStorageService = new ConnectionLogsCrudServiceInMemory();

    SearchConnectionLogUseCase usecase;

    ConnectionLogDetailFixtures connectionLogDetailFixtures = new ConnectionLogDetailFixtures(API_ID, REQUEST_ID);

    @BeforeEach
    void setUp() {
        usecase = new SearchConnectionLogUseCase(logStorageService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(logStorageService).forEach(InMemoryAlternative::reset);
        GraviteeContext.cleanContext();
    }

    @Test
    void should_return_connection_log_for_a_request_on_api() {
        final ConnectionLogDetail connectionLogDetail = connectionLogDetailFixtures.aConnectionLogDetail().toBuilder().build();
        logStorageService.initWithConnectionLogDetails(Storage.of(connectionLogDetail));

        var result = usecase.execute(new SearchConnectionLogUseCase.Input(API_ID, REQUEST_ID));

        assertThat(result).isEqualTo(new SearchConnectionLogUseCase.Output(Optional.of(connectionLogDetail)));
    }

    @Test
    void should_return_empty_connection_log_for_non_existing_request() {
        logStorageService.initWithConnectionLogDetails(
            Storage.of(connectionLogDetailFixtures.aConnectionLogDetail("other-req").toBuilder().build())
        );

        var result = usecase.execute(new SearchConnectionLogUseCase.Input(API_ID, REQUEST_ID));

        assertThat(result).isEqualTo(new SearchConnectionLogUseCase.Output(Optional.empty()));
    }

    @Test
    void should_return_empty_connection_log_for_non_existing_api() {
        logStorageService.initWithConnectionLogDetails(
            Storage.of(connectionLogDetailFixtures.aConnectionLogDetail().toBuilder().apiId("other-api").build())
        );

        var result = usecase.execute(new SearchConnectionLogUseCase.Input(API_ID, REQUEST_ID));

        assertThat(result).isEqualTo(new SearchConnectionLogUseCase.Output(Optional.empty()));
    }
}
