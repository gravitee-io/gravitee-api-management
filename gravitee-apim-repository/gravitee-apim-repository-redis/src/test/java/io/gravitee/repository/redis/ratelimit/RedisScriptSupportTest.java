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
package io.gravitee.repository.redis.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.exception.RedisOperationTimeoutException;
import io.vertx.core.Future;
import io.vertx.redis.client.Response;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisScriptSupportTest {

    @Test
    void mapTimeout_wraps_timeout_exception_with_the_configured_delay() {
        Future<Response> future = RedisScriptSupport.mapTimeout(new TimeoutException("timed out"), 50);

        assertThat(future.failed()).isTrue();
        assertThat(future.cause()).isInstanceOf(RedisOperationTimeoutException.class).hasMessage("Operation on Redis took more than 50ms");
    }

    @Test
    void mapTimeout_passes_through_other_failures() {
        RuntimeException original = new RuntimeException("READONLY You can't write against a read only replica.");

        Future<Response> future = RedisScriptSupport.mapTimeout(original, 50);

        assertThat(future.failed()).isTrue();
        assertThat(future.cause()).isSameAs(original);
    }

    @Test
    void isNoScript_matches_through_the_cause_chain_only() {
        assertThat(RedisScriptSupport.isNoScript(new RuntimeException("NOSCRIPT No matching script"))).isTrue();
        assertThat(RedisScriptSupport.isNoScript(new RuntimeException("wrapper", new IllegalStateException("NOSCRIPT x")))).isTrue();
        assertThat(RedisScriptSupport.isNoScript(new RuntimeException("LOADING dataset"))).isFalse();
        assertThat(RedisScriptSupport.isNoScript(new RuntimeException((String) null))).isFalse();
    }
}
