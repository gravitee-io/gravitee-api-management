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
package io.gravitee.rest.api.management.v2.rest.resource.asyncjob;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import fixtures.core.model.AsyncJobFixture;
import inmemory.AsyncJobCrudServiceInMemory;
import io.gravitee.apim.core.async_job.model.AsyncJobError;
import io.gravitee.rest.api.management.v2.rest.model.AsyncJob;
import io.gravitee.rest.api.management.v2.rest.model.AsyncJobStatus;
import io.gravitee.rest.api.management.v2.rest.model.AsyncJobsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AsyncJobsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";

    @Autowired
    AsyncJobCrudServiceInMemory asyncJobCrudServiceInMemory;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/async-jobs";
    }

    @BeforeEach
    void init() {
        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    void cleanUp() {
        asyncJobCrudServiceInMemory.reset();
        GraviteeContext.cleanContext();
    }

    @Test
    void should_return_progress_details_and_errors() {
        asyncJobCrudServiceInMemory.initWith(
            List.of(
                AsyncJobFixture.BASE.get()
                    .id("job-1")
                    .initiatorId(USER_NAME)
                    .environmentId(ENVIRONMENT)
                    .type(io.gravitee.apim.core.async_job.model.AsyncJob.Type.SCORING_REQUEST)
                    .upperLimit(40L)
                    .processed(12L)
                    .details("{\"imported\":10,\"failed\":2}")
                    .errors(
                        List.of(
                            new AsyncJobError("slug_conflict", "Slug already used", "agent-1"),
                            new AsyncJobError("fetch_failed", "Provider unavailable", null)
                        )
                    )
                    .build()
            )
        );

        var response = rootTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        var job = response.readEntity(AsyncJobsResponse.class).getData().getFirst();
        assertThat(job.getUpperLimit()).isEqualTo(40L);
        assertThat(job.getProcessed()).isEqualTo(12L);
        assertThat(job.getDetails()).isEqualTo(Map.of("imported", 10, "failed", 2));
        assertThat(job.getErrors()).containsExactly(
            new io.gravitee.rest.api.management.v2.rest.model.AsyncJobError()
                .code("slug_conflict")
                .message("Slug already used")
                .itemId("agent-1"),
            new io.gravitee.rest.api.management.v2.rest.model.AsyncJobError().code("fetch_failed").message("Provider unavailable")
        );
    }

    @Test
    void should_only_return_jobs_of_the_source() {
        asyncJobCrudServiceInMemory.initWith(
            List.of(
                aJobOfCurrentUser("job-1").toBuilder().sourceId("source-1").build(),
                aJobOfCurrentUser("job-2").toBuilder().sourceId("source-2").build()
            )
        );

        var response = rootTarget().queryParam("sourceId", "source-1").request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertThat(response.readEntity(AsyncJobsResponse.class).getData()).extracting(AsyncJob::getId).containsExactly("job-1");
    }

    @Test
    void should_filter_partial_jobs() {
        asyncJobCrudServiceInMemory.initWith(List.of(aJobOfCurrentUser("job-1").partial(), aJobOfCurrentUser("job-2").complete()));

        var response = rootTarget().queryParam("status", "PARTIAL").request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertThat(response.readEntity(AsyncJobsResponse.class).getData())
            .extracting(AsyncJob::getId, AsyncJob::getStatus)
            .containsExactly(tuple("job-1", AsyncJobStatus.PARTIAL));
    }

    @Test
    void should_filter_pending_aim_discovery_jobs_of_a_source() {
        var discovery = aJobOfCurrentUser("discovery")
            .toBuilder()
            .sourceId("agent-source")
            .type(io.gravitee.apim.core.async_job.model.AsyncJob.Type.AIM_AGENT_DISCOVERY)
            .build();
        asyncJobCrudServiceInMemory.initWith(
            List.of(
                discovery,
                discovery.toBuilder().id("finished-discovery").build().complete(),
                discovery.toBuilder().id("import").type(io.gravitee.apim.core.async_job.model.AsyncJob.Type.AIM_AGENT_IMPORT).build(),
                discovery.toBuilder().id("other-source").sourceId("another-agent-source").build()
            )
        );

        var response = rootTarget()
            .queryParam("sourceId", "agent-source")
            .queryParam("type", "AIM_AGENT_DISCOVERY")
            .queryParam("status", "PENDING")
            .request()
            .get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertThat(response.readEntity(AsyncJobsResponse.class).getData())
            .extracting(AsyncJob::getId, AsyncJob::getType)
            .containsExactly(tuple("discovery", AsyncJob.TypeEnum.AIM_AGENT_DISCOVERY));
    }

    @Test
    void should_not_return_jobs_started_by_another_user() {
        asyncJobCrudServiceInMemory.initWith(
            List.of(aJobOfCurrentUser("job-1"), aJobOfCurrentUser("job-2").toBuilder().initiatorId("another-user").build())
        );

        var response = rootTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        assertThat(response.readEntity(AsyncJobsResponse.class).getData()).extracting(AsyncJob::getId).containsExactly("job-1");
    }

    @Test
    void should_return_403_without_api_read_permission() {
        shouldReturn403(RolePermission.ENVIRONMENT_API, ENVIRONMENT, RolePermissionAction.READ, () -> rootTarget().request().get());
    }

    private static io.gravitee.apim.core.async_job.model.AsyncJob aJobOfCurrentUser(String id) {
        return AsyncJobFixture.anAsyncJob(id).toBuilder().initiatorId(USER_NAME).environmentId(ENVIRONMENT).build();
    }
}
