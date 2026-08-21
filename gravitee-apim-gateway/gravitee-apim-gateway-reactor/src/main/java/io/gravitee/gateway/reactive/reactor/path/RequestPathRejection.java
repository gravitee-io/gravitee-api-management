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
package io.gravitee.gateway.reactive.reactor.path;

import io.gravitee.gateway.env.RequestPathConfiguration;
import io.gravitee.gateway.env.RequestPathHandling;

/**
 * Answers whether the gateway will refuse to route on a path, for a caller that needs to know it
 * before the request is dispatched.
 *
 * <p>There are two ways a request is refused, and only stating both keeps a caller correct:
 *
 * <ul>
 *   <li>{@link RequestPathHandling#REJECT} meeting a path that is not already in its normalized
 *       form — the mode's whole purpose;
 *   <li>a path that has <b>no</b> normalized form at all, because it carries a malformed percent
 *       sequence. That one is refused under {@link RequestPathHandling#NORMALIZE} too: the gateway
 *       cannot know which octets the client meant, so there is nothing to decide on.
 * </ul>
 *
 * <p><b>Why this exists next to the dispatcher rather than inside it.</b> {@code
 * DefaultHttpRequestDispatcher.dispatch} runs on every request the gateway serves, and deliberately
 * inlines this decision so it can reuse the single scan it already pays for and allocate nothing.
 * Calling this method there would scan the path a second time.
 *
 * <p>The duplication is therefore intentional, and it is guarded by {@code
 * DefaultHttpRequestDispatcherTest.The_rejection_decision}, which drives the <b>dispatcher</b> over
 * a table of modes and paths and asserts that it refused exactly the ones this method announces.
 * Two readings of one rule drift the moment one is touched alone, and this drift fails open. Note
 * what that guard is not: asserting this method against the normalizer it is built from restates its
 * own body and passes whatever the dispatcher does.
 *
 * @author GraviteeSource Team
 */
public final class RequestPathRejection {

    private RequestPathRejection() {}

    /**
     * @return {@code true} when dispatching this path would answer 400 instead of routing it.
     */
    public static boolean applies(final RequestPathConfiguration configuration, final String path) {
        if (!configuration.isEnabled() || !RequestPathNormalizer.needsNormalization(path)) {
            return false;
        }
        if (configuration.getHandling() == RequestPathHandling.REJECT) {
            return true;
        }
        return RequestPathNormalizer.normalize(path) == null;
    }
}
