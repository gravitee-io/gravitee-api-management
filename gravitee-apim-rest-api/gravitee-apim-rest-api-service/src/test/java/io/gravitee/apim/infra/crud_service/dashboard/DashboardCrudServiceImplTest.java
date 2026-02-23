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
package io.gravitee.apim.infra.crud_service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.dashboard.model.Dashboard;
import io.gravitee.apim.core.dashboard.model.DashboardWidget;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CustomDashboardRepository;
import io.gravitee.repository.management.model.CustomDashboard;
import io.gravitee.repository.management.model.CustomDashboardWidget;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class DashboardCrudServiceImplTest {

    CustomDashboardRepository customDashboardRepository;

    DashboardCrudServiceImpl service;

    private static final String DASHBOARD_ID = "dashboard-id";
    private static final String ORGANIZATION_ID = "org-id";
    private static final String DASHBOARD_NAME = "llm-proxy";
    private static final String CREATED_BY = "user-id";
    private static final Instant CREATED_AT = Instant.parse("2025-10-07T06:50:30Z");
    private static final Instant LAST_MODIFIED = Instant.parse("2025-10-07T06:50:30Z");

    @BeforeEach
    void setUp() {
        customDashboardRepository = mock(CustomDashboardRepository.class);
        service = new DashboardCrudServiceImpl(customDashboardRepository);
    }

    @Nested
    class Create {

        @BeforeEach
        @SneakyThrows
        void setUp() {
            when(customDashboardRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @SneakyThrows
        void should_create_a_dashboard() {
            var dashboard = aDashboard();
            service.create(dashboard);

            var captor = ArgumentCaptor.forClass(CustomDashboard.class);
            verify(customDashboardRepository).create(captor.capture());

            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(aRepositoryDashboard());
        }

        @Test
        @SneakyThrows
        void should_return_the_created_dashboard() {
            var dashboard = aDashboard();
            var result = service.create(dashboard);

            assertThat(result).isNotNull().usingRecursiveComparison().isEqualTo(dashboard);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            when(customDashboardRepository.create(any())).thenThrow(TechnicalException.class);

            var throwable = catchThrowable(() -> service.create(aDashboard()));

            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to create a dashboard");
        }
    }

    @Nested
    class FindById {

        @Test
        @SneakyThrows
        void should_find_a_dashboard() {
            when(customDashboardRepository.findById(DASHBOARD_ID)).thenReturn(Optional.of(aRepositoryDashboard()));

            var result = service.findById(DASHBOARD_ID);

            assertThat(result).isPresent().get().usingRecursiveComparison().isEqualTo(aDashboard());
        }

        @Test
        @SneakyThrows
        void should_return_empty_if_dashboard_not_found() {
            when(customDashboardRepository.findById(DASHBOARD_ID)).thenReturn(Optional.empty());

            var result = service.findById(DASHBOARD_ID);

            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            when(customDashboardRepository.findById(DASHBOARD_ID)).thenThrow(TechnicalException.class);

            var throwable = catchThrowable(() -> service.findById(DASHBOARD_ID));

            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to find dashboard: " + DASHBOARD_ID);
        }
    }

    @Nested
    class Update {

        @BeforeEach
        @SneakyThrows
        void setUp() {
            when(customDashboardRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @SneakyThrows
        void should_update_a_dashboard() {
            var dashboard = aDashboard();
            service.update(dashboard);

            var captor = ArgumentCaptor.forClass(CustomDashboard.class);
            verify(customDashboardRepository).update(captor.capture());

            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(aRepositoryDashboard());
        }

        @Test
        @SneakyThrows
        void should_return_the_updated_dashboard() {
            var dashboard = aDashboard();
            var result = service.update(dashboard);

            assertThat(result).isNotNull().usingRecursiveComparison().isEqualTo(dashboard);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            when(customDashboardRepository.update(any())).thenThrow(TechnicalException.class);

            var throwable = catchThrowable(() -> service.update(aDashboard()));

            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to update dashboard: " + DASHBOARD_ID);
        }
    }

    @Nested
    class Delete {

        @Test
        @SneakyThrows
        void should_delete_a_dashboard() {
            service.delete(DASHBOARD_ID);
            verify(customDashboardRepository).delete(DASHBOARD_ID);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            doThrow(new TechnicalException("exception")).when(customDashboardRepository).delete(DASHBOARD_ID);

            assertThatThrownBy(() -> service.delete(DASHBOARD_ID))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while trying to delete dashboard: " + DASHBOARD_ID);

            verify(customDashboardRepository).delete(DASHBOARD_ID);
        }
    }

    private static Dashboard aDashboard() {
        return Dashboard.builder()
            .id(DASHBOARD_ID)
            .organizationId(ORGANIZATION_ID)
            .name(DASHBOARD_NAME)
            .createdBy(CREATED_BY)
            .createdAt(CREATED_AT.atZone(ZoneId.systemDefault()))
            .lastModified(LAST_MODIFIED.atZone(ZoneId.systemDefault()))
            .labels(Map.of("team", "apim"))
            .widgets(
                List.of(
                    DashboardWidget.builder()
                        .id("1")
                        .title("Requests")
                        .type("stats")
                        .layout(DashboardWidget.Layout.builder().cols(1).rows(1).x(0).y(0).build())
                        .request(
                            DashboardWidget.Request.builder()
                                .type("measures")
                                .timeRange(
                                    DashboardWidget.TimeRange.builder()
                                        .from(Instant.parse("2025-10-07T06:50:30Z"))
                                        .to(Instant.parse("2025-12-07T11:35:30Z"))
                                        .build()
                                )
                                .metrics(
                                    List.of(
                                        DashboardWidget.MetricRequest.builder().name("HTTP_REQUESTS").measures(List.of("COUNT")).build()
                                    )
                                )
                                .build()
                        )
                        .build()
                )
            )
            .build();
    }

    private static CustomDashboard aRepositoryDashboard() {
        return CustomDashboard.builder()
            .id(DASHBOARD_ID)
            .organizationId(ORGANIZATION_ID)
            .name(DASHBOARD_NAME)
            .createdBy(CREATED_BY)
            .createdAt(Date.from(CREATED_AT))
            .lastModified(Date.from(LAST_MODIFIED))
            .labels(Map.of("team", "apim"))
            .widgets(
                List.of(
                    CustomDashboardWidget.builder()
                        .id("1")
                        .title("Requests")
                        .type("stats")
                        .layout(CustomDashboardWidget.Layout.builder().cols(1).rows(1).x(0).y(0).build())
                        .request(
                            CustomDashboardWidget.Request.builder()
                                .type("measures")
                                .timeRange(
                                    CustomDashboardWidget.TimeRange.builder()
                                        .from("2025-10-07T06:50:30Z")
                                        .to("2025-12-07T11:35:30Z")
                                        .build()
                                )
                                .metrics(
                                    List.of(
                                        CustomDashboardWidget.MetricRequest.builder()
                                            .name("HTTP_REQUESTS")
                                            .measures(List.of("COUNT"))
                                            .build()
                                    )
                                )
                                .build()
                        )
                        .build()
                )
            )
            .build();
    }
}
