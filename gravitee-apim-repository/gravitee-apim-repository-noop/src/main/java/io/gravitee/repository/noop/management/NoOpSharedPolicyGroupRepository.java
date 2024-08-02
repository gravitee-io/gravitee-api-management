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
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SharedPolicyGroupRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.api.search.SharedPolicyGroupCriteria;
import io.gravitee.repository.management.api.search.Sortable;
import io.gravitee.repository.management.model.SharedPolicyGroup;
import java.util.List;
import java.util.Optional;

/**
 * @author GraviteeSource Team
 */
public class NoOpSharedPolicyGroupRepository
    extends AbstractNoOpManagementRepository<SharedPolicyGroup, String>
    implements SharedPolicyGroupRepository {

    @Override
    public Page<SharedPolicyGroup> search(SharedPolicyGroupCriteria criteria, Pageable pageable, Sortable sortable) {
        return new Page<>(List.of(), 0, 0, 0L);
    }

    @Override
    public Optional<SharedPolicyGroup> findByEnvironmentIdAndCrossId(String environmentId, String crossId) throws TechnicalException {
        return Optional.empty();
    }
}
