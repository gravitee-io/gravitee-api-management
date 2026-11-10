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
package io.gravitee.apim.core.portal.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.InstallationAccessQueryServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import java.net.URI;
import java.net.URL;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NavigationPortalDomainServiceTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";
    private static final PortalId DEFAULT_PORTAL_ID = PortalId.of("11111111-1111-1111-1111-111111111111");
    private static final PortalId AUTOMATION_PORTAL_ID = PortalId.of("22222222-2222-2222-2222-222222222222");

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private final InstallationAccessQueryServiceInMemory installationAccessQueryService = new InstallationAccessQueryServiceInMemory();
    private NavigationPortalDomainService cut;

    @BeforeEach
    void setUp() {
        portalCrudService.reset();
        installationAccessQueryService.reset();
        cut = new NavigationPortalDomainService(
            new DefaultPortalDomainService(portalCrudService, new PortalUrlDomainService(installationAccessQueryService)),
            portalCrudService
        );
    }

    @SneakyThrows
    private static URL url(String value) {
        return URI.create(value).toURL();
    }

    private static Portal aPortal(PortalId id, String name, boolean defaultPortal) {
        return Portal.of(
            id,
            ENV_ID,
            ORG_ID,
            name,
            PortalNavigationStructure.empty(),
            null,
            defaultPortal,
            url("https://portal.acme.io/next")
        );
    }

    @Test
    void should_return_the_default_portal() {
        portalCrudService.initWith(List.of(aPortal(DEFAULT_PORTAL_ID, "Developer Portal", true)));

        var portal = cut.resolve(ORG_ID, ENV_ID);

        assertThat(portal.getId()).isEqualTo(DEFAULT_PORTAL_ID);
    }

    @Test
    void should_create_the_default_portal_when_the_environment_has_none() {
        installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");

        var portal = cut.resolve(ORG_ID, ENV_ID);

        assertThat(portalCrudService.storage())
            .singleElement()
            .satisfies(stored -> {
                assertThat(stored.isDefault()).isTrue();
                assertThat(stored.getId()).isEqualTo(portal.getId());
            });
    }

    @Test
    void should_refuse_when_the_only_portal_comes_from_the_automation_api() {
        installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
        portalCrudService.initWith(List.of(aPortal(AUTOMATION_PORTAL_ID, "Automation Portal", false)));

        assertThatThrownBy(() -> cut.resolve(ORG_ID, ENV_ID))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("[" + ENV_ID + "]")
            .hasMessageContaining("its portal is managed through the Automation API");
        assertThat(portalCrudService.storage()).singleElement().extracting(Portal::getId).isEqualTo(AUTOMATION_PORTAL_ID);
    }

    @Test
    void should_refuse_while_the_default_portal_is_postponed() {
        installationAccessQueryService.withPortalUrl(ENV_ID, "not a url");

        assertThatThrownBy(() -> cut.resolve(ORG_ID, ENV_ID))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("[" + ENV_ID + "]")
            .hasMessageContaining("its portal URL is not set or invalid: fix the portal URL setting");
        assertThat(portalCrudService.storage()).isEmpty();
    }

    @Test
    void should_refuse_when_the_environment_has_several_portals() {
        portalCrudService.initWith(
            List.of(aPortal(DEFAULT_PORTAL_ID, "Developer Portal", true), aPortal(AUTOMATION_PORTAL_ID, "Automation Portal", false))
        );

        assertThatThrownBy(() -> cut.resolve(ORG_ID, ENV_ID))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("[" + ENV_ID + "]")
            .hasMessageContaining("it has several portals");
    }

    @Test
    void should_find_the_reference_of_the_default_portal() {
        portalCrudService.initWith(List.of(aPortal(DEFAULT_PORTAL_ID, "Developer Portal", true)));

        var reference = cut.findReference(ENV_ID);

        assertThat(reference.portalId()).isEqualTo(DEFAULT_PORTAL_ID);
    }

    @Test
    void should_never_create_the_default_portal_when_finding_a_reference() {
        installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");

        assertThatThrownBy(() -> cut.findReference(ENV_ID))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("[" + ENV_ID + "]")
            .hasMessageContaining("its default portal is not created yet: save the portal settings to create it");
        assertThat(portalCrudService.storage()).isEmpty();
    }

    @Test
    void should_refuse_finding_a_reference_when_the_only_portal_comes_from_the_automation_api() {
        portalCrudService.initWith(List.of(aPortal(AUTOMATION_PORTAL_ID, "Automation Portal", false)));

        assertThatThrownBy(() -> cut.findReference(ENV_ID))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("its portal is managed through the Automation API");
    }

    @Test
    void should_refuse_finding_a_reference_when_the_environment_has_several_portals() {
        portalCrudService.initWith(
            List.of(aPortal(DEFAULT_PORTAL_ID, "Developer Portal", true), aPortal(AUTOMATION_PORTAL_ID, "Automation Portal", false))
        );

        assertThatThrownBy(() -> cut.findReference(ENV_ID))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("it has several portals");
    }
}
