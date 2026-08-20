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
package io.gravitee.gateway.http.vertx;

import lombok.Builder;
import lombok.Getter;

/**
 * What the dispatcher knows about a request before the wrapper for it exists.
 *
 * <p>It is a single constructor parameter on purpose. Every field the dispatcher needs to seed used
 * to be one more positional argument, on this class and on each of its subtypes, and each addition
 * moved a {@code public} constructor and the {@code protected} factory method that calls it. That
 * is not theoretical: it silently disabled an override in the debug service, which kept compiling
 * while no longer overriding anything. A builder cannot do that — a new field moves no signature,
 * so a subclass or a plugin that extends the construction path keeps working untouched.
 *
 * <p>All fields are optional. A {@code null} means "the wrapper decides", which is the behaviour
 * that existed before the dispatcher had anything to say.
 *
 * @author GraviteeSource Team
 */
@Getter
@Builder
public class VertxHttpServerRequestOptions {

    /**
     * The path the request reports, so that everything derived from it — starting with the
     * {@code pathInfo} a contextualized request computes — matches the path the gateway resolved
     * rather than the one received. {@code null} reports the native path, which is the historical
     * behaviour. {@code uri()} reports the untouched native value either way.
     */
    private final String path;

    /**
     * When the gateway started handling this request, in milliseconds since the epoch, taken before
     * any work is done on it. {@code null} stamps the clock at construction, which excludes
     * everything the dispatcher did beforehand from every latency the gateway reports.
     */
    private final Long timestamp;
}
