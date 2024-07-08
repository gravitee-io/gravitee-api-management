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
package io.gravitee.apim.infra.query_service.installation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import java.lang.reflect.Field;
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
class InstallationAccessQueryServiceImplTest {

    public static final String ORGANIZATION_ID = "orga#id";
    public static final String DEFAULT_ORGANIZATION_ID = "DEFAULT";
    public static final String DEFAULT_ENVIRONMENT_ID = "DEFAULT";

    @Mock
    private InstallationTypeDomainService installationTypeDomainService;

    @Mock
    private AccessPointQueryService accessPointQueryService;

    private MockEnvironment environment;
    private InstallationAccessQueryServiceImpl cut;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
        cut = new InstallationAccessQueryServiceImpl(environment, installationTypeDomainService, accessPointQueryService);
        setValue("cockpitEnabled", false);
        setValue("apiURL", null);
        setValue("managementProxyPath", "/management");
        setValue("portalProxyPath", "/portal");
    }

    @Test
    void should_throw_validation_error_when_api_url_is_malformed() {
        when(installationTypeDomainService.isMultiTenant()).thenReturn(false);
        setValue("apiURL", "wrong");

        assertThatThrownBy(() -> cut.afterPropertiesSet())
            .isInstanceOf(InstallationAccessQueryServiceImpl.InvalidInstallationUrlException.class);
    }

    @Test
    void should_set_default_url_for_DEFAULT_organization_when_installation_is_not_multi_tenant_and_cockpit_enabled_but_without_configuration() {
        when(installationTypeDomainService.isMultiTenant()).thenReturn(false);
        setValue("cockpitEnabled", true);

        cut.afterPropertiesSet();
        assertThat(cut.getConsoleAPIUrl(DEFAULT_ORGANIZATION_ID)).isNull();
        assertThat(cut.getPortalAPIUrl(DEFAULT_ORGANIZATION_ID)).isNull();
        assertThat(cut.getConsoleUrl(DEFAULT_ORGANIZATION_ID)).isEqualTo("http://localhost:3000");
        assertThat(cut.getPortalUrl(DEFAULT_ORGANIZATION_ID)).isEqualTo("http://localhost:4100");
    }

    @Test
    void should_build_url_for_DEFAULT_organization_when_installation_is_not_multi_tenant() {
        when(installationTypeDomainService.isMultiTenant()).thenReturn(false);
        environment.withProperty("installation.standalone.console.url", "http://console.url");
        environment.withProperty("installation.standalone.portal.url", "http://portal.url");

        cut.afterPropertiesSet();
        assertThat(cut.getConsoleAPIUrl(DEFAULT_ORGANIZATION_ID)).isNull();
        assertThat(cut.getPortalAPIUrl(DEFAULT_ORGANIZATION_ID)).isNull();
        assertThat(cut.getConsoleUrl(DEFAULT_ORGANIZATION_ID)).isEqualTo("http://console.url");
        assertThat(cut.getConsoleUrls(DEFAULT_ORGANIZATION_ID)).containsOnly("http://console.url");
        assertThat(cut.getPortalUrl(DEFAULT_ORGANIZATION_ID)).isEqualTo("http://portal.url");
        assertThat(cut.getPortalUrls(DEFAULT_ORGANIZATION_ID)).containsOnly("http://portal.url");
    }

    @Test
    void should_use_console_api_url_when_installation_is_not_multi_tenant_and_console_url_is_defined() {
        when(installationTypeDomainService.isMultiTenant()).thenReturn(false);
        setValue("consoleApiUrl", "http://console.api.url");
        setValue("portalApiUrl", "http://console.api.url");

        cut.afterPropertiesSet();
        assertThat(cut.getConsoleAPIUrl(DEFAULT_ORGANIZATION_ID)).isEqualTo("http://console.api.url/management");
        assertThat(cut.getPortalAPIUrl(DEFAULT_ORGANIZATION_ID)).isEqualTo("http://console.api.url/portal");
    }

    @Test
    void should_use_api_url_when_installation_is_not_multi_tenant_and_console_and_portal_urls_are_not_defined() {
        when(installationTypeDomainService.isMultiTenant()).thenReturn(false);
        setValue("apiURL", "http://api.url");

        cut.afterPropertiesSet();
        assertThat(cut.getConsoleAPIUrl(DEFAULT_ORGANIZATION_ID)).isEqualTo("http://api.url/management");
        assertThat(cut.getPortalAPIUrl(DEFAULT_ORGANIZATION_ID)).isEqualTo("http://api.url/portal");
    }

    @Test
    void should_urls_when_installation_is_not_multi_tenant() {
        when(installationTypeDomainService.isMultiTenant()).thenReturn(false);
        environment.withProperty("installation.standalone.console.urls[0].orgId", "orgId");
        environment.withProperty("installation.standalone.console.urls[0].url", "http://orgId.console.url");
        environment.withProperty("installation.standalone.console.urls[1].orgId", "orgId1");
        environment.withProperty("installation.standalone.console.urls[1].url", "http://orgId1.console.url");
        environment.withProperty("installation.standalone.portal.urls[0].envId", "envId");
        environment.withProperty("installation.standalone.portal.urls[0].url", "http://envId.portal.url");
        environment.withProperty("installation.standalone.portal.urls[1].envId", "envId1");
        environment.withProperty("installation.standalone.portal.urls[1].url", "http://envId1.portal.url");

        cut.afterPropertiesSet();
        assertThat(cut.getConsoleAPIUrl(DEFAULT_ORGANIZATION_ID)).isNull();
        assertThat(cut.getPortalAPIUrl(DEFAULT_ORGANIZATION_ID)).isNull();
        assertThat(cut.getConsoleUrl("orgId")).isEqualTo("http://orgId.console.url");
        assertThat(cut.getConsoleUrls("orgId")).containsOnly("http://orgId.console.url");
        assertThat(cut.getConsoleUrl("orgId1")).isEqualTo("http://orgId1.console.url");
        assertThat(cut.getConsoleUrls("orgId1")).containsOnly("http://orgId1.console.url");
        assertThat(cut.getPortalUrl("envId")).isEqualTo("http://envId.portal.url");
        assertThat(cut.getPortalUrls("envId")).containsOnly("http://envId.portal.url");
        assertThat(cut.getPortalUrl("envId1")).isEqualTo("http://envId1.portal.url");
        assertThat(cut.getPortalUrls("envId1")).containsOnly("http://envId1.portal.url");
    }

    @Test
    void should_do_nothing_when_installation_is_multi_tenant() {
        when(installationTypeDomainService.isMultiTenant()).thenReturn(true);
        cut.afterPropertiesSet();
        String consoleAPIUrl = cut.getConsoleAPIUrl(ORGANIZATION_ID);
        assertThat(consoleAPIUrl).isNull(); // Because no urls added to the local map
    }

    @Test
    void should_use_legacy_api_urls_when_installation_is_not_multi_tenant_and_no_installation_configuration() {
        when(installationTypeDomainService.isMultiTenant()).thenReturn(false);
        environment.withProperty("console.api.url", "http://api.url/path/management");
        environment.withProperty("console.ui.url", "http://console.url");
        environment.withProperty("console.portal.url", "http://portal.url");

        cut.afterPropertiesSet();
        assertThat(cut.getConsoleApiPath()).isEqualTo("/path/management");
        assertThat(cut.getPortalApiPath()).isEqualTo("/portal");
        assertThat(cut.getConsoleAPIUrl(DEFAULT_ORGANIZATION_ID)).isEqualTo("http://api.url/path/management");
        assertThat(cut.getPortalAPIUrl(DEFAULT_ENVIRONMENT_ID)).isEqualTo("http://api.url/portal");
        assertThat(cut.getConsoleUrl(DEFAULT_ORGANIZATION_ID)).isEqualTo("http://console.url");
        assertThat(cut.getPortalUrl(DEFAULT_ENVIRONMENT_ID)).isEqualTo("http://portal.url");
    }

    private void setValue(final String field, final Object value) {
        try {
            Field heightField = cut.getClass().getDeclaredField(field);
            heightField.setAccessible(true);
            heightField.set(cut, value);
        } catch (Exception e) {
            fail(String.format("Unable to set value '%s' on field '%s'", value, field), e);
        }
    }
}
