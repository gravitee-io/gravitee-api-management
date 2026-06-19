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
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class EventBusAuthzEnginePortTest {

    private static final String ENV = "env-1";
    private static final String SCOPE = "api-a";
    private static final String SCOPE_ADDRESS = EventBusAuthzEnginePort.SCOPE_ADDRESS_PREFIX + ENV + ":" + SCOPE;

    private Vertx vertx;
    private EventBusAuthzEnginePort port;
    private final ConcurrentLinkedQueue<JsonObject> received = new ConcurrentLinkedQueue<>();
    private MessageConsumer<JsonObject> fakePluginConsumer;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        // These tests exercise routing/commit, not per-node sharding — treat every scope as hosted.
        port = new EventBusAuthzEnginePort(io.vertx.rxjava3.core.Vertx.newInstance(vertx), HostedScopesFixtures.servingAll());
        fakePluginConsumer = vertx
            .eventBus()
            .<JsonObject>consumer(SCOPE_ADDRESS, msg -> {
                received.add(msg.body());
                msg.reply(new JsonObject().put("commitGeneration", 1L));
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
        assertThat(EventBusAuthzEnginePort.SCOPE_ADDRESS_PREFIX).isEqualTo("service:authz-pdp:sync:scope:");
        ctx.completeNow();
    }

    @Test
    void addOrUpdateEntity_publishes_op_with_uid_attributes_parents(VertxTestContext ctx) {
        port
            .addOrUpdateEntity(
                ENV,
                "Resource::\"api.bookings\"",
                Map.of("region", "eu", "active", true),
                List.of("Resource::\"api.parent\""),
                Set.of(SCOPE)
            )
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
            .addOrUpdateEntity(ENV, "Resource::\"api.x\"", Map.of(), List.of(), Set.of(SCOPE))
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
            .removeEntity(ENV, "Resource::\"api.bookings\"", Set.of(SCOPE))
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
    void tag_scoped_default_routes_to_the_default_engine_on_a_matching_node(VertxTestContext ctx) {
        EventBusAuthzEnginePort tagPort = new EventBusAuthzEnginePort(
            io.vertx.rxjava3.core.Vertx.newInstance(vertx),
            new AuthzHostedScopes(Set.of("us"))
        );
        ConcurrentLinkedQueue<JsonObject> defaultReceived = new ConcurrentLinkedQueue<>();
        vertx
            .eventBus()
            .<JsonObject>consumer(EventBusAuthzEnginePort.DEFAULT_ADDRESS, msg -> {
                defaultReceived.add(msg.body());
                msg.reply(new JsonObject().put("commitGeneration", 1L));
            });

        tagPort
            .addOrUpdatePolicy(ENV, "doc-1", "p", "permit(principal, action, resource);", Set.of("default@us"))
            .subscribe(
                () -> {
                    ctx.verify(() -> {
                        JsonObject body = defaultReceived.poll();
                        assertThat(body).isNotNull();
                        assertThat(body.getString("op")).isEqualTo("addOrUpdatePolicy");
                    });
                    ctx.completeNow();
                },
                ctx::failNow
            );
    }

    @Test
    void tag_scoped_default_is_skipped_on_a_node_without_that_tag(VertxTestContext ctx) {
        EventBusAuthzEnginePort tagPort = new EventBusAuthzEnginePort(
            io.vertx.rxjava3.core.Vertx.newInstance(vertx),
            new AuthzHostedScopes(Set.of("eu"))
        );
        ConcurrentLinkedQueue<JsonObject> defaultReceived = new ConcurrentLinkedQueue<>();
        vertx
            .eventBus()
            .<JsonObject>consumer(EventBusAuthzEnginePort.DEFAULT_ADDRESS, msg -> {
                defaultReceived.add(msg.body());
                msg.reply(new JsonObject().put("commitGeneration", 1L));
            });

        // The node carries tag "eu", not "us" — "default@us" is not served here, so routing is a no-op.
        tagPort
            .addOrUpdatePolicy(ENV, "doc-1", "p", "permit(principal, action, resource);", Set.of("default@us"))
            .subscribe(
                () -> {
                    ctx.verify(() -> assertThat(defaultReceived).isEmpty());
                    ctx.completeNow();
                },
                ctx::failNow
            );
    }

    @Test
    void wildcard_target_also_routes_to_the_default_engine(VertxTestContext ctx) {
        // "*" expands to the node's hosted scopes plus the always-on default engine. The default engine
        // listens on the unscoped address, so a wildcard doc must still reach it — otherwise default-scoped
        // evaluations would never see a "*" policy/entity.
        ConcurrentLinkedQueue<JsonObject> defaultReceived = new ConcurrentLinkedQueue<>();
        vertx
            .eventBus()
            .<JsonObject>consumer(EventBusAuthzEnginePort.DEFAULT_ADDRESS, msg -> {
                defaultReceived.add(msg.body());
                msg.reply(new JsonObject().put("commitGeneration", 1L));
            });

        port
            .addOrUpdatePolicy(ENV, "doc-1", "p", "permit(principal, action, resource);", Set.of("*"))
            .subscribe(
                () -> {
                    ctx.verify(() -> {
                        JsonObject body = defaultReceived.poll();
                        assertThat(body).isNotNull();
                        assertThat(body.getString("op")).isEqualTo("addOrUpdatePolicy");
                        assertThat(body.getString("docId")).isEqualTo("doc-1");
                    });
                    ctx.completeNow();
                },
                ctx::failNow
            );
    }

    @Test
    void addOrUpdatePolicy_publishes_op_docId_name_policyText(VertxTestContext ctx) {
        port
            .addOrUpdatePolicy(ENV, "p-1", "Allow read", "permit(principal, action, resource);", Set.of(SCOPE))
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
            .removePolicy(ENV, "p-1", Set.of(SCOPE))
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
            .addOrUpdatePolicy(ENV, "p-1", "Allow read", "permit(principal, action, resource);", Set.of(SCOPE))
            .andThen(port.commit())
            .subscribe(
                () -> {
                    ctx.verify(() -> {
                        received.poll(); // the staged mutation
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
    void commit_to_an_evicted_address_completes_instead_of_failing_the_cycle(VertxTestContext ctx) {
        // The scope is armed (staged successfully), then its plugin consumer is torn down (eviction)
        // before the commit lands. A per-address NO_HANDLERS must NOT fail the commit — a scope that
        // was evicted between stage and commit cannot be allowed to wedge the whole sync cycle.
        port
            .addOrUpdatePolicy(ENV, "p-1", "Allow read", "permit(principal, action, resource);", Set.of(SCOPE))
            .subscribe(
                () -> {
                    fakePluginConsumer.unregister();
                    port.commit().subscribe(ctx::completeNow, t -> ctx.failNow("Per-address NO_HANDLERS must be swallowed, got: " + t));
                },
                ctx::failNow
            );
    }

    @Test
    void one_evicted_address_does_not_block_the_commit_for_the_others(VertxTestContext ctx) {
        String deadScope = "scope-evicted";
        String deadAddress = EventBusAuthzEnginePort.SCOPE_ADDRESS_PREFIX + ENV + ":" + deadScope;
        // Register a consumer for the dead scope only long enough to stage successfully, then drop it.
        MessageConsumer<JsonObject> deadConsumer = vertx
            .eventBus()
            .<JsonObject>consumer(deadAddress, msg -> msg.reply(new JsonObject().put("commitGeneration", 1L)));

        port
            .addOrUpdatePolicy(ENV, "p-dead", "x", "permit();", Set.of(deadScope))
            .andThen(port.addOrUpdatePolicy(ENV, "p-live", "x", "permit();", Set.of(SCOPE)))
            .subscribe(
                () -> {
                    deadConsumer.unregister();
                    received.clear(); // drop the staged mutations; keep only what the commit produces
                    port
                        .commit()
                        .subscribe(
                            () -> {
                                ctx.verify(() -> {
                                    // the live scope still received its commit (the dead one was skipped)
                                    JsonObject live = received.poll();
                                    assertThat(live).isNotNull();
                                    assertThat(live.getString("op")).isEqualTo("commit");
                                });
                                ctx.completeNow();
                            },
                            t -> ctx.failNow("One evicted address must not fail the whole commit, got: " + t)
                        );
                },
                ctx::failNow
            );
    }

    @Test
    void a_failed_stage_does_not_arm_its_address_for_commit(VertxTestContext ctx) {
        // Stage a mutation against a scope whose plugin consumer is NOT registered: the staging send
        // fails with NO_HANDLERS. That failed stage must NOT leave the address armed, otherwise the
        // next commit would seal a generation the engine never received the mutation for (fail-open).
        String missingScope = "scope-without-consumer";
        port
            .addOrUpdatePolicy(ENV, "p-1", "Allow read", "permit();", Set.of(missingScope))
            .subscribe(
                () -> ctx.failNow("Expected the stage to fail with NO_HANDLERS"),
                stageError -> {
                    // Now commit: the failed address must not be among the committed ones. Only the
                    // healthy SCOPE address (which we DID stage successfully below) should be committed.
                    port
                        .addOrUpdatePolicy(ENV, "p-2", "Allow read", "permit();", Set.of(SCOPE))
                        .andThen(port.commit())
                        .subscribe(
                            () -> {
                                ctx.verify(() -> {
                                    // healthy scope got its mutation + a commit; failed scope got neither.
                                    JsonObject staged = received.poll();
                                    assertThat(staged).isNotNull();
                                    assertThat(staged.getString("op")).isEqualTo("addOrUpdatePolicy");
                                    JsonObject commit = received.poll();
                                    assertThat(commit).isNotNull();
                                    assertThat(commit.getString("op")).isEqualTo("commit");
                                    // nothing else (the failed scope was never committed)
                                    assertThat(received.poll()).isNull();
                                });
                                ctx.completeNow();
                            },
                            ctx::failNow
                        );
                }
            );
    }

    @Test
    void slow_consumer_does_not_stall_the_cycle_beyond_the_configured_timeout(VertxTestContext ctx) {
        // Without an explicit DeliveryOptions timeout, Vertx defaults to 30s — long enough to wedge a
        // sync cycle when the PDP plugin is unhealthy. Stage a mutation, then swap in a consumer that
        // never replies. The commit must give up at the configured timeout (not 30s) and, because a
        // wedged scope must not fail the whole cycle, complete rather than propagate the timeout.
        port
            .addOrUpdatePolicy(ENV, "p-1", "Allow read", "permit(principal, action, resource);", Set.of(SCOPE))
            .subscribe(
                () -> {
                    fakePluginConsumer.unregister();
                    MessageConsumer<JsonObject> blackHole = vertx.eventBus().<JsonObject>consumer(SCOPE_ADDRESS, msg -> {});
                    long start = System.currentTimeMillis();
                    port
                        .commit()
                        .subscribe(
                            () -> {
                                blackHole.unregister();
                                ctx.verify(() -> {
                                    long elapsed = System.currentTimeMillis() - start;
                                    assertThat(elapsed)
                                        .as("Should give up at the port's configured timeout, not the Vertx default 30s")
                                        .isLessThan(EventBusAuthzEnginePort.REPLY_TIMEOUT_MS + 2_000L);
                                });
                                ctx.completeNow();
                            },
                            t -> {
                                blackHole.unregister();
                                ctx.failNow("A wedged scope must not fail the whole commit, got: " + t);
                            }
                        );
                },
                ctx::failNow
            );
    }

    @Test
    void commitScope_commits_only_its_own_address_and_leaves_other_armed_scopes_for_the_global_commit() {
        String scopeB = "scope-b";
        String addressB = EventBusAuthzEnginePort.SCOPE_ADDRESS_PREFIX + ENV + ":" + scopeB;
        ConcurrentLinkedQueue<JsonObject> bReceived = new ConcurrentLinkedQueue<>();
        MessageConsumer<JsonObject> bConsumer = vertx
            .eventBus()
            .<JsonObject>consumer(addressB, msg -> {
                bReceived.add(msg.body());
                msg.reply(new JsonObject().put("commitGeneration", 1L));
            });

        // Arm scope A by staging a mutation (do NOT commit it).
        port.addOrUpdatePolicy(ENV, "p-a", "n", "permit();", Set.of(SCOPE)).blockingAwait();
        received.clear();

        // commitScope for scope B must seal ONLY scope B, not drain scope A's armed address.
        port.commitScope(ENV, scopeB).blockingAwait();

        assertThat(bReceived).hasSize(1);
        assertThat(bReceived.peek().getString("op")).isEqualTo("commit");
        assertThat(received).as("scope A must not be committed by commitScope(B)").isEmpty();

        // Scope A is still armed: a subsequent global commit seals it.
        port.commit().blockingAwait();
        assertThat(received)
            .extracting(b -> b.getString("op"))
            .containsExactly("commit");

        bConsumer.unregister();
    }

    @Test
    void commit_stops_re_arming_a_permanently_dead_address_after_the_attempt_cap() {
        // Arm scope A, then kill its consumer so every commit fails fast with NO_HANDLERS.
        port.addOrUpdatePolicy(ENV, "p-a", "n", "permit();", Set.of(SCOPE)).blockingAwait();
        fakePluginConsumer.unregister();
        received.clear();

        // Each failing commit re-arms the address — until the attempt cap is exceeded and it is abandoned.
        for (int i = 0; i <= EventBusAuthzEnginePort.MAX_COMMIT_ATTEMPTS; i++) {
            port.commit().blockingAwait();
        }

        // Revive a healthy consumer: a further commit must NOT reach the abandoned address.
        MessageConsumer<JsonObject> revived = vertx
            .eventBus()
            .<JsonObject>consumer(SCOPE_ADDRESS, msg -> {
                received.add(msg.body());
                msg.reply(new JsonObject().put("commitGeneration", 2L));
            });
        port.commit().blockingAwait();
        revived.unregister();

        assertThat(received).as("a permanently dead address must stop being retried after the cap").isEmpty();
    }

    @Test
    void route_skips_a_named_scope_not_hosted_on_this_node() {
        AuthzHostedScopes hosted = new AuthzHostedScopes();
        hosted.markHosted(ENV, "scope-here");
        EventBusAuthzEnginePort scopedPort = new EventBusAuthzEnginePort(io.vertx.rxjava3.core.Vertx.newInstance(vertx), hosted);

        ConcurrentLinkedQueue<JsonObject> here = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<JsonObject> elsewhere = new ConcurrentLinkedQueue<>();
        MessageConsumer<JsonObject> hereConsumer = vertx
            .eventBus()
            .<JsonObject>consumer(EventBusAuthzEnginePort.SCOPE_ADDRESS_PREFIX + ENV + ":scope-here", msg -> {
                here.add(msg.body());
                msg.reply(new JsonObject());
            });
        MessageConsumer<JsonObject> elsewhereConsumer = vertx
            .eventBus()
            .<JsonObject>consumer(EventBusAuthzEnginePort.SCOPE_ADDRESS_PREFIX + ENV + ":scope-elsewhere", msg -> {
                elsewhere.add(msg.body());
                msg.reply(new JsonObject());
            });

        scopedPort.addOrUpdatePolicy(ENV, "p", "n", "permit();", Set.of("scope-here", "scope-elsewhere")).blockingAwait();

        hereConsumer.unregister();
        elsewhereConsumer.unregister();
        assertThat(here).as("the hosted scope receives the document").hasSize(1);
        assertThat(elsewhere).as("the non-hosted scope is skipped, not routed").isEmpty();
    }

    @Test
    void route_always_serves_the_default_scope_even_when_nothing_is_hosted() {
        EventBusAuthzEnginePort scopedPort = new EventBusAuthzEnginePort(
            io.vertx.rxjava3.core.Vertx.newInstance(vertx),
            new AuthzHostedScopes()
        );

        ConcurrentLinkedQueue<JsonObject> def = new ConcurrentLinkedQueue<>();
        MessageConsumer<JsonObject> defConsumer = vertx
            .eventBus()
            .<JsonObject>consumer(EventBusAuthzEnginePort.DEFAULT_ADDRESS, msg -> {
                def.add(msg.body());
                msg.reply(new JsonObject());
            });

        scopedPort.addOrUpdatePolicy(ENV, "p", "n", "permit();", Set.of("default")).blockingAwait();

        defConsumer.unregister();
        assertThat(def).as("the default scope is always served regardless of hosting").hasSize(1);
    }
}
