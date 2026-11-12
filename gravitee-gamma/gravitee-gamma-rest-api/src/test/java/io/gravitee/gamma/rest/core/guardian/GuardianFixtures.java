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

import io.gravitee.apim.infra.json.jackson.JacksonJsonDeserializer;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyCapabilities;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.PolicyPluginProvider;
import io.gravitee.gamma.rest.core.guardian.domain_service.ValidateGuardianDomainService;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.CreateGuardian;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianLifecycleState;
import io.gravitee.gamma.rest.core.guardian.model.UpdateGuardianCommand;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
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

    private GuardianFixtures() {}

    public static PolicyPluginProvider aPolicyPluginProvider() {
        return aPolicyPluginProvider(() ->
            Map.of("data-logging-masking", new PolicyCapabilities(true, Map.of(ApiType.LLM_PROXY, Set.of(Phase.REQUEST, Phase.RESPONSE))))
        );
    }

    public static PolicyPluginProvider aPolicyPluginProvider(Supplier<Map<String, PolicyCapabilities>> policies) {
        var provider = Mockito.mock(PolicyPluginProvider.class);
        Mockito.when(provider.findAllCapabilities()).thenAnswer(call -> policies.get());
        return provider;
    }

    public static ValidateGuardianDomainService aGuardianValidator() {
        return aGuardianValidator(new InMemoryGuardianRepository());
    }

    public static ValidateGuardianDomainService aGuardianValidator(GuardianRepository repository) {
        return new ValidateGuardianDomainService(repository, aPolicyPluginProvider(), new JacksonJsonDeserializer());
    }

    public static Step aPolicy() {
        return Step.builder().name("Detect credentials").policy("data-logging-masking").enabled(true).build();
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
