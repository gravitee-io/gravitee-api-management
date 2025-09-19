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
package io.gravitee.rest.api.management.v2.rest.resource.api.event;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.core.model.EventFixtures;
import inmemory.EventQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.rest.api.management.v2.rest.model.BaseUser;
import io.gravitee.rest.api.management.v2.rest.model.Event;
import io.gravitee.rest.api.management.v2.rest.model.EventsResponse;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiResourceTest;
import io.gravitee.rest.api.management.v2.rest.resource.api.event.param.SearchApiEventsParam;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiEventsResourceTest extends ApiResourceTest {

    private static final BaseUserEntity USER = BaseUserEntity.builder()
        .id("user-id")
        .firstname("John")
        .lastname("Doe")
        .email("john.doe@gravitee.io")
        .build();

    @Inject
    EventQueryServiceInMemory eventQueryService;

    @Inject
    UserCrudServiceInMemory userCrudService;

    WebTarget target;

    @BeforeEach
    public void setup() {
        target = rootTarget();

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
        userCrudService.initWith(List.of(USER));
    }

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        Stream.of(eventQueryService, userCrudService).forEach(InMemoryAlternative::reset);
    }

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/events";
    }

    @Nested
    class SearchApiEvents {

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_EVENT),
                    eq(API),
                    eq(RolePermissionAction.READ)
                )
            ).thenReturn(false);

            final Response response = target.request().get();

            assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_return_events() {
            eventQueryService.initWith(List.of(EventFixtures.anApiEvent(API).toBuilder().environments(Set.of(ENVIRONMENT)).build()));

            final Response response = target.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(EventsResponse.class)
                .extracting(EventsResponse::getData)
                .isEqualTo(
                    List.of(
                        new Event()
                            .id("event-id")
                            .environmentIds(List.of(ENVIRONMENT))
                            .initiator(new BaseUser().id("user-id").displayName("John Doe"))
                            .type(io.gravitee.rest.api.management.v2.rest.model.EventType.PUBLISH_API)
                            .createdAt(OffsetDateTime.parse("2020-02-01T20:22:02.00Z"))
                            .payload("event-payload")
                            .properties(Map.of("API_ID", API, "USER", "user-id"))
                    )
                );
        }

        @Test
        public void should_return_audits_filtered_by_interval() {
            eventQueryService.initWith(
                List.of(
                    EventFixtures.anApiEvent(API)
                        .toBuilder()
                        .id("1")
                        .environments(Set.of(ENVIRONMENT))
                        .createdAt(ZonedDateTime.parse("2020-02-01T20:00:00.00Z"))
                        .build(),
                    EventFixtures.anApiEvent(API)
                        .toBuilder()
                        .id("2")
                        .environments(Set.of(ENVIRONMENT))
                        .createdAt(ZonedDateTime.parse("2020-02-02T20:00:00.00Z"))
                        .build(),
                    EventFixtures.anApiEvent(API)
                        .toBuilder()
                        .id("3")
                        .environments(Set.of(ENVIRONMENT))
                        .createdAt(ZonedDateTime.parse("2020-02-04T20:00:00.00Z"))
                        .build()
                )
            );

            final Response response = target
                .queryParam(SearchApiEventsParam.FROM_QUERY_PARAM_NAME, Instant.parse("2020-02-01T00:01:00.00Z").toEpochMilli())
                .queryParam(SearchApiEventsParam.TO_QUERY_PARAM_NAME, Instant.parse("2020-02-03T23:59:59.00Z").toEpochMilli())
                .request()
                .get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(EventsResponse.class)
                .extracting(r -> r.getData() != null ? r.getData().stream().map(Event::getId).toList() : null)
                .isEqualTo(List.of("2", "1"));
        }

        @Test
        public void should_return_audits_filtered_by_event_type() {
            eventQueryService.initWith(
                List.of(
                    EventFixtures.anApiEvent(API).toBuilder().id("1").environments(Set.of(ENVIRONMENT)).type(EventType.PUBLISH_API).build(),
                    EventFixtures.anApiEvent(API).toBuilder().id("2").environments(Set.of(ENVIRONMENT)).type(EventType.START_API).build(),
                    EventFixtures.anApiEvent(API).toBuilder().id("3").environments(Set.of(ENVIRONMENT)).type(EventType.STOP_API).build()
                )
            );

            final Response response = target
                .queryParam(SearchApiEventsParam.EVENT_TYPES_QUERY_PARAM_NAME, "PUBLISH_API,STOP_API")
                .request()
                .get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(EventsResponse.class)
                .extracting(r -> r.getData() != null ? r.getData().stream().map(Event::getId).toList() : null)
                .isEqualTo(List.of("1", "3"));
        }

        @Test
        public void should_compute_pagination() {
            var total = 20L;
            var pageSize = 5;
            eventQueryService.initWith(
                LongStream.range(0, total)
                    .mapToObj(i -> EventFixtures.anApiEvent(API).toBuilder().environments(Set.of(ENVIRONMENT)).build())
                    .collect(Collectors.toList())
            );

            final Response response = target
                .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, 2)
                .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, pageSize)
                .request()
                .get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(EventsResponse.class)
                .extracting(EventsResponse::getPagination)
                .isEqualTo(new Pagination().page(2).perPage(pageSize).pageCount(4).pageItemsCount(pageSize).totalCount(total));
        }

        @Test
        public void should_compute_links() {
            var total = 20L;
            var page = 2;
            var pageSize = 5;
            eventQueryService.initWith(
                LongStream.range(0, total)
                    .mapToObj(i -> EventFixtures.anApiEvent(API).toBuilder().environments(Set.of(ENVIRONMENT)).build())
                    .collect(Collectors.toList())
            );

            final Response response = target
                .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, 2)
                .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, pageSize)
                .request()
                .get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(EventsResponse.class)
                .extracting(EventsResponse::getLinks)
                .isEqualTo(
                    new Links()
                        .self(target.queryParam("page", page).queryParam("perPage", pageSize).getUri().toString())
                        .first(target.queryParam("page", 1).queryParam("perPage", pageSize).getUri().toString())
                        .last(target.queryParam("page", 4).queryParam("perPage", pageSize).getUri().toString())
                        .previous(target.queryParam("page", 1).queryParam("perPage", pageSize).getUri().toString())
                        .next(target.queryParam("page", 3).queryParam("perPage", pageSize).getUri().toString())
                );
        }

        @Nested
        class Validation {

            @Test
            public void should_return_400_if_negative_interval_params() {
                final Response response = target
                    .queryParam(SearchApiEventsParam.FROM_QUERY_PARAM_NAME, -1)
                    .queryParam(SearchApiEventsParam.TO_QUERY_PARAM_NAME, -1)
                    .request()
                    .get();

                assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400).hasMessage("Validation error");
            }

            @Test
            public void should_return_400_if_to_is_before_from_param() {
                final Response response = target
                    .queryParam(SearchApiEventsParam.FROM_QUERY_PARAM_NAME, 2)
                    .queryParam(SearchApiEventsParam.TO_QUERY_PARAM_NAME, 1)
                    .request()
                    .get();

                assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400).hasMessage("Validation error");
            }
        }
    }
}
