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
package io.gravitee.apim.infra.query_service.cockpit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.cockpit.model.AccessPointTemplate;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class CockpitAccessServiceImplTest {

    @Mock
    private InstallationTypeDomainService installationTypeDomainService;

    private MockEnvironment environment;
    private CockpitAccessServiceImpl cut;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
        cut = new CockpitAccessServiceImpl(installationTypeDomainService, environment);
    }

    @Test
    void should_build_access_point_template_when_installation_is_multi_tenant() {
        when(installationTypeDomainService.isMultiTenant()).thenReturn(true);
        environment.withProperty(
            "installation.multi-tenant.accessPoints.organization.console.host",
            "{organization}.{account}.apim-console.gravitee.io"
        );
        environment.withProperty(
            "installation.multi-tenant.accessPoints.organization.console-api.host",
            "{organization}.{account}.apim-console-api.gravitee.io"
        );
        environment.withProperty(
            "installation.multi-tenant.accessPoints.environment.portal.host",
            "{environment}.{organization}.{account}.apim-portal.gravitee.io"
        );
        environment.withProperty(
            "installation.multi-tenant.accessPoints.environment.portal-api.host",
            "{environment}.{organization}.{account}.apim-portal-api.gravitee.io"
        );
        environment.withProperty(
            "installation.multi-tenant.accessPoints.environment.gateway.host",
            "{environment}.{organization}.{account}.apim-gateway.gravitee.io"
        );

        cut.afterPropertiesSet();
        Map<AccessPointTemplate.Type, List<AccessPointTemplate>> accessPointsTemplate = cut.getAccessPointsTemplate();

        List<AccessPointTemplate> orgTemplates = accessPointsTemplate.get(AccessPointTemplate.Type.ORGANIZATION);
        assertThat(orgTemplates).hasSize(2);
        List<AccessPointTemplate> envTemplates = accessPointsTemplate.get(AccessPointTemplate.Type.ENVIRONMENT);
        assertThat(envTemplates).hasSize(3);
    }

    @Test
    void should_throw_exception_when_no_access_point_template_built_when_installation_is_multi_tenant() {
        when(installationTypeDomainService.isMultiTenant()).thenReturn(true);
        assertThrows(CockpitAccessServiceImpl.InvalidAccessPointException.class, () -> cut.afterPropertiesSet());
    }

    @Test
    void should_thrown_exception_when_building_access_point_template_when_installation_is_multi_tenant_if_domain_is_invalid() {
        when(installationTypeDomainService.isMultiTenant()).thenReturn(true);
        environment.withProperty("installation.multi-tenant.accessPoints.organization.console.host", "-INVALID-gravitee.io-");

        assertThrows(CockpitAccessServiceImpl.InvalidAccessPointException.class, () -> cut.afterPropertiesSet());
    }

    @Test
    void should_do_nothing_when_installation_is_not_multi_tenant() {
        when(installationTypeDomainService.isMultiTenant()).thenReturn(false);
        cut.afterPropertiesSet();
        Map<AccessPointTemplate.Type, List<AccessPointTemplate>> accessPointsTemplate = cut.getAccessPointsTemplate();
        assertThat(accessPointsTemplate).isEmpty();
    }
}
