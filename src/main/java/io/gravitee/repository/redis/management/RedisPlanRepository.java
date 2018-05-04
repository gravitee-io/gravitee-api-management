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
package io.gravitee.repository.redis.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.redis.management.internal.PlanRedisRepository;
import io.gravitee.repository.redis.management.model.RedisPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisPlanRepository implements PlanRepository {

    @Autowired
    private PlanRedisRepository planRedisRepository;

    @Override
    public Set<Plan> findByApi(String api) {
        return planRedisRepository.findByApi(api)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<Plan> findById(String plan) throws TechnicalException {
        RedisPlan redisPlan = planRedisRepository.find(plan);
        return Optional.ofNullable(convert(redisPlan));
    }

    @Override
    public Plan create(Plan plan) throws TechnicalException {
        RedisPlan redisPlan = planRedisRepository.saveOrUpdate(convert(plan));
        return convert(redisPlan);
    }

    @Override
    public Plan update(Plan plan) throws TechnicalException {
        if (plan == null || plan.getId() == null) {
            throw new IllegalStateException("Plan to update must have an id");
        }

        RedisPlan redisPlan = planRedisRepository.find(plan.getId());

        if (redisPlan == null) {
            throw new IllegalStateException(String.format("No plan found with id [%s]", plan.getId()));
        }

        RedisPlan redisPlanUpdated = planRedisRepository.saveOrUpdate(convert(plan));
        return convert(redisPlanUpdated);
    }

    @Override
    public void delete(String plan) throws TechnicalException {
        planRedisRepository.delete(plan);
    }

    private Plan convert(RedisPlan redisPlan) {
        if (redisPlan == null) {
            return null;
        }

        Plan plan = new Plan();
        plan.setId(redisPlan.getId());
        plan.setName(redisPlan.getName());
        plan.setOrder(redisPlan.getOrder());

        if (redisPlan.getType() != null) {
            plan.setType(Plan.PlanType.valueOf(redisPlan.getType()));
        }

        plan.setCharacteristics(redisPlan.getCharacteristics());
        plan.setApis(redisPlan.getApis());
        plan.setDescription(redisPlan.getDescription());
        plan.setDefinition(redisPlan.getDefinition());
        plan.setExcludedGroups(redisPlan.getExcludedGroups());

        if (redisPlan.getValidation() != null) {
            plan.setValidation(Plan.PlanValidationType.valueOf(redisPlan.getValidation()));
        }

        if (redisPlan.getCreatedAt() != 0) {
            plan.setCreatedAt(new Date(redisPlan.getCreatedAt()));
        }

        if (redisPlan.getUpdatedAt() != 0) {
            plan.setUpdatedAt(new Date(redisPlan.getUpdatedAt()));
        }

        if (redisPlan.getStatus() != null) {
            plan.setStatus(Plan.Status.valueOf(redisPlan.getStatus()));
        }

        if (redisPlan.getSecurity() != null) {
            plan.setSecurity(Plan.PlanSecurityType.valueOf(redisPlan.getSecurity()));
        }

        if (redisPlan.getPublishedAt() != 0) {
            plan.setPublishedAt(new Date(redisPlan.getPublishedAt()));
        }

        if (redisPlan.getClosedAt() != 0) {
            plan.setClosedAt(new Date(redisPlan.getClosedAt()));
        }
        plan.setSecurityDefinition(redisPlan.getSecurityDefinition());
        return plan;
    }

    private RedisPlan convert(Plan plan) {
        RedisPlan redisPlan = new RedisPlan();
        redisPlan.setId(plan.getId());
        redisPlan.setName(plan.getName());
        redisPlan.setOrder(plan.getOrder());

        if (plan.getType() != null) {
            redisPlan.setType(plan.getType().name());
        }

        redisPlan.setCharacteristics(plan.getCharacteristics());
        redisPlan.setApis(plan.getApis());
        redisPlan.setDescription(plan.getDescription());
        redisPlan.setDefinition(plan.getDefinition());
        redisPlan.setExcludedGroups(plan.getExcludedGroups());

        if (plan.getValidation() != null) {
            redisPlan.setValidation(plan.getValidation().name());
        }

        if (plan.getCreatedAt() != null) {
            redisPlan.setCreatedAt(plan.getCreatedAt().getTime());
        }

        if (plan.getUpdatedAt() != null) {
            redisPlan.setUpdatedAt(plan.getUpdatedAt().getTime());
        }

        if (plan.getStatus() != null) {
            redisPlan.setStatus(plan.getStatus().name());
        }

        if (plan.getSecurity() != null) {
            redisPlan.setSecurity(plan.getSecurity().name());
        }

        if (plan.getClosedAt() != null) {
            redisPlan.setClosedAt(plan.getClosedAt().getTime());
        }

        if (plan.getPublishedAt() != null) {
            redisPlan.setPublishedAt(plan.getPublishedAt().getTime());
        }
        redisPlan.setSecurityDefinition(plan.getSecurityDefinition());

        return redisPlan;
    }
}
