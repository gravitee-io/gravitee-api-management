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
package io.gravitee.management.service;

import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.NewPlanEntity;
import io.gravitee.management.model.PlanEntity;
import io.gravitee.management.model.UpdatePlanEntity;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.User;

import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface PlanService {

    PlanEntity findById(String plan);

    Set<PlanEntity> findByApi(String api);

    PlanEntity create(NewPlanEntity plan);

    PlanEntity update(UpdatePlanEntity plan);

    PlanEntity close(String plan);

    void delete(String plan);

    PlanEntity publish(String plan);
}
