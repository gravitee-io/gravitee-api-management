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

import io.vertx.core.Vertx;
import io.vertx.core.internal.CloseFuture;
import io.vertx.core.internal.VertxInternal;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Counts the resources (clients among them) registered on the root close future of a Vert.x instance.
 */
final class RetainedClients {

    private RetainedClients() {}

    static int count(Vertx vertx) {
        CloseFuture closeFuture = ((VertxInternal) vertx).closeFuture();
        try {
            Field children = CloseFuture.class.getDeclaredField("children");
            children.setAccessible(true);
            Map<?, ?> map = (Map<?, ?>) children.get(closeFuture);
            return map == null ? 0 : map.size();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                "CloseFuture.children is gone: Vert.x internals changed, this does not mean the leak is fixed.",
                e
            );
        }
    }
}
