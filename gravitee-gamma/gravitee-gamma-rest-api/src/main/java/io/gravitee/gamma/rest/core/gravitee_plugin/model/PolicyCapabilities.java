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
package io.gravitee.gamma.rest.core.gravitee_plugin.model;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import java.util.Map;
import java.util.Set;

/**
 * What an installed policy declares about where it can run.
 *
 * @param phases the Guardian phases the policy runs in, for each API type its manifest declares. An API
 *     type may be declared with no Guardian phase, when the manifest only lists flow phases a Guardian
 *     never runs in.
 */
public record PolicyCapabilities(String name, boolean guardrail, Map<ApiType, Set<Phase>> phases) {
    public boolean supports(Set<ApiType> apiTypes) {
        return phases.keySet().containsAll(apiTypes);
    }

    /** Whether a Guardian of these API types and phase can run this policy as one of its guardrails. */
    public boolean isGuardrailFor(Set<ApiType> apiTypes, Phase phase) {
        return guardrail && runsIn(apiTypes, phase);
    }

    public boolean runsIn(Set<ApiType> apiTypes, Phase phase) {
        return apiTypes.stream().allMatch(apiType -> phases.getOrDefault(apiType, Set.of()).contains(phase));
    }
}
