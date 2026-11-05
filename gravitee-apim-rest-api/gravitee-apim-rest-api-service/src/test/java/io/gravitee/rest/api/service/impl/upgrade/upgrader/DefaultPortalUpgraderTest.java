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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import inmemory.InstallationAccessQueryServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.portal.domain_service.DefaultPortalDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalUrlDomainService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.common.GraviteeContext;
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
class DefaultPortalUpgraderTest {

    private static final Environment ENV_A = Environment.builder().id("env-a").organizationId("org-1").name("A").build();
    private static final Environment ENV_B = Environment.builder().id("env-b").organizationId("org-1").name("B").build();
    private static final Environment ENV_C = Environment.builder().id("env-c").organizationId("org-2").name("C").build();
    private static final PortalId AUTOMATION_PORTAL_ID = PortalId.of("22222222-2222-2222-2222-222222222222");
    private static final PortalId SECOND_PORTAL_ID = PortalId.of("33333333-3333-3333-3333-333333333333");

    @Mock
    EnvironmentRepository environmentRepository;

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private final InstallationAccessQueryServiceInMemory installationAccessQueryService = new InstallationAccessQueryServiceInMemory();
    private DefaultPortalUpgrader upgrader;

    @BeforeEach
    void setUp() {
        portalCrudService.reset();
        installationAccessQueryService.reset();
        installationAccessQueryService
            .withPortalUrl(ENV_A.getId(), "https://a.acme.io")
            .withPortalUrl(ENV_B.getId(), "https://b.acme.io/")
            .withPortalUrl(ENV_C.getId(), "https://c.acme.io");
        upgrader = new DefaultPortalUpgrader(
            environmentRepository,
            new DefaultPortalDomainService(portalCrudService, new PortalUrlDomainService(installationAccessQueryService))
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

    private static Portal aPortal(PortalId id, Environment environment, String name) {
        return Portal.of(
            id,
            environment.getId(),
            environment.getOrganizationId(),
            name,
            PortalNavigationStructure.empty(),
            null,
            false,
            Portal.URL_PLACEHOLDER
        );
    }

    /** A comparable snapshot of every stored field, since Portal equality is by id only. */
    private List<String> storedState() {
        return portalCrudService
            .storage()
            .stream()
            .map(p ->
                String.join(
                    "|",
                    p.getId().toString(),
                    p.getEnvironmentId(),
                    p.getName(),
                    String.valueOf(p.isDefault()),
                    p.getUrl().toString()
                )
            )
            .toList();
    }

    @Test
    @SneakyThrows
    void should_create_default_portal_for_every_environment() {
        givenEnvironments(ENV_A, ENV_B, ENV_C);

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(portalCrudService.storage())
            .hasSize(3)
            .allSatisfy(portal -> {
                assertThat(portal.isDefault()).isTrue();
                assertThat(portal.getName()).isEqualTo("Developer Portal");
            })
            .extracting(Portal::getEnvironmentId, Portal::getOrganizationId, portal -> portal.getUrl().toString())
            .containsExactlyInAnyOrder(
                tuple("env-a", "org-1", "https://a.acme.io/next"),
                tuple("env-b", "org-1", "https://b.acme.io/next"),
                tuple("env-c", "org-2", "https://c.acme.io/next")
            );
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
        upgrader = new DefaultPortalUpgrader(
            environmentRepository,
            new DefaultPortalDomainService(portalCrudService, new PortalUrlDomainService(memoizingInstallationAccess))
        );
        givenEnvironments(ENV_A, ENV_B);

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(portalCrudService.storage())
            .extracting(Portal::getEnvironmentId, portal -> portal.getUrl().toString())
            .containsExactlyInAnyOrder(tuple("env-a", "https://a.acme.io/next"), tuple("env-b", "https://b.acme.io/next"));
    }

    @Test
    @SneakyThrows
    void should_leave_environment_with_single_automation_portal_without_default() {
        givenEnvironments(ENV_A);
        portalCrudService.initWith(List.of(aPortal(AUTOMATION_PORTAL_ID, ENV_A, "Automation Portal")));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(portalCrudService.storage())
            .singleElement()
            .satisfies(portal -> {
                assertThat(portal.getId()).isEqualTo(AUTOMATION_PORTAL_ID);
                assertThat(portal.isDefault()).isFalse();
            });
    }

    @Test
    @SneakyThrows
    void should_replace_placeholder_url_on_every_portal() {
        givenEnvironments(ENV_A, ENV_B);
        portalCrudService.initWith(
            List.of(
                aPortal(AUTOMATION_PORTAL_ID, ENV_A, "Automation Portal"),
                Portal.of(
                    SECOND_PORTAL_ID,
                    ENV_B.getId(),
                    "org-1",
                    "Developer Portal",
                    PortalNavigationStructure.empty(),
                    null,
                    true,
                    Portal.URL_PLACEHOLDER
                )
            )
        );

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(portalCrudService.storage())
            .extracting(portal -> portal.getUrl().toString())
            .containsExactlyInAnyOrder("https://a.acme.io/next", "https://b.acme.io/next");
    }

    @Test
    @SneakyThrows
    void should_keep_placeholder_when_url_cannot_be_resolved() {
        installationAccessQueryService.reset();
        givenEnvironments(ENV_A);

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(portalCrudService.storage())
            .singleElement()
            .satisfies(portal -> {
                assertThat(portal.isDefault()).isTrue();
                assertThat(portal.getUrl()).hasToString(Portal.URL_PLACEHOLDER.toString());
            });
    }

    @Test
    @SneakyThrows
    void should_change_nothing_on_rerun() {
        givenEnvironments(ENV_A, ENV_B);
        portalCrudService.initWith(List.of(aPortal(AUTOMATION_PORTAL_ID, ENV_A, "Automation Portal")));
        upgrader.upgrade();
        var afterFirstRun = storedState();

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(storedState()).isEqualTo(afterFirstRun);
    }

    @Test
    @SneakyThrows
    void should_fail_before_changing_anything_when_an_environment_has_several_portals() {
        givenEnvironments(ENV_A, ENV_B, ENV_C);
        portalCrudService.initWith(
            List.of(
                aPortal(AUTOMATION_PORTAL_ID, ENV_B, "Automation Portal"),
                aPortal(SECOND_PORTAL_ID, ENV_B, "Second Portal"),
                aPortal(PortalId.of("44444444-4444-4444-4444-444444444444"), ENV_C, "C one"),
                aPortal(PortalId.of("55555555-5555-5555-5555-555555555555"), ENV_C, "C two")
            )
        );
        var before = storedState();

        assertThatThrownBy(() -> upgrader.upgrade())
            .isInstanceOf(UpgraderException.class)
            .hasMessageContaining("Environment [env-b] has 2 portals")
            .hasMessageContaining("[22222222-2222-2222-2222-222222222222] \"Automation Portal\"")
            .hasMessageContaining("[33333333-3333-3333-3333-333333333333] \"Second Portal\"")
            .hasMessageContaining("Environment [env-c] has 2 portals")
            .hasMessageContaining("remove the other portal records")
            .hasMessageNotContaining("env-a");
        assertThat(storedState()).isEqualTo(before);
        assertThat(portalCrudService.findByEnvironmentId(ENV_A.getId())).isEmpty();
    }

    @Test
    void should_run_after_default_apis_folder_upgrader() {
        assertThat(upgrader.getOrder())
            .isEqualTo(UpgraderOrder.DEFAULT_PORTAL_UPGRADER)
            .isEqualTo(728)
            .isGreaterThan(UpgraderOrder.ENVIRONMENTS_DEFAULT_APIS_FOLDER_UPGRADER);
    }
}
