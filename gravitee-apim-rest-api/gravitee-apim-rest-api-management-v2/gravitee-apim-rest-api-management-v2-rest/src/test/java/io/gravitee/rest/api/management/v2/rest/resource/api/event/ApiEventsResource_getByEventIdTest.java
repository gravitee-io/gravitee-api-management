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
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.Mockito.when;

import fixtures.core.model.EventFixtures;
import inmemory.EventQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.rest.api.management.v2.rest.model.BaseUser;
import io.gravitee.rest.api.management.v2.rest.model.Event;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiResourceTest;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiEventsResource_getByEventIdTest extends ApiResourceTest {

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
        String eventId = "2";
        target = rootTarget(eventId);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
        userCrudService.initWith(List.of(USER));
    }

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        Stream.of(eventQueryService).forEach(InMemoryAlternative::reset);
    }

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/events";
    }

    @Test
    void should_return_event_by_specific_event_id() {
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

        final Response response = target.request().get();

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(Event.class)
            .isEqualTo(
                new Event()
                    .id("2")
                    .environmentIds(List.of(ENVIRONMENT))
                    .initiator(new BaseUser().id("user-id").displayName("John Doe"))
                    .type(io.gravitee.rest.api.management.v2.rest.model.EventType.PUBLISH_API)
                    .createdAt(OffsetDateTime.parse("2020-02-02T20:00:00.00Z"))
                    .payload("event-payload")
                    .properties(Map.of("API_ID", API, "USER", "user-id"))
            );
    }

    @Test
    void should_return_400_if_eventId_blank() {
        final Response response = rootTarget(" ").request().get();
        assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400).hasMessage("Validation error");
    }

    @Test
    void should_return_403_if_incorrect_permissions() {
        when(
            permissionService.hasPermission(GraviteeContext.getExecutionContext(), RolePermission.API_EVENT, API, RolePermissionAction.READ)
        ).thenReturn(false);

        final Response response = target.request().get();

        assertThat(response)
            .hasStatus(FORBIDDEN_403)
            .asError()
            .hasHttpStatus(FORBIDDEN_403)
            .hasMessage("You do not have sufficient rights to access this resource");
    }

    @Test
    void should_return_404_if_event_not_found() {
        final Response response = target.request().get();
        assertThat(response).hasStatus(NOT_FOUND_404).asError().hasHttpStatus(NOT_FOUND_404).hasMessage("Not API event with id: 2 found");
    }
}
