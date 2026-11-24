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
package io.gravitee.gamma.rest.core.guardian.domain_service;

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.DECISION_POLICY;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.DECISION_RESOURCE_TYPE;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.INSTALLED_RESOURCE_TYPE;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianValidator;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aPolicyPluginProvider;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.theInstalledPolicies;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.theResourceReferences;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonPointer;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gamma.rest.core.gravitee_plugin.inmemory.InMemoryPolicyValidation;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.ResourceReference;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.PolicyPluginProvider;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A step whose policy schema declares a property naming a resource ({@code gioConfig.uiType: resource-type})
 * must name a resource the Guardian holds, enabled, and of a type the policy accepts there: the gateway
 * hands the policy the resource of that name, and a Guardian saved without one would deploy a guardrail policy
 * that fails on every call.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateGuardianResourceReferencesTest {

    private static final String STEP = "Ask the judge";

    private final InMemoryPolicyValidation policyValidation = new InMemoryPolicyValidation();
    private final Map<String, List<ResourceReference>> references = theResourceReferences();
    private final PolicyPluginProvider policies = aPolicyPluginProvider(() -> theInstalledPolicies(), references);
    private final ValidateGuardianDomainService validator = aGuardianValidator(
        new InMemoryGuardianRepository(),
        policies,
        policyValidation
    );

    @Test
    void should_accept_a_step_naming_an_enabled_resource_of_the_accepted_type() {
        var guardian = aGuardianWith(aDecisionStep("judge"), aResource("judge", DECISION_RESOURCE_TYPE, true));

        assertThat(refusalsOf(guardian)).isEmpty();
    }

    @Test
    void should_refuse_a_step_naming_a_resource_the_guardian_does_not_hold() {
        var guardian = aGuardianWith(aDecisionStep("missing"), aResource("judge", DECISION_RESOURCE_TYPE, true));

        assertThat(refusalsOf(guardian)).containsExactly(
            "Step [Ask the judge] references resource [missing], which the Guardian does not hold."
        );
    }

    @Test
    void should_refuse_a_step_naming_a_resource_of_the_wrong_type() {
        var guardian = aGuardianWith(aDecisionStep("classifier"), aResource("classifier", INSTALLED_RESOURCE_TYPE, true));

        assertThat(refusalsOf(guardian)).containsExactly(
            "Step [Ask the judge] references a resource of the wrong type: [classifier] is of type [" +
                INSTALLED_RESOURCE_TYPE +
                "], where the policy accepts [" +
                DECISION_RESOURCE_TYPE +
                "]."
        );
    }

    /** The gateway only loads the enabled resources of a Guardian: a disabled one is as good as absent. */
    @Test
    void should_refuse_a_step_naming_a_disabled_resource() {
        var guardian = aGuardianWith(aDecisionStep("judge"), aResource("judge", DECISION_RESOURCE_TYPE, false));

        assertThat(refusalsOf(guardian)).containsExactly("Step [Ask the judge] references resource [judge], which is disabled.");
    }

    /** As the console pickers match them: {@code cache} offers a {@code cache-redis} resource. */
    @Test
    void should_accept_a_resource_whose_type_starts_with_an_accepted_one() {
        givenTheDecisionPolicyAccepts("ai-model");
        var guardian = aGuardianWith(aDecisionStep("judge"), aResource("judge", DECISION_RESOURCE_TYPE, true));

        assertThat(refusalsOf(guardian)).isEmpty();
    }

    /** No type named in the picker means any type, but still a resource the Guardian holds, enabled. */
    @Test
    void should_accept_any_type_when_the_policy_names_none() {
        givenTheDecisionPolicyAccepts();
        var guardian = aGuardianWith(aDecisionStep("classifier"), aResource("classifier", INSTALLED_RESOURCE_TYPE, true));

        assertThat(refusalsOf(guardian)).isEmpty();
    }

    @Test
    void should_refuse_a_step_naming_a_resource_the_guardian_does_not_hold_when_the_policy_names_no_type() {
        givenTheDecisionPolicyAccepts();

        assertThat(refusalsOf(aGuardianWith(aDecisionStep("missing")))).containsExactly(
            "Step [Ask the judge] references resource [missing], which the Guardian does not hold."
        );
    }

    /** A resource without a type is refused as such by the resource validation: one cause, not two. */
    @Test
    void should_leave_a_resource_without_type_to_the_resource_validation() {
        var guardian = aGuardianWith(aDecisionStep("judge"), aResource("judge", null, true));

        assertThat(refusalsOf(guardian)).containsExactly("Resource [judge] has no type.");
    }

    /** Refused as a malformed definition, rather than failing on the hole while judging a step. */
    @Test
    void should_refuse_a_null_resource_entry() {
        var guardian = aGuardianWith(aDecisionStep("judge"), null, aResource("judge", DECISION_RESOURCE_TYPE, true));

        assertThat(refusalsOf(guardian)).containsExactly("Guardian resources must not contain null entries.");
    }

    /** The write stores the configuration the policy accepts: that is the one whose reference must hold. */
    @Test
    void should_judge_the_reference_of_the_configuration_the_policy_corrects_on_a_write() {
        policyValidation.givenAccepts(aDecisionConfiguration("judge"), aDecisionConfiguration("missing"));
        var guardian = aGuardianWith(aDecisionStep("judge"), aResource("judge", DECISION_RESOURCE_TYPE, true));

        assertThatThrownBy(() -> validator.validateAgainstInstalledPlugins(guardian))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage("Step [Ask the judge] references resource [missing], which the Guardian does not hold.");
    }

    /** One cause per step: a configuration only accepted corrected is refused as such, its reference unread. */
    @Test
    void should_not_judge_the_reference_of_a_configuration_only_accepted_corrected_when_nothing_is_rewritten() {
        policyValidation.givenAccepts(aDecisionConfiguration("missing"), aDecisionConfiguration("judge"));
        var guardian = aGuardianWith(aDecisionStep("missing"), aResource("judge", DECISION_RESOURCE_TYPE, true));

        assertThat(refusalsOf(guardian)).containsExactly(
            "Step [Ask the judge] has a configuration its policy only accepts corrected: edit and save the Guardian to store the configuration the policy accepts."
        );
    }

    @Test
    void should_read_the_resource_properties_of_a_policy_once_per_validation() {
        var guardian = aGuardian(definition ->
            definition
                .policies(List.of(aDecisionStep("judge"), aDecisionStep("judge")))
                .resources(List.of(aResource("judge", DECISION_RESOURCE_TYPE, true)))
        ).build();

        refusalsOf(guardian);

        verify(policies, times(1)).findResourceReferences(DECISION_POLICY);
    }

    /** Whether the property is required is the schema's business, judged before this. */
    @ParameterizedTest
    @ValueSource(strings = { "{}", "{\"resource\":\"\"}", "{\"resource\":null}" })
    void should_leave_a_step_that_names_no_resource_to_the_schema(String configuration) {
        var step = aDecisionStep("judge");
        step.setConfiguration(configuration);

        assertThat(refusalsOf(aGuardianWith(step))).isEmpty();
    }

    @Test
    void should_not_judge_a_disabled_step() {
        var step = aDecisionStep("missing");
        step.setEnabled(false);

        assertThat(refusalsOf(aGuardianWith(step))).isEmpty();
    }

    /** One cause per step: a configuration the schema refuses is not read for the resource it names. */
    @Test
    void should_not_judge_the_reference_of_a_configuration_the_schema_refuses() {
        policyValidation.givenRefuses(DECISION_POLICY, "$.scope: is missing but it is required");

        assertThat(refusalsOf(aGuardianWith(aDecisionStep("missing")))).containsExactly(
            "Step [Ask the judge] has an invalid configuration: $.scope: is missing but it is required."
        );
    }

    /** What the write path stores is what was validated, so it refuses too, with a 400. */
    @Test
    void should_refuse_the_write_naming_the_step() {
        var guardian = aGuardianWith(aDecisionStep("missing"));

        assertThatThrownBy(() -> validator.validateAgainstInstalledPlugins(guardian))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage("Step [Ask the judge] references resource [missing], which the Guardian does not hold.");
    }

    /** A draft stored before the rule existed is refused at deployment rather than published broken. */
    @Test
    void should_refuse_the_deployment_of_a_stored_draft_naming_the_step() {
        var guardian = aGuardianWith(aDecisionStep("missing"));

        assertThatThrownBy(() -> validator.validateStoredAgainstInstalledPlugins(guardian))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage("Step [Ask the judge] references resource [missing], which the Guardian does not hold.");
    }

    private List<String> refusalsOf(Guardian guardian) {
        return validator
            .validateAndSanitize(new ValidateGuardianDomainService.Input(guardian))
            .severe()
            .orElse(List.of())
            .stream()
            .map(Validator.Error::getMessage)
            .toList();
    }

    private void givenTheDecisionPolicyAccepts(String... types) {
        references.put(DECISION_POLICY, List.of(new ResourceReference(JsonPointer.compile("/resource"), List.of(types))));
    }

    private static Guardian aGuardianWith(Step step, Resource... resources) {
        return aGuardian(definition -> definition.policies(List.of(step)).resources(Arrays.asList(resources))).build();
    }

    private static Step aDecisionStep(String resource) {
        return Step.builder().name(STEP).policy(DECISION_POLICY).enabled(true).configuration(aDecisionConfiguration(resource)).build();
    }

    private static String aDecisionConfiguration(String resource) {
        return "{\"resource\":\"" + resource + "\"}";
    }

    private static Resource aResource(String name, String type, boolean enabled) {
        return new Resource(name, type, "{}", enabled);
    }
}
