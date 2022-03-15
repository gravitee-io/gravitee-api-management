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

import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.management.model.Subscription;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
public class ApiKeysHandlerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @InjectMocks
    private final ApiKeysHandler apiKeysHandler = new ApiKeysHandler();

    private WebClient client;
    private Vertx vertx;

    @Before
    public void setUp(TestContext context) throws Exception {
        MockitoAnnotations.openMocks(this);

        vertx = Vertx.vertx();
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());
        router.post("/_search").handler(apiKeysHandler::findByCriteria);

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
    public void searchShouldRespondWithApiAndPlanAndSubscriptionProperties(TestContext context) throws TechnicalException {
        Subscription subscription = new Subscription();
        subscription.setId("subscription-id");
        subscription.setApi("subscription-api-id");
        subscription.setPlan("subscription-plan-id");

        ApiKey apiKey = new ApiKey();
        apiKey.setSubscriptions(List.of("subscription-id"));

        when(apiKeyRepository.findByCriteria(any())).thenReturn(List.of(apiKey));
        when(subscriptionRepository.findByIdIn(argThat(ids -> apiKey.getSubscriptions().containsAll(ids))))
            .thenReturn(List.of(subscription));

        Async async = context.async();

        client
            .post("/_search")
            .expect(ResponsePredicate.SC_OK)
            .expect(ResponsePredicate.JSON)
            .as(BodyCodec.jsonArray())
            .sendJsonObject(new JsonObject())
            .onSuccess(
                response -> {
                    JsonArray responseBody = response.body();
                    context.assertEquals(1, responseBody.size());
                    JsonObject responseApiKey = responseBody.getJsonObject(0);
                    context.assertEquals("subscription-id", responseApiKey.getString("subscription"));
                    context.assertEquals("subscription-api-id", responseApiKey.getString("api"));
                    context.assertEquals("subscription-plan-id", responseApiKey.getString("plan"));
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

    private int getRandomPort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }
}
