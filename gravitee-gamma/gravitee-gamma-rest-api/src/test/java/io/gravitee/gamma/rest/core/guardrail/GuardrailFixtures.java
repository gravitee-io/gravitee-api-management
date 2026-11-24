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
package io.gravitee.gamma.rest.core.guardrail;

import com.fasterxml.jackson.core.JsonPointer;
import inmemory.CredentialCrudServiceInMemory;
import inmemory.EnvironmentCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.core.resource.domain_service.ValidateResourceDomainService;
import io.gravitee.apim.core.secret.domain_service.SecretReferencePermissionDomainService;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.apim.infra.credential.SpelSecretExpressionParser;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDeserializer;
import io.gravitee.definition.model.guardrail.GuardrailSteps;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardrail.Guardrail.Phase;
import io.gravitee.gamma.rest.core.gravitee_plugin.inmemory.InMemoryPolicyValidation;
import io.gravitee.gamma.rest.core.gravitee_plugin.inmemory.InMemoryResourceValidation;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.DeclaredFlows;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PlatformPlugin;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyCapabilities;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyFlowType;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.ResourceReference;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.PolicyPluginProvider;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.ResourcePluginProvider;
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailUsageDomainService;
import io.gravitee.gamma.rest.core.guardrail.domain_service.ValidateGuardrailDomainService;
import io.gravitee.gamma.rest.core.guardrail.domain_service.ValidateGuardrailResourcesDomainService;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardedApiPort;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardrailRepository;
import io.gravitee.gamma.rest.core.guardrail.model.CreateGuardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardedApi;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailLifecycleState;
import io.gravitee.gamma.rest.core.guardrail.model.UpdateGuardrailCommand;
import io.gravitee.gamma.rest.core.guardrail.port.repository.GuardrailRepository;
import io.gravitee.gamma.rest.core.guardrail.use_case.DeployGuardrailUseCase;
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
 * Builders the Guardrail domain tests start from.
 *
 * <p>Every one returns a builder rather than a built object, so a test states only the field it
 * cares about and the rest stays out of the way.
 *
 * @author GraviteeSource Team
 */
public final class GuardrailFixtures {

    public static final String ENVIRONMENT_ID = "env-id";
    public static final String ORGANIZATION_ID = "org-id";

    /** A UUID, because that is what the creation endpoint accepts as a caller-supplied id. */
    public static final String GUARDRAIL_ID = "3f1b0f2a-6c14-4b0e-9a3f-7d2c8e5a1b40";

    /** The installed guardrail policy the fixtures build their sequences from. */
    public static final String GUARDRAIL_POLICY = "data-logging-masking";

    /** The resource type the fixtures install, so a resource can name one the node has. */
    public static final String INSTALLED_RESOURCE_TYPE = "ai-model-token-classification";

    /** An installed guardrail policy whose configuration names a resource, in its {@code resource} property. */
    public static final String DECISION_POLICY = "guardrail-decision";

    /** The only resource type {@link #DECISION_POLICY} accepts in that property, installed too. */
    public static final String DECISION_RESOURCE_TYPE = "ai-model-decision";

    public static final String API_ID = "api-id";

    private GuardrailFixtures() {}

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
                    flows(PolicyFlowType.LLM_PROXY, "REQUEST,RESPONSE", PolicyFlowType.GUARDRAIL, "REQUEST,RESPONSE")
                ),
                DECISION_POLICY,
                new PolicyCapabilities(
                    true,
                    flows(PolicyFlowType.LLM_PROXY, "REQUEST,RESPONSE", PolicyFlowType.GUARDRAIL, "REQUEST,RESPONSE")
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
        return aPolicyPluginProvider(policies, theResourceReferences());
    }

    /** The same, with the properties naming a resource read from a map a test may change, by policy id. */
    public static PolicyPluginProvider aPolicyPluginProvider(
        Supplier<Map<String, PolicyCapabilities>> policies,
        Map<String, List<ResourceReference>> references
    ) {
        var provider = Mockito.mock(PolicyPluginProvider.class);
        Mockito.when(provider.findAllCapabilities()).thenAnswer(call -> policies.get());
        Mockito.when(provider.findResourceReferences(Mockito.anyString())).thenAnswer(call ->
            references.getOrDefault(call.<String>getArgument(0), List.of())
        );
        return provider;
    }

    /** {@link #DECISION_POLICY} naming a resource of {@link #DECISION_RESOURCE_TYPE} in {@code /resource}, in a mutable map. */
    public static Map<String, List<ResourceReference>> theResourceReferences() {
        return new HashMap<>(
            Map.of(DECISION_POLICY, List.of(new ResourceReference(JsonPointer.compile("/resource"), List.of(DECISION_RESOURCE_TYPE))))
        );
    }

    /** The usage service of an environment where no API runs any Guardrail, so no usage rule refuses anything. */
    public static GuardrailUsageDomainService aGuardrailUsage() {
        return new GuardrailUsageDomainService(new InMemoryGuardedApiPort());
    }

    /**
     * The usage service of an environment where one API runs this Guardrail. A Guardrail the gateways run for an API
     * keeps running there: it can be neither withdrawn nor deleted, and what decides where it runs — its phase and
     * its API types — cannot change under the API.
     */
    public static GuardrailUsageDomainService aGuardrailUsageRunning(String guardrailId) {
        var apis = new InMemoryGuardedApiPort();
        apis.givenApi(
            aGuardedApi()
                .internalFlow(Flow.builder().request(List.of(GuardrailSteps.of(guardrailId, "Credential leakage"))).build())
                .build()
        );
        return new GuardrailUsageDomainService(apis);
    }

    /** The deployment use case under the registry and the schema answers the tests install by default. */
    public static DeployGuardrailUseCase aDeployGuardrailUseCase(
        GuardrailRepository repository,
        GuardrailDeploymentDomainService deployments,
        AuditDomainService audits
    ) {
        return aDeployGuardrailUseCase(repository, deployments, audits, aGuardrailUsage());
    }

    /** The same, over an environment whose APIs are the ones this usage service reads. */
    public static DeployGuardrailUseCase aDeployGuardrailUseCase(
        GuardrailRepository repository,
        GuardrailDeploymentDomainService deployments,
        AuditDomainService audits,
        GuardrailUsageDomainService usage
    ) {
        return new DeployGuardrailUseCase(repository, deployments, audits, aGuardrailValidator(repository), usage);
    }

    /** The deployment use case under the registry the tests install by default, and these schema answers. */
    public static DeployGuardrailUseCase aDeployGuardrailUseCase(
        GuardrailRepository repository,
        GuardrailDeploymentDomainService deployments,
        AuditDomainService audits,
        PolicyValidationDomainService policyValidation
    ) {
        return aDeployGuardrailUseCase(repository, deployments, audits, aGuardrailUsage(), policyValidation);
    }

    /** The same, over an environment whose APIs are the ones this usage service reads. */
    public static DeployGuardrailUseCase aDeployGuardrailUseCase(
        GuardrailRepository repository,
        GuardrailDeploymentDomainService deployments,
        AuditDomainService audits,
        GuardrailUsageDomainService usage,
        PolicyValidationDomainService policyValidation
    ) {
        return new DeployGuardrailUseCase(
            repository,
            deployments,
            audits,
            aGuardrailValidator(repository, aPolicyPluginProvider(), policyValidation),
            usage
        );
    }

    public static ValidateGuardrailDomainService aGuardrailValidator() {
        return aGuardrailValidator(new InMemoryGuardrailRepository());
    }

    public static ValidateGuardrailDomainService aGuardrailValidator(GuardrailRepository repository) {
        return aGuardrailValidator(repository, aPolicyPluginProvider());
    }

    public static ValidateGuardrailDomainService aGuardrailValidator(GuardrailRepository repository, PolicyPluginProvider policies) {
        return aGuardrailValidator(repository, policies, new InMemoryPolicyValidation());
    }

    /** Validates against the registry and the schema answers a test installed itself. */
    public static ValidateGuardrailDomainService aGuardrailValidator(
        GuardrailRepository repository,
        PolicyPluginProvider policies,
        PolicyValidationDomainService policyValidation
    ) {
        return aGuardrailValidator(repository, policies, policyValidation, new InMemoryResourceValidation());
    }

    /** The same, with the answers of the resource plugins a test installed too. */
    public static ValidateGuardrailDomainService aGuardrailValidator(
        GuardrailRepository repository,
        PolicyPluginProvider policies,
        PolicyValidationDomainService policyValidation,
        ValidateResourceDomainService resourceValidation
    ) {
        return new ValidateGuardrailDomainService(
            repository,
            policies,
            policyValidation,
            aResourceValidator(aResourcePluginProvider(), resourceValidation),
            new JacksonJsonDeserializer()
        );
    }

    /** Judges resources against these installed types and the schema answers a test installed. */
    public static ValidateGuardrailResourcesDomainService aResourceValidator(
        ResourcePluginProvider installed,
        ValidateResourceDomainService resourceValidation
    ) {
        return new ValidateGuardrailResourcesDomainService(installed, theEnvironments(), resourceValidation, new JacksonJsonDeserializer());
    }

    public static final String A_STORE_SECRET_REFERENCE = "{#secrets.get('/gravitee/db-password:value?envId=" + ENVIRONMENT_ID + "')}";

    public static SecretReferencePermissionDomainService aSecretReferenceValidator(PermissionDomainService permissions) {
        return new SecretReferencePermissionDomainService(
            new SpelSecretExpressionParser(),
            permissions,
            new CredentialCrudServiceInMemory(),
            GraviteeJacksonMapper.getInstance()
        );
    }

    public static SecretReferencePermissionDomainService aSecretReferenceValidatorWithoutPermissions() {
        return aSecretReferenceValidator(Mockito.mock(PermissionDomainService.class));
    }

    /** A validator that sees exactly these installed policies. */
    public static ValidateGuardrailDomainService aGuardrailValidatorAgainst(Map<String, PolicyCapabilities> installed) {
        return aGuardrailValidator(new InMemoryGuardrailRepository(), aPolicyPluginProvider(() -> installed));
    }

    public static Step aPolicy() {
        return Step.builder().name("Detect credentials").policy(GUARDRAIL_POLICY).enabled(true).build();
    }

    /** The same guardrail policy, switched off: part of the stored sequence, run by nothing. */
    public static Step aDisabledPolicy() {
        return Step.builder().name("Detect credentials").policy(GUARDRAIL_POLICY).enabled(false).build();
    }

    /** A stored definition: identified, scoped to its environment and carrying one guardrail policy. */
    public static io.gravitee.definition.model.v4.guardrail.Guardrail.GuardrailBuilder aGuardrailDefinition() {
        return io.gravitee.definition.model.v4.guardrail.Guardrail.builder()
            .id(GUARDRAIL_ID)
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
    public static io.gravitee.definition.model.v4.guardrail.Guardrail.GuardrailBuilder aDefinitionToSubmit() {
        return io.gravitee.definition.model.v4.guardrail.Guardrail.builder()
            .name("Prompt injection shield")
            .phase(Phase.REQUEST)
            .apiTypes(Set.of(ApiType.LLM_PROXY))
            .policies(List.of(aPolicy()))
            .resources(List.of());
    }

    public static Guardrail.GuardrailBuilder aGuardrail(
        UnaryOperator<io.gravitee.definition.model.v4.guardrail.Guardrail.GuardrailBuilder> definition
    ) {
        return aGuardrail().definition(definition.apply(aGuardrailDefinition()).build());
    }

    /** A creation payload: the definition's attributes flattened, plus the description. */
    public static CreateGuardrail.CreateGuardrailBuilder aCreateGuardrail() {
        return CreateGuardrail.builder()
            .name("Prompt injection shield")
            .description("Flags a prompt that tries to override the system instructions.")
            .phase(Phase.REQUEST)
            .apiTypes(Set.of(ApiType.LLM_PROXY))
            .policies(List.of(aPolicy()))
            .resources(List.of());
    }

    /** An update payload: the whole definition, plus the description. */
    public static UpdateGuardrailCommand.UpdateGuardrailCommandBuilder anUpdateGuardrailCommand() {
        return UpdateGuardrailCommand.builder().definition(aDefinitionToSubmit().build()).description("Renamed description");
    }

    /** An LLM proxy of the fixtures' environment that runs no Guardrail yet. */
    public static GuardedApi.GuardedApiBuilder aGuardedApi() {
        return GuardedApi.builder()
            .id(API_ID)
            .environmentId(ENVIRONMENT_ID)
            .name("Support assistant")
            .type(ApiType.LLM_PROXY)
            .supportsGuardrails(true);
    }

    public static Guardrail.GuardrailBuilder aGuardrail() {
        return Guardrail.builder()
            .definition(aGuardrailDefinition().build())
            .description("Blocks a reply that echoes an API key.")
            .lifecycleState(GuardrailLifecycleState.UNDEPLOYED)
            .createdAt(Instant.parse("2026-02-01T20:22:02Z"))
            .updatedAt(Instant.parse("2026-02-02T20:22:02Z"));
    }
}
