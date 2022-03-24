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
package io.gravitee.repository.bridge.server.handler;

import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.Subscription;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.handler.BodyHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author GraviteeSource Team
 */
@RunWith(VertxUnitRunner.class)
public class SubscriptionsHandlerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private final SubscriptionsHandler subscriptionsHandler = new SubscriptionsHandler();

    private WebClient client;
    private Vertx vertx;

    @Before
    public void setUp(TestContext context) throws Exception {
        MockitoAnnotations.openMocks(this);

        vertx = Vertx.vertx();
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());
        router.post("/_findByIds").handler(subscriptionsHandler::findByIds);

        int port = getRandomPort();

        client = WebClient.create(vertx, new WebClientOptions().setDefaultHost("localhost").setDefaultPort(port));

        vertx.deployVerticle(
            new AbstractVerticle() {
                @Override
                public void start() {
                    vertx.createHttpServer().requestHandler(router).listen(port);
                }
            },
            context.asyncAssertSuccess()
        );
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void findByIds_should_return_subscription_list(TestContext context) throws TechnicalException {
        when(subscriptionRepository.findByIdIn(List.of("id1", "idX", "id4")))
            .thenReturn(List.of(buildSubscription("sub-id1"), buildSubscription("sub-idX"), buildSubscription("sub-id4")));

        Async async = context.async();

        client
            .post("/_findByIds")
            .expect(ResponsePredicate.SC_OK)
            .expect(ResponsePredicate.JSON)
            .as(BodyCodec.jsonArray())
            .sendJson(List.of("id1", "idX", "id4"))
            .onSuccess(
                response -> {
                    JsonArray responseBody = response.body();
                    context.assertEquals(3, responseBody.size());
                    context.assertEquals("sub-id1", responseBody.getJsonObject(0).getString("id"));
                    context.assertEquals("sub-idX", responseBody.getJsonObject(1).getString("id"));
                    context.assertEquals("sub-id4", responseBody.getJsonObject(2).getString("id"));
                    async.complete();
                }
            )
            .onFailure(
                err -> {
                    context.fail(err);
                    async.complete();
                }
            );
    }

    @Test
    public void findByIds_with_empty_id_list_returns_http_500_server_error(TestContext context) {
        Async async = context.async();

        client
            .post("/_findByIds")
            .expect(ResponsePredicate.SC_INTERNAL_SERVER_ERROR)
            .sendJson(List.of())
            .onSuccess(response -> async.complete())
            .onFailure(
                err -> {
                    context.fail(err);
                    async.complete();
                }
            );
    }

    @Test
    public void findByIds_without_id_list_returns_http_500_server_error(TestContext context) {
        Async async = context.async();
        client
            .post("/_findByIds")
            .expect(ResponsePredicate.SC_INTERNAL_SERVER_ERROR)
            .send()
            .onSuccess(response -> async.complete())
            .onFailure(
                err -> {
                    context.fail(err);
                    async.complete();
                }
            );
    }

    private int getRandomPort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }

    private Subscription buildSubscription(String id) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        return subscription;
    }
}
