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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.services.sync.process.repository.DefaultSyncManager;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DistributedResyncHandlerTest {

    private DefaultSyncManager syncManager;
    private RoutingContext ctx;
    private HttpServerResponse response;

    @BeforeEach
    void setUp() {
        syncManager = mock(DefaultSyncManager.class);
        ctx = mock(RoutingContext.class);
        response = mock(HttpServerResponse.class);
        when(ctx.response()).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
    }

    @Test
    void should_accept_the_rebuild_on_the_primary() {
        when(syncManager.requestDistributedResync()).thenReturn(true);

        new DistributedResyncHandler(syncManager).handle(ctx);

        verify(response).setStatusCode(HttpStatusCode.ACCEPTED_202);
        assertThat(body().getBoolean("accepted")).isTrue();
    }

    @Test
    void should_reject_the_rebuild_on_a_secondary() {
        when(syncManager.requestDistributedResync()).thenReturn(false);

        new DistributedResyncHandler(syncManager).handle(ctx);

        verify(response).setStatusCode(HttpStatusCode.CONFLICT_409);
        assertThat(body().getBoolean("accepted")).isFalse();
    }

    private JsonObject body() {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(response).end(body.capture());
        return new JsonObject(body.getValue());
    }
}
