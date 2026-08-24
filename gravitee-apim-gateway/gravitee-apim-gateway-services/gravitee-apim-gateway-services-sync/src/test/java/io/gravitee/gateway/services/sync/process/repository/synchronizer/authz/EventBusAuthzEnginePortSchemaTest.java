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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class EventBusAuthzEnginePortSchemaTest {

    private static final String DEFAULT_ADDRESS = "service:authz-pdp:sync";
    private static final String SCHEMA_TEXT = "entity User;";

    private Vertx vertx;
    private final ConcurrentLinkedQueue<String> hits = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<JsonObject> commands = new ConcurrentLinkedQueue<>();

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() {
        vertx.close();
    }

    private EventBusAuthzEnginePort portServingAll() {
        return new EventBusAuthzEnginePort(vertx, HostedScopesFixtures.servingAll(), new AuthzAppliedRevisions());
    }

    private void recordAndReplyOn(String address) {
        vertx
            .eventBus()
            .<JsonObject>consumer(address, msg -> {
                hits.add(address);
                commands.add(msg.body());
                msg.reply(new JsonObject().put("commitGeneration", 1L));
            });
    }

    @Test
    void the_add_command_carries_only_docId_and_schemaText() {
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-a");

        portServingAll().addOrUpdateSchema("env-1", "s1", "fleet schema", SCHEMA_TEXT, Set.of("api-a"), 1L).blockingAwait();

        JsonObject command = commands.poll();
        assertThat(command.getString("op")).isEqualTo("addOrUpdateSchema");
        assertThat(command.getString("docId")).isEqualTo("s1");
        assertThat(command.getString("schemaText")).isEqualTo(SCHEMA_TEXT);
        assertThat(command.fieldNames())
            .as("the PDP consumer reads only docId and schemaText, so name must not go on the bus")
            .containsExactlyInAnyOrder("op", "docId", "schemaText");
    }

    @Test
    void the_remove_command_carries_only_docId() {
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-a");

        portServingAll().removeSchema("env-1", "s1", Set.of("api-a")).blockingAwait();

        JsonObject command = commands.poll();
        assertThat(command.getString("op")).isEqualTo("removeSchema");
        assertThat(command.fieldNames()).containsExactlyInAnyOrder("op", "docId");
    }

    @Test
    void a_scoped_schema_reaches_its_env_namespaced_address_only() {
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-a");
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-b");

        portServingAll().addOrUpdateSchema("env-1", "s1", "n", SCHEMA_TEXT, Set.of("api-a"), 1L).blockingAwait();

        assertThat(hits).containsExactly("service:authz-pdp:sync:scope:env-1:api-a");
    }

    @Test
    void a_tagged_scope_addresses_the_base_engine_with_the_tag_stripped() {
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:stock");
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:stock@eu");

        portServingAll().addOrUpdateSchema("env-1", "s1", "n", SCHEMA_TEXT, Set.of("stock@eu"), 1L).blockingAwait();

        assertThat(hits).containsExactly("service:authz-pdp:sync:scope:env-1:stock");
    }

    @Test
    void two_tag_variants_of_one_engine_send_twice_to_the_same_address_idempotently() {
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:stock");

        portServingAll().addOrUpdateSchema("env-1", "s1", "n", SCHEMA_TEXT, Set.of("stock@eu", "stock@us"), 1L).blockingAwait();

        assertThat(hits).containsExactly("service:authz-pdp:sync:scope:env-1:stock", "service:authz-pdp:sync:scope:env-1:stock");
        assertThat(commands)
            .extracting(c -> c.getString("docId"))
            .containsExactly("s1", "s1");
    }

    @Test
    void a_scope_this_node_does_not_serve_is_not_routed() {
        AuthzHostedScopes hosted = new AuthzHostedScopes();
        hosted.markHosted("env-1", "api-a");
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-a");
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-b");

        new EventBusAuthzEnginePort(vertx, hosted, new AuthzAppliedRevisions())
            .addOrUpdateSchema("env-1", "s1", "n", SCHEMA_TEXT, Set.of("api-b"), 1L)
            .blockingAwait();

        assertThat(hits).isEmpty();
    }

    @Test
    void wildcard_reaches_every_hosted_engine_but_never_the_bootstrap_engine() {
        // D7: "*" on a schema means every NAMED engine of the environment. The default engine is shared
        // across environments, so a wildcard schema must not land on it.
        AuthzHostedScopes hosted = new AuthzHostedScopes();
        hosted.markHosted("env-1", "api-a");
        hosted.markHosted("env-1", "api-b");
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-a");
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-b");
        recordAndReplyOn(DEFAULT_ADDRESS);

        new EventBusAuthzEnginePort(vertx, hosted, new AuthzAppliedRevisions())
            .addOrUpdateSchema("env-1", "s1", "n", SCHEMA_TEXT, Set.of("*"), 1L)
            .blockingAwait();

        assertThat(hits).containsExactlyInAnyOrder("service:authz-pdp:sync:scope:env-1:api-a", "service:authz-pdp:sync:scope:env-1:api-b");
        assertThat(hits).as("the bootstrap engine is cross-environment, D7 excludes it").doesNotContain(DEFAULT_ADDRESS);
    }

    @Test
    void wildcard_removal_also_spares_the_bootstrap_engine() {
        AuthzHostedScopes hosted = new AuthzHostedScopes();
        hosted.markHosted("env-1", "api-a");
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-a");
        recordAndReplyOn(DEFAULT_ADDRESS);

        new EventBusAuthzEnginePort(vertx, hosted, new AuthzAppliedRevisions()).removeSchema("env-1", "s1", Set.of("*")).blockingAwait();

        assertThat(hits).containsExactly("service:authz-pdp:sync:scope:env-1:api-a");
    }

    @Test
    void an_explicit_default_target_still_reaches_the_bootstrap_engine() {
        // D7 removes the IMPLICIT bootstrap fan-out only. Targeting "default" by name is a documented,
        // cross-environment choice and stays possible.
        recordAndReplyOn(DEFAULT_ADDRESS);

        portServingAll().addOrUpdateSchema("env-1", "s1", "n", SCHEMA_TEXT, Set.of("default"), 1L).blockingAwait();

        assertThat(hits).containsExactly(DEFAULT_ADDRESS);
    }

    @Test
    void an_empty_target_set_sends_nothing() {
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-a");

        portServingAll().addOrUpdateSchema("env-1", "s1", "n", SCHEMA_TEXT, Set.of(), 1L).blockingAwait();

        assertThat(hits).isEmpty();
    }

    @Test
    void a_policy_wildcard_still_reaches_the_bootstrap_engine() {
        // Regression: the schema-specific expansion must not change the shared policy path.
        AuthzHostedScopes hosted = new AuthzHostedScopes();
        hosted.markHosted("env-1", "api-a");
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-a");
        recordAndReplyOn(DEFAULT_ADDRESS);

        new EventBusAuthzEnginePort(vertx, hosted, new AuthzAppliedRevisions())
            .addOrUpdatePolicy("env-1", "p1", "n", "permit(principal, action, resource);", Set.of("*"), 1L)
            .blockingAwait();

        assertThat(hits).contains(DEFAULT_ADDRESS);
    }

    @Test
    void an_entity_wildcard_still_reaches_the_bootstrap_engine() {
        AuthzHostedScopes hosted = new AuthzHostedScopes();
        hosted.markHosted("env-1", "api-a");
        recordAndReplyOn("service:authz-pdp:sync:scope:env-1:api-a");
        recordAndReplyOn(DEFAULT_ADDRESS);

        new EventBusAuthzEnginePort(vertx, hosted, new AuthzAppliedRevisions())
            .addOrUpdateEntity("env-1", "User::\"alice\"", java.util.Map.of(), List.of(), Set.of("*"), 1L)
            .blockingAwait();

        assertThat(hits).contains(DEFAULT_ADDRESS);
    }
}
