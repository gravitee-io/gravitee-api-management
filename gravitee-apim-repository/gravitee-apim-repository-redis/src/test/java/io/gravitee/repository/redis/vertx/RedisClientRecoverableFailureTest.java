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

import io.gravitee.repository.exception.RedisOperationTimeoutException;
import java.nio.channels.ClosedChannelException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Guards which Redis errors start a reconnect. READONLY means the TCP session is still up
 * but the node is no longer writable (Sentinel demotion). That must reconnect so the next
 * client asks Sentinel for the current master.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisClientRecoverableFailureTest {

    @Test
    void should_treat_readonly_as_recoverable() {
        assertThat(
            RedisClient.isRecoverableConnectionFailure(new RuntimeException("READONLY You can't write against a read only replica."))
        ).isTrue();
    }

    @Test
    void should_treat_a_wrapped_readonly_cause_as_recoverable() {
        Throwable wrapped = new RuntimeException(
            "Failed to run rate-limit script on Redis",
            new RuntimeException("READONLY You can't write against a read only replica.")
        );
        assertThat(RedisClient.isRecoverableConnectionFailure(wrapped)).isTrue();
    }

    @Test
    void should_treat_readonly_after_leading_whitespace_as_recoverable() {
        assertThat(RedisClient.isRecoverableConnectionFailure(new RuntimeException("  READONLY replica"))).isTrue();
    }

    @Test
    void should_treat_redis_6_script_wrapped_readonly_as_recoverable() {
        assertThat(
            RedisClient.isRecoverableConnectionFailure(
                new RuntimeException(
                    "ERR Error running script (call to f_1e84a64d649a247ba53ce64b0be7bbedd64fe1b4): @user_script:12: @user_script: 12: -READONLY You can't write against a read only replica."
                )
            )
        ).isTrue();
    }

    @Test
    void should_not_treat_noscript_as_recoverable() {
        assertThat(RedisClient.isRecoverableConnectionFailure(new RuntimeException("NOSCRIPT No matching script"))).isFalse();
    }

    @Test
    void should_not_treat_operation_timeout_as_recoverable() {
        assertThat(RedisClient.isRecoverableConnectionFailure(new RedisOperationTimeoutException(30_000))).isFalse();
    }

    @Test
    void should_not_treat_generic_errors_as_recoverable() {
        assertThat(RedisClient.isRecoverableConnectionFailure(new RuntimeException("malformed payload"))).isFalse();
        assertThat(RedisClient.isRecoverableConnectionFailure(new IllegalStateException())).isFalse();
        assertThat(RedisClient.isRecoverableConnectionFailure(new RuntimeException((String) null))).isFalse();
    }

    @Test
    void should_still_treat_dropped_connections_as_recoverable() {
        assertThat(RedisClient.isRecoverableConnectionFailure(new RuntimeException("Connection is closed"))).isTrue();
        assertThat(RedisClient.isRecoverableConnectionFailure(new ClosedChannelException())).isTrue();
    }
}
