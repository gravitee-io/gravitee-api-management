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
package io.gravitee.gamma.rest.infra.config;

import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.GUARDRAIL_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.EventCrudInMemory;
import inmemory.EventLatestCrudInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import inmemory.EventQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.crud_service.ApiDefinitionCrudService;
import io.gravitee.apim.core.api.domain_service.DeployApiInternalFlowDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.deployment.service_provider.ApiDeploymentGate;
import io.gravitee.apim.core.deployment.service_provider.ApiDeploymentValidator;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.event.query_service.EventLatestQueryService;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.apim.core.generic_event.domain_service.GenericEventPublicationDomainService;
import io.gravitee.apim.core.generic_event.domain_service.GenericEventWriterDomainService;
import io.gravitee.apim.core.json.JsonDeserializer;
import io.gravitee.apim.core.json.JsonSerializer;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import io.gravitee.apim.core.resource.domain_service.ValidateResourceDomainService;
import io.gravitee.apim.core.secret.domain_service.SecretReferencePermissionDomainService;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.apim.infra.deployment.CompositeApiDeploymentValidator;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDeserializer;
import io.gravitee.apim.infra.json.jackson.JacksonJsonSerializer;
import io.gravitee.definition.model.guardrail.GuardrailSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.PolicyPluginProvider;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.ResourcePluginProvider;
import io.gravitee.gamma.rest.core.guardrail.domain_service.DescribeApiGuardrailsDomainService;
import io.gravitee.gamma.rest.core.guardrail.domain_service.SetApiGuardrailsDomainService;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.port.service_provider.GuardedApiPort;
import io.gravitee.gamma.rest.core.guardrail.port.service_provider.GuardedApiReadPermissionPort;
import io.gravitee.gamma.rest.core.guardrail.use_case.DeployGuardrailUseCase;
import io.gravitee.gamma.rest.infra.adapter.GuardrailsDeploymentValidatorAdapter;
import io.gravitee.repository.management.api.GuardrailRepository;
import io.gravitee.rest.api.service.exceptions.ApiNotDeployableException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class GammaGuardrailsConfigurationTest {

    @Test
    void should_wire_the_gamma_deployment_use_case_to_the_apim_parent_context() throws Exception {
        var repository = mock(GuardrailRepository.class);
        var mapper = GraviteeJacksonMapper.getInstance();
        var writer = new GenericEventWriterDomainService(new EventCrudInMemory(), new EventLatestCrudInMemory(), mapper);
        try (var parent = new AnnotationConfigApplicationContext(); var gamma = new AnnotationConfigApplicationContext()) {
            parent.registerBean(GuardrailRepository.class, () -> repository);
            parent.registerBean(GenericEventWriterDomainService.class, () -> writer);
            parent.registerBean(AuditDomainService.class, () -> mock(AuditDomainService.class));
            parent.registerBean(SecretReferencePermissionDomainService.class, () -> mock(SecretReferencePermissionDomainService.class));
            parent.registerBean(PolicyPluginProvider.class, () -> mock(PolicyPluginProvider.class));
            // the shared service the API flows validate their own steps with, owned by the rest-api
            // context: the Guardrail validator is injected with it directly
            parent.registerBean(PolicyValidationDomainService.class, () -> mock(PolicyValidationDomainService.class));
            parent.registerBean(ValidateResourceDomainService.class, () -> mock(ValidateResourceDomainService.class));
            parent.registerBean(ResourcePluginProvider.class, () -> mock(ResourcePluginProvider.class));
            parent.registerBean(EnvironmentCrudService.class, () -> mock(EnvironmentCrudService.class));
            parent.registerBean(EventLatestQueryService.class, EventLatestQueryServiceInMemory::new);
            parent.registerBean(EventQueryService.class, EventQueryServiceInMemory::new);
            parent.registerBean(UserCrudService.class, UserCrudServiceInMemory::new);
            parent.registerBean(GenericEventPublicationDomainService.class, () -> new GenericEventPublicationDomainService(writer, mapper));
            parent.registerBean(JsonSerializer.class, () -> new JacksonJsonSerializer());
            parent.registerBean(JsonDeserializer.class, () -> new JacksonJsonDeserializer());
            parent.registerBean(PermissionDomainService.class, () -> mock(PermissionDomainService.class));
            parent.registerBean(DeployApiInternalFlowDomainService.class, GammaGuardrailsConfigurationTest::deployApiInternalFlow);
            parent.registerBean(ApiCrudService.class, ApiCrudServiceInMemory::new);
            parent.registerBean(ApiQueryService.class, () -> new ApiQueryServiceInMemory());
            parent.refresh();
            gamma.setParent(parent);
            gamma.register(GammaGuardrailsConfiguration.class);
            gamma.refresh();

            var input = new DeployGuardrailUseCase.Input(
                AuditInfo.builder().organizationId(ORGANIZATION_ID).environmentId(ENVIRONMENT_ID).build(),
                GUARDRAIL_ID,
                null
            );
            assertThatThrownBy(() -> gamma.getBean(DeployGuardrailUseCase.class).execute(input)).isInstanceOf(
                GuardrailNotFoundException.class
            );
            verify(repository).findById(GUARDRAIL_ID);
        }
    }

    /**
     * The contract the Gamma modules owning a Guardrail-bearing API surface are coded against: both halves of "the
     * Guardrails this API runs" are domain services resolvable from the context, so a module never calls a use case of
     * this one.
     */
    @Test
    void should_offer_the_api_guardrail_domain_services_to_a_gamma_module() {
        var mapper = GraviteeJacksonMapper.getInstance();
        var writer = new GenericEventWriterDomainService(new EventCrudInMemory(), new EventLatestCrudInMemory(), mapper);
        try (var parent = new AnnotationConfigApplicationContext(); var gamma = new AnnotationConfigApplicationContext()) {
            parent.registerBean(GuardrailRepository.class, () -> mock(GuardrailRepository.class));
            parent.registerBean(GenericEventWriterDomainService.class, () -> writer);
            parent.registerBean(GenericEventPublicationDomainService.class, () -> new GenericEventPublicationDomainService(writer, mapper));
            parent.registerBean(ApiCrudService.class, ApiCrudServiceInMemory::new);
            parent.registerBean(ApiQueryService.class, () -> new ApiQueryServiceInMemory());
            parent.registerBean(JsonSerializer.class, () -> new JacksonJsonSerializer());
            parent.registerBean(JsonDeserializer.class, () -> new JacksonJsonDeserializer());
            parent.registerBean(PermissionDomainService.class, () -> mock(PermissionDomainService.class));
            parent.registerBean(DeployApiInternalFlowDomainService.class, GammaGuardrailsConfigurationTest::deployApiInternalFlow);
            parent.registerBean(EventLatestQueryService.class, EventLatestQueryServiceInMemory::new);
            parent.registerBean(EventQueryService.class, EventQueryServiceInMemory::new);
            parent.registerBean(UserCrudService.class, UserCrudServiceInMemory::new);
            parent.registerBean(PolicyPluginProvider.class, () -> mock(PolicyPluginProvider.class));
            parent.registerBean(PolicyValidationDomainService.class, () -> mock(PolicyValidationDomainService.class));
            parent.registerBean(ValidateResourceDomainService.class, () -> mock(ValidateResourceDomainService.class));
            parent.registerBean(ResourcePluginProvider.class, () -> mock(ResourcePluginProvider.class));
            parent.registerBean(EnvironmentCrudService.class, () -> mock(EnvironmentCrudService.class));
            parent.registerBean(AuditDomainService.class, () -> mock(AuditDomainService.class));
            parent.registerBean(SecretReferencePermissionDomainService.class, () -> mock(SecretReferencePermissionDomainService.class));
            parent.refresh();
            gamma.setParent(parent);
            gamma.register(GammaGuardrailsConfiguration.class);
            gamma.refresh();

            assertSoftly(softly -> {
                softly
                    .assertThat(gamma.getBean(SetApiGuardrailsDomainService.class))
                    .as("the write a module sets the Guardrails of its API with")
                    .isNotNull();
                softly
                    .assertThat(gamma.getBean(DescribeApiGuardrailsDomainService.class))
                    .as("the read a module names the Guardrails of its API with")
                    .isNotNull();
                softly
                    .assertThat(gamma.getBean(GuardedApiPort.class))
                    .as("the port a module reads the Guardrails its API holds from")
                    .isNotNull();
                softly
                    .assertThat(gamma.getBean(GuardedApiReadPermissionPort.class))
                    .as("the port the APIs a Guardrail runs on are filtered on the caller's rights with")
                    .isNotNull();
            });
        }
    }

    /**
     * How a deployment ever runs the Guardrail check: the module's bean is registered in the parent context after that
     * context is refreshed, and {@link CompositeApiDeploymentValidator} reads the validators through an
     * {@code ObjectProvider}, which resolves them on every call. Inject them as a collection instead and the check
     * silently stops running on every deployment of the installation until the next restart.
     *
     * <p>The gate is asked here rather than the module's bean: it is what every injection point of the rest-api
     * receives, and the module's validator is only ever reached as one of its candidates. Nothing else pins that
     * link — the check would leave every deployment and every start of the installation the day this bean stops
     * being an {@link ApiDeploymentValidator}.
     */
    @Test
    void should_offer_the_deployment_validator_to_a_parent_context_already_refreshed() {
        var mapper = GraviteeJacksonMapper.getInstance();
        var writer = new GenericEventWriterDomainService(new EventCrudInMemory(), new EventLatestCrudInMemory(), mapper);
        try (var parent = new AnnotationConfigApplicationContext(); var gamma = new AnnotationConfigApplicationContext()) {
            parent.registerBean(GuardrailRepository.class, () -> mock(GuardrailRepository.class));
            parent.registerBean(GenericEventWriterDomainService.class, () -> writer);
            parent.registerBean(GenericEventPublicationDomainService.class, () -> new GenericEventPublicationDomainService(writer, mapper));
            parent.registerBean(ApiCrudService.class, ApiCrudServiceInMemory::new);
            parent.registerBean(ApiQueryService.class, () -> new ApiQueryServiceInMemory());
            parent.registerBean(JsonSerializer.class, () -> new JacksonJsonSerializer());
            parent.registerBean(JsonDeserializer.class, () -> new JacksonJsonDeserializer());
            parent.registerBean(PermissionDomainService.class, () -> mock(PermissionDomainService.class));
            parent.registerBean(DeployApiInternalFlowDomainService.class, GammaGuardrailsConfigurationTest::deployApiInternalFlow);
            parent.registerBean(EventLatestQueryService.class, EventLatestQueryServiceInMemory::new);
            parent.registerBean(EventQueryService.class, EventQueryServiceInMemory::new);
            parent.registerBean(UserCrudService.class, UserCrudServiceInMemory::new);
            parent.registerBean(PolicyPluginProvider.class, () -> mock(PolicyPluginProvider.class));
            parent.registerBean(PolicyValidationDomainService.class, () -> mock(PolicyValidationDomainService.class));
            parent.registerBean(ValidateResourceDomainService.class, () -> mock(ValidateResourceDomainService.class));
            parent.registerBean(ResourcePluginProvider.class, () -> mock(ResourcePluginProvider.class));
            parent.registerBean(EnvironmentCrudService.class, () -> mock(EnvironmentCrudService.class));
            parent.registerBean(AuditDomainService.class, () -> mock(AuditDomainService.class));
            parent.registerBean(SecretReferencePermissionDomainService.class, () -> mock(SecretReferencePermissionDomainService.class));
            // the bean of the rest-api context the core asks the SPI through, which the module never knows about
            parent.register(CompositeApiDeploymentValidator.class);
            parent.refresh();
            gamma.setParent(parent);
            gamma.register(GammaGuardrailsConfiguration.class);
            gamma.refresh();

            // what the plugin handler does once the module context is up
            parent.getBeanFactory().registerSingleton("gammaGuardrailsDeploymentValidator", gamma.getBean(ApiDeploymentValidator.class));

            assertSoftly(softly -> {
                softly
                    .assertThat(parent.getBeanFactory().getBeansOfType(ApiDeploymentValidator.class).values())
                    .as("validators the parent context hands over after its refresh")
                    .singleElement()
                    .isInstanceOf(GuardrailsDeploymentValidatorAdapter.class);
                softly
                    .assertThat(parent.getBean(ApiDeploymentGate.class))
                    .as("the gate every injection point of the rest-api receives")
                    .isInstanceOf(CompositeApiDeploymentValidator.class);
                softly
                    .assertThatThrownBy(() ->
                        parent.getBean(ApiDeploymentGate.class).validateInternalFlowDeployable(anApiRunningAWithdrawnGuardrail())
                    )
                    .as("the refusal of the module's validator, raised through the composite")
                    .isInstanceOf(ApiNotDeployableException.class)
                    .hasMessageContaining(GUARDRAIL_ID);
            });
        }
    }

    /** An API whose internal flow runs a Guardrail the environment does not hold, which the module's bean refuses. */
    private static Api anApiRunningAWithdrawnGuardrail() {
        var api = ApiFixtures.aLLMProxyApiV4().toBuilder().environmentId(ENVIRONMENT_ID).build();
        api
            .getApiDefinitionHttpV4()
            .setInternalFlow(Flow.builder().request(List.of(GuardrailSteps.of(GUARDRAIL_ID, "Credential leakage"))).build());
        return api;
    }

    /** The rest-api context owns it; here it only has to be resolvable. */
    private static DeployApiInternalFlowDomainService deployApiInternalFlow() {
        return new DeployApiInternalFlowDomainService(
            mock(ApiDefinitionCrudService.class),
            new EventCrudInMemory(),
            new EventLatestCrudInMemory(),
            new EventLatestQueryServiceInMemory()
        );
    }
}
