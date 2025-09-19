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
package io.gravitee.apim.core.plugin.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import inmemory.PolicyPluginQueryServiceInMemory;
import io.gravitee.apim.core.plugin.domain_service.PluginFilterByLicenseDomainService;
import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.node.api.license.LicenseManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetPolicyPluginsUseCaseTest {

    private final PolicyPluginQueryServiceInMemory policyPluginQueryServiceInMemory = new PolicyPluginQueryServiceInMemory();
    private LicenseManager licenseManager;

    private GetPolicyPluginsUseCase getPolicyPluginsUseCase;

    @BeforeEach
    void setup() {
        licenseManager = mock(LicenseManager.class);
        getPolicyPluginsUseCase = new GetPolicyPluginsUseCase(
            policyPluginQueryServiceInMemory,
            new PluginFilterByLicenseDomainService(licenseManager)
        );
    }

    @Test
    void getPoliciesByOrganization() {
        policyPluginQueryServiceInMemory.initWith(
            List.of(
                PolicyPlugin.builder().id("policy-1").name("Policy 1").feature("feature-policy-1").deployed(true).build(),
                PolicyPlugin.builder().id("policy-2").name("Policy 2").feature("feature-policy-2").deployed(true).build(),
                PolicyPlugin.builder().id("policy-3").name("Policy 3").feature("feature-policy-3").deployed(false).build()
            )
        );
        var license = mock(io.gravitee.node.api.license.License.class);
        when(licenseManager.getOrganizationLicenseOrPlatform("org-id")).thenReturn(license);

        when(license.isFeatureEnabled("feature-policy-1")).thenReturn(false);
        when(license.isFeatureEnabled("feature-policy-2")).thenReturn(true);
        when(license.isFeatureEnabled("feature-policy-3")).thenReturn(true);

        assertThat(
            getPolicyPluginsUseCase.getPoliciesByOrganization(new GetPolicyPluginsUseCase.Input("org-id")).plugins()
        ).containsExactly(
            PolicyPlugin.builder().id("policy-1").name("Policy 1").feature("feature-policy-1").deployed(false).build(),
            PolicyPlugin.builder().id("policy-2").name("Policy 2").feature("feature-policy-2").deployed(true).build(),
            PolicyPlugin.builder().id("policy-3").name("Policy 3").feature("feature-policy-3").deployed(false).build()
        );
    }
}
