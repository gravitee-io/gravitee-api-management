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
package io.gravitee.apim.core.api.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.api.domain_service.StopApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.dynamicproperty.DynamicPropertyService;
import io.gravitee.node.api.license.ForbiddenFeatureException;
import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.LicenseManager;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SyncApisAfterLicenseUpdateUseCaseTest {

    ApiQueryServiceInMemory apiQueryServiceInMemory;
    LicenseManager licenseManager;
    SyncApisAfterLicenseUpdateUseCase useCase;
    StopApiDomainService stopApiDomainService;

    @BeforeEach
    void setup() {
        licenseManager = mock(LicenseManager.class);
        apiQueryServiceInMemory = new ApiQueryServiceInMemory();
        stopApiDomainService =
            new StopApiDomainService() {
                @Override
                public Api stop(Api apiToStop, AuditInfo auditInfo) {
                    return apiToStop;
                }
            };
        useCase = new SyncApisAfterLicenseUpdateUseCase(apiQueryServiceInMemory, licenseManager, stopApiDomainService);
    }

    @Test
    @SneakyThrows
    void should_stop_all_apis_not_compatible_with_license_and_return_list() {
        io.gravitee.definition.model.Api api1Definition = new io.gravitee.definition.model.Api();
        io.gravitee.definition.model.Api api2Definition = new io.gravitee.definition.model.Api();
        Services services = new Services();
        services.setDynamicPropertyService(new DynamicPropertyService());
        api2Definition.setServices(services);
        io.gravitee.definition.model.Api api3Definition = new io.gravitee.definition.model.Api();
        api3Definition.setResources(List.of(Resource.builder().build()));
        var api1 = Api.builder().apiDefinition(api1Definition).lifecycleState(Api.LifecycleState.STARTED).build();
        var api2 = Api.builder().apiDefinition(api2Definition).lifecycleState(Api.LifecycleState.STARTED).build();
        var api3 = Api.builder().apiDefinition(api3Definition).lifecycleState(Api.LifecycleState.STARTED).build();
        var api4 = Api.builder().apiDefinition(api3Definition).lifecycleState(Api.LifecycleState.STOPPED).build();
        givenApis(List.of(api1, api2, api3, api4));
        doNothing()
            .when(licenseManager)
            .validatePluginFeatures(
                "org-id",
                api1Definition
                    .getPlugins()
                    .stream()
                    .map(p -> new io.gravitee.node.api.license.LicenseManager.Plugin(p.type(), p.id()))
                    .collect(Collectors.toSet())
            );
        doThrow(InvalidLicenseException.class)
            .when(licenseManager)
            .validatePluginFeatures(
                "org-id",
                api2Definition
                    .getPlugins()
                    .stream()
                    .map(p -> new io.gravitee.node.api.license.LicenseManager.Plugin(p.type(), p.id()))
                    .collect(Collectors.toSet())
            );
        doThrow(ForbiddenFeatureException.class)
            .when(licenseManager)
            .validatePluginFeatures(
                "org-id",
                api3Definition
                    .getPlugins()
                    .stream()
                    .map(p -> new io.gravitee.node.api.license.LicenseManager.Plugin(p.type(), p.id()))
                    .collect(Collectors.toSet())
            );
        var res = useCase.execute(new SyncApisAfterLicenseUpdateUseCase.Input("org-id"));
        assertThat(res.stoppedApis()).containsOnly(api2, api3);
    }

    private void givenApis(List<Api> apis) {
        apiQueryServiceInMemory.initWith(apis);
    }
}
