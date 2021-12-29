/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.bridge.client.management;

import io.gravitee.repository.bridge.client.utils.BodyCodecs;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpPlanRepository extends AbstractRepository implements PlanRepository {

    @Override
    public Optional<Plan> findById(String planId) throws TechnicalException {
        return blockingGet(get("/plans/" + planId, BodyCodecs.optional(Plan.class)).send()).payload();
    }

    @Override
    public Plan create(Plan item) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Plan update(Plan item) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public void delete(String s) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public List<Plan> findByApis(List<String> apiIds) throws TechnicalException {
        return blockingGet(post("/plans/", BodyCodecs.list(Plan.class)).send(apiIds)).payload();
    }

    @Override
    public Set<Plan> findByApi(String apiId) throws TechnicalException {
        return blockingGet(get("/apis/" + apiId + "/plans", BodyCodecs.set(Plan.class)).send()).payload();
    }

    @Override
    public Set<Plan> findAll() throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Plan> findByIdIn(Collection<String> ids) throws TechnicalException {
        throw new IllegalStateException();
    }
}
