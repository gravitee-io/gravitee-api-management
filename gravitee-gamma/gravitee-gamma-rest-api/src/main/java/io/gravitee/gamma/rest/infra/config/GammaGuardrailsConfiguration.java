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

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.DeployApiInternalFlowDomainService;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.deployment.service_provider.ApiDeploymentValidator;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.gamma.rest.core.guardrail.domain_service.ValidateApiGuardrailsDomainService;
import io.gravitee.gamma.rest.core.guardrail.port.repository.GuardrailRepository;
import io.gravitee.gamma.rest.core.guardrail.port.service_provider.ConditionSyntaxPort;
import io.gravitee.gamma.rest.core.guardrail.port.service_provider.GuardedApiPort;
import io.gravitee.gamma.rest.core.guardrail.port.service_provider.GuardedApiReadPermissionPort;
import io.gravitee.gamma.rest.infra.adapter.ConditionSyntaxPortAdapter;
import io.gravitee.gamma.rest.infra.adapter.GammaGuardrailRepositoryAdapter;
import io.gravitee.gamma.rest.infra.adapter.GuardedApiPortAdapter;
import io.gravitee.gamma.rest.infra.adapter.GuardedApiReadPermissionPortAdapter;
import io.gravitee.gamma.rest.infra.adapter.GuardrailsDeploymentValidatorAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Lazy;

/**
 * Spring wiring for the Guardrails domain.
 *
 * <p>{@code @Lazy} on the SPI parameter is load-bearing: the repository plugin handler registers the
 * {@code GuardrailRepository} bean after the rest-api Spring context refresh completes, so eager
 * resolution at {@code @Bean} processing time throws {@code NoSuchBeanDefinitionException}. Mirrors
 * {@code GammaDashboardsConfiguration}.
 *
 * @author GraviteeSource Team
 */
@Configuration
@ComponentScan(
    basePackages = "io.gravitee.gamma.rest.core.guardrail",
    includeFilters = {
        @ComponentScan.Filter(type = FilterType.ANNOTATION, value = DomainService.class),
        @ComponentScan.Filter(type = FilterType.ANNOTATION, value = UseCase.class),
    }
)
public class GammaGuardrailsConfiguration {

    @Bean
    public GuardrailRepository gammaGuardrailRepositoryPort(
        @Lazy io.gravitee.repository.management.api.GuardrailRepository guardrailRepository
    ) {
        return new GammaGuardrailRepositoryAdapter(guardrailRepository);
    }

    @Bean
    public GuardedApiPort gammaGuardedApiPort(
        ApiCrudService apiCrudService,
        ApiQueryService apiQueryService,
        DeployApiInternalFlowDomainService deployApiInternalFlow
    ) {
        return new GuardedApiPortAdapter(apiCrudService, apiQueryService, deployApiInternalFlow);
    }

    /**
     * No {@code @Lazy}: {@link PermissionDomainService} is an ordinary bean of the parent context, unlike the
     * repository SPI above, which a plugin handler registers after the refresh.
     */
    @Bean
    public GuardedApiReadPermissionPort gammaGuardedApiReadPermissionPort(PermissionDomainService permissionDomainService) {
        return new GuardedApiReadPermissionPortAdapter(permissionDomainService);
    }

    @Bean
    public ConditionSyntaxPort gammaConditionSyntaxPort() {
        return new ConditionSyntaxPortAdapter();
    }

    /** Collected by APIM with the other {@link ApiDeploymentValidator}s, so a deployment runs it. */
    @Bean
    public ApiDeploymentValidator gammaGuardrailsDeploymentValidator(ValidateApiGuardrailsDomainService validateApiGuardrails) {
        return new GuardrailsDeploymentValidatorAdapter(validateApiGuardrails);
    }
}
