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
package io.gravitee.gamma.authorization.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.plugin.gamma.api.identity.AmNotConfiguredException;
import io.gravitee.gamma.authorization.core.am.exception.AmSyncConflictException;
import io.gravitee.gamma.authorization.core.am.use_case.GetAmUserSyncStatusUseCase;
import io.gravitee.gamma.authorization.core.am.use_case.StartAmUserSyncUseCase;
import io.gravitee.gamma.authorization.rest.dto.AmSyncStartResponse;
import io.gravitee.gamma.authorization.rest.dto.AmSyncStatusResponse;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

class AuthzAmUserSyncResourceTest extends JerseyTest {

    // TestGraviteeContextFilter pins the caller organization to this value.
    private static final String ORG = "test-org";

    private StartAmUserSyncUseCase startAmUserSyncUseCase;
    private GetAmUserSyncStatusUseCase getAmUserSyncStatusUseCase;

    @BeforeEach
    void resetMocks() {
        reset(startAmUserSyncUseCase, getAmUserSyncStatusUseCase);
    }

    @Override
    protected Application configure() {
        startAmUserSyncUseCase = mock(StartAmUserSyncUseCase.class);
        getAmUserSyncStatusUseCase = mock(GetAmUserSyncStatusUseCase.class);
        GenericApplicationContext emptySpringContext = new GenericApplicationContext();
        emptySpringContext.refresh();
        ResourceConfig config = new ResourceConfig()
            .register(TestGraviteeContextFilter.class)
            .register(new AuthzAmUserSyncResource(startAmUserSyncUseCase, getAmUserSyncStatusUseCase))
            .register(JacksonFeature.class);
        config.property("contextConfig", emptySpringContext);
        return config;
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(JacksonFeature.class);
    }

    private static AsyncJob job(String id, AsyncJob.Status status) {
        return AsyncJob.builder().id(id).sourceId(ORG).type(AsyncJob.Type.AM_USER_SYNC).status(status).build();
    }

    @Test
    void post_starts_a_sync_and_returns_202() {
        when(startAmUserSyncUseCase.execute(any()))
            .thenReturn(new StartAmUserSyncUseCase.Output(job("job-1", AsyncJob.Status.PENDING), 2000L));

        try (Response response = target("/users/sync").request().post(null)) {
            assertThat(response.getStatus()).isEqualTo(202);
            AmSyncStartResponse body = response.readEntity(AmSyncStartResponse.class);
            assertThat(body.jobId()).isEqualTo("job-1");
            assertThat(body.status()).isEqualTo("PENDING");
            assertThat(body.totalUsers()).isEqualTo(2000L);
        }
    }

    @Test
    void post_returns_409_when_a_sync_is_already_running() {
        when(startAmUserSyncUseCase.execute(any())).thenThrow(new AmSyncConflictException(ORG));

        try (Response response = target("/users/sync").request().post(null)) {
            assertThat(response.getStatus()).isEqualTo(409);
        }
    }

    @Test
    void post_returns_503_when_am_is_not_configured() {
        when(startAmUserSyncUseCase.execute(any())).thenThrow(new AmNotConfiguredException());

        try (Response response = target("/users/sync").request().post(null)) {
            assertThat(response.getStatus()).isEqualTo(503);
        }
    }

    @Test
    void get_returns_the_current_status() {
        AsyncJob completed = job("job-1", AsyncJob.Status.SUCCESS).toBuilder().upperLimit(4L).updatedAt(ZonedDateTime.now()).build();
        when(getAmUserSyncStatusUseCase.execute(any())).thenReturn(new GetAmUserSyncStatusUseCase.Output(Optional.of(completed)));

        try (Response response = target("/users/sync").request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            AmSyncStatusResponse body = response.readEntity(AmSyncStatusResponse.class);
            assertThat(body.jobId()).isEqualTo("job-1");
            assertThat(body.status()).isEqualTo("SUCCESS");
            assertThat(body.entitiesUpserted()).isEqualTo(4L);
        }
    }

    @Test
    void get_returns_the_failed_status_with_error_message() {
        AsyncJob failed = job("job-1", AsyncJob.Status.ERROR).toBuilder().errorMessage("AM unreachable").updatedAt(ZonedDateTime.now()).build();
        when(getAmUserSyncStatusUseCase.execute(any())).thenReturn(new GetAmUserSyncStatusUseCase.Output(Optional.of(failed)));

        try (Response response = target("/users/sync").request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            AmSyncStatusResponse body = response.readEntity(AmSyncStatusResponse.class);
            assertThat(body.status()).isEqualTo("ERROR");
            assertThat(body.error()).isEqualTo("AM unreachable");
        }
    }

    @Test
    void get_returns_404_when_no_sync_has_run() {
        when(getAmUserSyncStatusUseCase.execute(any())).thenReturn(new GetAmUserSyncStatusUseCase.Output(Optional.empty()));

        try (Response response = target("/users/sync").request().get()) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }
}
