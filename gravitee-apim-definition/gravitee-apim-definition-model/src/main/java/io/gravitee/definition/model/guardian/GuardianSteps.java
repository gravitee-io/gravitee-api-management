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
package io.gravitee.definition.model.guardian;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.stream.Stream;

/**
 * The {@code guardian-policy} steps through which a flow runs Guardians: one step per Guardian, carrying its id.
 *
 * @author GraviteeSource Team
 */
public final class GuardianSteps {

    public static final String POLICY = "guardian-policy";

    private static final String GUARDIAN_ID = "guardianId";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GuardianSteps() {}

    public static Step of(String guardianId, String name) {
        return Step.builder()
            .name(name)
            .policy(POLICY)
            .configuration(MAPPER.createObjectNode().put(GUARDIAN_ID, guardianId).toString())
            .build();
    }

    /** The Guardian a step runs, empty when it is not a {@code guardian-policy} step or does not name one. */
    public static Optional<String> guardianIdOf(Step step) {
        if (step == null || !POLICY.equals(step.getPolicy()) || step.getConfiguration() == null) {
            return Optional.empty();
        }
        try {
            JsonNode guardianId = MAPPER.readTree(step.getConfiguration()).get(GUARDIAN_ID);
            return guardianId == null || guardianId.asText().isBlank() ? Optional.empty() : Optional.of(guardianId.asText());
        } catch (JsonProcessingException e) {
            // The gateway refuses such a step when it loads the API; it runs no Guardian either way.
            return Optional.empty();
        }
    }

    /** The Guardians a flow runs, request ones first, each phase in execution order. */
    public static SequencedSet<String> guardianIdsOf(Flow flow) {
        if (flow == null) {
            return new LinkedHashSet<>();
        }
        return Stream.of(flow.getRequest(), flow.getResponse())
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .map(GuardianSteps::guardianIdOf)
            .flatMap(Optional::stream)
            .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
    }
}
