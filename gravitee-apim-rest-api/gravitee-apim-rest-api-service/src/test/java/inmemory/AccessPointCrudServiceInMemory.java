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
package inmemory;

import io.gravitee.apim.core.access_point.crud_service.AccessPointCrudService;
import io.gravitee.apim.core.access_point.model.AccessPoint;
import java.util.List;

/** Writes into the storage of an {@link AccessPointQueryServiceInMemory}, so what is created can be read back. */
public class AccessPointCrudServiceInMemory implements AccessPointCrudService {

    private final AccessPointQueryServiceInMemory accessPoints;
    private int created;

    public AccessPointCrudServiceInMemory(AccessPointQueryServiceInMemory accessPoints) {
        this.accessPoints = accessPoints;
    }

    @Override
    public void updateAccessPoints(AccessPoint.ReferenceType referenceType, String referenceId, List<AccessPoint> accessPoints) {
        throw new UnsupportedOperationException("Not used by in-memory tests");
    }

    @Override
    public void deleteAccessPoints(AccessPoint.ReferenceType referenceType, String referenceId) {
        accessPoints.storage.removeIf(
            accessPoint -> accessPoint.getReferenceType() == referenceType && referenceId.equals(accessPoint.getReferenceId())
        );
    }

    @Override
    public void deleteUnreferencedAccessPoints(AccessPoint.ReferenceType referenceType, String referenceId) {
        throw new UnsupportedOperationException("Not used by in-memory tests");
    }

    @Override
    public AccessPoint create(AccessPoint accessPoint) {
        var stored = accessPoint.toBuilder().id(accessPoint.getId() != null ? accessPoint.getId() : "access-point-" + ++created).build();
        accessPoints.storage.add(stored);
        return stored;
    }

    @Override
    public void delete(String accessPointId) {
        accessPoints.storage.removeIf(accessPoint -> accessPointId.equals(accessPoint.getId()));
    }
}
