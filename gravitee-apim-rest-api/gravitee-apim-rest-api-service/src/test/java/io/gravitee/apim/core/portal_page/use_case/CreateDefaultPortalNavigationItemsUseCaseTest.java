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
package io.gravitee.apim.core.portal_page.use_case;

import static fixtures.core.model.PortalNavigationItemFixtures.ENV_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.ORG_ID;
import static org.assertj.core.api.Assertions.assertThat;

import inmemory.ApiCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateDefaultPortalNavigationItemsUseCaseTest {

    private PortalNavigationItemDomainService portalNavigationItemDomainService;
    private CreateDefaultPortalNavigationItemsUseCase useCase;
    private PortalNavigationItemsCrudServiceInMemory crudService;
    private PortalNavigationItemsQueryServiceInMemory queryService;
    private PortalPageContentCrudServiceInMemory pageContentCrudService;
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();

    @BeforeEach
    void setUp() {
        final var storage = new ArrayList<PortalNavigationItem>();

        crudService = new PortalNavigationItemsCrudServiceInMemory(storage);
        queryService = new PortalNavigationItemsQueryServiceInMemory(storage);
        pageContentCrudService = new PortalPageContentCrudServiceInMemory();
        portalNavigationItemDomainService = new PortalNavigationItemDomainService(
            crudService,
            queryService,
            pageContentCrudService,
            apiCrudService
        );
        useCase = new CreateDefaultPortalNavigationItemsUseCase(portalNavigationItemDomainService, pageContentCrudService);
    }

    @Test
    void should_create_default_items() {
        // When
        useCase.execute(ORG_ID, ENV_ID);

        // Then
        final var items = queryService.storage();

        final var guidesFolder = items
            .stream()
            .filter(item -> item.getTitle().equals("Guides"))
            .findFirst()
            .get();
        assertThat(guidesFolder).isNotNull();
        assertThat(guidesFolder).isInstanceOf(PortalNavigationFolder.class);
        assertThat(guidesFolder.getParentId()).isNull();
        assertThat(guidesFolder.getTitle()).isEqualTo("Guides");
        assertThat(guidesFolder.getOrder()).isEqualTo(0);
        assertThat(guidesFolder.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
        assertThat(guidesFolder.getPublished()).isTrue();

        final var gettingStartedPage = items
            .stream()
            .filter(item -> item.getTitle().equals("Getting started"))
            .findFirst()
            .get();
        assertThat(gettingStartedPage).isNotNull();
        assertThat(gettingStartedPage).isInstanceOf(PortalNavigationPage.class);
        assertThat(gettingStartedPage.getParentId()).isEqualTo(guidesFolder.getId());
        assertThat(gettingStartedPage.getTitle()).isEqualTo("Getting started");
        assertThat(((PortalNavigationPage) gettingStartedPage).getPortalPageContentId()).isNotNull();
        assertThat(gettingStartedPage.getOrder()).isEqualTo(0);
        assertThat(gettingStartedPage.getOrganizationId()).isEqualTo(guidesFolder.getOrganizationId());
        assertThat(gettingStartedPage.getEnvironmentId()).isEqualTo(guidesFolder.getEnvironmentId());
        assertThat(gettingStartedPage.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
        assertThat(gettingStartedPage.getPublished()).isTrue();

        final var coreConceptsFolder = items
            .stream()
            .filter(item -> item.getTitle().equals("Core concepts"))
            .findFirst()
            .get();
        assertThat(coreConceptsFolder).isNotNull();
        assertThat(coreConceptsFolder).isInstanceOf(PortalNavigationFolder.class);
        assertThat(coreConceptsFolder.getParentId()).isEqualTo(guidesFolder.getId());
        assertThat(coreConceptsFolder.getTitle()).isEqualTo("Core concepts");
        assertThat(coreConceptsFolder.getOrder()).isEqualTo(1);
        assertThat(coreConceptsFolder.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
        assertThat(coreConceptsFolder.getPublished()).isTrue();

        final var authPage = items
            .stream()
            .filter(item -> item.getTitle().equals("Authentication"))
            .findFirst()
            .get();
        assertThat(authPage).isNotNull();
        assertThat(authPage).isInstanceOf(PortalNavigationPage.class);
        assertThat(authPage.getParentId()).isEqualTo(coreConceptsFolder.getId());
        assertThat(authPage.getTitle()).isEqualTo("Authentication");
        assertThat(((PortalNavigationPage) authPage).getPortalPageContentId()).isNotNull();
        assertThat(authPage.getOrder()).isEqualTo(0);
        assertThat(authPage.getOrganizationId()).isEqualTo(coreConceptsFolder.getOrganizationId());
        assertThat(authPage.getEnvironmentId()).isEqualTo(coreConceptsFolder.getEnvironmentId());
        assertThat(authPage.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
        assertThat(authPage.getPublished()).isTrue();

        final var firstCallPage = items
            .stream()
            .filter(item -> item.getTitle().equals("Making your first API call"))
            .findFirst()
            .get();
        assertThat(firstCallPage).isNotNull();
        assertThat(firstCallPage).isInstanceOf(PortalNavigationPage.class);
        assertThat(firstCallPage.getParentId()).isEqualTo(coreConceptsFolder.getId());
        assertThat(firstCallPage.getTitle()).isEqualTo("Making your first API call");
        assertThat(((PortalNavigationPage) firstCallPage).getPortalPageContentId()).isNotNull();
        assertThat(firstCallPage.getOrder()).isEqualTo(1);
        assertThat(firstCallPage.getOrganizationId()).isEqualTo(coreConceptsFolder.getOrganizationId());
        assertThat(firstCallPage.getEnvironmentId()).isEqualTo(coreConceptsFolder.getEnvironmentId());
        assertThat(firstCallPage.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
        assertThat(firstCallPage.getPublished()).isTrue();

        final var docsLink = items
            .stream()
            .filter(item -> item.getTitle().equals("Docs"))
            .findFirst()
            .get();
        assertThat(docsLink).isNotNull();
        assertThat(docsLink).isInstanceOf(PortalNavigationLink.class);
        assertThat(docsLink.getParentId()).isNull();
        assertThat(docsLink.getTitle()).isEqualTo("Docs");
        assertThat(((PortalNavigationLink) docsLink).getUrl()).isEqualTo(
            "https://documentation.gravitee.io/apim/developer-portal/new-developer-portal"
        );
        assertThat(docsLink.getOrder()).isEqualTo(1);
        assertThat(docsLink.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
        assertThat(docsLink.getPublished()).isTrue();
    }

    @Test
    void should_create_home_page() {
        // When
        useCase.execute(ORG_ID, ENV_ID);

        // Then
        final var items = queryService.storage();

        final var homePage = items
            .stream()
            .filter(item -> item.getTitle().equals("Home Page"))
            .findFirst()
            .orElse(null);

        assertThat(homePage).isNotNull();
        assertThat(homePage).isInstanceOf(PortalNavigationPage.class);
        assertThat(homePage.getParentId()).isNull();
        assertThat(((PortalNavigationPage) homePage).getPortalPageContentId()).isNotNull();
        assertThat(homePage.getOrganizationId()).isEqualTo(ORG_ID);
        assertThat(homePage.getEnvironmentId()).isEqualTo(ENV_ID);
        assertThat(homePage.getVisibility()).isEqualTo(PortalVisibility.PUBLIC);
        assertThat(homePage.getPublished()).isTrue();
    }
}
