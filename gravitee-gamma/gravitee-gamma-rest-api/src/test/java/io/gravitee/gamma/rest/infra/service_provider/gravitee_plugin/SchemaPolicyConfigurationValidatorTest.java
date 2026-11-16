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
package io.gravitee.gamma.rest.infra.service_provider.gravitee_plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDeserializer;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyConfigurationCheck;
import io.gravitee.json.validation.JsonSchemaValidatorImpl;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import io.gravitee.rest.api.service.impl.JsonSchemaServiceImpl;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SchemaPolicyConfigurationValidatorTest {

    /**
     * A guardrail schema of the shape the plugins declare, carrying the three traits that make the
     * validator <em>correct</em> a document before accepting it: a required property with a default, a
     * closed object, and an optional property a caller may send as {@code null}.
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
    private final SchemaPolicyConfigurationValidator validator = new SchemaPolicyConfigurationValidator(
        policyValidation,
        new JacksonJsonDeserializer()
    );

    /**
     * Runs the configuration through the very validator an API flow validates its own steps with — the
     * schema chain, not a stubbed answer — so what this pins is the behaviour of that chain. What it
     * does not pin is the plugin lookup around it: that the schema handed here is the one the plugin
     * declares is {@code PolicyPluginServiceImpl}'s own business, covered by its tests.
     */
    private void given_a_policy_declaring_the_guardrail_schema() {
        var schemas = new JsonSchemaServiceImpl(new JsonSchemaValidatorImpl());
        when(policyValidation.validateAndSanitizeConfiguration(eq("rule-guardrail"), anyString())).thenAnswer(call ->
            schemas.validate(GUARDRAIL_SCHEMA, call.getArgument(1))
        );
    }

    @Test
    void should_accept_a_configuration_the_policy_schema_accepts() {
        given_a_policy_declaring_the_guardrail_schema();

        assertThat(
            validator.check("rule-guardrail", "{\"mode\":\"REPORT\",\"rules\":[{\"policy\":\"ban-topics\",\"threshold\":0.5}]}")
        ).isEqualTo(
            new PolicyConfigurationCheck.Conforming("{\"mode\":\"REPORT\",\"rules\":[{\"policy\":\"ban-topics\",\"threshold\":0.5}]}")
        );
    }

    /**
     * The point of answering the configuration rather than a bare verdict: the validator accepts a
     * document it corrected, and storing the submitted one would store a configuration the policy was
     * never validated with — a Guardian the API accepted that a gateway still cannot start (AIAM-1750).
     */
    @Test
    void should_answer_the_configuration_the_policy_accepts_rather_than_the_one_submitted() {
        given_a_policy_declaring_the_guardrail_schema();

        SoftAssertions.assertSoftly(softly -> {
            softly
                .assertThat(validator.check("rule-guardrail", "{\"rules\":[{\"policy\":\"ban-topics\"}]}"))
                .as("the default of the missing required property is injected")
                .isEqualTo(new PolicyConfigurationCheck.Conforming("{\"rules\":[{\"policy\":\"ban-topics\"}],\"mode\":\"BLOCK\"}"));
            softly
                .assertThat(validator.check("rule-guardrail", "{\"mode\":\"BLOCK\",\"comment\":null,\"rules\":[{\"policy\":\"p\"}]}"))
                .as("the null entry is dropped, as the #4087 fix does for an API flow")
                .isEqualTo(new PolicyConfigurationCheck.Conforming("{\"mode\":\"BLOCK\",\"rules\":[{\"policy\":\"p\"}]}"));
            softly
                .assertThat(validator.check("rule-guardrail", "{\"mode\":\"BLOCK\",\"unknown\":1,\"rules\":[{\"policy\":\"p\"}]}"))
                .as("the property the closed schema forbids is pruned")
                .isEqualTo(new PolicyConfigurationCheck.Conforming("{\"mode\":\"BLOCK\",\"rules\":[{\"policy\":\"p\"}]}"));
        });
    }

    @Test
    void should_answer_the_schema_error_of_a_configuration_the_policy_refuses() {
        given_a_policy_declaring_the_guardrail_schema();

        SoftAssertions.assertSoftly(softly -> {
            softly
                .assertThat(violation("{}"))
                .as("no configuration at all, which the gateway could not start the policy with")
                .contains("rules");
            softly.assertThat(violation("{\"mode\":\"BLOCK\",\"rules\":\"ban-topics\"}")).as("rules of the wrong type").isNotBlank();
            softly
                .assertThat(violation("{\"mode\":\"BLOCK\",\"rules\":[]}"))
                .as("an empty rule sequence, which minItems refuses")
                .isNotBlank();
            softly
                .assertThat(violation("{\"mode\":\"BLOCK\",\"rules\":[{\"policy\":\"p\",\"threshold\":5}]}"))
                .as("a threshold out of the range the schema allows")
                .isNotBlank();
            softly.assertThat(violation("{\"mode\":\"OFF\",\"rules\":[{\"policy\":\"p\"}]}")).as("a value out of the enum").isNotBlank();
        });
    }

    @Test
    void should_refuse_a_configuration_that_is_no_json_object_without_asking_the_validator() {
        SoftAssertions.assertSoftly(softly -> {
            for (String configuration : new String[] { "{apiKey:\"s3cr3t\"", "[]", "42", "\"s3cr3t\"", "null" }) {
                softly
                    .assertThat(validator.check("rule-guardrail", configuration))
                    .as("configuration %s", configuration)
                    .isInstanceOf(PolicyConfigurationCheck.Violation.class)
                    .extracting(check -> ((PolicyConfigurationCheck.Violation) check).message(), InstanceOfAssertFactories.STRING)
                    .as("the parser message would quote the configuration, which may carry a credential")
                    .doesNotContain("s3cr3t");
            }
        });
    }

    @Test
    void should_report_a_policy_the_registry_no_longer_knows_as_a_refused_step() {
        when(policyValidation.validateAndSanitizeConfiguration(eq("gone"), anyString())).thenThrow(new PluginNotFoundException("gone"));

        assertThat(validator.check("gone", "{}"))
            .as("a 404 out of a validation endpoint is what the port exists to prevent")
            .isEqualTo(new PolicyConfigurationCheck.Violation("the policy is not installed"));
    }

    @Test
    void should_answer_a_violation_even_when_the_refusal_carries_no_message() {
        when(policyValidation.validateAndSanitizeConfiguration(eq("rule-guardrail"), anyString())).thenThrow(new InvalidDataException());

        assertThat(violation("{}")).as("a step is still refused, with something to show the user").isNotBlank();
    }

    private String violation(String configuration) {
        var check = validator.check("rule-guardrail", configuration);
        return check instanceof PolicyConfigurationCheck.Violation violation ? violation.message() : null;
    }
}
