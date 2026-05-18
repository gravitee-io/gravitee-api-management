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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class EventBusAuthzEnginePortTest {

    private Vertx vertx;
    private EventBusAuthzEnginePort port;
    private final ConcurrentLinkedQueue<JsonObject> received = new ConcurrentLinkedQueue<>();
    private MessageConsumer<JsonObject> fakePluginConsumer;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        port = new EventBusAuthzEnginePort(vertx);
        fakePluginConsumer = vertx
            .eventBus()
            .<JsonObject>consumer(EventBusAuthzEnginePort.SYNC_ADDRESS, msg -> {
                received.add(msg.body());
                msg.reply(new JsonObject());
            });
    }

    @AfterEach
    void tearDown() {
        if (fakePluginConsumer != null) {
            fakePluginConsumer.unregister();
        }
        vertx.close();
    }

    @Test
    void address_literal_is_pinned_to_match_the_plugin_side(VertxTestContext ctx) {
        assertThat(EventBusAuthzEnginePort.SYNC_ADDRESS).isEqualTo("service:authz-pdp:sync");
        ctx.completeNow();
    }

    @Test
    void addOrUpdateEntity_publishes_op_with_uid_attributes_parents(VertxTestContext ctx) {
        port
            .addOrUpdateEntity("Resource::\"api.bookings\"", Map.of("region", "eu", "active", true), List.of("Resource::\"api.parent\""))
            .subscribe(
                () -> {
                    ctx.verify(() -> {
                        JsonObject body = received.poll();
                        assertThat(body).isNotNull();
                        assertThat(body.getString("op")).isEqualTo("addOrUpdateEntity");
                        assertThat(body.getString("uid")).isEqualTo("Resource::\"api.bookings\"");
                        assertThat(body.getJsonObject("attributes").getString("region")).isEqualTo("eu");
                        assertThat(body.getJsonObject("attributes").getBoolean("active")).isTrue();
                        assertThat(body.getJsonArray("parents").getString(0)).isEqualTo("Resource::\"api.parent\"");
                    });
                    ctx.completeNow();
                },
                ctx::failNow
            );
    }

    @Test
    void addOrUpdateEntity_omits_attributes_field_when_empty(VertxTestContext ctx) {
        port
            .addOrUpdateEntity("Resource::\"api.x\"", Map.of(), List.of())
            .subscribe(
                () -> {
                    ctx.verify(() -> {
                        JsonObject body = received.poll();
                        assertThat(body.containsKey("attributes")).isFalse();
                        assertThat(body.containsKey("parents")).isFalse();
                    });
                    ctx.completeNow();
                },
                ctx::failNow
            );
    }

    @Test
    void removeEntity_publishes_op_and_uid_only(VertxTestContext ctx) {
        port
            .removeEntity("Resource::\"api.bookings\"")
            .subscribe(
                () -> {
                    ctx.verify(() -> {
                        JsonObject body = received.poll();
                        assertThat(body.getString("op")).isEqualTo("removeEntity");
                        assertThat(body.getString("uid")).isEqualTo("Resource::\"api.bookings\"");
                    });
                    ctx.completeNow();
                },
                ctx::failNow
            );
    }

    @Test
    void addOrUpdatePolicy_publishes_op_docId_name_policyText(VertxTestContext ctx) {
        port
            .addOrUpdatePolicy("p-1", "Allow read", "permit(principal, action, resource);")
            .subscribe(
                () -> {
                    ctx.verify(() -> {
                        JsonObject body = received.poll();
                        assertThat(body.getString("op")).isEqualTo("addOrUpdatePolicy");
                        assertThat(body.getString("docId")).isEqualTo("p-1");
                        assertThat(body.getString("name")).isEqualTo("Allow read");
                        assertThat(body.getString("policyText")).isEqualTo("permit(principal, action, resource);");
                    });
                    ctx.completeNow();
                },
                ctx::failNow
            );
    }

    @Test
    void removePolicy_publishes_op_and_docId(VertxTestContext ctx) {
        port
            .removePolicy("p-1")
            .subscribe(
                () -> {
                    ctx.verify(() -> {
                        JsonObject body = received.poll();
                        assertThat(body.getString("op")).isEqualTo("removePolicy");
                        assertThat(body.getString("docId")).isEqualTo("p-1");
                    });
                    ctx.completeNow();
                },
                ctx::failNow
            );
    }

    @Test
    void commit_publishes_just_op(VertxTestContext ctx) {
        port
            .commit()
            .subscribe(
                () -> {
                    ctx.verify(() -> {
                        JsonObject body = received.poll();
                        assertThat(body.getString("op")).isEqualTo("commit");
                        assertThat(body.size()).isEqualTo(1);
                    });
                    ctx.completeNow();
                },
                ctx::failNow
            );
    }

    @Test
    void no_handlers_propagates_as_completable_error(VertxTestContext ctx) {
        fakePluginConsumer.unregister();

        port
            .commit()
            .subscribe(
                () -> ctx.failNow("Expected error when no handlers registered"),
                t -> {
                    ctx.verify(() -> {
                        assertThat(t).isInstanceOf(ReplyException.class);
                        assertThat(((ReplyException) t).failureType().name()).isEqualTo("NO_HANDLERS");
                    });
                    ctx.completeNow();
                }
            );
    }

    @Test
    void slow_consumer_propagates_as_timeout_so_a_wedged_pdp_does_not_stall_the_cycle(VertxTestContext ctx) {
        // I4: without an explicit DeliveryOptions timeout, Vertx defaults to 30s — long enough
        // to wedge a sync cycle when the PDP plugin is unhealthy. Replace the fast consumer with
        // one that never replies and verify the call fails fast.
        fakePluginConsumer.unregister();
        MessageConsumer<JsonObject> blackHole = vertx.eventBus().<JsonObject>consumer(EventBusAuthzEnginePort.SYNC_ADDRESS, msg -> {});

        long start = System.currentTimeMillis();
        port
            .commit()
            .subscribe(
                () -> {
                    blackHole.unregister();
                    ctx.failNow("Expected timeout when consumer never replies");
                },
                t -> {
                    blackHole.unregister();
                    ctx.verify(() -> {
                        assertThat(t).isInstanceOf(ReplyException.class);
                        assertThat(((ReplyException) t).failureType().name()).isEqualTo("TIMEOUT");
                        long elapsed = System.currentTimeMillis() - start;
                        assertThat(elapsed)
                            .as("Should fail at port's configured timeout, not Vertx default 30s")
                            .isLessThan(EventBusAuthzEnginePort.REPLY_TIMEOUT_MS + 2_000L);
                    });
                    ctx.completeNow();
                }
            );
    }
}
