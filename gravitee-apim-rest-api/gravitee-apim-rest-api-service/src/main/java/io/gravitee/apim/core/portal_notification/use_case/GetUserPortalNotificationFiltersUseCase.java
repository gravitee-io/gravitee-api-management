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
package io.gravitee.apim.core.portal_notification.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal_notification.domain_service.PortalNotificationNamesDomainService;
import io.gravitee.apim.core.portal_notification.model.NotificationReference;
import io.gravitee.apim.core.portal_notification.query_service.PortalNotificationQueryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The APIs and applications the user's notifications are about, to filter the inbox on.
 */
@UseCase
public class GetUserPortalNotificationFiltersUseCase {

    private static final Comparator<NotificationReference> NAMED_FIRST_BY_NAME = Comparator.comparing(
        NotificationReference::name,
        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
    ).thenComparing(NotificationReference::id);

    private final PortalNotificationQueryService portalNotificationQueryService;
    private final PortalNotificationNamesDomainService portalNotificationNames;

    public GetUserPortalNotificationFiltersUseCase(
        PortalNotificationQueryService portalNotificationQueryService,
        PortalNotificationNamesDomainService portalNotificationNames
    ) {
        this.portalNotificationQueryService = portalNotificationQueryService;
        this.portalNotificationNames = portalNotificationNames;
    }

    public Output execute(Input input) {
        var environmentId = input.executionContext().getEnvironmentId();
        var referenceIds = portalNotificationQueryService.findReferenceIds(input.user(), environmentId);
        return new Output(
            references(referenceIds.apiIds(), portalNotificationNames.apiNames(referenceIds.apiIds(), environmentId)),
            references(
                referenceIds.applicationIds(),
                portalNotificationNames.applicationNames(referenceIds.applicationIds(), environmentId)
            )
        );
    }

    private static List<NotificationReference> references(Set<String> ids, Map<String, String> names) {
        return ids
            .stream()
            .map(id -> new NotificationReference(id, names.get(id)))
            .sorted(NAMED_FIRST_BY_NAME)
            .toList();
    }

    public record Input(ExecutionContext executionContext, String user) {}

    /**
     * Sorted by name, deleted APIs and applications (no name) last.
     */
    public record Output(List<NotificationReference> apis, List<NotificationReference> applications) {}
}
