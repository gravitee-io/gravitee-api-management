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
package io.gravitee.gamma.rest.spring;

import static org.mockito.Mockito.mock;

import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.plugin.gamma.internal.GammaModuleService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PermissionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResourceContextConfiguration {

    @Bean
    public ConfigurableInstallationAccessQueryService installationAccessQueryService() {
        return new ConfigurableInstallationAccessQueryService();
    }

    @Bean
    public PermissionService permissionService() {
        return mock(PermissionService.class);
    }

    @Bean
    public LicenseManager licenseManager() {
        return mock(LicenseManager.class);
    }

    @Bean
    public ParameterService parameterService() {
        return mock(ParameterService.class);
    }

    @Bean
    public GammaModuleService gammaModuleService() {
        return mock(GammaModuleService.class);
    }

    @Bean
    public EnvironmentService environmentService() {
        return mock(EnvironmentService.class);
    }
}
