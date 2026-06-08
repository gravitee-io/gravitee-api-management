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
package io.gravitee.apim.core.portal.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import fixtures.core.model.PortalFixtures;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.domain_service.PortalNavigationListingDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalNavigationSyncDomainService;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.PortalId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetPortalUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private final PortalNavigationItemsCrudServiceInMemory navCrudService = new PortalNavigationItemsCrudServiceInMemory();
    private final PortalNavigationItemsQueryServiceInMemory navQueryService = new PortalNavigationItemsQueryServiceInMemory(
        navCrudService.storage()
    );
    private final PortalPageContentCrudServiceInMemory pageContentCrudService = new PortalPageContentCrudServiceInMemory();
    private PortalNavigationSyncDomainService syncDomainService;
    private GetPortalUseCase useCase;

    private PortalNavigationListingDomainService listingDomainService;

    @BeforeEach
    void setUp() {
        syncDomainService = new PortalNavigationSyncDomainService(navCrudService, navQueryService, pageContentCrudService);
        listingDomainService = new PortalNavigationListingDomainService(navQueryService);
        useCase = new GetPortalUseCase(portalCrudService, listingDomainService);
    }

    @AfterEach
    void tearDown() {
        portalCrudService.reset();
        navCrudService.reset();
        pageContentCrudService.reset();
    }

    @Test
    void should_return_portal() {
        var portal = PortalFixtures.aPortal();
        portalCrudService.initWith(List.of(portal));

        var output = useCase.execute(new GetPortalUseCase.Input(AUDIT_INFO, portal.getId()));

        assertThat(output.portal()).isEqualTo(portal);
        assertThat(output.navigation()).isEmpty();
    }

    @Test
    void should_throw_when_missing() {
        var unknownId = PortalId.of("00000000-0000-0000-0000-0000000000ff");
        var throwable = catchThrowable(() -> useCase.execute(new GetPortalUseCase.Input(AUDIT_INFO, unknownId)));

        assertThat(throwable).isInstanceOf(PortalNotFoundException.class).hasMessage("Portal [ " + unknownId + " ] not found");
    }

    @Test
    void should_throw_when_id_exists_in_different_environment() {
        var portal = PortalFixtures.aPortal();
        portalCrudService.initWith(List.of(portal));

        var otherEnvAudit = AuditInfo.builder()
            .organizationId("organization-id")
            .environmentId("other-env")
            .actor(AuditActor.builder().userId("user-id").build())
            .build();

        var throwable = catchThrowable(() -> useCase.execute(new GetPortalUseCase.Input(otherEnvAudit, portal.getId())));

        assertThat(throwable).isInstanceOf(PortalNotFoundException.class);
    }

    @Test
    void should_return_navigation_paths_from_stored_folders() {
        var portal = PortalFixtures.aPortal();
        portalCrudService.initWith(List.of(portal));
        syncDomainService.sync(
            AUDIT_INFO,
            portal.getId(),
            List.of(new NavigationPath("/a", Optional.empty()), new NavigationPath("/a/b", Optional.empty()))
        );

        var output = useCase.execute(new GetPortalUseCase.Input(AUDIT_INFO, portal.getId()));

        assertThat(output.navigation()).extracting(NavigationPath::path).containsExactly("/a", "/a/b");
    }
}
