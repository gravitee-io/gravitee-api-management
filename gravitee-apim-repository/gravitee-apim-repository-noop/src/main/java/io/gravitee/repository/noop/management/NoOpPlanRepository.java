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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpPlanRepository extends AbstractNoOpManagementRepository<Plan, String> implements PlanRepository {

    @Override
    public List<Plan> findByApisAndEnvironments(final List<String> apiIds, final Set<String> environments) throws TechnicalException {
        return List.of();
    }

    @Override
    public Set<Plan> findByApi(String apiId) throws TechnicalException {
        return Set.of();
    }

    @Override
    public Set<Plan> findByIdIn(final Collection<String> ids) throws TechnicalException {
        return Set.of();
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        return List.of();
    }
}
