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
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Re-fetches every sourced portal navigation page whose auto-fetch cron has elapsed. Meant to be
 * triggered by the auto-fetch scheduler, across all environments. A page failing to fetch never stops
 * the others: the failure is recorded on the page itself.
 */
@UseCase
@CustomLog
@RequiredArgsConstructor
public class AutoFetchPortalNavigationItemsUseCase {

    private final PortalNavigationItemsQueryService queryService;
    private final PortalNavigationItemSourceDomainService sourceDomainService;
    private final PortalNavigationItemDomainService domainService;

    public Output execute() {
        var duePages = queryService
            .findAllWithAutoFetchEnabled()
            .stream()
            .filter(PortalNavigationPage.class::isInstance)
            .map(PortalNavigationPage.class::cast)
            .filter(page -> page.getSource() != null)
            .filter(page -> sourceDomainService.isAutoFetchDue(page.getSource()))
            .toList();

        int succeeded = 0;
        int failed = 0;
        for (var page : duePages) {
            if (fetch(page)) {
                succeeded++;
            } else {
                failed++;
            }
        }
        return new Output(succeeded, failed);
    }

    /** Never throws: one failing page must not stop the ones after it. */
    private boolean fetch(PortalNavigationPage page) {
        try {
            log.debug(
                "Auto-fetching portal navigation page [id={}, title={}, environmentId={}]",
                page.getId().json(),
                page.getTitle(),
                page.getEnvironmentId()
            );
            var updated = domainService.fetchPageContent(page);
            return updated.getSource() != null && updated.getSource().getLastFetchError() == null;
        } catch (Exception e) {
            log.warn(
                "Failed to auto-fetch portal navigation page [id={}, title={}, environmentId={}, sourceType={}]",
                page.getId().json(),
                page.getTitle(),
                page.getEnvironmentId(),
                page.getSource() == null ? null : page.getSource().getSourceType(),
                e
            );
            return false;
        }
    }

    public record Output(int succeeded, int failed) {}
}
