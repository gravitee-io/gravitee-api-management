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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.LicenseRepository;
import io.gravitee.repository.management.api.search.LicenseCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.License;
import java.util.List;
import java.util.Optional;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpLicenseRepository extends AbstractNoOpManagementRepository<License, String> implements LicenseRepository {

    @Override
    public Optional<License> findById(String referenceId, License.ReferenceType referenceType) {
        return Optional.empty();
    }

    @Override
    public void delete(String referenceId, License.ReferenceType referenceType) {
        // noop
    }

    @Override
    public Page<License> findByCriteria(LicenseCriteria criteria, Pageable pageable) {
        return new Page<>(List.of(), 0, 0, 0L);
    }
}
