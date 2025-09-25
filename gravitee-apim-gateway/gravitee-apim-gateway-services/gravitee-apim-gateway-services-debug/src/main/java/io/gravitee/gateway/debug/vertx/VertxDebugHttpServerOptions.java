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
package io.gravitee.gateway.debug.vertx;

import io.gravitee.node.vertx.server.http.VertxHttpServerOptions;
import lombok.experimental.SuperBuilder;
import org.springframework.core.env.Environment;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuperBuilder
public class VertxDebugHttpServerOptions extends VertxHttpServerOptions {

    public abstract static class VertxDebugHttpServerOptionsBuilder<
        C extends VertxDebugHttpServerOptions,
        B extends VertxDebugHttpServerOptions.VertxDebugHttpServerOptionsBuilder<C, B>
    >
        extends VertxHttpServerOptionsBuilder<C, B> {

        public B environment(Environment environment) {
            super.environment(environment);

            this.port(Integer.parseInt(environment.getProperty("debug.port", "8482")));
            this.host(environment.getProperty("debug.host", "localhost"));
            this.haProxyProtocol(false);

            return self();
        }
    }
}
