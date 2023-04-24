/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.sync.process.repository.handler;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.services.sync.process.repository.DefaultSyncManager;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(VertxExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultSyncHandlerTest {

    @Mock
    private DefaultSyncManager defaultSyncManager;

    private SyncHandler cut;
    private WebClient client;

    @BeforeEach
    public void beforeEach(Vertx vertx, VertxTestContext testContext) throws Exception {
        MockitoAnnotations.openMocks(this);
        cut = new SyncHandler(defaultSyncManager);

        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());
        router.post("/path").handler(cut);

        int port = getRandomPort();

        client = WebClient.create(vertx, new WebClientOptions().setDefaultHost("localhost").setDefaultPort(port));

        vertx.deployVerticle(
            new AbstractVerticle() {
                @Override
                public void start() {
                    vertx.createHttpServer().requestHandler(router).listen(port);
                }
            },
            testContext.succeedingThenComplete()
        );
    }

    @AfterEach
    public void afterEach(Vertx vertx, VertxTestContext testContext) {
        vertx.close(testContext.succeedingThenComplete());
    }

    @Test
    void should_return_sync_manager_stats(VertxTestContext testContext) {
        when(defaultSyncManager.syncDone()).thenReturn(true);
        when(defaultSyncManager.syncCounter()).thenReturn(1L);
        when(defaultSyncManager.nextSyncTime()).thenReturn(1L);
        when(defaultSyncManager.lastSyncOnError()).thenReturn(true);
        when(defaultSyncManager.lastSyncErrorMessage()).thenReturn("error");
        when(defaultSyncManager.totalSyncOnError()).thenReturn(1L);

        client
            .post("/path")
            .expect(ResponsePredicate.SC_OK)
            .expect(ResponsePredicate.JSON)
            .as(BodyCodec.jsonObject())
            .send()
            .onComplete(
                testContext.succeeding(response ->
                    testContext.verify(() -> {
                        JsonObject responseBody = response.body();
                        assertThat(responseBody.getString("initialDone")).isEqualTo("true");
                        assertThat(responseBody.getString("counter")).isEqualTo("1");
                        assertThat(responseBody.getString("nextSyncTime")).isEqualTo("1");
                        assertThat(responseBody.getString("lastOnError")).isEqualTo("true");
                        assertThat(responseBody.getString("lastErrorMessage")).isEqualTo("error");
                        assertThat(responseBody.getString("totalOnErrors")).isEqualTo("1");
                        testContext.completeNow();
                    })
                )
            )
            .onFailure(testContext::failNow);
    }

    @Test
    void should_return_code_503_when_sync_manager_is_not_done(VertxTestContext testContext) {
        when(defaultSyncManager.syncDone()).thenReturn(false);
        when(defaultSyncManager.syncCounter()).thenReturn(1L);
        when(defaultSyncManager.nextSyncTime()).thenReturn(1L);
        when(defaultSyncManager.lastSyncOnError()).thenReturn(true);
        when(defaultSyncManager.lastSyncErrorMessage()).thenReturn("error");
        when(defaultSyncManager.totalSyncOnError()).thenReturn(1L);

        client
            .post("/path")
            .expect(ResponsePredicate.SC_SERVICE_UNAVAILABLE)
            .expect(ResponsePredicate.JSON)
            .as(BodyCodec.jsonObject())
            .send()
            .onComplete(
                testContext.succeeding(response ->
                    testContext.verify(() -> {
                        JsonObject responseBody = response.body();
                        assertThat(responseBody.getString("initialDone")).isEqualTo("false");
                        assertThat(responseBody.getString("counter")).isEqualTo("1");
                        assertThat(responseBody.getString("nextSyncTime")).isEqualTo("1");
                        assertThat(responseBody.getString("lastOnError")).isEqualTo("true");
                        assertThat(responseBody.getString("lastErrorMessage")).isEqualTo("error");
                        assertThat(responseBody.getString("totalOnErrors")).isEqualTo("1");
                        testContext.completeNow();
                    })
                )
            )
            .onFailure(testContext::failNow);
    }

    private int getRandomPort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }
}
