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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The flows a policy plugin declares it can run in.
 *
 * @param phases the phases of each declared flow, as the manifest lists them: not validated, so a phase the platform
 *     does not know is kept as is. A flow the manifest does not declare is absent.
 */
public record DeclaredFlows(Map<PolicyFlowType, List<String>> phases) {
    public DeclaredFlows {
        // not Map.copyOf: the iteration order of the flows is the order of their JSON keys
        phases = Collections.unmodifiableMap(new LinkedHashMap<>(phases));
    }

    /**
     * Whether every one of {@code flows} is declared and, when {@code phase} is given, declares it in each of them, or in
     * at least one declared flow when no flow is requested. A blank phase filters nothing.
     */
    public boolean declares(Set<PolicyFlowType> flows, String phase) {
        if (!phases.keySet().containsAll(flows)) {
            return false;
        }
        if (phase == null || phase.isBlank()) {
            return true;
        }
        if (flows.isEmpty()) {
            return phases
                .values()
                .stream()
                .anyMatch(declared -> declared.contains(phase));
        }
        return flows.stream().allMatch(flow -> phases.get(flow).contains(phase));
    }
}
