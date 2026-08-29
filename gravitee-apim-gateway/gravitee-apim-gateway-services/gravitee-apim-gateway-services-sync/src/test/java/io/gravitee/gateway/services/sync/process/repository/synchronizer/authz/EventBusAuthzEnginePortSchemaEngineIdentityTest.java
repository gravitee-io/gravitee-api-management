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

/**
 * A schema is bound to an engine, and an engine is the base scope with the sharding tag stripped, so it
 * must reach every node hosting that engine whatever tags the node carries. Policies and entities are
 * bound to a routing scope instead and stay tag-gated.
 */
@ExtendWith(VertxExtension.class)
class EventBusAuthzEnginePortSchemaEngineIdentityTest {

    private static final String STOCK_ADDRESS = "service:authz-pdp:sync:scope:env-1:stock";
    private static final String SCHEMA_TEXT = "entity User;";
    private static final String POLICY_TEXT = "permit(principal, action, resource);";

    private Vertx vertx;
    private final ConcurrentLinkedQueue<String> hits = new ConcurrentLinkedQueue<>();

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() {
        vertx.close();
    }

    private EventBusAuthzEnginePort portOn(AuthzHostedScopes hosted) {
        return new EventBusAuthzEnginePort(vertx, hosted, new AuthzAppliedRevisions());
    }

    private AuthzHostedScopes nodeTagged(String tag, String hostedScope) {
        AuthzHostedScopes hosted = new AuthzHostedScopes(Set.of(tag));
        hosted.markHosted("env-1", hostedScope);
        return hosted;
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
    void a_schema_bound_to_the_engine_reaches_a_node_that_hosts_it_under_a_tag() {
        recordAndReplyOn(STOCK_ADDRESS);

        portOn(nodeTagged("eu", "stock@eu")).addOrUpdateSchema("env-1", "s1", "n", SCHEMA_TEXT, Set.of("stock"), 1L).blockingAwait();

        assertThat(hits).containsExactly(STOCK_ADDRESS);
    }

    @Test
    void removing_it_follows_the_same_rule() {
        recordAndReplyOn(STOCK_ADDRESS);

        portOn(nodeTagged("eu", "stock@eu")).removeSchema("env-1", "s1", Set.of("stock")).blockingAwait();

        assertThat(hits).containsExactly(STOCK_ADDRESS);
    }

    @Test
    void a_policy_bound_to_the_same_bare_scope_is_still_tag_gated() {
        // The engine-identity rule is deliberately schema-only: a bare scope carries no tag, so a tagged
        // node does not serve it, exactly as before.
        recordAndReplyOn(STOCK_ADDRESS);

        portOn(nodeTagged("eu", "stock@eu")).addOrUpdatePolicy("env-1", "p1", "n", POLICY_TEXT, Set.of("stock"), 1L).blockingAwait();

        assertThat(hits).isEmpty();
    }

    @Test
    void a_schema_does_not_reach_a_node_hosting_no_engine_of_that_name() {
        recordAndReplyOn(STOCK_ADDRESS);
        AuthzHostedScopes elsewhere = nodeTagged("eu", "orders@eu");

        portOn(elsewhere).addOrUpdateSchema("env-1", "s1", "n", SCHEMA_TEXT, Set.of("stock"), 1L).blockingAwait();

        assertThat(hits).isEmpty();
    }

    @Test
    void the_engine_is_addressed_once_even_though_two_tagged_scopes_share_it() {
        // An untagged catch-all node hosts stock@eu and stock@us, which are one engine. Routing by engine
        // identity sends one command, where expanding the id into both scopes would send two.
        recordAndReplyOn(STOCK_ADDRESS);
        AuthzHostedScopes catchAll = new AuthzHostedScopes();
        catchAll.markHosted("env-1", "stock@eu");
        catchAll.markHosted("env-1", "stock@us");

        portOn(catchAll).addOrUpdateSchema("env-1", "s1", "n", SCHEMA_TEXT, Set.of("stock"), 1L).blockingAwait();

        assertThat(hits).containsExactly(STOCK_ADDRESS);
    }

    @Test
    void an_explicitly_tagged_schema_target_still_reaches_the_engine_it_names() {
        recordAndReplyOn(STOCK_ADDRESS);

        portOn(nodeTagged("eu", "stock@eu")).addOrUpdateSchema("env-1", "s1", "n", SCHEMA_TEXT, Set.of("stock@eu"), 1L).blockingAwait();

        assertThat(hits).containsExactly(STOCK_ADDRESS);
    }
}
