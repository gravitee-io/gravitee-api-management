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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
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
            AccessPointCriteria accessPointCriteria = AccessPointCriteria
                .builder()
                .referenceType(AccessPointReferenceType.valueOf(referenceType.name()))
                .referenceIds(Set.of(referenceId))
                .status(AccessPointStatus.CREATED)
                .build();
            var existingAccessPoints = accessPointRepository.findByCriteria(accessPointCriteria, null, null);

            var apsToCreate = new ArrayList<io.gravitee.repository.management.model.AccessPoint>();
            var apsToDelete = new ArrayList<>(existingAccessPoints);

            for (AccessPoint accessPoint : accessPoints) {
                var newAccessPoint = AccessPointAdapter.INSTANCE.fromEntity(accessPoint);

                var existingAPsForTarget = existingAccessPoints
                    .stream()
                    .filter(existingAccessPoint -> existingAccessPoint.getTarget().equals(newAccessPoint.getTarget()))
                    .toList();

                // if there aren't access points for this target, it needs to be added for creation
                if (existingAPsForTarget.isEmpty()) {
                    apsToCreate.add(newAccessPoint);
                    continue;
                }

                // Search in the existing access points if the new one is already present (comparing equality)
                // if yes => it needs to be removed from the list of APs to delete
                //          and the new AP doesn't need to be created
                // if no => then it will remain in the deletion list to be cleaned up
                //          the new AP doesn't exist and needs to be added for creation
                var shouldCreateNewAP = true;
                for (var existingAccessPoint : existingAPsForTarget) {
                    if (
                        existingAccessPoint.getHost().equalsIgnoreCase(newAccessPoint.getHost()) &&
                        existingAccessPoint.isSecured() == newAccessPoint.isSecured() &&
                        existingAccessPoint.isOverriding() == newAccessPoint.isOverriding()
                    ) {
                        apsToDelete.remove(existingAccessPoint);
                        shouldCreateNewAP = false;
                        break;
                    }
                }
                if (shouldCreateNewAP) {
                    apsToCreate.add(newAccessPoint);
                }
            }

            // traverse the lists of access-points and do all required updates
            for (var apToCreate : apsToCreate) {
                this.createAccessPoint(apToCreate);
            }
            for (var apToDelete : apsToDelete) {
                this.deleteAccessPoint(apToDelete);
            }
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while updating access points", e);
        }
    }

    @Override
    public void deleteAccessPoints(final AccessPoint.ReferenceType referenceType, final String referenceId) {
        try {
            var accessPointCriteria = AccessPointCriteria
                .builder()
                .referenceType(AccessPointReferenceType.valueOf(referenceType.name()))
                .referenceIds(Set.of(referenceId))
                .status(AccessPointStatus.CREATED)
                .build();
            var existingAccessPoints = accessPointRepository.findByCriteria(accessPointCriteria, null, null);
            for (var accessPoint : existingAccessPoints) {
                deleteAccessPoint(accessPoint);
            }
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while deleting access points", e);
        }
    }

    private void createAccessPoint(io.gravitee.repository.management.model.AccessPoint newAccessPoint) throws TechnicalException {
        if (newAccessPoint.getId() == null) {
            newAccessPoint.setId(UuidString.generateRandom());
        }
        newAccessPoint.setUpdatedAt(new Date());
        newAccessPoint.setStatus(AccessPointStatus.CREATED);
        var createdAccessPoint = accessPointRepository.create(newAccessPoint);
        eventManager.publishEvent(AccessPointEvent.CREATED, AccessPointAdapter.INSTANCE.toEntity(createdAccessPoint));
    }

    private void deleteAccessPoint(io.gravitee.repository.management.model.AccessPoint accessPointToDelete) throws TechnicalException {
        accessPointToDelete.setStatus(AccessPointStatus.DELETED);
        accessPointToDelete.setUpdatedAt(new Date());
        var updatedAccessPoint = accessPointRepository.update(accessPointToDelete);
        eventManager.publishEvent(AccessPointEvent.DELETED, AccessPointAdapter.INSTANCE.toEntity(updatedAccessPoint));
    }
}
