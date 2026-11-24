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
package io.gravitee.gamma.rest.core.guardrail.domain_service;

import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.A2A_PROXY;
import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.GUARDRAIL;
import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.LLM_PROXY;
import static io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType.MCP_PROXY;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.GUARDRAIL_POLICY;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrail;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrailValidator;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aPolicy;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aPolicyPluginProvider;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.flows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardrail.Guardrail.Phase;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gamma.rest.core.gravitee_plugin.inmemory.InMemoryPolicyValidation;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.DeclaredFlows;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyCapabilities;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardrailRepository;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidateGuardrailDomainServiceTest {

    private final Map<String, PolicyCapabilities> policies = new LinkedHashMap<>(
        Map.of("data-logging-masking", aGuardrailPolicy(flows(LLM_PROXY, "REQUEST,RESPONSE", GUARDRAIL, "REQUEST,RESPONSE")))
    );

    /** The schema answers, which a test installs to make a step conforming, corrected or refused. */
    private final InMemoryPolicyValidation policyValidation = new InMemoryPolicyValidation();

    private final ValidateGuardrailDomainService validator = aGuardrailValidator(
        new InMemoryGuardrailRepository(),
        aPolicyPluginProvider(() -> policies),
        policyValidation
    );

    @Test
    void should_accept_a_guardrail_policy_supporting_the_guardrail_protocol() {
        assertThat(validate(aGuardrail().build()).severe()).isEmpty();
    }

    @Test
    void should_name_each_incompatible_guardrail_policy_and_the_required_protocol() {
        policies.put("data-logging-masking", aGuardrailPolicy(flows(MCP_PROXY, "REQUEST", GUARDRAIL, "REQUEST")));
        policies.put("other-guardrail", aGuardrailPolicy(flows(A2A_PROXY, "REQUEST", GUARDRAIL, "REQUEST")));
        var guardrail = aGuardrail(definition ->
            definition.policies(List.of(aPolicy(), step("Other guardrail policy", "other-guardrail", true)))
        ).build();

        var errors = validate(guardrail).severe().orElseThrow();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).as("both incompatible steps are reported together").hasSize(2);
            softly
                .assertThat(errors)
                .extracting(Validator.Error::getMessage)
                .anySatisfy(message -> assertThat(message).contains("Detect credentials", "data-logging-masking", "llm-proxy"));
            softly
                .assertThat(errors)
                .extracting(Validator.Error::getMessage)
                .anySatisfy(message -> assertThat(message).contains("Other guardrail policy", "other-guardrail", "llm-proxy"));
        });
    }

    @Test
    void should_accept_a_guardrail_policy_whose_api_type_flow_lists_another_phase() {
        policies.put("data-logging-masking", aGuardrailPolicy(flows(LLM_PROXY, "RESPONSE", GUARDRAIL, "REQUEST")));
        var guardrail = aGuardrail(definition -> definition.phase(Phase.REQUEST)).build();

        assertThat(validate(guardrail).severe())
            .as("the guardrail flow says where the policy runs, the API type flow only which API types it applies to")
            .isEmpty();
    }

    @Test
    void should_accept_a_guardrail_policy_that_runs_in_the_guardrail_phase() {
        policies.put("data-logging-masking", aGuardrailPolicy(flows(LLM_PROXY, "RESPONSE", GUARDRAIL, "RESPONSE")));
        var guardrail = aGuardrail(definition -> definition.phase(Phase.RESPONSE)).build();

        assertThat(validate(guardrail).severe()).isEmpty();
    }

    @Test
    void should_ignore_the_api_type_phases_of_a_conventional_policy() {
        policies.put("data-logging-masking", aConventionalPolicy(flows(LLM_PROXY, "RESPONSE", GUARDRAIL, "REQUEST")));
        var guardrail = aGuardrail(definition -> definition.phase(Phase.REQUEST)).build();

        assertThat(validate(guardrail).severe()).isEmpty();
    }

    @Test
    void should_refuse_an_unknown_enabled_policy() {
        policies.clear();

        assertThat(validate(aGuardrail().build()).severe().orElseThrow())
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .asString()
            .contains("Detect credentials", "data-logging-masking", "not installed");
    }

    @Test
    void should_refuse_a_guardrail_policy_without_protocol_metadata() {
        policies.put("data-logging-masking", aGuardrailPolicy(flows()));

        assertThat(validate(aGuardrail().build()).severe().orElseThrow())
            .as("every refusal of the step is reported at once")
            .extracting(Validator.Error::getMessage)
            .satisfiesExactlyInAnyOrder(
                message -> assertThat(message).contains("Detect credentials", "does not support Guardrails"),
                message -> assertThat(message).contains("Detect credentials", "llm-proxy")
            );
    }

    @Test
    void should_ignore_an_unknown_disabled_step() {
        policies.clear();
        var guardrail = aGuardrail(definition ->
            definition.policies(List.of(step("Disabled guardrail policy", "not-installed", false)))
        ).build();

        assertThat(validate(guardrail).severe()).isEmpty();
    }

    @Test
    void should_accept_a_conventional_policy_declaring_the_guardrail_phase() {
        policies.put("data-logging-masking", aConventionalPolicy(flows(GUARDRAIL, "REQUEST")));

        assertThat(validate(aGuardrail().build()).severe()).isEmpty();
    }

    @Test
    void should_refuse_a_conventional_policy_not_declaring_the_guardrail_flow() {
        policies.put("data-logging-masking", aConventionalPolicy(flows(LLM_PROXY, "REQUEST,RESPONSE")));

        assertThat(validate(aGuardrail().build()).severe().orElseThrow())
            .as("a policy that does not declare guardrail= cannot run in a Guardrail")
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .asString()
            .contains("Detect credentials", "data-logging-masking", "does not support Guardrails", "install a version that supports them");
    }

    @Test
    void should_refuse_a_guardrail_policy_not_declaring_the_guardrail_phase() {
        policies.put("data-logging-masking", aGuardrailPolicy(flows(LLM_PROXY, "REQUEST,RESPONSE", GUARDRAIL, "RESPONSE")));
        var guardrail = aGuardrail(definition -> definition.phase(Phase.REQUEST)).build();

        assertThat(validate(guardrail).severe().orElseThrow())
            .as("a guardrail policy declaring guardrail=RESPONSE only cannot run in a REQUEST Guardrail")
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .asString()
            .contains("Detect credentials", "data-logging-masking", "phase [REQUEST]", "only supports Guardrails of phase [RESPONSE]");
    }

    @Test
    void should_refuse_a_guardrail_without_phase() {
        var guardrail = aGuardrail(definition -> definition.phase(null)).build();

        assertThat(validate(guardrail).severe().orElseThrow())
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .isEqualTo("A Guardrail phase is required.");
    }

    @Test
    void should_accept_a_guardrail_policy_declaring_the_guardrail_phase_and_the_api_type() {
        policies.put("data-logging-masking", aGuardrailPolicy(flows(LLM_PROXY, "REQUEST,RESPONSE", GUARDRAIL, "REQUEST,RESPONSE")));

        SoftAssertions.assertSoftly(softly -> {
            for (var phase : Phase.values()) {
                var guardrail = aGuardrail(definition -> definition.phase(phase)).build();
                softly.assertThat(validate(guardrail).severe()).as("%s Guardrail", phase).isEmpty();
            }
        });
    }

    @Test
    void should_accept_an_empty_guardrail_policy_sequence() {
        var guardrail = aGuardrail(definition -> definition.policies(List.of())).build();

        assertThat(validate(guardrail).severe()).isEmpty();
    }

    @Test
    void should_refuse_an_explicit_scope_protocol_even_when_the_manifest_supports_the_api_type() {
        var policy = aPolicy();
        policy.setConfiguration("{\"scope\":{\"protocol\":\"MCP\"}}");
        var guardrail = aGuardrail(definition -> definition.policies(List.of(policy))).build();

        assertThat(validate(guardrail).severe().orElseThrow())
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .asString()
            .contains("Detect credentials", "MCP", "llm-proxy");
    }

    @Test
    void should_accept_an_explicit_compatible_scope_protocol() {
        var policy = aPolicy();
        policy.setConfiguration("{\"scope\":{\"protocol\":\"LLM\"}}");
        var guardrail = aGuardrail(definition -> definition.policies(List.of(policy))).build();

        assertThat(validate(guardrail).severe()).isEmpty();
    }

    @Test
    void should_accept_the_default_scope_when_no_protocol_is_declared() {
        var policy = aPolicy();
        policy.setConfiguration("{\"scope\":{\"patterns\":[\"sensitive\"]}}");
        var guardrail = aGuardrail(definition -> definition.policies(List.of(policy))).build();

        assertThat(validate(guardrail).severe()).isEmpty();
    }

    @Test
    void should_refuse_an_unknown_or_non_textual_scope_protocol() {
        SoftAssertions.assertSoftly(softly -> {
            for (String protocol : List.of("\"UNKNOWN\"", "42", "null")) {
                var policy = aPolicy();
                policy.setConfiguration("{\"scope\":{\"protocol\":" + protocol + "}}");
                var guardrail = aGuardrail(definition -> definition.policies(List.of(policy))).build();

                softly.assertThat(validate(guardrail).severe()).as("invalid protocol %s", protocol).isPresent();
            }
        });
    }

    @Test
    void should_map_each_scope_protocol_to_the_api_type_the_gateway_expects() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(scopeErrors("LLM")).as("LLM designates llm-proxy").isEmpty();
            softly
                .assertThat(scopeErrors("MCP"))
                .as("MCP designates mcp-proxy")
                .singleElement()
                .asString()
                .contains("scope protocol [MCP], incompatible");
            softly
                .assertThat(scopeErrors("A2A"))
                .as("A2A designates a2a-proxy")
                .singleElement()
                .asString()
                .contains("scope protocol [A2A], incompatible");
            for (String protocol : List.of("llm", "HTTP", "GUARDRAIL")) {
                softly
                    .assertThat(scopeErrors(protocol))
                    .as("%s is no scope protocol", protocol)
                    .singleElement()
                    .asString()
                    .contains("invalid scope protocol");
            }
        });
    }

    private List<String> scopeErrors(String protocol) {
        var policy = aPolicy();
        policy.setConfiguration("{\"scope\":{\"protocol\":\"" + protocol + "\"}}");
        var guardrail = aGuardrail(definition -> definition.policies(List.of(policy))).build();
        return validate(guardrail).severe().stream().flatMap(List::stream).map(Validator.Error::getMessage).toList();
    }

    @Test
    void should_refuse_a_scope_that_is_no_object() {
        SoftAssertions.assertSoftly(softly -> {
            for (String configuration : List.of("{\"scope\":[]}", "{\"scope\":\"invalid\"}")) {
                var policy = aPolicy();
                policy.setConfiguration(configuration);
                var guardrail = aGuardrail(definition -> definition.policies(List.of(policy))).build();

                softly.assertThat(validate(guardrail).severe()).as("invalid scope shape in %s", configuration).isPresent();
            }
        });
    }

    @Test
    void should_refuse_a_step_whose_configuration_breaks_the_policy_schema() {
        policyValidation.givenRefuses("data-logging-masking", "$.rules: is missing but it is required");
        var policy = aPolicy();
        policy.setConfiguration("{\"scope\":{\"protocol\":\"LLM\"}}");
        var guardrail = aGuardrail(definition -> definition.policies(List.of(policy))).build();

        var errors = validate(guardrail).severe().orElseThrow();

        SoftAssertions.assertSoftly(softly -> {
            softly
                .assertThat(errors)
                .as("the step and the schema error, as an API flow reports them")
                .singleElement()
                .extracting(Validator.Error::getMessage)
                .asString()
                .contains("Detect credentials", "$.rules: is missing but it is required");
            softly
                .assertThat(policyValidation.checks())
                .as("the configuration of that very step is what was judged")
                .containsExactly(new InMemoryPolicyValidation.Check("data-logging-masking", "{\"scope\":{\"protocol\":\"LLM\"}}"));
        });
    }

    @Test
    void should_store_the_configuration_the_policy_accepts_rather_than_the_one_submitted() {
        policyValidation.givenAccepts("{\"rules\":[]}", "{\"rules\":[],\"threshold\":0.5}");
        var policy = aPolicy();
        policy.setConfiguration("{\"rules\":[]}");

        validator.validateAgainstInstalledPlugins(aGuardrailOf(policy));

        assertThat(policy.getConfiguration())
            .as("the corrected configuration, which is the one the gateway will build the policy from")
            .isEqualTo("{\"rules\":[],\"threshold\":0.5}");
    }

    /**
     * The reverse of the ticket's own case: a step carrying no configuration is judged as an empty object,
     * and a schema that completes that object has its completed configuration stored. Keeping {@code null}
     * would store a configuration the policy was never validated with.
     */
    @Test
    void should_store_the_completed_configuration_of_a_step_that_carried_none() {
        policyValidation.givenAccepts("{}", "{\"mode\":\"BLOCK\"}");
        var policy = aPolicy();

        validator.validateAgainstInstalledPlugins(aGuardrailOf(policy));

        assertThat(policy.getConfiguration()).isEqualTo("{\"mode\":\"BLOCK\"}");
    }

    @Test
    void should_leave_a_step_whose_policy_accepts_an_empty_object_carrying_no_configuration() {
        var policy = aPolicy();

        validator.validateAgainstInstalledPlugins(aGuardrailOf(policy));

        assertThat(policy.getConfiguration()).as("nothing was corrected, so there is nothing to store").isNull();
    }

    /**
     * The validation rebuilds the document it returns, so the same configuration can come back with its
     * properties in another order. A path that may not rewrite what it reads must not read that as a
     * correction, or it would refuse a draft nothing is wrong with.
     */
    @Test
    void should_not_read_a_reordered_configuration_as_a_correction() {
        policyValidation.givenAccepts("{\"rules\":[],\"mode\":\"BLOCK\"}", "{ \"mode\" : \"BLOCK\", \"rules\" : [] }");
        var policy = aPolicy();
        policy.setConfiguration("{\"rules\":[],\"mode\":\"BLOCK\"}");

        validator.validateStoredAgainstInstalledPlugins(aGuardrailOf(policy));

        assertThat(policy.getConfiguration()).as("left exactly as it is stored").isEqualTo("{\"rules\":[],\"mode\":\"BLOCK\"}");
    }

    @Test
    void should_refuse_a_stored_configuration_its_policy_would_correct_rather_than_publish_it() {
        policyValidation.givenAccepts("{\"rules\":[]}", "{\"rules\":[],\"threshold\":0.5}");
        var policy = aPolicy();
        policy.setConfiguration("{\"rules\":[]}");
        var guardrail = aGuardrailOf(policy);

        SoftAssertions.assertSoftly(softly -> {
            softly
                .assertThatThrownBy(() -> validator.validateStoredAgainstInstalledPlugins(guardrail))
                .as("publishing it would publish a configuration the policy was never validated with")
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContainingAll("Detect credentials", "edit and save the Guardrail");
            softly.assertThat(policy.getConfiguration()).as("and the definition it read is left alone").isEqualTo("{\"rules\":[]}");
        });
    }

    @Test
    void should_not_rewrite_the_guardrail_it_is_asked_to_validate() {
        policyValidation.givenAccepts("{\"rules\":[]}", "{\"rules\":[],\"threshold\":0.5}");
        var policy = aPolicy();
        policy.setConfiguration("{\"rules\":[]}");

        var result = validate(aGuardrailOf(policy));

        SoftAssertions.assertSoftly(softly -> {
            softly
                .assertThat(policy.getConfiguration())
                .as("the Validator contract is no licence to rewrite a caller's object")
                .isEqualTo("{\"rules\":[]}");
            softly.assertThat(result.severe()).as("the correction it could not apply is reported instead").isPresent();
        });
    }

    @Test
    void should_refuse_a_step_whose_policy_the_validation_no_longer_resolves() {
        policyValidation.givenNotInstalled("data-logging-masking");

        assertThat(validate(aGuardrail().build()).severe().orElseThrow())
            .as("the registry read is a snapshot: a policy uninstalled since is a refused step, not a 404")
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .asString()
            .contains("Detect credentials", "the policy is not installed");
    }

    @Test
    void should_judge_a_step_carrying_no_configuration_as_one_carrying_an_empty_object() {
        policyValidation.givenRefuses("data-logging-masking", "$.rules: is missing but it is required");

        SoftAssertions.assertSoftly(softly -> {
            for (String configuration : Arrays.asList(null, "", "   ")) {
                policyValidation.reset();
                policyValidation.givenRefuses("data-logging-masking", "$.rules: is missing but it is required");
                var policy = aPolicy();
                policy.setConfiguration(configuration);
                var guardrail = aGuardrailOf(policy);

                softly
                    .assertThat(validate(guardrail).severe())
                    .as("a schema requiring a property refuses configuration [%s]", configuration)
                    .isPresent();
                softly
                    .assertThat(policyValidation.checks())
                    .as("configuration [%s] is judged as an empty object", configuration)
                    .extracting(InMemoryPolicyValidation.Check::configuration)
                    .containsExactly("{}");
            }
        });
    }

    @Test
    void should_report_the_schema_violation_of_every_step_at_once() {
        policies.put("other-guardrail", aGuardrailPolicy(flows(LLM_PROXY, "REQUEST", GUARDRAIL, "REQUEST")));
        policyValidation.givenRefuses("data-logging-masking", "$.rules: is missing but it is required");
        policyValidation.givenRefuses("other-guardrail", "$.threshold: must have a maximum value of 1");
        var guardrail = aGuardrail(definition ->
            definition.policies(List.of(aPolicy(), step("Other guardrail policy", "other-guardrail", true)))
        ).build();

        assertThat(validate(guardrail).severe().orElseThrow())
            .extracting(Validator.Error::getMessage)
            .satisfiesExactlyInAnyOrder(
                message -> assertThat(message).contains("Detect credentials", "$.rules"),
                message -> assertThat(message).contains("Other guardrail policy", "$.threshold")
            );
    }

    @Test
    void should_refuse_with_one_sentence_per_error() {
        policies.put("other-guardrail", aGuardrailPolicy(flows(LLM_PROXY, "REQUEST", GUARDRAIL, "REQUEST")));
        policyValidation.givenRefuses("other-guardrail", "$.threshold: must have a maximum value of 1");
        var policy = aPolicy();
        policy.setConfiguration("{\"scope\":{\"protocol\":\"MCP\"}}");
        var guardrail = aGuardrail(definition ->
            definition.policies(List.of(policy, step("Other guardrail policy", "other-guardrail", true)))
        ).build();

        assertThatThrownBy(() -> validator.validateAgainstInstalledPlugins(guardrail))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage(
                "Guardrail policy step [Detect credentials] declares scope protocol [MCP], incompatible with Guardrail API types [llm-proxy]. " +
                    "Step [Other guardrail policy] has an invalid configuration: $.threshold: must have a maximum value of 1."
            );
    }

    @Test
    void should_not_judge_the_schema_of_a_step_refused_for_its_scope_protocol() {
        policyValidation.givenRefuses("data-logging-masking", "$.scope.protocol: does not have a value in the enumeration");
        var policy = aPolicy();
        policy.setConfiguration("{\"scope\":{\"protocol\":\"MCP\"}}");
        var guardrail = aGuardrail(definition -> definition.policies(List.of(policy))).build();

        assertThat(validate(guardrail).severe().orElseThrow())
            .as("one cause, one error")
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .asString()
            .contains("scope protocol [MCP], incompatible");
    }

    @Test
    void should_not_judge_the_schema_of_a_guardrail_policy_that_does_not_support_the_api_type() {
        policies.put("data-logging-masking", aGuardrailPolicy(flows(MCP_PROXY, "REQUEST", GUARDRAIL, "REQUEST")));
        policyValidation.givenRefuses("data-logging-masking", "$.rules: is missing but it is required");

        assertThat(validate(aGuardrail().build()).severe().orElseThrow())
            .as("its configuration is moot on a Guardrail it cannot run in")
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .asString()
            .contains("does not support Guardrail API types [llm-proxy]");
    }

    @Test
    void should_not_check_the_schema_of_a_disabled_step() {
        policyValidation.givenRefuses("data-logging-masking", "$.rules: is missing but it is required");
        var guardrail = aGuardrail(definition ->
            definition.policies(List.of(step("Disabled guardrail policy", "data-logging-masking", false)))
        ).build();

        assertThat(validate(guardrail).severe()).isEmpty();
        assertThat(policyValidation.checks()).as("a step run by nothing is judged by nothing").isEmpty();
    }

    @Test
    void should_not_check_the_schema_of_a_policy_that_is_not_installed() {
        policies.clear();
        policyValidation.givenRefuses("data-logging-masking", "$.rules: is missing but it is required");

        assertThat(validate(aGuardrail().build()).severe().orElseThrow())
            .as("the step is refused for the policy it names, and no schema is asked of a plugin that is gone")
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .asString()
            .contains("not installed");
        assertThat(policyValidation.checks()).isEmpty();
    }

    @Test
    void should_refuse_a_configuration_that_is_no_json_object_without_exposing_it() {
        SoftAssertions.assertSoftly(softly -> {
            for (String configuration : List.of("{invalid-sensitive-configuration", "[]", "42", "\"sensitive-configuration\"")) {
                var policy = aPolicy();
                policy.setConfiguration(configuration);
                var guardrail = aGuardrail(definition -> definition.policies(List.of(policy))).build();

                softly
                    .assertThat(validate(guardrail).severe().orElseThrow())
                    .as("configuration %s is refused on its shape", configuration)
                    .singleElement()
                    .extracting(Validator.Error::getMessage)
                    .asString()
                    .contains("Detect credentials", "must be a JSON object")
                    .doesNotContain("sensitive-configuration");
            }
        });
    }

    @Test
    void should_check_the_schema_of_an_enabled_step_that_is_no_guardrail_policy() {
        policies.put("data-logging-masking", aConventionalPolicy(flows(GUARDRAIL, "REQUEST")));
        policyValidation.givenRefuses("data-logging-masking", "$.header: is missing but it is required");

        assertThat(validate(aGuardrail().build()).severe().orElseThrow())
            .as("a conventional policy the gateway could not build is refused too, not only a guardrail policy")
            .singleElement()
            .extracting(Validator.Error::getMessage)
            .asString()
            .contains("Detect credentials", "$.header");
    }

    /** The resources are judged by their own service; what is this class's is reporting them beside the steps. */
    @Test
    void should_report_a_refused_resource_beside_a_refused_step() {
        policyValidation.givenRefuses(GUARDRAIL_POLICY, "$.rules: is missing but it is required");
        var guardrail = aGuardrail(definition ->
            definition.resources(List.of(new Resource("classifier", "absent-from-the-node", "{}", true)))
        ).build();

        assertThat(validate(guardrail).severe().orElseThrow())
            .extracting(Validator.Error::getMessage)
            .containsExactly(
                "Step [Detect credentials] has an invalid configuration: $.rules: is missing but it is required.",
                "Resource [classifier] references resource type [absent-from-the-node], which is not installed."
            );
    }

    private Validator.Result<ValidateGuardrailDomainService.Input> validate(Guardrail guardrail) {
        return validator.validateAndSanitize(new ValidateGuardrailDomainService.Input(guardrail));
    }

    private static Guardrail aGuardrailOf(Step step) {
        return aGuardrail(definition -> definition.policies(List.of(step))).build();
    }

    private static PolicyCapabilities aGuardrailPolicy(DeclaredFlows flows) {
        return new PolicyCapabilities(true, flows);
    }

    private static PolicyCapabilities aConventionalPolicy(DeclaredFlows flows) {
        return new PolicyCapabilities(false, flows);
    }

    private static Step step(String name, String policy, boolean enabled) {
        return Step.builder().name(name).policy(policy).enabled(enabled).build();
    }
}
