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

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.rxjava3.core.Vertx;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class EventBusAuthzEnginePortScopeTest {

    private Vertx vertx;
    private EventBusAuthzEnginePort port;
    private final ConcurrentLinkedQueue<String> hits = new ConcurrentLinkedQueue<>();

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        // Addressing test: treat every scope as hosted so routing is exercised regardless of placement.
        port = new EventBusAuthzEnginePort(vertx, HostedScopesFixtures.servingAll());
    }

    @AfterEach
    void tearDown() {
        vertx.close();
    }

    private void recordAndReplyOn(String address) {
        vertx
            .eventBus()
            .<JsonObject>consumer(address, msg -> {
                hits.add(address);
                msg.reply(new JsonObject().put("commitGeneration", 1L));
            });
    }

    @Test
    void scoped_policy_is_sent_to_its_env_namespaced_scope_address_only() {
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-a");
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-b");
        port.addOrUpdatePolicy("env-1", "p1", "n", "permit(principal, action, resource);", Set.of("api-a")).blockingAwait();
        assertThat(hits).containsExactly("service:authz-pdp:sync:scope:env-1:api-a");
    }

    @Test
    void a_tagged_named_scope_addresses_the_base_engine_with_the_tag_stripped() {
        // "orders@us" is a regional replica: the tag selects WHERE it runs, not the engine address — so it
        // must land on the base engine address ":scope:env-1:orders", NOT ":scope:env-1:orders@us".
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:orders");
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:orders@us");
        port.addOrUpdatePolicy("env-1", "p1", "n", "permit(principal, action, resource);", Set.of("orders@us")).blockingAwait();
        assertThat(hits).containsExactly("service:authz-pdp:sync:scope:env-1:orders");
    }

    @Test
    void same_scope_id_in_two_envs_does_not_co_mingle() {
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-a");
        recordAndReplyOn("service:authz-pdp:sync:scope:env-2:api-a");
        port.addOrUpdatePolicy("env-1", "p1", "n", "permit(principal, action, resource);", Set.of("api-a")).blockingAwait();
        assertThat(hits).containsExactly("service:authz-pdp:sync:scope:env-1:api-a");
    }

    @Test
    void wildcard_policy_fans_out_unicast_to_each_hosted_scope_plus_default() {
        // "*" is expanded on this node to the scopes it actually hosts (api-a, api-b) plus the always-on
        // default engine, then routed per-scope via request/reply — no fire-and-forget broadcast address.
        AuthzHostedScopes hosted = new AuthzHostedScopes();
        hosted.markHosted("env-1", "api-a");
        hosted.markHosted("env-1", "api-b");
        EventBusAuthzEnginePort scopedPort = new EventBusAuthzEnginePort(vertx, hosted);
        recordAndReplyOn("service:authz-pdp:sync");
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-a");
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-b");
        scopedPort.addOrUpdatePolicy("env-1", "p1", "n", "permit(principal, action, resource);", Set.of("*")).blockingAwait();
        assertThat(hits).containsExactlyInAnyOrder(
            "service:authz-pdp:sync",
            "service:authz-pdp:sync:scope:env-1:api-a",
            "service:authz-pdp:sync:scope:env-1:api-b"
        );
    }

    @Test
    void wildcard_policy_does_not_route_to_scopes_hosted_in_another_environment() {
        // The wildcard expands only to THIS environment's hosted scopes; a scope hosted for env-2 must not
        // receive an env-1 wildcard document. env-1 hosts nothing extra, so only the default engine is hit.
        AuthzHostedScopes hosted = new AuthzHostedScopes();
        hosted.markHosted("env-2", "other");
        EventBusAuthzEnginePort scopedPort = new EventBusAuthzEnginePort(vertx, hosted);
        recordAndReplyOn("service:authz-pdp:sync");
        recordAndReplyOn("service:authz-pdp:sync:scope:env-2:other");
        scopedPort.addOrUpdatePolicy("env-1", "p1", "n", "permit(principal, action, resource);", Set.of("*")).blockingAwait();
        assertThat(hits).containsExactly("service:authz-pdp:sync");
    }

    @Test
    void default_scope_is_sent_unicast_to_the_bare_sync_address() {
        recordAndReplyOn("service:authz-pdp:sync");
        port.addOrUpdatePolicy("env-1", "p1", "n", "permit(principal, action, resource);", Set.of("default")).blockingAwait();
        assertThat(hits).containsExactly("service:authz-pdp:sync");
    }

    @Test
    void default_scope_ignores_environment_id() {
        recordAndReplyOn("service:authz-pdp:sync");
        port.addOrUpdatePolicy("env-9", "p1", "n", "permit(principal, action, resource);", Set.of("default")).blockingAwait();
        assertThat(hits).containsExactly("service:authz-pdp:sync");
    }

    @Test
    void commit_to_default_scope_uses_unicast_request_reply() {
        recordAndReplyOn("service:authz-pdp:sync");
        port.addOrUpdatePolicy("env-1", "p1", "n", "permit(principal, action, resource);", Set.of("default")).blockingAwait();
        hits.clear();
        port.commit().blockingAwait();
        assertThat(hits).containsExactly("service:authz-pdp:sync");
    }

    @Test
    void empty_scope_set_sends_nothing() {
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-a");
        port.addOrUpdatePolicy("env-1", "p1", "n", "permit(principal, action, resource);", Set.of()).blockingAwait();
        assertThat(hits).isEmpty();
    }

    @Test
    void commit_targets_every_touched_scope_then_clears() {
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-a");
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-b");
        port.addOrUpdatePolicy("env-1", "p1", "n", "permit(principal, action, resource);", Set.of("api-a")).blockingAwait();
        port.addOrUpdateEntity("env-1", "User::\"alice\"", java.util.Map.of(), java.util.List.of(), Set.of("api-b")).blockingAwait();
        hits.clear();
        port.commit().blockingAwait();
        assertThat(hits).containsExactlyInAnyOrder("service:authz-pdp:sync:scope:env-1:api-a", "service:authz-pdp:sync:scope:env-1:api-b");
        hits.clear();
        port.commit().blockingAwait();
        assertThat(hits).isEmpty();
    }
}
