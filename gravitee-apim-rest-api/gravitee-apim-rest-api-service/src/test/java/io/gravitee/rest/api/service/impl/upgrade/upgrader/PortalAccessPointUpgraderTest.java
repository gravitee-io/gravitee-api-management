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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import inmemory.InstallationAccessQueryServiceInMemory;
import inmemory.PortalAddressDomainServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import io.gravitee.apim.core.portal.domain_service.DefaultPortalDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalContentOwnershipDomainService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalAccessPointUpgraderTest {

    private static final Environment ENV_A = Environment.builder().id("env-a").organizationId("org-1").name("A").build();
    private static final Environment ENV_B = Environment.builder().id("env-b").organizationId("org-1").name("B").build();
    private static final PortalId PORTAL_A = PortalId.of("22222222-2222-2222-2222-222222222222");
    private static final PortalId PORTAL_A_BIS = PortalId.of("33333333-3333-3333-3333-333333333333");
    private static final PortalId PORTAL_B = PortalId.of("44444444-4444-4444-4444-444444444444");

    @Mock
    EnvironmentRepository environmentRepository;

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private final InstallationAccessQueryServiceInMemory installationAccessQueryService = new InstallationAccessQueryServiceInMemory();
    private PortalAddressDomainServiceInMemory addresses;
    private PortalAccessPointUpgrader upgrader;

    @BeforeEach
    void setUp() {
        portalCrudService.reset();
        installationAccessQueryService.reset();
        installationAccessQueryService
            .withPortalUrl(ENV_A.getId(), "https://a.acme.io")
            .withPortalUrl(ENV_B.getId(), "http://localhost:4100");
        givenUpgraderOver(installationAccessQueryService);
    }

    private void givenUpgraderOver(InstallationAccessQueryServiceInMemory installation) {
        addresses = new PortalAddressDomainServiceInMemory(portalCrudService, installation);
        upgrader = new PortalAccessPointUpgrader(
            environmentRepository,
            new DefaultPortalDomainService(
                portalCrudService,
                addresses,
                new PortalContentOwnershipDomainService(portalCrudService, new PortalNavigationItemsCrudServiceInMemory())
            )
        );
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @SneakyThrows
    private void givenEnvironments(Environment... environments) {
        when(environmentRepository.findAll()).thenReturn(new LinkedHashSet<>(List.of(environments)));
    }

    private static Portal anUnlinkedPortal(PortalId id, Environment environment, boolean defaultPortal) {
        return Portal.of(
            id,
            environment.getId(),
            environment.getOrganizationId(),
            "Portal " + id,
            PortalNavigationStructure.empty(),
            null,
            defaultPortal,
            null
        );
    }

    @Test
    @SneakyThrows
    void should_link_the_portals_of_each_environment_to_one_shared_access_point_at_its_portal_url() {
        givenEnvironments(ENV_A, ENV_B);
        portalCrudService.initWith(
            List.of(
                anUnlinkedPortal(PORTAL_A, ENV_A, true),
                anUnlinkedPortal(PORTAL_A_BIS, ENV_A, false),
                anUnlinkedPortal(PORTAL_B, ENV_B, true)
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(addresses.accessPoints().storage())
            .extracting(
                accessPoint -> accessPoint.getReferenceId(),
                accessPoint -> accessPoint.getHost(),
                accessPoint -> accessPoint.isSecured()
            )
            .containsExactlyInAnyOrder(tuple("env-a", "a.acme.io", true), tuple("env-b", "localhost:4100", false));
        assertThat(portalCrudService.storage())
            .extracting(Portal::getId, portal -> urlOf(portal).toString())
            .containsExactlyInAnyOrder(
                tuple(PORTAL_A, "https://a.acme.io/next"),
                tuple(PORTAL_A_BIS, "https://a.acme.io/next"),
                tuple(PORTAL_B, "http://localhost:4100/next")
            );
    }

    @Test
    @SneakyThrows
    void should_leave_the_portals_of_an_environment_whose_url_cannot_be_resolved_unlinked() {
        installationAccessQueryService.reset();
        installationAccessQueryService.withPortalUrl(ENV_B.getId(), "http://localhost:4100");
        givenEnvironments(ENV_A, ENV_B);
        portalCrudService.initWith(List.of(anUnlinkedPortal(PORTAL_A, ENV_A, true), anUnlinkedPortal(PORTAL_B, ENV_B, true)));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(portalCrudService.storage())
            .extracting(Portal::getId, portal -> portal.getAccessPointId() != null)
            .containsExactlyInAnyOrder(tuple(PORTAL_A, false), tuple(PORTAL_B, true));
    }

    @Test
    @SneakyThrows
    void should_resolve_each_environment_url_when_the_portal_url_parameter_is_memoized_per_thread() {
        var memoizingInstallationAccess = new InstallationAccessQueryServiceInMemory() {
            // ParameterService.find memoizes per thread by parameter key only, whatever the environment
            @Override
            public String getPortalUrl(String environmentId) {
                return GraviteeContext.getCurrentParameters().computeIfAbsent(Key.PORTAL_URL, key -> super.getPortalUrl(environmentId));
            }
        };
        memoizingInstallationAccess.withPortalUrl(ENV_A.getId(), "https://a.acme.io").withPortalUrl(ENV_B.getId(), "https://b.acme.io");
        givenUpgraderOver(memoizingInstallationAccess);
        givenEnvironments(ENV_A, ENV_B);
        portalCrudService.initWith(List.of(anUnlinkedPortal(PORTAL_A, ENV_A, true), anUnlinkedPortal(PORTAL_B, ENV_B, true)));
        GraviteeContext.getCurrentParameters().put(Key.PORTAL_URL, "https://stale.acme.io");

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(addresses.accessPoints().storage())
            .extracting(accessPoint -> accessPoint.getReferenceId(), accessPoint -> accessPoint.getHost())
            .containsExactlyInAnyOrder(tuple("env-a", "a.acme.io"), tuple("env-b", "b.acme.io"));
    }

    @Test
    @SneakyThrows
    void should_change_nothing_on_rerun() {
        givenEnvironments(ENV_A);
        portalCrudService.initWith(List.of(anUnlinkedPortal(PORTAL_A, ENV_A, true)));
        upgrader.upgrade();
        var linkedTo = portalCrudService.storage().getFirst().getAccessPointId();

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(portalCrudService.storage()).singleElement().extracting(Portal::getAccessPointId).isEqualTo(linkedTo);
        assertThat(addresses.accessPoints().storage()).hasSize(1);
    }

    @Test
    void should_run_after_the_default_portal_upgrader() {
        assertThat(upgrader.getOrder()).isGreaterThan(UpgraderOrder.DEFAULT_PORTAL_UPGRADER);
    }

    private URL urlOf(Portal portal) {
        return addresses.resolveUrls(portal.getEnvironmentId(), List.of(portal)).get(portal.getId());
    }
}
