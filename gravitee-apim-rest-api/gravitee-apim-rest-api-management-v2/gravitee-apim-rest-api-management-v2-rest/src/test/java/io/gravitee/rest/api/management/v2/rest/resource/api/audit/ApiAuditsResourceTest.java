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
package io.gravitee.rest.api.management.v2.rest.resource.api.audit;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditFixtures;
import inmemory.AuditCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import io.gravitee.rest.api.management.v2.rest.model.Audit;
import io.gravitee.rest.api.management.v2.rest.model.AuditPropertiesInner;
import io.gravitee.rest.api.management.v2.rest.model.AuditReference;
import io.gravitee.rest.api.management.v2.rest.model.AuditsResponse;
import io.gravitee.rest.api.management.v2.rest.model.BaseUser;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiResourceTest;
import io.gravitee.rest.api.management.v2.rest.resource.api.audit.param.SearchApiAuditsParam;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
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
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiAuditsResourceTest extends ApiResourceTest {

    @Inject
    AuditCrudServiceInMemory auditCrudServiceInMemory;

    WebTarget target;

    @BeforeEach
    public void setup() {
        target = rootTarget();

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        Stream.of(auditCrudServiceInMemory).forEach(InMemoryAlternative::reset);
    }

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/audits";
    }

    @Nested
    class GetAudits {

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_AUDIT),
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
        public void should_return_audits() {
            auditCrudServiceInMemory.initWith(
                List.of(
                    AuditFixtures.anApiAudit()
                        .toBuilder()
                        .organizationId(ORGANIZATION)
                        .environmentId(ENVIRONMENT)
                        .referenceId(API)
                        .properties(Map.of("API", API))
                        .build()
                )
            );

            final Response response = target.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(AuditsResponse.class)
                .extracting(AuditsResponse::getData)
                .isEqualTo(
                    List.of(
                        new Audit()
                            .id("audit-id")
                            .reference(new AuditReference().id(API).type(AuditReference.TypeEnum.API).name("my-api-name"))
                            .organizationId(ORGANIZATION)
                            .environmentId(ENVIRONMENT)
                            .user(new BaseUser().id("user-id").displayName("user-id-name"))
                            .event("event-1")
                            .createdAt(OffsetDateTime.parse("2020-02-01T20:22:02.00Z"))
                            .properties(List.of(new AuditPropertiesInner().key("API").value(API).name("my-api-name")))
                            .patch(
                                """
                                [{ "op": "add", "path": "/hello", "value": ["world"] }]"""
                            )
                    )
                );
        }

        @Test
        public void should_return_audits_filtered_by_interval() {
            auditCrudServiceInMemory.initWith(
                List.of(
                    AuditFixtures.anApiAudit()
                        .toBuilder()
                        .id("1")
                        .organizationId(ORGANIZATION)
                        .environmentId(ENVIRONMENT)
                        .referenceId(API)
                        .createdAt(ZonedDateTime.parse("2020-02-01T20:00:00.00Z"))
                        .build(),
                    AuditFixtures.anApiAudit()
                        .toBuilder()
                        .id("2")
                        .organizationId(ORGANIZATION)
                        .environmentId(ENVIRONMENT)
                        .referenceId(API)
                        .createdAt(ZonedDateTime.parse("2020-02-02T20:00:00.00Z"))
                        .build(),
                    AuditFixtures.anApiAudit()
                        .toBuilder()
                        .id("3")
                        .organizationId(ORGANIZATION)
                        .environmentId(ENVIRONMENT)
                        .referenceId(API)
                        .createdAt(ZonedDateTime.parse("2020-02-04T20:00:00.00Z"))
                        .build()
                )
            );

            final Response response = target
                .queryParam(SearchApiAuditsParam.FROM_QUERY_PARAM_NAME, Instant.parse("2020-02-01T00:01:00.00Z").toEpochMilli())
                .queryParam(SearchApiAuditsParam.TO_QUERY_PARAM_NAME, Instant.parse("2020-02-03T23:59:59.00Z").toEpochMilli())
                .request()
                .get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(AuditsResponse.class)
                .extracting(r -> r.getData() != null ? r.getData().stream().map(Audit::getId).toList() : null)
                .isEqualTo(List.of("2", "1"));
        }

        @Test
        public void should_return_audits_filtered_by_events() {
            auditCrudServiceInMemory.initWith(
                List.of(
                    AuditFixtures.anApiAudit()
                        .toBuilder()
                        .id("1")
                        .organizationId(ORGANIZATION)
                        .environmentId(ENVIRONMENT)
                        .referenceId(API)
                        .event("event1")
                        .build(),
                    AuditFixtures.anApiAudit()
                        .toBuilder()
                        .id("2")
                        .organizationId(ORGANIZATION)
                        .environmentId(ENVIRONMENT)
                        .referenceId(API)
                        .event("event2")
                        .build(),
                    AuditFixtures.anApiAudit()
                        .toBuilder()
                        .id("3")
                        .organizationId(ORGANIZATION)
                        .environmentId(ENVIRONMENT)
                        .referenceId(API)
                        .event("event3")
                        .build()
                )
            );

            final Response response = target.queryParam(SearchApiAuditsParam.EVENTS_QUERY_PARAM_NAME, "event1,event3").request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(AuditsResponse.class)
                .extracting(r -> r.getData() != null ? r.getData().stream().map(Audit::getId).toList() : null)
                .isEqualTo(List.of("1", "3"));
        }

        @Test
        public void should_compute_pagination() {
            var total = 20L;
            var pageSize = 5;
            auditCrudServiceInMemory.initWith(
                LongStream.range(0, total)
                    .mapToObj(i ->
                        AuditFixtures.anApiAudit()
                            .toBuilder()
                            .organizationId(ORGANIZATION)
                            .environmentId(ENVIRONMENT)
                            .referenceId(API)
                            .build()
                    )
                    .toList()
            );

            final Response response = target
                .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, 2)
                .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, pageSize)
                .request()
                .get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(AuditsResponse.class)
                .extracting(AuditsResponse::getPagination)
                .isEqualTo(new Pagination().page(2).perPage(pageSize).pageCount(4).pageItemsCount(pageSize).totalCount(total));
        }

        @Test
        public void should_compute_links() {
            var total = 20L;
            var page = 2;
            var pageSize = 5;
            auditCrudServiceInMemory.initWith(
                LongStream.range(0, total)
                    .mapToObj(i ->
                        AuditFixtures.anApiAudit()
                            .toBuilder()
                            .organizationId(ORGANIZATION)
                            .environmentId(ENVIRONMENT)
                            .referenceId(API)
                            .build()
                    )
                    .toList()
            );

            final Response response = target
                .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, 2)
                .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, pageSize)
                .request()
                .get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(AuditsResponse.class)
                .extracting(AuditsResponse::getLinks)
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
                    .queryParam(SearchApiAuditsParam.FROM_QUERY_PARAM_NAME, -1)
                    .queryParam(SearchApiAuditsParam.TO_QUERY_PARAM_NAME, -1)
                    .request()
                    .get();

                assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400).hasMessage("Validation error");
            }

            @Test
            public void should_return_400_if_to_is_before_from_param() {
                final Response response = target
                    .queryParam(SearchApiAuditsParam.FROM_QUERY_PARAM_NAME, 2)
                    .queryParam(SearchApiAuditsParam.TO_QUERY_PARAM_NAME, 1)
                    .request()
                    .get();

                assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasHttpStatus(BAD_REQUEST_400).hasMessage("Validation error");
            }
        }
    }
}
