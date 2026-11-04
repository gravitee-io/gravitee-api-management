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
package io.gravitee.apim.infra.query_service.portal_notification;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_notification.model.PortalNotification;
import io.gravitee.apim.core.portal_notification.model.PortalNotificationQuery;
import io.gravitee.apim.core.portal_notification.model.PortalNotificationReferenceIds;
import io.gravitee.apim.core.portal_notification.query_service.PortalNotificationQueryService;
import io.gravitee.apim.infra.adapter.PortalNotificationAdapter;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNotificationRepository;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.rest.api.model.common.Pageable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class PortalNotificationQueryServiceImpl implements PortalNotificationQueryService {

    private final PortalNotificationRepository portalNotificationRepository;

    public PortalNotificationQueryServiceImpl(@Lazy PortalNotificationRepository portalNotificationRepository) {
        this.portalNotificationRepository = portalNotificationRepository;
    }

    @Override
    public Page<PortalNotification> search(PortalNotificationQuery query, Pageable pageable) {
        try {
            // The core pages from 1, the repository from 0.
            var page = portalNotificationRepository.search(
                PortalNotificationAdapter.INSTANCE.toRepository(query),
                new PageableBuilder().pageNumber(pageable.getPageNumber() - 1).pageSize(pageable.getPageSize()).build()
            );
            var content = page.getContent().stream().map(PortalNotificationAdapter.INSTANCE::toModel).toList();
            return new Page<>(content, pageable.getPageNumber(), content.size(), page.getTotalElements());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while searching the notifications of user " + query.user(), e);
        }
    }

    @Override
    public PortalNotificationReferenceIds findReferenceIds(String user, String environmentId) {
        try {
            return PortalNotificationAdapter.INSTANCE.toModel(portalNotificationRepository.findDistinctReferences(user, environmentId));
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while finding the notification references of user " + user, e);
        }
    }
}
