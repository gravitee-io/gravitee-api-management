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
package io.gravitee.gamma.rest.core.guardrail.model;

import io.gravitee.definition.model.v4.guardrail.Guardrail.Phase;
import java.util.List;

/**
 * A Guardrail enabled on an API, as the API lists it. Everything but the order and the condition is read from the
 * revision the gateways run, not from the Guardrail's draft and not from the step: a name or an effect the publisher has
 * not deployed describes nothing the calls of this API meet.
 *
 * @param name the name of the deployed revision; {@code null} when no revision of this Guardrail is deployed, which
 *     includes the Guardrail having been withdrawn or the environment no longer holding it.
 * @param order the 1-based position of the Guardrail among those of its phase.
 * @param effects what the guardrail policies of the deployed revision do to the calls they evaluate, empty when none of them
 *     says (see {@link GuardrailPolicyEffects}) or when no revision is deployed.
 * @param condition the condition the API's step runs the Guardrail under, read from the step: it belongs to the API,
 *     not to the Guardrail. {@code null} when the Guardrail runs on every call.
 * @author GraviteeSource Team
 */
public record ApiGuardrail(String guardrailId, String name, Phase phase, int order, List<GuardrailPolicyEffect> effects, String condition) {
    public ApiGuardrail {
        effects = effects == null ? List.of() : List.copyOf(effects);
    }

    /** A Guardrail the API runs on every call. */
    public ApiGuardrail(String guardrailId, String name, Phase phase, int order, List<GuardrailPolicyEffect> effects) {
        this(guardrailId, name, phase, order, effects, null);
    }
}
