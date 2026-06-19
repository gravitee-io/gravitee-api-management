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

import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthzHostedScopesTest {

    @Test
    void default_and_wildcard_are_always_served() {
        AuthzHostedScopes scopes = new AuthzHostedScopes(Set.of());
        assertThat(scopes.serves("env-1", "default")).isTrue();
        assertThat(scopes.serves("env-1", "*")).isTrue();
    }

    @Test
    void tag_scoped_default_is_served_only_on_a_node_carrying_that_tag() {
        AuthzHostedScopes scopes = new AuthzHostedScopes(Set.of("us"));
        assertThat(scopes.serves("env-1", "default@us")).isTrue();
        assertThat(scopes.serves("env-1", "default@eu")).isFalse();
    }

    @Test
    void tag_scoped_default_is_served_on_an_untagged_catch_all_node() {
        // Tag gating mirrors API sharding: an untagged gateway is a catch-all, so it serves every
        // tag-scoped default (and every other scope) regardless of the tag.
        AuthzHostedScopes scopes = new AuthzHostedScopes(Set.of());
        assertThat(scopes.serves("env-1", "default@us")).isTrue();
    }

    @Test
    void a_node_with_multiple_tags_serves_each_tag_scoped_default() {
        AuthzHostedScopes scopes = new AuthzHostedScopes(Set.of("us", "eu"));
        assertThat(scopes.serves("env-1", "default@us")).isTrue();
        assertThat(scopes.serves("env-1", "default@eu")).isTrue();
    }

    @Test
    void a_named_scope_is_served_only_after_it_is_provisioned() {
        // Untagged (catch-all) node so the tag gate is always open — this isolates the provisioning gate:
        // a named engine is served only once it is actually hosted here.
        AuthzHostedScopes scopes = new AuthzHostedScopes(Set.of());
        assertThat(scopes.serves("env-1", "some-scope")).isFalse();
        scopes.markHosted("env-1", "some-scope");
        assertThat(scopes.serves("env-1", "some-scope")).isTrue();
        scopes.unmarkHosted("env-1", "some-scope");
        assertThat(scopes.serves("env-1", "some-scope")).isFalse();
    }

    @Test
    void a_named_tagged_scope_requires_both_the_provisioned_engine_and_the_node_tag() {
        // "orders@us" (a regional replica) is served only when this node hosts the "orders" engine AND
        // carries the "us" tag — the placement gate is the conjunction of both, not either alone.
        AuthzHostedScopes usNode = new AuthzHostedScopes(Set.of("us"));
        usNode.markHosted("env-1", "orders");
        assertThat(usNode.serves("env-1", "orders@us")).isTrue();
        // hosted but wrong node tag → not served (this is what keeps orders@eu off a us node)
        assertThat(usNode.serves("env-1", "orders@eu")).isFalse();

        // right tag but engine not provisioned here → not served
        AuthzHostedScopes euNodeNoEngine = new AuthzHostedScopes(Set.of("eu"));
        assertThat(euNodeNoEngine.serves("env-1", "orders@eu")).isFalse();
    }

    @Test
    void an_untagged_catch_all_node_serves_a_hosted_named_tagged_scope_for_any_tag() {
        // Catch-all (API sharding): an untagged gateway hosts every engine, so its "orders" engine serves
        // both orders@us and orders@eu documents (the untagged node is the union of all tags).
        AuthzHostedScopes catchAll = new AuthzHostedScopes(Set.of());
        catchAll.markHosted("env-1", "orders");
        assertThat(catchAll.serves("env-1", "orders@us")).isTrue();
        assertThat(catchAll.serves("env-1", "orders@eu")).isTrue();
    }

    @Test
    void an_excluded_tag_is_not_served_even_when_the_engine_is_hosted() {
        // "!eu" excludes eu from a node that otherwise carries us — mirrors API exclusion tags.
        AuthzHostedScopes node = new AuthzHostedScopes(Set.of("us", "!eu"));
        node.markHosted("env-1", "orders");
        assertThat(node.serves("env-1", "orders@us")).isTrue();
        assertThat(node.serves("env-1", "orders@eu")).isFalse();
    }
}
