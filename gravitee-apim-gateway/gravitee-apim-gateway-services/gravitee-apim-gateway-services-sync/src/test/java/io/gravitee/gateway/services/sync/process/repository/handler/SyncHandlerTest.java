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
package io.gravitee.gateway.services.sync.process.repository.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncMetrics;
import io.gravitee.gateway.services.sync.process.repository.DefaultSyncManager;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SyncHandlerTest {

    private DefaultSyncManager syncManager;
    private RoutingContext ctx;
    private HttpServerResponse response;

    @BeforeEach
    void setUp() {
        syncManager = mock(DefaultSyncManager.class);
        ctx = mock(RoutingContext.class);
        response = mock(HttpServerResponse.class);
        when(ctx.response()).thenReturn(response);
        when(syncManager.syncDone()).thenReturn(true);
    }

    @Test
    void should_not_expose_distributed_status_when_distributed_sync_is_disabled() {
        when(syncManager.distributedSyncEnabled()).thenReturn(false);

        JsonObject body = handle();

        assertThat(body.containsKey("distributed")).isFalse();
        assertThat(body.containsKey("lastSuccessfulSyncAt")).isTrue();
    }

    @Test
    void should_expose_role_and_failure_counters_when_distributed_sync_is_enabled() {
        DistributedSyncMetrics metrics = new DistributedSyncMetrics();
        metrics.recordDeadLetter(DistributedEventType.SUBSCRIPTION, "sub");
        metrics.recordDistributionFailure();
        when(syncManager.distributedSyncEnabled()).thenReturn(true);
        when(syncManager.actingPrimary()).thenReturn(true);
        when(syncManager.distributedSyncMetrics()).thenReturn(Optional.of(metrics));

        JsonObject distributed = handle().getJsonObject("distributed");

        assertThat(distributed.getString("role")).isEqualTo("primary");
        assertThat(distributed.getLong("deadLetters")).isEqualTo(1L);
        assertThat(distributed.getJsonObject("deadLettersByType").getLong("SUBSCRIPTION")).isEqualTo(1L);
        assertThat(distributed.getLong("distributionFailures")).isEqualTo(1L);
    }

    private JsonObject handle() {
        new SyncHandler(syncManager).handle(ctx);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(response).write(body.capture());
        return new JsonObject(body.getValue());
    }
}
