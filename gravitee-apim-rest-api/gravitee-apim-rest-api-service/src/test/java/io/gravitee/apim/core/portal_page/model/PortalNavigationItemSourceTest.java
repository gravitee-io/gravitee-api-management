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
package io.gravitee.apim.core.portal_page.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import java.time.Instant;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemSourceTest {

    private static final Instant LAST_FETCHED_AT = Instant.parse("2026-07-17T10:00:00Z");
    private static final Instant LAST_FETCH_ATTEMPT_AT = Instant.parse("2026-07-17T11:00:00Z");

    private PortalNavigationItem aPageWithSource(PortalNavigationItemSource source) {
        var item = PortalNavigationItem.from(
            CreatePortalNavigationItem.builder()
                .type(PortalNavigationItemType.PAGE)
                .title("My Page")
                .segment("my-page")
                .area(PortalArea.TOP_NAVBAR)
                .order(0)
                .portalPageContentId(PortalPageContentId.random())
                .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
                .source(source)
                .build(),
            "org-id",
            "env-id",
            null
        );
        return item;
    }

    private UpdatePortalNavigationItem.UpdatePortalNavigationItemBuilder anUpdate() {
        return UpdatePortalNavigationItem.builder()
            .type(PortalNavigationItemType.PAGE)
            .title("My Page")
            .segment("my-page")
            .order(0)
            .published(true)
            .visibility(PortalVisibility.PUBLIC);
    }

    @Test
    void should_carry_source_from_create_model() {
        var source = PortalNavigationItemSource.builder().sourceType("github-fetcher").sourceConfiguration("{}").build();

        var item = aPageWithSource(source);

        assertThat(item.getSource()).isEqualTo(source);
    }

    @Test
    void should_have_no_source_by_default() {
        var item = aPageWithSource(null);

        assertThat(item.getSource()).isNull();
    }

    @Test
    void should_set_source_on_update() {
        var item = aPageWithSource(null);
        var source = PortalNavigationItemSource.builder().sourceType("http-fetcher").sourceConfiguration("{}").build();

        item.update(anUpdate().source(source).build());

        assertThat(item.getSource()).isEqualTo(source);
    }

    @Test
    void should_remove_source_on_update_without_source() {
        var item = aPageWithSource(PortalNavigationItemSource.builder().sourceType("http-fetcher").sourceConfiguration("{}").build());

        item.update(anUpdate().build());

        assertThat(item.getSource()).isNull();
    }

    @Test
    void should_preserve_server_managed_fetch_state_when_origin_is_unchanged() {
        var item = aPageWithSource(
            PortalNavigationItemSource.builder()
                .sourceType("http-fetcher")
                .sourceConfiguration("{\"url\":\"https://example.com/a.md\"}")
                .lastFetchedAt(LAST_FETCHED_AT)
                .lastFetchAttemptAt(LAST_FETCH_ATTEMPT_AT)
                .lastFetchError("boom")
                .build()
        );

        item.update(
            anUpdate()
                .source(
                    PortalNavigationItemSource.builder()
                        .sourceType("http-fetcher")
                        .sourceConfiguration("{\"url\":\"https://example.com/a.md\"}")
                        .useAutoFetch(true)
                        .fetchCron("0 */10 * * * *")
                        .build()
                )
                .build()
        );

        var source = item.getSource();
        assertThat(source).isNotNull();
        assertThat(source.isUseAutoFetch()).isTrue();
        assertThat(source.getFetchCron()).isEqualTo("0 */10 * * * *");
        assertThat(source.getLastFetchedAt()).isEqualTo(LAST_FETCHED_AT);
        assertThat(source.getLastFetchAttemptAt()).isEqualTo(LAST_FETCH_ATTEMPT_AT);
        assertThat(source.getLastFetchError()).isEqualTo("boom");
    }

    @Test
    void should_preserve_fetch_state_when_configuration_only_differs_in_formatting() {
        var item = aPageWithSource(
            PortalNavigationItemSource.builder()
                .sourceType("http-fetcher")
                .sourceConfiguration("{\n  \"url\" : \"https://example.com/a.md\",\n  \"useAuth\" : true\n}")
                .lastFetchedAt(LAST_FETCHED_AT)
                .lastFetchAttemptAt(LAST_FETCH_ATTEMPT_AT)
                .build()
        );

        item.update(
            anUpdate()
                .source(
                    PortalNavigationItemSource.builder()
                        .sourceType("http-fetcher")
                        .sourceConfiguration("{\"useAuth\":true,\"url\":\"https://example.com/a.md\"}")
                        .build()
                )
                .build()
        );

        assertThat(item.getSource()).isNotNull();
        assertThat(item.getSource().getLastFetchedAt()).isEqualTo(LAST_FETCHED_AT);
        assertThat(item.getSource().getLastFetchAttemptAt()).isEqualTo(LAST_FETCH_ATTEMPT_AT);
    }

    @Test
    void should_reset_fetch_state_when_source_configuration_changes() {
        var item = aPageWithSource(
            PortalNavigationItemSource.builder()
                .sourceType("http-fetcher")
                .sourceConfiguration("{\"url\":\"https://example.com/a.md\"}")
                .lastFetchedAt(LAST_FETCHED_AT)
                .lastFetchAttemptAt(LAST_FETCH_ATTEMPT_AT)
                .lastFetchError("boom")
                .build()
        );

        item.update(
            anUpdate()
                .source(
                    PortalNavigationItemSource.builder()
                        .sourceType("http-fetcher")
                        .sourceConfiguration("{\"url\":\"https://example.com/b.md\"}")
                        .build()
                )
                .build()
        );

        var source = item.getSource();
        assertThat(source).isNotNull();
        assertThat(source.getSourceConfiguration()).isEqualTo("{\"url\":\"https://example.com/b.md\"}");
        assertThat(source.getLastFetchedAt()).isNull();
        assertThat(source.getLastFetchAttemptAt()).isNull();
        assertThat(source.getLastFetchError()).isNull();
    }

    @Test
    void should_reset_fetch_state_when_source_type_changes() {
        var item = aPageWithSource(
            PortalNavigationItemSource.builder()
                .sourceType("http-fetcher")
                .sourceConfiguration("{}")
                .lastFetchedAt(LAST_FETCHED_AT)
                .lastFetchAttemptAt(LAST_FETCH_ATTEMPT_AT)
                .lastFetchError("boom")
                .build()
        );

        item.update(
            anUpdate().source(PortalNavigationItemSource.builder().sourceType("github-fetcher").sourceConfiguration("{}").build()).build()
        );

        var source = item.getSource();
        assertThat(source).isNotNull();
        assertThat(source.getLastFetchedAt()).isNull();
        assertThat(source.getLastFetchAttemptAt()).isNull();
        assertThat(source.getLastFetchError()).isNull();
    }

    @Test
    void should_never_take_fetch_state_from_the_client() {
        var item = aPageWithSource(null);

        item.update(
            anUpdate()
                .source(
                    PortalNavigationItemSource.builder()
                        .sourceType("http-fetcher")
                        .sourceConfiguration("{}")
                        .lastFetchedAt(LAST_FETCHED_AT)
                        .lastFetchAttemptAt(LAST_FETCH_ATTEMPT_AT)
                        .lastFetchError("forged")
                        .build()
                )
                .build()
        );

        var source = item.getSource();
        assertThat(source).isNotNull();
        assertThat(source.getLastFetchedAt()).isNull();
        assertThat(source.getLastFetchAttemptAt()).isNull();
        assertThat(source.getLastFetchError()).isNull();
    }
}
