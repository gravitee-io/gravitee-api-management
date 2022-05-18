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
package io.gravitee.gateway.standalone.websocket;

import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.junit.rules.ApiDeployer;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.*;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/websocket/teams.json")
@RunWith(VertxUnitRunner.class)
public class WebsocketAcceptTest extends AbstractWebSocketGatewayTest {

    private final long ACCEPT_TIMEOUT = 5000;

    @Rule
    public final TestRule chain = RuleChain.outerRule(new ApiDeployer(this));

    private final Vertx vertx = Vertx.vertx();

    @Before
    public void setUp(TestContext context) {
        Vertx vertx = Vertx.vertx();

        vertx
            .createHttpServer()
            .webSocketHandler(
                event -> {
                    event.accept();
                    event.writeTextMessage("PING");
                }
            )
            .listen(16664, context.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void websocket_accepted_request(TestContext context) {
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(8082).setDefaultHost("localhost"));

        // Expect two frames: one with the text data, and another one for the close event
        Async async = context.async(2);

        httpClient
            .webSocket("/test")
            .onSuccess(
                socket -> {
                    socket.frameHandler(
                        frame -> {
                            if (!frame.isClose()) {
                                context.assertTrue(frame.isText());
                                context.assertEquals("PING", frame.textData());
                                async.countDown();
                            }
                            async.countDown();
                        }
                    );
                }
            )
            .onFailure(
                err -> {
                    context.fail(err);
                    async.complete();
                }
            );

        async.awaitSuccess(ACCEPT_TIMEOUT);
    }
}
