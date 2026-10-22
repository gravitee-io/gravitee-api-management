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
package io.gravitee.gateway.handlers.guardian.registry;

import io.gravitee.gateway.handlers.guardian.ReactableGuardian;
import io.gravitee.gateway.handlers.guardian.reactor.GuardianReactor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;

/**
 * Started Guardian reactors, looked up by the {@code guardian-policy} at request time.
 * <p>
 * The reactor handler registry starts the replacement of an updated Guardian before stopping the previous one, so
 * {@link #unregister(GuardianReactor)} only removes the entry when it still points to that very reactor.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class GuardianRegistry {

    private final Map<GuardianKey, GuardianReactor> reactors = new ConcurrentHashMap<>();

    public GuardianReactor get(String guardianId, String environmentId) {
        return reactors.get(new GuardianKey(guardianId, environmentId));
    }

    public void register(GuardianReactor reactor) {
        GuardianReactor previous = reactors.put(keyOf(reactor), reactor);
        if (previous != null) {
            log.debug("Guardian [{}] of environment [{}] replaced its running reactor", reactor.id(), environmentIdOf(reactor));
        }
    }

    public void unregister(GuardianReactor reactor) {
        reactors.remove(keyOf(reactor), reactor);
    }

    private static GuardianKey keyOf(GuardianReactor reactor) {
        return new GuardianKey(reactor.id(), environmentIdOf(reactor));
    }

    private static String environmentIdOf(GuardianReactor reactor) {
        ReactableGuardian reactable = reactor.reactableGuardian();
        return reactable == null ? null : reactable.getEnvironmentId();
    }

    private record GuardianKey(String guardianId, String environmentId) {}
}
