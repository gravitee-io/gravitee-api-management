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
package io.gravitee.rest.api.storage.analytics.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.log.v4.api.LogRepository;
import io.gravitee.repository.log.v4.model.ConnectionLog;
import io.gravitee.repository.log.v4.model.ConnectionLogQuery;
import io.gravitee.repository.log.v4.model.ConnectionLogQuery.Filter;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.v4.log.BaseConnectionLog;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class LogStorageServiceImplTest {

    LogRepository logRepository;

    LogStorageServiceImpl logStorageService;

    @BeforeEach
    void setUp() {
        logRepository = mock(LogRepository.class);
        logStorageService = new LogStorageServiceImpl(logRepository);
    }

    @Nested
    class SearchApiConnectionLog {

        @Test
        void should_search_api_connection_logs() throws AnalyticsException {
            when(logRepository.searchConnectionLog(any())).thenReturn(new LogResponse<>(0L, List.of()));

            logStorageService.searchApiConnectionLog("apiId", new PageableImpl(1, 10));

            var captor = ArgumentCaptor.forClass(ConnectionLogQuery.class);
            verify(logRepository).searchConnectionLog(captor.capture());

            assertThat(captor.getValue())
                .isEqualTo(ConnectionLogQuery.builder().filter(Filter.builder().appId("apiId").build()).page(1).size(10).build());
        }

        @Test
        void should_return_api_connection_logs() throws AnalyticsException {
            when(logRepository.searchConnectionLog(any()))
                .thenReturn(
                    new LogResponse<>(
                        1L,
                        List.of(
                            ConnectionLog
                                .builder()
                                .apiId("api-id")
                                .applicationId("app-id")
                                .planId("plan-id")
                                .clientIdentifier("client-identifier")
                                .status(200)
                                .method(HttpMethod.GET)
                                .transactionId("transaction-id")
                                .requestEnded(true)
                                .timestamp("2020-02-01T20:00:00.00Z")
                                .build()
                        )
                    )
                );

            var result = logStorageService.searchApiConnectionLog("apiId", new PageableImpl(1, 10));

            SoftAssertions.assertSoftly(soft -> {
                assertThat(result.total()).isOne();
                assertThat(result.logs())
                    .isEqualTo(
                        List.of(
                            BaseConnectionLog
                                .builder()
                                .apiId("api-id")
                                .applicationId("app-id")
                                .planId("plan-id")
                                .clientIdentifier("client-identifier")
                                .status(200)
                                .method(HttpMethod.GET)
                                .transactionId("transaction-id")
                                .requestEnded(true)
                                .timestamp("2020-02-01T20:00:00.00Z")
                                .build()
                        )
                    );
            });
        }
    }
}
