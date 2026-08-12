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

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.ApiCrudServiceInMemory;
import inmemory.PortalNavigationItemSourceDomainServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentCrudServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemSource;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AutoFetchPortalNavigationItemsUseCaseTest {

    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";
    private static final String OTHER_ENV_ID = "other-env-id";
    private static final String DEFAULT_CONTENT = "# Default content";
    private static final Instant NOW = Instant.parse("2026-08-05T12:34:56Z");

    private PortalNavigationItemsCrudServiceInMemory crudService;
    private PortalPageContentCrudServiceInMemory pageContentCrudService;
    private PortalNavigationItemSourceDomainServiceInMemory sourceDomainService;
    private AutoFetchPortalNavigationItemsUseCase useCase;

    @AfterEach
    void unfreezeTime() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneId.systemDefault()));
        var storage = new ArrayList<PortalNavigationItem>();
        crudService = new PortalNavigationItemsCrudServiceInMemory(storage);
        var queryService = new PortalNavigationItemsQueryServiceInMemory(storage);
        pageContentCrudService = new PortalPageContentCrudServiceInMemory();
        sourceDomainService = new PortalNavigationItemSourceDomainServiceInMemory();

        var domainService = new PortalNavigationItemDomainService(
            crudService,
            queryService,
            pageContentCrudService,
            PortalPageContentQueryServiceInMemory.sharing(pageContentCrudService.storage()),
            new ApiCrudServiceInMemory(),
            sourceDomainService
        );
        useCase = new AutoFetchPortalNavigationItemsUseCase(queryService, sourceDomainService, domainService);
    }

    @Test
    void should_fetch_pages_with_auto_fetch_enabled_across_environments() {
        var page = aPage("Sourced Page", ENV_ID, autoFetchSource().build());
        var pageInOtherEnvironment = aPage("Other Sourced Page", OTHER_ENV_ID, autoFetchSource().build());

        var output = useCase.execute();

        assertThat(output.succeeded()).isEqualTo(2);
        assertThat(output.failed()).isZero();
        assertThat(fetchedContentOf(page)).isEqualTo(PortalNavigationItemSourceDomainServiceInMemory.MARKDOWN);
        assertThat(fetchedContentOf(pageInOtherEnvironment)).isEqualTo(PortalNavigationItemSourceDomainServiceInMemory.MARKDOWN);
    }

    @Test
    void should_update_last_fetched_at_after_a_successful_fetch() {
        var page = aPage("Sourced Page", ENV_ID, autoFetchSource().lastFetchError("previous error").build());

        useCase.execute();

        assertThat(page.getSource().getLastFetchedAt()).isEqualTo(NOW);
        assertThat(page.getSource().getLastFetchAttemptAt()).isEqualTo(NOW);
        assertThat(page.getSource().getLastFetchError()).isNull();
    }

    @Test
    void should_not_fetch_pages_whose_cron_is_not_due_yet() {
        var notDue = aPage("Not Due Page", ENV_ID, autoFetchSource().build());
        var due = aPage("Due Page", ENV_ID, autoFetchSource().build());
        sourceDomainService.markAutoFetchNotDue(notDue.getSource());

        var output = useCase.execute();

        assertThat(output.succeeded()).isEqualTo(1);
        assertThat(notDue.getSource().getLastFetchedAt()).isNull();
        assertThat(notDue.getSource().getLastFetchAttemptAt()).isNull();
        assertThat(fetchedContentOf(notDue)).isEqualTo(DEFAULT_CONTENT);
        assertThat(fetchedContentOf(due)).isEqualTo(PortalNavigationItemSourceDomainServiceInMemory.MARKDOWN);
    }

    @Test
    void should_ignore_pages_without_auto_fetch_and_items_that_are_not_pages() {
        var withoutAutoFetch = aPage("Manual Page", ENV_ID, manualSource().build());
        aPage("Inline Page", ENV_ID, null);
        aFolder("Sourced Folder", autoFetchSource().build());

        var output = useCase.execute();

        assertThat(output.succeeded()).isZero();
        assertThat(output.failed()).isZero();
        assertThat(withoutAutoFetch.getSource().getLastFetchedAt()).isNull();
        assertThat(withoutAutoFetch.getSource().getLastFetchAttemptAt()).isNull();
    }

    @Test
    void should_keep_fetching_other_pages_when_one_fetch_fails() {
        var failing = aPage("Failing Page", ENV_ID, autoFetchSource().build());
        var working = aPage("Working Page", ENV_ID, autoFetchSource().build());
        sourceDomainService.failNextFetchWith(new TechnicalDomainException("fetch went wrong"));

        var output = useCase.execute();

        assertThat(output.succeeded()).isEqualTo(1);
        assertThat(output.failed()).isEqualTo(1);
        assertThat(failing.getSource().getLastFetchError()).isEqualTo("Unable to fetch content from source type http-fetcher.");
        assertThat(failing.getSource().getLastFetchError()).doesNotContain("example.com");
        assertThat(failing.getSource().getLastFetchedAt()).isNull();
        // The failed attempt is still stamped, so the next run waits for the cron instead of retrying at once
        assertThat(failing.getSource().getLastFetchAttemptAt()).isEqualTo(NOW);
        assertThat(working.getSource().getLastFetchedAt()).isEqualTo(NOW);
        assertThat(fetchedContentOf(working)).isEqualTo(PortalNavigationItemSourceDomainServiceInMemory.MARKDOWN);
    }

    @Test
    void should_keep_fetching_other_pages_when_one_page_content_is_missing() {
        var orphan = aPage("Orphan Page", ENV_ID, autoFetchSource().build());
        pageContentCrudService.delete(orphan.getPortalPageContentId());
        var working = aPage("Working Page", ENV_ID, autoFetchSource().build());

        var output = useCase.execute();

        assertThat(output.succeeded()).isEqualTo(1);
        assertThat(output.failed()).isEqualTo(1);
        assertThat(working.getSource().getLastFetchedAt()).isEqualTo(NOW);
        // Stamped even though the fetch never ran, otherwise the broken page is picked up on every tick
        assertThat(orphan.getSource().getLastFetchAttemptAt()).isEqualTo(NOW);
    }

    @Test
    void should_report_nothing_when_no_item_uses_auto_fetch() {
        var output = useCase.execute();

        assertThat(output.succeeded()).isZero();
        assertThat(output.failed()).isZero();
    }

    private PortalNavigationItemSource.PortalNavigationItemSourceBuilder autoFetchSource() {
        return manualSource().useAutoFetch(true).fetchCron("0 */10 * * * *");
    }

    private PortalNavigationItemSource.PortalNavigationItemSourceBuilder manualSource() {
        return PortalNavigationItemSource.builder()
            .sourceType("http-fetcher")
            .sourceConfiguration("{\"url\":\"https://example.com/doc.md\"}");
    }

    private PortalNavigationFolder aFolder(String title, PortalNavigationItemSource source) {
        var folder = PortalNavigationFolder.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title(title)
            .segment(PortalNavigationItem.slugify(title).value())
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .source(source)
            .build();
        crudService.create(folder);
        return folder;
    }

    private PortalNavigationPage aPage(String title, String environmentId, PortalNavigationItemSource source) {
        var content = GraviteeMarkdownPageContent.create(ORG_ID, environmentId, DEFAULT_CONTENT);
        pageContentCrudService.create(content);

        var page = PortalNavigationPage.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(environmentId)
            .title(title)
            .segment(PortalNavigationItem.slugify(title).value())
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .portalPageContentId(content.getId())
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .source(source)
            .build();
        crudService.create(page);
        return page;
    }

    private String fetchedContentOf(PortalNavigationPage page) {
        return pageContentCrudService
            .storage()
            .stream()
            .filter(content -> content.getId().equals(page.getPortalPageContentId()))
            .findFirst()
            .map(content -> ((GraviteeMarkdownPageContent) content).getContent().value())
            .orElseThrow();
    }
}
