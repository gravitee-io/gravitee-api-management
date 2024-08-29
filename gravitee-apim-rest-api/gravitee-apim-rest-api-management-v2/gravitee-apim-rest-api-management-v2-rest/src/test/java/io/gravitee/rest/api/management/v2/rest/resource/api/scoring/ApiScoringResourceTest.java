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
package io.gravitee.rest.api.management.v2.rest.resource.api.scoring;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.ACCEPTED_202;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import fixtures.core.log.model.MessageLogFixtures;
import fixtures.core.model.ApiFixtures;
import fixtures.repository.ConnectionLogDetailFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import io.gravitee.apim.core.log.model.MessageOperation;
import io.gravitee.rest.api.management.v2.rest.model.ApiLog;
import io.gravitee.rest.api.management.v2.rest.model.ApiLogRequestContent;
import io.gravitee.rest.api.management.v2.rest.model.ApiLogResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiLogResponseContent;
import io.gravitee.rest.api.management.v2.rest.model.ApiLogsResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiMessageLog;
import io.gravitee.rest.api.management.v2.rest.model.ApiMessageLogContent;
import io.gravitee.rest.api.management.v2.rest.model.ApiMessageLogsResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiScoringTriggerResponse;
import io.gravitee.rest.api.management.v2.rest.model.BaseApplication;
import io.gravitee.rest.api.management.v2.rest.model.BasePlan;
import io.gravitee.rest.api.management.v2.rest.model.HttpMethod;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.model.PlanMode;
import io.gravitee.rest.api.management.v2.rest.model.PlanSecurity;
import io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType;
import io.gravitee.rest.api.management.v2.rest.model.ScoringStatus;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiResourceTest;
import io.gravitee.rest.api.management.v2.rest.resource.api.log.param.SearchLogsParam;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ApiScoringResourceTest extends ApiResourceTest {

    WebTarget evaluateTarget;

    @Inject
    ApiCrudServiceInMemory apiCrudService;

    @BeforeEach
    public void setup() {
        evaluateTarget = rootTarget().path("_evaluate");

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @Override
    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();

        Stream.of(apiCrudService).forEach(InMemoryAlternative::reset);
    }

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/scoring";
    }

    @Nested
    class Evaluate {

        @Test
        void should_return_404_if_not_found() {
            final Response response = evaluateTarget.request().post(null);
            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);

            MAPIAssertions
                .assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Api not found.");
        }

        @Test
        void should_return_202_response() {
            apiCrudService.initWith(List.of(ApiFixtures.aFederatedApi().toBuilder().id(API).build()));

            final Response response = evaluateTarget.request().post(null);

            MAPIAssertions
                .assertThat(response)
                .hasStatus(ACCEPTED_202)
                .asEntity(ApiScoringTriggerResponse.class)
                .isEqualTo(ApiScoringTriggerResponse.builder().status(ScoringStatus.PENDING).build());
        }
    }
}
