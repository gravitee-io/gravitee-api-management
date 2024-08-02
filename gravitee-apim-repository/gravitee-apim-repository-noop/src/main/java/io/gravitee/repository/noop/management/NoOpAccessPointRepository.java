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
package io.gravitee.repository.noop.management;

import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.api.search.AccessPointCriteria;
import io.gravitee.repository.management.model.AccessPoint;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.AccessPointTarget;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NoOpAccessPointRepository extends AbstractNoOpManagementRepository<AccessPoint, String> implements AccessPointRepository {

    @Override
    public Optional<AccessPoint> findByHost(String host) {
        return Optional.empty();
    }

    @Override
    public List<AccessPoint> findByTarget(AccessPointTarget target) {
        return List.of();
    }

    @Override
    public List<AccessPoint> findByReferenceAndTarget(
        AccessPointReferenceType referenceType,
        String referenceId,
        AccessPointTarget target
    ) {
        return List.of();
    }

    @Override
    public List<AccessPoint> findByCriteria(AccessPointCriteria criteria, Long page, Long size) {
        return List.of();
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, AccessPointReferenceType referenceType) {
        return Collections.emptyList();
    }
}
