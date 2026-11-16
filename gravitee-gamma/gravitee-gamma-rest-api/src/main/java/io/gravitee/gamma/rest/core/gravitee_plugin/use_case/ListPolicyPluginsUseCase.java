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
package io.gravitee.gamma.rest.core.gravitee_plugin.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PlatformPolicyPlugin;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.PolicyPluginProvider;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListPolicyPluginsUseCase {

    private static final Comparator<PlatformPolicyPlugin> BY_NAME_THEN_ID = Comparator.comparing(
        (PlatformPolicyPlugin policy) -> policy.plugin().name(),
        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
    ).thenComparing(policy -> policy.plugin().id());

    private final PolicyPluginProvider policyPluginProvider;

    public record Input(String organizationId, Set<PolicyFlowType> flows, String phase) {
        public Input {
            flows = flows == null ? Set.of() : Set.copyOf(flows);
        }
    }

    public record Output(List<PlatformPolicyPlugin> policies) {}

    public Output execute(Input input) {
        var policies = policyPluginProvider
            .findAllPolicies(input.organizationId(), flows -> flows.declares(input.flows(), input.phase()))
            .stream()
            .sorted(BY_NAME_THEN_ID)
            .toList();
        return new Output(policies);
    }
}
