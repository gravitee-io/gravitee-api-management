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
import inmemory.PortalListingCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.domain_service.PortalAutomationScopeDomainService;
import io.gravitee.apim.core.portal.domain_service.PortalNavigationSyncDomainService;
import io.gravitee.apim.core.portal.domain_service.ValidatePortalDomainService;
import io.gravitee.apim.core.portal.domain_service.navigation.plan.NavigationSyncPlanExecutor;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.query_service.AutomationManagedNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.domain_service.PortalDocumentationSyncDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeletePortalUseCaseTest {

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
    private final PortalPageContentQueryServiceInMemory pageContentQueryService = new PortalPageContentQueryServiceInMemory();
    private final PortalListingCrudServiceInMemory portalListingCrudService = new PortalListingCrudServiceInMemory();

    private CreateOrUpdatePortalUseCase setupUseCase;
    private DeletePortalUseCase useCase;

    @BeforeEach
    void setUp() {
        var navSync = new PortalNavigationSyncDomainService(
            navQueryService,
            new AutomationManagedNavigationItemsQueryService(portalListingCrudService, pageContentQueryService),
            new NavigationSyncPlanExecutor(navCrudService, navQueryService, pageContentCrudService)
        );
        var scopeEnforcer = new PortalAutomationScopeDomainService(portalCrudService);
        setupUseCase = new CreateOrUpdatePortalUseCase(
            new ValidatePortalDomainService(scopeEnforcer),
            portalCrudService,
            navSync,
            pageContentQueryService,
            new PortalDocumentationSyncDomainService(navCrudService, navQueryService),
            scopeEnforcer
        );
        useCase = new DeletePortalUseCase(portalCrudService, navSync);
    }

    @AfterEach
    void tearDown() {
        portalCrudService.reset();
        navCrudService.reset();
        pageContentCrudService.reset();
        pageContentQueryService.reset();
    }

    @Test
    void should_delete() {
        var portal = PortalFixtures.aPortal();
        portalCrudService.initWith(List.of(portal));

        useCase.execute(new DeletePortalUseCase.Input(AUDIT_INFO, portal.getId()));

        assertThat(portalCrudService.storage()).isEmpty();
    }

    @Test
    void should_throw_when_missing() {
        var unknownId = PortalId.of("00000000-0000-0000-0000-0000000000ff");
        var throwable = catchThrowable(() -> useCase.execute(new DeletePortalUseCase.Input(AUDIT_INFO, unknownId)));

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

        var throwable = catchThrowable(() -> useCase.execute(new DeletePortalUseCase.Input(otherEnvAudit, portal.getId())));

        assertThat(throwable).isInstanceOf(PortalNotFoundException.class);
        assertThat(portalCrudService.storage()).containsExactly(portal);
    }

    @Test
    void should_cascade_delete_portal_folders() {
        var portal = PortalFixtures.aPortal();
        setupUseCase.execute(
            new CreateOrUpdatePortalUseCase.Input(
                AUDIT_INFO,
                portal,
                List.of(new NavigationPath("/projects/alpha", null), new NavigationPath("/projects/beta", null))
            )
        );
        assertThat(navCrudService.storage()).as("setup should materialize folders").isNotEmpty();

        useCase.execute(new DeletePortalUseCase.Input(AUDIT_INFO, portal.getId()));

        assertThat(portalCrudService.storage()).isEmpty();
        assertThat(navCrudService.storage()).as("portal's folders should be cascade-cleaned").isEmpty();
    }

    @Test
    void should_not_touch_folders_managed_by_other_portals() {
        var otherEnvAudit = AuditInfo.builder()
            .organizationId(AUDIT_INFO.organizationId())
            .environmentId("other-env")
            .actor(AUDIT_INFO.actor())
            .build();
        var portalA = PortalFixtures.aPortal();
        var portalB = Portal.of(
            PortalId.of("00000000-0000-0000-0000-0000000000a2"),
            otherEnvAudit.environmentId(),
            otherEnvAudit.organizationId(),
            "Other Portal"
        );
        setupUseCase.execute(new CreateOrUpdatePortalUseCase.Input(AUDIT_INFO, portalA, List.of(new NavigationPath("/alpha", null))));
        setupUseCase.execute(new CreateOrUpdatePortalUseCase.Input(otherEnvAudit, portalB, List.of(new NavigationPath("/beta", null))));
        assertThat(navCrudService.storage()).hasSize(2);

        useCase.execute(new DeletePortalUseCase.Input(AUDIT_INFO, portalA.getId()));

        assertThat(portalCrudService.storage()).extracting(Portal::getId).containsExactly(portalB.getId());
        assertThat(navCrudService.storage()).extracting(PortalNavigationItem::getTitle).containsExactly("beta");
    }

    @Test
    void should_delete_when_portal_has_no_navigation() {
        var portal = PortalFixtures.aPortal();
        portalCrudService.initWith(List.of(portal));

        useCase.execute(new DeletePortalUseCase.Input(AUDIT_INFO, portal.getId()));

        assertThat(portalCrudService.storage()).isEmpty();
        assertThat(navCrudService.storage()).isEmpty();
    }
}
