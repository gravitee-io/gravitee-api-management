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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonArray;
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
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(VertxExtension.class)
public class EventsLatestHandlerTest {

    private final WorkerExecutor executor = Vertx.vertx().createSharedWorkerExecutor("eventLatestHandlerTestWorker");

    @Mock
    private EventLatestRepository eventLatestRepository;

    @InjectMocks
    private final EventsLatestHandler eventsLatestHandler = new EventsLatestHandler(executor);

    private WebClient client;

    @BeforeEach
    public void beforeEach(Vertx vertx, VertxTestContext testContext) throws Exception {
        MockitoAnnotations.openMocks(this);

        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());
        router.post("/_search").handler(eventsLatestHandler::search);
        router.post("/_createOrPatch").handler(eventsLatestHandler::createOrPatch);

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
        executor.close();
    }

    @Nested
    class SearchTest {

        @Test
        void should_return_events_without_criteria_group_or_pagination(VertxTestContext testContext) {
            Event event = new Event();
            event.setId("eventId");

            when(eventLatestRepository.search(EventCriteria.builder().build(), null, null, null)).thenReturn(List.of(event));

            client
                .post("/_search")
                .expect(ResponsePredicate.SC_OK)
                .expect(ResponsePredicate.JSON)
                .as(BodyCodec.jsonArray())
                .send()
                .onComplete(
                    testContext.succeeding(response ->
                        testContext.verify(() -> {
                            JsonArray responseBody = response.body();
                            assertThat(responseBody.size()).isEqualTo(1);
                            JsonObject responseEvent = responseBody.getJsonObject(0);
                            assertThat(responseEvent.getString("id")).isEqualTo("eventId");
                            testContext.completeNow();
                        })
                    )
                )
                .onFailure(testContext::failNow);
        }

        @Test
        void should_return_events_with_criteria_group_and_pagination(VertxTestContext testContext) {
            Event event = new Event();
            event.setId("eventId");

            EventCriteria eventCriteria = EventCriteria
                .builder()
                .to(123)
                .from(123)
                .environments(Set.of("env"))
                .type(EventType.START_API)
                .property("property", "value")
                .build();

            when(eventLatestRepository.search(eventCriteria, Event.EventProperties.API_ID, 1L, 100L)).thenReturn(List.of(event));

            client
                .post("/_search")
                .addQueryParam("page", "1")
                .addQueryParam("size", "100")
                .addQueryParam("group", "API_ID")
                .expect(ResponsePredicate.SC_OK)
                .expect(ResponsePredicate.JSON)
                .as(BodyCodec.jsonArray())
                .sendJson(eventCriteria)
                .onComplete(
                    testContext.succeeding(response ->
                        testContext.verify(() -> {
                            JsonArray responseBody = response.body();
                            assertThat(responseBody.size()).isEqualTo(1);
                            JsonObject responseEvent = responseBody.getJsonObject(0);
                            assertThat(responseEvent.getString("id")).isEqualTo("eventId");
                            testContext.completeNow();
                        })
                    )
                )
                .onFailure(testContext::failNow);
        }

        @Test
        void should_return_500_code_errors_when_repository_throw_exception(VertxTestContext testContext) {
            when(eventLatestRepository.search(EventCriteria.builder().build(), Event.EventProperties.API_ID, 1L, 100L))
                .thenThrow(new RuntimeException());

            client
                .post("/_search")
                .addQueryParam("page", "1")
                .addQueryParam("size", "100")
                .addQueryParam("group", "API_ID")
                .expect(ResponsePredicate.SC_INTERNAL_SERVER_ERROR)
                .as(BodyCodec.string())
                .send()
                .onComplete(
                    testContext.succeeding(response ->
                        testContext.verify(() -> {
                            String responseBody = response.body();
                            assertThat(responseBody).isNotNull();
                            testContext.completeNow();
                        })
                    )
                )
                .onFailure(testContext::failNow);
        }
    }

    @Nested
    class CreateOrPatchTest {

        @Test
        void should_return_created_event(VertxTestContext testContext) throws TechnicalException {
            Event event = new Event();
            event.setId("eventId");

            when(eventLatestRepository.createOrPatch(event)).thenReturn(event);

            client
                .post("/_createOrPatch")
                .expect(ResponsePredicate.SC_OK)
                .expect(ResponsePredicate.JSON)
                .as(BodyCodec.json(Event.class))
                .sendJson(event)
                .onComplete(
                    testContext.succeeding(response ->
                        testContext.verify(() -> {
                            Event eventReceive = response.body();
                            assertThat(eventReceive).isEqualTo(event);
                            testContext.completeNow();
                        })
                    )
                )
                .onFailure(testContext::failNow);
        }

        @Test
        void should_return_500_code_errors_when_repository_throw_exception(VertxTestContext testContext) throws TechnicalException {
            Event event = new Event();
            event.setId("eventId");

            when(eventLatestRepository.createOrPatch(event)).thenThrow(new RuntimeException());

            client
                .post("/_createOrPatch")
                .expect(ResponsePredicate.SC_INTERNAL_SERVER_ERROR)
                .as(BodyCodec.string())
                .send()
                .onComplete(
                    testContext.succeeding(response ->
                        testContext.verify(() -> {
                            String errorReceive = response.body();
                            assertThat(errorReceive).isNotNull();
                            testContext.completeNow();
                        })
                    )
                )
                .onFailure(testContext::failNow);
        }
    }

    private int getRandomPort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }
}
