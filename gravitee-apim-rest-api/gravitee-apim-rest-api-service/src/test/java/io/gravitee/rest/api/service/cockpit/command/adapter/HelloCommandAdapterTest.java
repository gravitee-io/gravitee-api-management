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
package io.gravitee.rest.api.service.cockpit.command.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.cockpit.model.AccessPointTemplate;
import io.gravitee.apim.core.cockpit.query_service.CockpitAccessService;
import io.gravitee.apim.core.installation.domain_service.InstallationTypeDomainService;
import io.gravitee.apim.core.installation.model.InstallationType;
import io.gravitee.cockpit.api.command.model.accesspoint.AccessPoint;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.hello.HelloCommand;
import io.gravitee.cockpit.api.command.v1.installation.AdditionalInfoConstants;
import io.gravitee.exchange.api.command.hello.HelloCommandPayload;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.spring.InstallationConfiguration;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class HelloCommandAdapterTest {

    private static final String HOSTNAME = "test.gravitee.io";
    private static final String CUSTOM_VALUE = "customValue";
    private static final String CUSTOM_KEY = "customKey";
    private static final String INSTALLATION_ID = "installation#1";

    @Mock
    private InstallationService installationService;

    @Mock
    private Node node;

    @Mock
    private InstallationTypeDomainService installationTypeDomainService;

    @Mock
    private CockpitAccessService cockpitAccessService;

    @Mock
    private PluginRegistry pluginRegistry;

    @Mock
    private InstallationConfiguration installationConfiguration;

    private HelloCommandAdapter cut;

    @BeforeEach
    public void beforeEach() {
        cut =
            new HelloCommandAdapter(
                node,
                installationService,
                installationTypeDomainService,
                cockpitAccessService,
                pluginRegistry,
                installationConfiguration
            );
    }

    @Test
    void shouldBuildAuthPath() {
        cut.authPath = "/auth/cockpit?token={token}";
        when(installationConfiguration.getManagementProxyPath()).thenReturn("/management/foobar/");

        cut.afterPropertiesSet();

        assertEquals(cut.buildAuthPath, "/management/foobar/auth/cockpit?token={token}");
    }

    @Test
    void shouldBuildAuthPathWithoutProxyPathSuffix() {
        cut.authPath = "/auth/cockpit?token={token}";
        when(installationConfiguration.getManagementProxyPath()).thenReturn("/management/foobar");

        cut.afterPropertiesSet();

        assertEquals(cut.buildAuthPath, "/management/foobar/auth/cockpit?token={token}");
    }

    @Test
    void shouldBuildAuthPathWithoutAuthPathPrefix() {
        cut.authPath = "auth/cockpit?token={token}";
        when(installationConfiguration.getManagementProxyPath()).thenReturn("/management/foobar/");
        cut.afterPropertiesSet();
        assertEquals(cut.buildAuthPath, "/management/foobar/auth/cockpit?token={token}");
    }

    @Test
    void shouldBuildAuthPathWithoutPrefix() {
        cut.authPath = "auth/cockpit?token={token}";
        when(installationConfiguration.getManagementProxyPath()).thenReturn("/management/foobar");
        cut.afterPropertiesSet();
        assertEquals(cut.buildAuthPath, "/management/foobar/auth/cockpit?token={token}");
    }

    @Test
    void produceType() {
        Assertions.assertEquals(CockpitCommandType.HELLO.name(), cut.supportType());
    }

    @Test
    void adapt() throws InterruptedException {
        final InstallationEntity installationEntity = new InstallationEntity();
        installationEntity.setId(INSTALLATION_ID);
        installationEntity.getAdditionalInformation().put(CUSTOM_KEY, CUSTOM_VALUE);

        when(node.hostname()).thenReturn(HOSTNAME);
        when(installationService.getOrInitialize()).thenReturn(installationEntity);
        when(installationTypeDomainService.get()).thenReturn(InstallationType.STANDALONE);

        final TestObserver<HelloCommand> obs = cut
            .adapt(null, new io.gravitee.exchange.api.command.hello.HelloCommand(new HelloCommandPayload()))
            .test();

        obs.await();
        obs.assertValue(helloCommand -> {
            assertEquals(CUSTOM_VALUE, helloCommand.getPayload().getAdditionalInformation().get(CUSTOM_KEY));
            assertTrue(helloCommand.getPayload().getAdditionalInformation().containsKey(AdditionalInfoConstants.AUTH_PATH));
            assertTrue(helloCommand.getPayload().getAdditionalInformation().containsKey(AdditionalInfoConstants.AUTH_BASE_URL));
            assertEquals(InstallationType.STANDALONE.getLabel(), helloCommand.getPayload().getInstallationType());

            assertEquals(HOSTNAME, helloCommand.getPayload().getNode().hostname());
            assertEquals(GraviteeContext.getDefaultOrganization(), helloCommand.getPayload().getDefaultOrganizationId());
            assertEquals(GraviteeContext.getDefaultEnvironment(), helloCommand.getPayload().getDefaultEnvironmentId());
            assertEquals(INSTALLATION_ID, helloCommand.getPayload().getNode().installationId());
            assertEquals(HOSTNAME, helloCommand.getPayload().getNode().hostname());

            return true;
        });
    }

    @Test
    void adaptMultiTenant() throws InterruptedException {
        final InstallationEntity installationEntity = new InstallationEntity();
        installationEntity.setId(INSTALLATION_ID);
        installationEntity.getAdditionalInformation().put(CUSTOM_KEY, CUSTOM_VALUE);

        when(node.hostname()).thenReturn(HOSTNAME);
        when(installationService.getOrInitialize()).thenReturn(installationEntity);
        when(installationTypeDomainService.get()).thenReturn(InstallationType.MULTI_TENANT);
        when(cockpitAccessService.getAccessPointsTemplate())
            .thenReturn(
                Map.of(
                    AccessPointTemplate.Type.ENVIRONMENT,
                    List.of(
                        AccessPointTemplate.builder().host("localhost").secured(false).target(AccessPointTemplate.Target.CONSOLE).build()
                    )
                )
            );
        final TestObserver<HelloCommand> obs = cut
            .adapt(null, new io.gravitee.exchange.api.command.hello.HelloCommand(new HelloCommandPayload()))
            .test();

        obs.await();
        obs.assertValue(helloCommand -> {
            assertEquals(CUSTOM_VALUE, helloCommand.getPayload().getAdditionalInformation().get(CUSTOM_KEY));
            assertTrue(helloCommand.getPayload().getAdditionalInformation().containsKey(AdditionalInfoConstants.AUTH_PATH));
            assertFalse(helloCommand.getPayload().getAdditionalInformation().containsKey(AdditionalInfoConstants.AUTH_BASE_URL));
            assertEquals(InstallationType.MULTI_TENANT.getLabel(), helloCommand.getPayload().getInstallationType());

            assertEquals(HOSTNAME, helloCommand.getPayload().getNode().hostname());
            assertEquals(GraviteeContext.getDefaultOrganization(), helloCommand.getPayload().getDefaultOrganizationId());
            assertEquals(GraviteeContext.getDefaultEnvironment(), helloCommand.getPayload().getDefaultEnvironmentId());
            assertEquals(INSTALLATION_ID, helloCommand.getPayload().getNode().installationId());
            assertEquals(HOSTNAME, helloCommand.getPayload().getNode().hostname());

            assertEquals(helloCommand.getPayload().getAccessPointsTemplate().size(), 1);
            AccessPoint accessPoint = helloCommand.getPayload().getAccessPointsTemplate().get(AccessPoint.Type.ENVIRONMENT).get(0);
            assertEquals(accessPoint.getHost(), "localhost");
            assertEquals(accessPoint.isSecured(), false);
            assertEquals(accessPoint.getTarget(), AccessPoint.Target.CONSOLE);
            return true;
        });
    }

    @Test
    void adaptWithException() {
        when(installationService.getOrInitialize()).thenThrow(new TechnicalManagementException());
        cut
            .adapt(null, new io.gravitee.exchange.api.command.hello.HelloCommand(new HelloCommandPayload()))
            .test()
            .assertError(TechnicalManagementException.class);
    }
}
