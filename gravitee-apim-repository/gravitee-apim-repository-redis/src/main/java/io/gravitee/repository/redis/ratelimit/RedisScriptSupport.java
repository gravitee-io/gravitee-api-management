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

import io.gravitee.repository.exception.RedisOperationTimeoutException;
import io.vertx.core.Future;
import io.vertx.redis.client.Response;
import java.util.concurrent.TimeoutException;

final class RedisScriptSupport {

    private static final String NOSCRIPT_PREFIX = "NOSCRIPT";

    private RedisScriptSupport() {}

    static Future<Response> mapTimeout(Throwable t, int operationTimeout) {
        if (t instanceof TimeoutException) {
            return Future.failedFuture(new RedisOperationTimeoutException(operationTimeout));
        }
        return Future.failedFuture(t);
    }

    /**
     * Returns true only when the error (or one of its causes) is a Redis {@code NOSCRIPT} reply.
     * Matches the error-code prefix (Redis error replies start with the uppercase code), not a
     * substring, so unrelated messages can't trigger a (non-idempotent) EVAL replay; walks the
     * cause chain and is case-insensitive to survive wrapping.
     */
    static boolean isNoScript(Throwable t) {
        int depth = 0;
        for (Throwable cause = t; cause != null && depth < 20; cause = cause.getCause(), depth++) {
            String message = cause.getMessage();
            if (message != null && message.stripLeading().regionMatches(true, 0, NOSCRIPT_PREFIX, 0, NOSCRIPT_PREFIX.length())) {
                return true;
            }
        }
        return false;
    }
}
