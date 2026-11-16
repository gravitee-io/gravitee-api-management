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

import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.GUARDIAN;
import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.LLM_PROXY;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianValidator;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aPolicyPluginProvider;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.flows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyCapabilities;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.json.validation.JsonSchemaValidatorImpl;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import io.gravitee.rest.api.service.impl.JsonSchemaServiceImpl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * The Guardian validation over the real schema chain, the one an API flow validates its own steps
 * with, rather than over installed answers.
 *
 * <p>What it pins is the behaviour the fix turns on: that chain <em>corrects</em> a document before
 * accepting it, so the configuration a policy accepts is not always the one it was given, and storing
 * the given one would store a configuration the policy was never validated with — a Guardian the API
 * accepted that no gateway can start (AIAM-1750).
 *
 * <p>What it does not pin is the plugin lookup around the schema: that the schema handed to the
 * validation is the one the plugin declares is {@code PolicyPluginServiceImpl}'s own business, covered
 * by its tests.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateGuardianAgainstPolicySchemasTest {

    private static final String GUARDRAIL_POLICY = "rule-guardrail";

    /**
     * A guardrail schema of the shape the plugins declare, carrying the three traits that make the
     * validation correct a document before accepting it: a required property with a default, a closed
     * object, and an optional property a caller may send as {@code null}.
     */
    private static final String GUARDRAIL_SCHEMA = """
        {
          "type": "object",
          "additionalProperties": false,
          "required": ["rules", "mode"],
          "properties": {
            "mode": { "type": "string", "enum": ["BLOCK", "REPORT"], "default": "BLOCK" },
            "comment": { "type": "string" },
            "rules": {
              "type": "array",
              "minItems": 1,
              "items": {
                "type": "object",
                "required": ["policy"],
                "properties": {
                  "policy": { "type": "string" },
                  "threshold": { "type": "number", "minimum": 0, "maximum": 1 }
                }
              }
            }
          }
        }""";

    private final PolicyValidationDomainService policyValidation = mock(PolicyValidationDomainService.class);

    private final ValidateGuardianDomainService validator = aGuardianValidator(
        new InMemoryGuardianRepository(),
        aPolicyPluginProvider(() ->
            Map.of(GUARDRAIL_POLICY, new PolicyCapabilities(true, flows(LLM_PROXY, "REQUEST,RESPONSE", GUARDIAN, "REQUEST,RESPONSE")))
        ),
        policyValidation
    );

    private void given_a_policy_declaring_the_guardrail_schema() {
        var schemas = new JsonSchemaServiceImpl(new JsonSchemaValidatorImpl());
        when(policyValidation.validateAndSanitizeConfiguration(eq(GUARDRAIL_POLICY), anyString())).thenAnswer(call ->
            schemas.validate(GUARDRAIL_SCHEMA, call.getArgument(1))
        );
    }

    @Test
    void should_leave_a_configuration_the_policy_schema_accepts_as_it_is() {
        given_a_policy_declaring_the_guardrail_schema();
        var submitted = "{\"mode\":\"REPORT\",\"rules\":[{\"policy\":\"ban-topics\",\"threshold\":0.5}]}";

        assertThat(stored(submitted)).as("nothing to correct, nothing to rewrite").isEqualTo(submitted);
    }

    @Test
    void should_store_the_configuration_the_policy_accepts_rather_than_the_one_submitted() {
        given_a_policy_declaring_the_guardrail_schema();

        assertSoftly(softly -> {
            softly
                .assertThat(stored("{\"rules\":[{\"policy\":\"ban-topics\"}]}"))
                .as("the default of the missing required property is injected")
                .isEqualTo("{\"rules\":[{\"policy\":\"ban-topics\"}],\"mode\":\"BLOCK\"}");
            softly
                .assertThat(stored("{\"mode\":\"BLOCK\",\"comment\":null,\"rules\":[{\"policy\":\"p\"}]}"))
                .as("the null entry is dropped, as the #4087 fix does for an API flow")
                .isEqualTo("{\"mode\":\"BLOCK\",\"rules\":[{\"policy\":\"p\"}]}");
            softly
                .assertThat(stored("{\"mode\":\"BLOCK\",\"unknown\":1,\"rules\":[{\"policy\":\"p\"}]}"))
                .as("the property the closed schema forbids is pruned")
                .isEqualTo("{\"mode\":\"BLOCK\",\"rules\":[{\"policy\":\"p\"}]}");
        });
    }

    /**
     * A step carrying no configuration is the Guardian of the ticket: the schema requires properties the
     * gateway would find nothing of. One with a default is completed and stored, which is what makes the
     * stored definition one the policy was validated with.
     */
    @Test
    void should_store_the_completed_configuration_of_a_step_that_carried_none() {
        var schemas = new JsonSchemaServiceImpl(new JsonSchemaValidatorImpl());
        var withDefaultsOnly = """
            {
              "type": "object",
              "required": ["mode"],
              "properties": { "mode": { "type": "string", "default": "BLOCK" } }
            }""";
        when(policyValidation.validateAndSanitizeConfiguration(eq(GUARDRAIL_POLICY), anyString())).thenAnswer(call ->
            schemas.validate(withDefaultsOnly, call.getArgument(1))
        );

        assertThat(stored(null))
            .as("judged as an empty object, accepted completed, and stored completed rather than left null")
            .isEqualTo("{\"mode\":\"BLOCK\"}");
    }

    @Test
    void should_refuse_a_step_the_policy_schema_refuses_and_say_what_is_wrong() {
        given_a_policy_declaring_the_guardrail_schema();

        assertSoftly(softly -> {
            softly
                .assertThat(refusal("{}"))
                .as("no configuration at all, which the gateway could not start the policy with")
                .contains("Detect credentials", "rules");
            softly.assertThat(refusal("{\"mode\":\"BLOCK\",\"rules\":\"ban-topics\"}")).as("rules of the wrong type").isNotBlank();
            softly
                .assertThat(refusal("{\"mode\":\"BLOCK\",\"rules\":[]}"))
                .as("an empty rule sequence, which minItems refuses")
                .isNotBlank();
            softly
                .assertThat(refusal("{\"mode\":\"BLOCK\",\"rules\":[{\"policy\":\"p\",\"threshold\":5}]}"))
                .as("a threshold out of the range the schema allows")
                .isNotBlank();
            softly.assertThat(refusal("{\"mode\":\"OFF\",\"rules\":[{\"policy\":\"p\"}]}")).as("a value out of the enum").isNotBlank();
        });
    }

    @Test
    void should_refuse_a_step_whose_policy_the_validation_no_longer_resolves() {
        when(policyValidation.validateAndSanitizeConfiguration(eq(GUARDRAIL_POLICY), anyString())).thenThrow(
            new PluginNotFoundException(GUARDRAIL_POLICY)
        );

        assertThat(refusal("{}"))
            .as("the registry read is a snapshot: a policy uninstalled since is a refused step, not the 404 of this exception")
            .contains("Detect credentials", "the policy is not installed");
    }

    @Test
    void should_refuse_a_step_even_when_the_refusal_carries_no_message() {
        when(policyValidation.validateAndSanitizeConfiguration(eq(GUARDRAIL_POLICY), anyString())).thenThrow(new InvalidDataException());

        assertThat(refusal("{}")).as("something to show the user").contains("Detect credentials", "does not match the schema");
    }

    /** The configuration stored on the step once its policy has judged the one given here. */
    private String stored(String configuration) {
        var step = aStep(configuration);
        validator.validateAgainstInstalledPolicies(aGuardianOf(step));
        return step.getConfiguration();
    }

    /** The refusal of a configuration no write path would store, or {@code null} when it is accepted. */
    private String refusal(String configuration) {
        try {
            validator.validateAgainstInstalledPolicies(aGuardianOf(aStep(configuration)));
            return null;
        } catch (ValidationDomainException e) {
            return e.getMessage();
        }
    }

    private static Step aStep(String configuration) {
        return Step.builder().name("Detect credentials").policy(GUARDRAIL_POLICY).enabled(true).configuration(configuration).build();
    }

    private static Guardian aGuardianOf(Step step) {
        return aGuardian(definition -> definition.policies(List.of(step))).build();
    }
}
