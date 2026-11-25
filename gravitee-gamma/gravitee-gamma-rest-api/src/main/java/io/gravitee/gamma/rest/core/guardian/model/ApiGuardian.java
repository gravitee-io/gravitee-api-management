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
package io.gravitee.gamma.rest.core.guardian.model;

import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import java.util.List;

/**
 * A Guardian enabled on an API, as the API lists it. Everything but the order and the condition is read from the
 * revision the gateways run, not from the Guardian's draft and not from the step: a name or an effect the publisher has
 * not deployed describes nothing the calls of this API meet.
 *
 * @param name the name of the deployed revision; {@code null} when no revision of this Guardian is deployed, which
 *     includes the Guardian having been withdrawn or the environment no longer holding it.
 * @param order the 1-based position of the Guardian among those of its phase.
 * @param effects what the guardrails of the deployed revision do to the calls they evaluate, empty when none of them
 *     says (see {@link GuardrailEffects}) or when no revision is deployed.
 * @param condition the condition the API's step runs the Guardian under, read from the step: it belongs to the API,
 *     not to the Guardian. {@code null} when the Guardian runs on every call.
 * @author GraviteeSource Team
 */
public record ApiGuardian(String guardianId, String name, Phase phase, int order, List<GuardrailEffect> effects, String condition) {
    public ApiGuardian {
        effects = effects == null ? List.of() : List.copyOf(effects);
    }
}
