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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.mongodb.management.internal.model.PlanMongo;
import io.gravitee.repository.mongodb.management.internal.plan.PlanMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoPlanRepository implements PlanRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoPlanRepository.class);

    @Autowired
    private GraviteeMapper mapper;

    @Autowired
    private PlanMongoRepository internalPlanRepository;

    @Override
    public List<Plan> findByApisAndEnvironments(List<String> apiIds, Set<String> environments) {
        return internalPlanRepository.findByApiInAndEnvironments(apiIds, environments).stream().map(this::map).collect(Collectors.toList());
    }

    @Override
    public Set<Plan> findByApi(String apiId) {
        return internalPlanRepository.findByApi(apiId).stream().map(this::map).collect(Collectors.toSet());
    }

    @Override
    public Optional<Plan> findById(String plan) throws TechnicalException {
        PlanMongo planMongo = internalPlanRepository.findById(plan).orElse(null);
        return Optional.ofNullable(map(planMongo));
    }

    @Override
    public Plan create(Plan plan) throws TechnicalException {
        PlanMongo planMongo = map(plan);
        planMongo = internalPlanRepository.insert(planMongo);
        return map(planMongo);
    }

    @Override
    public Plan update(Plan plan) throws TechnicalException {
        if (plan == null || plan.getId() == null) {
            throw new IllegalStateException("Plan to update must have an id");
        }

        PlanMongo planMongo = internalPlanRepository.findById(plan.getId()).orElse(null);

        if (planMongo == null) {
            throw new IllegalStateException(String.format("No plan found with id [%s]", plan.getId()));
        }

        planMongo = map(plan);
        planMongo = internalPlanRepository.save(planMongo);
        return map(planMongo);
    }

    @Override
    public void delete(String plan) throws TechnicalException {
        internalPlanRepository.deleteById(plan);
    }

    private PlanMongo map(Plan plan) {
        return (plan == null) ? null : mapper.map(plan);
    }

    private Plan map(PlanMongo planMongo) {
        return (planMongo == null) ? null : mapper.map(planMongo);
    }

    @Override
    public Set<Plan> findAll() throws TechnicalException {
        return internalPlanRepository.findAll().stream().map(this::map).collect(Collectors.toSet());
    }

    @Override
    public Set<Plan> findByIdIn(Collection<String> ids) throws TechnicalException {
        try {
            Iterable<PlanMongo> plans = internalPlanRepository.findAllById(ids);
            return StreamSupport.stream(plans.spliterator(), false).map(this::map).collect(Collectors.toSet());
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find plans by id list", ex);
        }
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        LOGGER.debug("Delete plan by environment [{}]", environmentId);
        try {
            final var plans = internalPlanRepository.deleteByEnvironmentId(environmentId).stream().map(PlanMongo::getId).toList();
            LOGGER.debug("Delete plan by environment [{}] - Done", environmentId);
            return plans;
        } catch (Exception e) {
            LOGGER.error("Failed to delete plan by environment [{}]", environmentId, e);
            throw new TechnicalException("Failed to delete plan by environment", e);
        }
    }
}
