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
package io.gravitee.gamma.rest.core.guardian.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.PolicyPluginProvider;
import io.gravitee.gamma.rest.core.guardian.model.Guardrail;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * Lists the installed guardrails a Guardian of one API type and phase can run, so the console offers
 * only the steps the Guardian validation would accept.
 *
 * <p>The API type and the phase arrive as the wire values a Guardian carries ({@code llm-proxy},
 * {@code REQUEST}); an unknown or missing one answers 400 rather than an empty list, which would read
 * as "nothing is installed".
 *
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class ListGuardrailsUseCase {

    private final PolicyPluginProvider policyPluginProvider;

    public record Input(String apiType, String phase) {}

    public record Output(List<Guardrail> guardrails) {}

    public Output execute(Input input) {
        var apiTypes = Set.of(apiType(input.apiType()));
        var phase = phase(input.phase());
        var guardrails = policyPluginProvider
            .findAllCapabilities()
            .entrySet()
            .stream()
            .filter(policy -> policy.getValue().isGuardrailFor(apiTypes, phase))
            .map(policy -> new Guardrail(policy.getKey(), policy.getValue().name()))
            .sorted(Comparator.comparing(Guardrail::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).thenComparing(Guardrail::id))
            .toList();
        return new Output(guardrails);
    }

    private static ApiType apiType(String label) {
        var apiType = ApiType.fromLabel(label);
        if (apiType == null) {
            throw new ValidationDomainException(
                "Invalid API type: " +
                    label +
                    ". Expected one of " +
                    Arrays.stream(ApiType.values()).map(ApiType::getLabel).sorted().toList() +
                    "."
            );
        }
        return apiType;
    }

    private static Phase phase(String name) {
        return Arrays.stream(Phase.values())
            .filter(phase -> phase.name().equals(name))
            .findFirst()
            .orElseThrow(() ->
                new ValidationDomainException("Invalid phase: " + name + ". Expected one of " + Arrays.toString(Phase.values()) + ".")
            );
    }
}
