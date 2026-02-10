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
package io.gravitee.gateway.reactive.handlers.api.v4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.reactive.policy.PolicyFactoryManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyClassLoaderFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ApiProductPlanPolicyManagerTest {

    private static final String API_ID = "api-1";
    private static final String ENV_ID = "env-1";

    @Mock
    private DefaultClassLoader classLoader;

    @Mock
    private PolicyFactoryManager policyFactoryManager;

    @Mock
    private PolicyConfigurationFactory policyConfigurationFactory;

    @Mock
    private ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager;

    @Mock
    private PolicyClassLoaderFactory policyClassLoaderFactory;

    @Mock
    private ComponentProvider componentProvider;

    @Mock
    private ApiProductRegistry apiProductRegistry;

    @BeforeEach
    void setUp() {
        lenient().when(apiProductRegistry.getProductPlanEntriesForApi(API_ID, ENV_ID)).thenReturn(List.of());
    }

    @Test
    void should_return_empty_dependencies_when_registry_is_null() {
        ApiProductPlanPolicyManager manager = new ApiProductPlanPolicyManager(
            classLoader,
            policyFactoryManager,
            policyConfigurationFactory,
            policyPluginManager,
            policyClassLoaderFactory,
            componentProvider,
            API_ID,
            ENV_ID,
            null
        );

        Set<Policy> dependencies = invokeDependencies(manager);

        assertThat(dependencies).isEmpty();
    }

    @Test
    void should_return_empty_dependencies_when_no_product_plans() {
        when(apiProductRegistry.getProductPlanEntriesForApi(API_ID, ENV_ID)).thenReturn(List.of());

        ApiProductPlanPolicyManager manager = new ApiProductPlanPolicyManager(
            classLoader,
            policyFactoryManager,
            policyConfigurationFactory,
            policyPluginManager,
            policyClassLoaderFactory,
            componentProvider,
            API_ID,
            ENV_ID,
            apiProductRegistry
        );

        Set<Policy> dependencies = invokeDependencies(manager);

        assertThat(dependencies).isEmpty();
    }

    @Test
    void should_return_policies_from_standard_mode_plans() {
        Plan plan1 = new Plan();
        plan1.setMode(PlanMode.STANDARD);
        plan1.setSecurity(new PlanSecurity("API_KEY", "{}"));

        when(apiProductRegistry.getProductPlanEntriesForApi(API_ID, ENV_ID)).thenReturn(
            List.of(new ApiProductRegistry.ApiProductPlanEntry("product-1", plan1))
        );

        ApiProductPlanPolicyManager manager = new ApiProductPlanPolicyManager(
            classLoader,
            policyFactoryManager,
            policyConfigurationFactory,
            policyPluginManager,
            policyClassLoaderFactory,
            componentProvider,
            API_ID,
            ENV_ID,
            apiProductRegistry
        );

        Set<Policy> dependencies = invokeDependencies(manager);

        assertThat(dependencies).hasSize(1);
        assertThat(dependencies).extracting(Policy::getName).containsExactly("api-key");
    }

    @Test
    void should_deduplicate_policies_by_name() {
        Plan plan1 = new Plan();
        plan1.setMode(PlanMode.STANDARD);
        plan1.setSecurity(new PlanSecurity("API_KEY", "{}"));

        Plan plan2 = new Plan();
        plan2.setMode(PlanMode.STANDARD);
        plan2.setSecurity(new PlanSecurity("API_KEY", "{\"different\":\"config\"}"));

        when(apiProductRegistry.getProductPlanEntriesForApi(API_ID, ENV_ID)).thenReturn(
            List.of(
                new ApiProductRegistry.ApiProductPlanEntry("product-1", plan1),
                new ApiProductRegistry.ApiProductPlanEntry("product-2", plan2)
            )
        );

        ApiProductPlanPolicyManager manager = new ApiProductPlanPolicyManager(
            classLoader,
            policyFactoryManager,
            policyConfigurationFactory,
            policyPluginManager,
            policyClassLoaderFactory,
            componentProvider,
            API_ID,
            ENV_ID,
            apiProductRegistry
        );

        Set<Policy> dependencies = invokeDependencies(manager);

        assertThat(dependencies).hasSize(1);
        assertThat(dependencies).extracting(Policy::getName).containsExactly("api-key");
    }

    @Test
    void should_convert_security_type_to_policy_name_format() {
        Plan plan = new Plan();
        plan.setMode(PlanMode.STANDARD);
        plan.setSecurity(new PlanSecurity("OAUTH2", "{}"));

        when(apiProductRegistry.getProductPlanEntriesForApi(API_ID, ENV_ID)).thenReturn(
            List.of(new ApiProductRegistry.ApiProductPlanEntry("product-1", plan))
        );

        ApiProductPlanPolicyManager manager = new ApiProductPlanPolicyManager(
            classLoader,
            policyFactoryManager,
            policyConfigurationFactory,
            policyPluginManager,
            policyClassLoaderFactory,
            componentProvider,
            API_ID,
            ENV_ID,
            apiProductRegistry
        );

        Set<Policy> dependencies = invokeDependencies(manager);

        assertThat(dependencies).extracting(Policy::getName).containsExactly("oauth2");
    }

    @SuppressWarnings("unchecked")
    private Set<Policy> invokeDependencies(ApiProductPlanPolicyManager manager) {
        return (Set<Policy>) ReflectionTestUtils.invokeMethod(manager, "dependencies");
    }
}
