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
package io.gravitee.gamma.rest.core.guardian;

import inmemory.EnvironmentCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.core.resource.domain_service.ValidateResourceDomainService;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDeserializer;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.gravitee_plugin.inmemory.InMemoryPolicyValidation;
import io.gravitee.gamma.rest.core.gravitee_plugin.inmemory.InMemoryResourceValidation;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.DeclaredFlows;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PlatformPlugin;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyCapabilities;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.ResourceReference;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.PolicyPluginProvider;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.ResourcePluginProvider;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardian.domain_service.ValidateGuardianDomainService;
import io.gravitee.gamma.rest.core.guardian.domain_service.ValidateGuardianResourcesDomainService;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.CreateGuardian;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianLifecycleState;
import io.gravitee.gamma.rest.core.guardian.model.UpdateGuardianCommand;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import io.gravitee.gamma.rest.core.guardian.use_case.DeployGuardianUseCase;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.mockito.Mockito;

/**
 * Builders the Guardian domain tests start from.
 *
 * <p>Every one returns a builder rather than a built object, so a test states only the field it
 * cares about and the rest stays out of the way.
 *
 * @author GraviteeSource Team
 */
public final class GuardianFixtures {

    public static final String ENVIRONMENT_ID = "env-id";
    public static final String ORGANIZATION_ID = "org-id";

    /** A UUID, because that is what the creation endpoint accepts as a caller-supplied id. */
    public static final String GUARDIAN_ID = "3f1b0f2a-6c14-4b0e-9a3f-7d2c8e5a1b40";

    /** The installed guardrail the fixtures build their sequences from. */
    public static final String GUARDRAIL_POLICY = "data-logging-masking";

    /** The resource type the fixtures install, so a resource can name one the node has. */
    public static final String INSTALLED_RESOURCE_TYPE = "ai-model-token-classification";

    /** An installed guardrail whose configuration names a resource, in its {@code resource} property. */
    public static final String DECISION_POLICY = "guardrail-decision";

    /** The only resource type {@link #DECISION_POLICY} accepts in that property, installed too. */
    public static final String DECISION_RESOURCE_TYPE = "ai-model-decision";

    private GuardianFixtures() {}

    public static ResourcePluginProvider aResourcePluginProvider() {
        return aResourcePluginProvider(() -> Set.of(INSTALLED_RESOURCE_TYPE, DECISION_RESOURCE_TYPE));
    }

    /** A catalog holding these types, each deployed by the node and enabled by the license. */
    public static ResourcePluginProvider aResourcePluginProvider(Supplier<Set<String>> types) {
        return aResourceCatalog(() ->
            types
                .get()
                .stream()
                .map(type -> aResourceType(type, true))
                .collect(Collectors.toSet())
        );
    }

    /** A catalog holding exactly these types, as the organization of the fixtures reads it. */
    public static ResourcePluginProvider aResourceCatalog(Supplier<Set<PlatformPlugin>> types) {
        var provider = Mockito.mock(ResourcePluginProvider.class);
        Mockito.when(provider.findAll(ORGANIZATION_ID)).thenAnswer(call -> types.get());
        return provider;
    }

    /** A resource type of the node; {@code deployed} is false when the node or the license holds it back. */
    public static PlatformPlugin aResourceType(String id, boolean deployed) {
        return PlatformPlugin.builder().id(id).name(id).deployed(deployed).build();
    }

    /** Resolves the environment of the fixtures to their organization, whose license decides what is enabled. */
    public static EnvironmentCrudService theEnvironments() {
        var environments = new EnvironmentCrudServiceInMemory();
        environments.initWith(List.of(Environment.builder().id(ENVIRONMENT_ID).organizationId(ORGANIZATION_ID).build()));
        return environments;
    }

    public static PolicyPluginProvider aPolicyPluginProvider() {
        var installed = theInstalledPolicies();
        return aPolicyPluginProvider(() -> installed);
    }

    /** The policies installed by default, in a map a test may change to install or remove one. */
    public static Map<String, PolicyCapabilities> theInstalledPolicies() {
        return new HashMap<>(
            Map.of(
                GUARDRAIL_POLICY,
                new PolicyCapabilities(
                    true,
                    flows(PolicyFlowType.LLM_PROXY, "REQUEST,RESPONSE", PolicyFlowType.GUARDIAN, "REQUEST,RESPONSE")
                ),
                DECISION_POLICY,
                new PolicyCapabilities(
                    true,
                    flows(PolicyFlowType.LLM_PROXY, "REQUEST,RESPONSE", PolicyFlowType.GUARDIAN, "REQUEST,RESPONSE")
                )
            )
        );
    }

    /** The flows a plugin manifest declares, each with its comma-separated phases. */
    public static DeclaredFlows flows() {
        return new DeclaredFlows(Map.of());
    }

    public static DeclaredFlows flows(PolicyFlowType flow, String phases) {
        var flows = new EnumMap<PolicyFlowType, List<String>>(PolicyFlowType.class);
        flows.put(flow, phases(phases));
        return new DeclaredFlows(flows);
    }

    public static DeclaredFlows flows(PolicyFlowType flow, String phases, PolicyFlowType otherFlow, String otherPhases) {
        var flows = new EnumMap<>(flows(flow, phases).phases());
        flows.put(otherFlow, phases(otherPhases));
        return new DeclaredFlows(flows);
    }

    /** Split and stripped the way the plugin provider reads a manifest. */
    private static List<String> phases(String phases) {
        return Arrays.stream(phases.split(",")).map(String::strip).toList();
    }

    public static PolicyPluginProvider aPolicyPluginProvider(Supplier<Map<String, PolicyCapabilities>> policies) {
        var provider = Mockito.mock(PolicyPluginProvider.class);
        Mockito.when(provider.findAllCapabilities()).thenAnswer(call -> policies.get());
        Mockito.when(provider.findResourceReferences(DECISION_POLICY)).thenReturn(
            List.of(new ResourceReference("/resource", List.of(DECISION_RESOURCE_TYPE)))
        );
        return provider;
    }

    /** The deployment use case under the registry and the schema answers the tests install by default. */
    public static DeployGuardianUseCase aDeployGuardianUseCase(
        GuardianRepository repository,
        GuardianDeploymentDomainService deployments,
        AuditDomainService audits
    ) {
        return new DeployGuardianUseCase(repository, deployments, audits, aGuardianValidator(repository));
    }

    /** The deployment use case under the registry the tests install by default, and these schema answers. */
    public static DeployGuardianUseCase aDeployGuardianUseCase(
        GuardianRepository repository,
        GuardianDeploymentDomainService deployments,
        AuditDomainService audits,
        PolicyValidationDomainService policyValidation
    ) {
        return new DeployGuardianUseCase(
            repository,
            deployments,
            audits,
            aGuardianValidator(repository, aPolicyPluginProvider(), policyValidation)
        );
    }

    public static ValidateGuardianDomainService aGuardianValidator() {
        return aGuardianValidator(new InMemoryGuardianRepository());
    }

    public static ValidateGuardianDomainService aGuardianValidator(GuardianRepository repository) {
        return aGuardianValidator(repository, aPolicyPluginProvider());
    }

    public static ValidateGuardianDomainService aGuardianValidator(GuardianRepository repository, PolicyPluginProvider policies) {
        return aGuardianValidator(repository, policies, new InMemoryPolicyValidation());
    }

    /** Validates against the registry and the schema answers a test installed itself. */
    public static ValidateGuardianDomainService aGuardianValidator(
        GuardianRepository repository,
        PolicyPluginProvider policies,
        PolicyValidationDomainService policyValidation
    ) {
        return aGuardianValidator(repository, policies, policyValidation, new InMemoryResourceValidation());
    }

    /** The same, with the answers of the resource plugins a test installed too. */
    public static ValidateGuardianDomainService aGuardianValidator(
        GuardianRepository repository,
        PolicyPluginProvider policies,
        PolicyValidationDomainService policyValidation,
        ValidateResourceDomainService resourceValidation
    ) {
        return new ValidateGuardianDomainService(
            repository,
            policies,
            policyValidation,
            aResourceValidator(aResourcePluginProvider(), resourceValidation),
            new JacksonJsonDeserializer()
        );
    }

    /** Judges resources against these installed types and the schema answers a test installed. */
    public static ValidateGuardianResourcesDomainService aResourceValidator(
        ResourcePluginProvider installed,
        ValidateResourceDomainService resourceValidation
    ) {
        return new ValidateGuardianResourcesDomainService(installed, theEnvironments(), resourceValidation, new JacksonJsonDeserializer());
    }

    /** A validator that sees exactly these installed policies. */
    public static ValidateGuardianDomainService aGuardianValidatorAgainst(Map<String, PolicyCapabilities> installed) {
        return aGuardianValidator(new InMemoryGuardianRepository(), aPolicyPluginProvider(() -> installed));
    }

    public static Step aPolicy() {
        return Step.builder().name("Detect credentials").policy(GUARDRAIL_POLICY).enabled(true).build();
    }

    /** The same guardrail, switched off: part of the stored sequence, run by nothing. */
    public static Step aDisabledPolicy() {
        return Step.builder().name("Detect credentials").policy(GUARDRAIL_POLICY).enabled(false).build();
    }

    /** A stored definition: identified, scoped to its environment and carrying one guardrail. */
    public static io.gravitee.definition.model.v4.guardian.Guardian.GuardianBuilder aGuardianDefinition() {
        return io.gravitee.definition.model.v4.guardian.Guardian.builder()
            .id(GUARDIAN_ID)
            .environmentId(ENVIRONMENT_ID)
            .name("Credential leakage")
            .phase(Phase.REQUEST)
            .apiTypes(Set.of(ApiType.LLM_PROXY))
            .policies(List.of(aPolicy()))
            .resources(List.of());
    }

    /**
     * A definition as a request body carries it: no id and no environment, the two fields the use
     * case fills in from the path.
     */
    public static io.gravitee.definition.model.v4.guardian.Guardian.GuardianBuilder aDefinitionToSubmit() {
        return io.gravitee.definition.model.v4.guardian.Guardian.builder()
            .name("Prompt injection shield")
            .phase(Phase.REQUEST)
            .apiTypes(Set.of(ApiType.LLM_PROXY))
            .policies(List.of(aPolicy()))
            .resources(List.of());
    }

    public static Guardian.GuardianBuilder aGuardian(
        UnaryOperator<io.gravitee.definition.model.v4.guardian.Guardian.GuardianBuilder> definition
    ) {
        return aGuardian().definition(definition.apply(aGuardianDefinition()).build());
    }

    /** A creation payload: the definition's attributes flattened, plus the description. */
    public static CreateGuardian.CreateGuardianBuilder aCreateGuardian() {
        return CreateGuardian.builder()
            .name("Prompt injection shield")
            .description("Flags a prompt that tries to override the system instructions.")
            .phase(Phase.REQUEST)
            .apiTypes(Set.of(ApiType.LLM_PROXY))
            .policies(List.of(aPolicy()))
            .resources(List.of());
    }

    /** An update payload: the whole definition, plus the description. */
    public static UpdateGuardianCommand.UpdateGuardianCommandBuilder anUpdateGuardianCommand() {
        return UpdateGuardianCommand.builder().definition(aDefinitionToSubmit().build()).description("Renamed description");
    }

    public static Guardian.GuardianBuilder aGuardian() {
        return Guardian.builder()
            .definition(aGuardianDefinition().build())
            .description("Blocks a reply that echoes an API key.")
            .lifecycleState(GuardianLifecycleState.UNDEPLOYED)
            .createdAt(Instant.parse("2026-02-01T20:22:02Z"))
            .updatedAt(Instant.parse("2026-02-02T20:22:02Z"));
    }
}
