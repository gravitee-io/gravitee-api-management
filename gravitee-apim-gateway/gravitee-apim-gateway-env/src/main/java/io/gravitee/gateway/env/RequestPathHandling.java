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
package io.gravitee.gateway.env;

/**
 * How the gateway treats dot segments in an incoming request path, before the listener context path
 * is resolved and therefore before any plan is enforced.
 *
 * <p>The distinction matters because the gateway both decides on that path and forwards it upstream.
 * Left untouched, a request can be authorized as one API and, once a conforming upstream applies
 * RFC 3986 §5.2.4, delivered to another one's backend.
 *
 * @author GraviteeSource Team
 */
public enum RequestPathHandling {
    /**
     * Byte-preserving pass-through: the path is used, and forwarded, exactly as it arrived. This
     * was the default up to 4.12 and is what a deployment sets explicitly to keep that behaviour,
     * so that request signing and encoded slashes keep working without any change.
     */
    RAW,

    /**
     * Answers {@code 400} when the normalized path differs from the one received. Nothing is
     * rewritten and no routing decision changes, which makes it the safest way to close the gap on
     * an existing platform.
     */
    REJECT,

    /**
     * The default since 4.13. Resolves the dot segments and uses the result for listener
     * resolution, plan enforcement, flow selection and the upstream URI. The request is not blocked, it is simply read for what it
     * means: {@code /alpha/api/../../beta/api} is a call to {@code beta}, so {@code beta}'s plan
     * applies.
     */
    NORMALIZE,
}
