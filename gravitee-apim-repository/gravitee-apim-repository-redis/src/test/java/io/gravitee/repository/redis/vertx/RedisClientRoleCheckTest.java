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
package io.gravitee.repository.redis.vertx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisClientRoleCheckTest {

    @Test
    void should_skip_role_when_not_sentinel() {
        RedisAPI api = mock(RedisAPI.class);

        RedisAPI result = RedisClient.applySentinelRoleCheck(api, false, t -> {})
            .toCompletionStage()
            .toCompletableFuture()
            .join();

        assertThat(result).isSameAs(api);
        verify(api, never()).role();
    }

    @Test
    void should_accept_sentinel_master_role() {
        RedisAPI api = mock(RedisAPI.class);
        Response master = roleReply("master");
        when(api.role()).thenReturn(Future.succeededFuture(master));

        RedisAPI result = RedisClient.applySentinelRoleCheck(api, true, t -> {})
            .toCompletionStage()
            .toCompletableFuture()
            .join();

        assertThat(result).isSameAs(api);
    }

    @Test
    void should_fail_sentinel_connect_when_role_is_slave() {
        RedisAPI api = mock(RedisAPI.class);
        Response slave = roleReply("slave");
        when(api.role()).thenReturn(Future.succeededFuture(slave));

        Future<RedisAPI> result = RedisClient.applySentinelRoleCheck(api, true, t -> {});

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(IllegalStateException.class).hasMessageContaining("ROLE=slave");
        assertThat(RedisClient.isExpectedConnectFailure(result.cause())).isTrue();
    }

    @Test
    void should_continue_when_sentinel_role_command_is_denied() {
        RedisAPI api = mock(RedisAPI.class);
        RuntimeException noperm = new RuntimeException("NOPERM this user has no permissions to run the 'role' command");
        when(api.role()).thenReturn(Future.failedFuture(noperm));
        AtomicReference<Throwable> skipped = new AtomicReference<>();

        RedisAPI result = RedisClient.applySentinelRoleCheck(api, true, skipped::set).toCompletionStage().toCompletableFuture().join();

        assertThat(result).isSameAs(api);
        assertThat(skipped.get()).isSameAs(noperm);
    }

    @Test
    void should_continue_when_sentinel_role_command_is_unknown() {
        RedisAPI api = mock(RedisAPI.class);
        RuntimeException unknown = new RuntimeException("ERR unknown command 'ROLE', with args beginning with: ");
        when(api.role()).thenReturn(Future.failedFuture(unknown));
        AtomicReference<Throwable> skipped = new AtomicReference<>();

        RedisAPI result = RedisClient.applySentinelRoleCheck(api, true, skipped::set).toCompletionStage().toCompletableFuture().join();

        assertThat(result).isSameAs(api);
        assertThat(skipped.get()).isSameAs(unknown);
    }

    @Test
    void should_fail_sentinel_connect_when_role_is_sentinel() {
        RedisAPI api = mock(RedisAPI.class);
        Response sentinel = roleReply("sentinel");
        when(api.role()).thenReturn(Future.succeededFuture(sentinel));

        Future<RedisAPI> result = RedisClient.applySentinelRoleCheck(api, true, t -> {});

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(IllegalStateException.class).hasMessageContaining("ROLE=sentinel");
        assertThat(RedisClient.isExpectedConnectFailure(result.cause())).isTrue();
    }

    @Test
    void should_treat_role_mismatch_as_expected_connect_failure() {
        assertThat(
            RedisClient.isExpectedConnectFailure(
                new IllegalStateException("Redis node is not writable (ROLE=slave); will retry with backoff")
            )
        ).isTrue();
        assertThat(RedisClient.isExpectedConnectFailure(new RuntimeException("Connection is closed"))).isFalse();
    }

    @Test
    void should_fail_sentinel_connect_when_role_command_fails_for_connection() {
        RedisAPI api = mock(RedisAPI.class);
        RuntimeException closed = new RuntimeException("Connection is closed");
        when(api.role()).thenReturn(Future.failedFuture(closed));
        AtomicReference<Throwable> skipped = new AtomicReference<>();

        Future<RedisAPI> result = RedisClient.applySentinelRoleCheck(api, true, skipped::set);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isSameAs(closed);
        assertThat(skipped.get()).isNull();
    }

    private static Response roleReply(String role) {
        Response name = mock(Response.class);
        when(name.toString()).thenReturn(role);
        Response reply = mock(Response.class);
        when(reply.size()).thenReturn(1);
        when(reply.get(0)).thenReturn(name);
        return reply;
    }
}
