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

import io.gravitee.common.http.RequestPathNormalizer;
import io.gravitee.gateway.env.RequestPathConfiguration;
import io.gravitee.gateway.env.RequestPathHandling;

/**
 * States, for the tests alone, which paths the gateway refuses to route on.
 *
 * <p>Two ways a request is refused, and only stating both makes the oracle correct:
 *
 * <ul>
 *   <li>{@link RequestPathHandling#REJECT} meeting a path that is not already in its normalized
 *       form — the mode's whole purpose;
 *   <li>a path that has <b>no</b> normalized form at all, because it carries a malformed percent
 *       sequence. That one is refused under {@link RequestPathHandling#NORMALIZE} too: the gateway
 *       cannot know which octets the client meant, so there is nothing to decide on.
 * </ul>
 *
 * <p><b>This is a test oracle, not production code, and it lives here for that reason.</b> The rule
 * is written once in production, inlined in {@code DefaultHttpRequestDispatcher.dispatch} so that it
 * reuses the single scan that method already pays for and allocates nothing. This class restates the
 * same rule independently, and {@code DefaultHttpRequestDispatcherTest.The_rejection_decision} drives
 * the <b>dispatcher</b> over a table of modes and paths, asserting it refused exactly what is
 * announced here.
 *
 * <p>Two independent readings compared against each other is what gives that test its value. One
 * restating the other would pass whatever the dispatcher did — which is what an earlier version of
 * this guard actually did, by asserting this class against the normalizer it is built from.
 *
 * <p>It was briefly production code, called by the debug dispatcher to predict a rejection before
 * delegating. That dispatcher now hooks into the rejection instead of predicting it, so no caller
 * remains. Promote it back the day one appears — with the caller, not in anticipation.
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
