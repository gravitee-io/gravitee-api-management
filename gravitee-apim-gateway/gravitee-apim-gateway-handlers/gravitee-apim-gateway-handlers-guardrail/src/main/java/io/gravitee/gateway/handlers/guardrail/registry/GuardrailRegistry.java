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
package io.gravitee.gateway.handlers.guardrail.registry;

import io.gravitee.gateway.handlers.guardrail.GuardrailKey;
import io.gravitee.gateway.handlers.guardrail.ReactableGuardrail;
import io.gravitee.gateway.handlers.guardrail.reactor.GuardrailReactor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;

/**
 * Started Guardrail reactors, looked up by the {@code guardrail-run} policy at request time.
 * <p>
 * The reactor handler registry starts the replacement of an updated Guardrail before stopping the previous one, so
 * {@link #unregister(GuardrailReactor)} only removes the entry when it still points to that very reactor.
 *
 * @author GraviteeSource Team
 */
@CustomLog
public class GuardrailRegistry {

    private final Map<GuardrailKey, GuardrailReactor> reactors = new ConcurrentHashMap<>();

    public GuardrailReactor get(String guardrailId, String environmentId) {
        return reactors.get(new GuardrailKey(guardrailId, environmentId));
    }

    public void register(GuardrailReactor reactor) {
        GuardrailKey key = keyOf(reactor);
        GuardrailReactor previous = reactors.put(key, reactor);
        if (previous != null) {
            log.debug("Guardrail [{}] of environment [{}] replaced its running reactor", key.guardrailId(), key.environmentId());
        }
    }

    public void unregister(GuardrailReactor reactor) {
        reactors.remove(keyOf(reactor), reactor);
    }

    private static GuardrailKey keyOf(GuardrailReactor reactor) {
        ReactableGuardrail reactable = reactor.reactableGuardrail();
        if (reactable == null) {
            throw new IllegalArgumentException(String.format("Guardrail reactor [%s] has no Guardrail", reactor));
        }
        return reactable.key();
    }
}
