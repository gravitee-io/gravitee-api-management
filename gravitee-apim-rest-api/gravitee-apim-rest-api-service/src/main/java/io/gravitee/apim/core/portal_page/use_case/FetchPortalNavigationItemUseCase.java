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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemSourceDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationSourcedItemsDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * Re-runs the fetch of a navigation item: a sourced PAGE is fetched on its own, any other item
 * fetches every sourced PAGE below it. A page failing to fetch never blocks the others; an internal
 * inconsistency aborts the run.
 */
@UseCase
@RequiredArgsConstructor
public class FetchPortalNavigationItemUseCase {

    private final PortalNavigationItemsQueryService queryService;
    private final PortalNavigationSourcedItemsDomainService sourcedItemsDomainService;
    private final PortalNavigationItemDomainService domainService;
    private final PortalNavigationItemSourceDomainService sourceDomainService;

    public Output execute(Input input) {
        var item = queryService.findByIdAndEnvironmentId(input.environmentId(), PortalNavigationItemId.of(input.navigationItemId()));
        if (item == null) {
            throw new PortalNavigationItemNotFoundException(input.navigationItemId());
        }

        if (item instanceof PortalNavigationPage page && page.getSource() != null) {
            return Output.ofItem(fetchSingle(page));
        }
        return Output.ofSummary(fetchDescendants(input.environmentId(), item));
    }

    private PortalNavigationItem fetchSingle(PortalNavigationPage page) {
        var updatedItem = domainService.fetchPageContent(page);
        if (updatedItem.getSource() != null) {
            sourceDomainService.removeSensitiveData(updatedItem.getSource());
        }
        return updatedItem;
    }

    private List<PageFetchResult> fetchDescendants(String environmentId, PortalNavigationItem item) {
        var sourcedPages = sourcedItemsDomainService.findSourcedPageDescendants(environmentId, item.getId());
        if (sourcedPages.isEmpty()) {
            // A PAGE owns no subtree, so the only source it could have carried is its own
            throw item instanceof PortalNavigationPage
                ? InvalidPortalNavigationItemDataException.noSourceConfigured(item.getId().json())
                : InvalidPortalNavigationItemDataException.noSourcedPageBelow(item.getId().json());
        }
        return sourcedPages.stream().map(this::fetch).toList();
    }

    private PageFetchResult fetch(PortalNavigationPage page) {
        var updated = domainService.fetchPageContent(page);
        var error = updated.getSource() == null ? null : updated.getSource().getLastFetchError();
        return new PageFetchResult(updated.getId().id(), updated.getTitle(), error == null, error);
    }

    @Builder
    public record Input(String environmentId, String navigationItemId) {}

    /** Exactly one of the two is set: the single fetched item, or the summary of a subtree fetch. */
    public record Output(@Nullable PortalNavigationItem item, @Nullable FetchSummary summary) {
        public Output {
            if ((item == null) == (summary == null)) {
                throw new IllegalArgumentException("A fetch result carries either an item or a summary, never both nor neither.");
            }
        }

        static Output ofItem(PortalNavigationItem item) {
            return new Output(item, null);
        }

        static Output ofSummary(List<PageFetchResult> results) {
            return new Output(null, FetchSummary.of(results));
        }
    }

    public record FetchSummary(int succeeded, int failed, List<PageFetchResult> results) {
        static FetchSummary of(List<PageFetchResult> results) {
            var succeeded = (int) results.stream().filter(PageFetchResult::success).count();
            return new FetchSummary(succeeded, results.size() - succeeded, results);
        }
    }

    public record PageFetchResult(UUID navigationItemId, String title, boolean success, @Nullable String error) {}
}
