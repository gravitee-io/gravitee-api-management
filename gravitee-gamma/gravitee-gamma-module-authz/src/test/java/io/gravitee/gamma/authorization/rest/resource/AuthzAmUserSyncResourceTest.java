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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnectionRepository;
import io.gravitee.apim.plugin.gamma.api.identity.AmNotConfiguredException;
import io.gravitee.gamma.authorization.am.AmSyncConflictException;
import io.gravitee.gamma.authorization.am.AmSyncJobManager;
import io.gravitee.gamma.authorization.am.AmSyncJobState;
import io.gravitee.gamma.authorization.rest.dto.AmSyncStartResponse;
import io.gravitee.gamma.authorization.rest.dto.AmSyncStatusResponse;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
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
    private static final AmConnection CONNECTION = new AmConnection("http://am:8093", "token", "domain-1", "domain-hrid", null);

    private AmConnectionRepository amConnectionRepository;
    private AmSyncJobManager jobManager;

    @BeforeEach
    void resetMocks() {
        reset(amConnectionRepository, jobManager);
    }

    @Override
    protected Application configure() {
        amConnectionRepository = mock(AmConnectionRepository.class);
        jobManager = mock(AmSyncJobManager.class);
        GenericApplicationContext emptySpringContext = new GenericApplicationContext();
        emptySpringContext.refresh();
        ResourceConfig config = new ResourceConfig()
            .register(TestGraviteeContextFilter.class)
            .register(new AuthzAmUserSyncResource(amConnectionRepository, jobManager))
            .register(JacksonFeature.class);
        config.property("contextConfig", emptySpringContext);
        return config;
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(JacksonFeature.class);
    }

    @Test
    void post_starts_a_sync_and_returns_202() {
        when(amConnectionRepository.requireByOrg(ORG)).thenReturn(CONNECTION);
        when(jobManager.start(any(), eq(CONNECTION))).thenReturn(AmSyncJobState.running("job-1", Instant.now()));

        try (Response response = target("/users/sync").request().post(null)) {
            assertThat(response.getStatus()).isEqualTo(202);
            AmSyncStartResponse body = response.readEntity(AmSyncStartResponse.class);
            assertThat(body.jobId()).isEqualTo("job-1");
            assertThat(body.status()).isEqualTo("RUNNING");
        }
    }

    @Test
    void post_returns_409_when_a_sync_is_already_running() {
        when(amConnectionRepository.requireByOrg(ORG)).thenReturn(CONNECTION);
        when(jobManager.start(any(), any())).thenThrow(new AmSyncConflictException(ORG));

        try (Response response = target("/users/sync").request().post(null)) {
            assertThat(response.getStatus()).isEqualTo(409);
        }
    }

    @Test
    void post_returns_503_when_am_is_not_configured() {
        when(amConnectionRepository.requireByOrg(ORG)).thenThrow(new AmNotConfiguredException());

        try (Response response = target("/users/sync").request().post(null)) {
            assertThat(response.getStatus()).isEqualTo(503);
        }
    }

    @Test
    void get_returns_the_current_status() {
        AmSyncJobState completed = AmSyncJobState.running("job-1", Instant.now()).completed(4, 4, Instant.now());
        when(jobManager.getStatus(ORG)).thenReturn(Optional.of(completed));

        try (Response response = target("/users/sync").request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            AmSyncStatusResponse body = response.readEntity(AmSyncStatusResponse.class);
            assertThat(body.jobId()).isEqualTo("job-1");
            assertThat(body.status()).isEqualTo("COMPLETED");
            assertThat(body.usersFetched()).isEqualTo(4);
            assertThat(body.entitiesUpserted()).isEqualTo(4);
        }
    }

    @Test
    void get_returns_404_when_no_sync_has_run() {
        when(jobManager.getStatus(ORG)).thenReturn(Optional.empty());

        try (Response response = target("/users/sync").request().get()) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }
}
