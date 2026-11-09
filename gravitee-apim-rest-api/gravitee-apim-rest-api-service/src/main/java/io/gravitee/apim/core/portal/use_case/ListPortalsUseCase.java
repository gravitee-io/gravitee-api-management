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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.domain_service.PortalSummaryDomainService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalSummary;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListPortalsUseCase {

    private static final Comparator<Portal> DEFAULT_FIRST_THEN_BY_NAME = Comparator.comparing((Portal portal) -> !portal.isDefault())
        .thenComparing(Portal::getName, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(portal -> portal.getId().toString());

    private final PortalCrudService portalCrudService;
    private final PortalSummaryDomainService portalSummaryDomainService;

    /** @param query keeps the portals whose name contains it, ignoring case; null or blank keeps them all */
    public record Input(String environmentId, String query, Pageable pageable) {}

    public record Output(Page<PortalSummary> portals) {}

    public Output execute(Input input) {
        // The license caps an environment at a handful of portals, so filtering and paging in memory is cheap.
        var matching = portalCrudService
            .findByEnvironmentId(input.environmentId())
            .stream()
            .filter(portal -> nameContains(portal, input.query()))
            .sorted(DEFAULT_FIRST_THEN_BY_NAME)
            .toList();

        var pageable = input.pageable();
        var from = (int) Math.min((long) (pageable.getPageNumber() - 1) * pageable.getPageSize(), matching.size());
        var to = Math.min(from + pageable.getPageSize(), matching.size());
        List<PortalSummary> content = portalSummaryDomainService.summarize(input.environmentId(), matching.subList(from, to));

        return new Output(new Page<>(content, pageable.getPageNumber(), content.size(), matching.size()));
    }

    private static boolean nameContains(Portal portal, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return portal.getName().toLowerCase(Locale.ROOT).contains(query.strip().toLowerCase(Locale.ROOT));
    }
}
