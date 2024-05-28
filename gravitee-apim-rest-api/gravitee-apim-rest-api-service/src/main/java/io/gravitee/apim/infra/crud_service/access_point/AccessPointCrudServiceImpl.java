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
package io.gravitee.apim.infra.crud_service.access_point;

import io.gravitee.apim.core.access_point.crud_service.AccessPointCrudService;
import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.access_point.model.AccessPointEvent;
import io.gravitee.apim.infra.adapter.AccessPointAdapter;
import io.gravitee.common.event.EventManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.api.search.AccessPointCriteria;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.AccessPointStatus;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.TransactionalService;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class AccessPointCrudServiceImpl extends TransactionalService implements AccessPointCrudService {

    private final AccessPointRepository accessPointRepository;
    private EventManager eventManager;

    public AccessPointCrudServiceImpl(@Lazy AccessPointRepository accessPointRepository, EventManager eventManager) {
        this.accessPointRepository = accessPointRepository;
        this.eventManager = eventManager;
    }

    @Override
    public void updateAccessPoints(
        final AccessPoint.ReferenceType referenceType,
        final String referenceId,
        final List<AccessPoint> accessPoints
    ) {
        try {
            Date updateStartTime = new Date();
            for (AccessPoint accessPoint : accessPoints) {
                var ap = AccessPointAdapter.INSTANCE.fromEntity(accessPoint);
                if (ap.getId() == null) {
                    ap.setId(UuidString.generateRandom());
                }
                ap.setUpdatedAt(new Date());
                ap.setStatus(AccessPointStatus.CREATED);
                io.gravitee.repository.management.model.AccessPoint createdAccessPoint = accessPointRepository.create(ap);
                eventManager.publishEvent(AccessPointEvent.CREATED, AccessPointAdapter.INSTANCE.toEntity(createdAccessPoint));
            }

            this.deleteAccessPointsBeforeUpdateTime(referenceType, referenceId, updateStartTime);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while creating access points", e);
        }
    }

    @Override
    public void deleteAccessPoints(final AccessPoint.ReferenceType referenceType, final String referenceId) {
        this.deleteAccessPointsBeforeUpdateTime(referenceType, referenceId, null);
    }

    private void deleteAccessPointsBeforeUpdateTime(
        final AccessPoint.ReferenceType referenceType,
        final String referenceId,
        Date updateStartTime
    ) {
        try {
            AccessPointCriteria accessPointCriteria = AccessPointCriteria
                .builder()
                .referenceType(AccessPointReferenceType.valueOf(referenceType.name()))
                .referenceIds(Collections.singletonList(referenceId))
                .status(AccessPointStatus.CREATED)
                .to(updateStartTime == null ? -1 : updateStartTime.getTime())
                .build();
            List<io.gravitee.repository.management.model.AccessPoint> deleteAccessPoints = accessPointRepository.updateStatusByCriteria(
                accessPointCriteria,
                AccessPointStatus.DELETED
            );

            if (deleteAccessPoints != null) {
                deleteAccessPoints
                    .stream()
                    .map(AccessPointAdapter.INSTANCE::toEntity)
                    .forEach(accessPoint -> eventManager.publishEvent(AccessPointEvent.DELETED, accessPoint));
            }
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while deleting access points", e);
        }
    }
}
