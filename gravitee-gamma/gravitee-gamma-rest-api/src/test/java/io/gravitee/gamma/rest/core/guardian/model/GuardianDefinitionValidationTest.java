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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.definition.model.v4.resource.Resource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Why {@link UpdateGuardianCommand} does not carry {@code @Valid} on its definition.
 *
 * <p>Not because validating a definition is unsafe: it is not. The definition declares
 * {@code @NotBlank} on {@code id} and {@code environmentId}, and an update payload carries neither —
 * both come from the path and are filled in once the stored Guardian has been read. Cascading would
 * therefore refuse every legitimate update, for a reason that has nothing to do with what the caller
 * sent.
 *
 * <p>Creation does not have the problem: {@link CreateGuardian} carries the definition's attributes
 * flattened, with its own constraints, and cascades only into {@code policies} and
 * {@code resources} — which these tests show validate cleanly.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianDefinitionValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static Guardian.GuardianBuilder aDefinition() {
        return Guardian.builder().name("n").phase(Guardian.Phase.REQUEST).apiTypes(Set.of(ApiType.LLM_PROXY));
    }

    @Test
    void should_validate_a_complete_definition_cleanly() {
        var definition = aDefinition().id("3f1b0f2a-6c14-4b0e-9a3f-7d2c8e5a1b40").environmentId("env").build();

        assertThat(validator.validate(definition)).isEmpty();
    }

    @Test
    void should_report_the_identity_as_missing_on_the_definition_an_update_payload_builds() {
        // which is exactly why the update command does not cascade: the path owns both fields
        var definition = aDefinition().build();

        assertThat(validator.validate(definition))
            .extracting(violation -> violation.getPropertyPath().toString())
            .containsExactlyInAnyOrder("id", "environmentId");
    }

    @Test
    void should_validate_a_step_cleanly_so_policies_may_cascade() {
        var step = Step.builder().name("s").policy("p").enabled(true).build();

        assertThat(validator.validate(step)).isEmpty();
    }

    @Test
    void should_validate_a_resource_cleanly_so_resources_may_cascade() {
        var resource = new Resource();
        resource.setName("r");
        resource.setType("t");

        assertThat(validator.validate(resource)).isEmpty();
    }
}
