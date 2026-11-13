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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * What the guardrails of a Guardian do to the calls they evaluate.
 *
 * <p>There is no platform-wide contract for this: {@code GuardrailPolicy} says nothing about effects, and each
 * guardrail policy spells its own in its configuration. This reads the one convention the guardrail policies of the
 * platform share — an {@code onMatch} and an {@code onNoMatch} object, each carrying an {@code action} — and reports
 * nothing for a policy that follows another one (a screen then shows no effect rather than a wrong one).
 *
 * @author GraviteeSource Team
 */
public final class GuardrailEffects {

    private static final String ON_MATCH = "onMatch";
    private static final String ON_NO_MATCH = "onNoMatch";
    private static final String ACTION = "action";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GuardrailEffects() {}

    /** The effects the sequence can apply, each once, strongest first. */
    public static List<GuardrailEffect> of(Guardian definition) {
        var effects = EnumSet.noneOf(GuardrailEffect.class);
        if (definition == null || definition.getPolicies() == null) {
            return List.of();
        }
        definition
            .getPolicies()
            .stream()
            .filter(Objects::nonNull)
            .filter(Step::isEnabled)
            .map(GuardrailEffects::configurationOf)
            .flatMap(Optional::stream)
            .forEach(configuration -> {
                actionOf(configuration, ON_MATCH).ifPresent(effects::add);
                actionOf(configuration, ON_NO_MATCH).ifPresent(effects::add);
            });
        return List.copyOf(effects);
    }

    private static Optional<JsonNode> configurationOf(Step step) {
        var configuration = step.getConfiguration();
        if (configuration == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(MAPPER.readTree(configuration));
        } catch (JsonProcessingException _) {
            // The gateway refuses such a step when it loads the Guardian; reporting no effect is the honest answer.
            return Optional.empty();
        }
    }

    private static Optional<GuardrailEffect> actionOf(JsonNode configuration, String outcome) {
        var action = configuration.path(outcome).path(ACTION);
        return action.isTextual() ? GuardrailEffect.byName(action.asText()) : Optional.empty();
    }
}
