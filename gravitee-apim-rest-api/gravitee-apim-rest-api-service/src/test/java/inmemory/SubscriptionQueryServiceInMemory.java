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

import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SubscriptionQueryServiceInMemory implements SubscriptionQueryService, InMemoryAlternative<SubscriptionEntity> {

    private final ArrayList<SubscriptionEntity> storage;
    private PlanCrudService planCrudService;

    public SubscriptionQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    public SubscriptionQueryServiceInMemory(SubscriptionCrudServiceInMemory subscriptionCrudServiceInMemory) {
        storage = subscriptionCrudServiceInMemory.storage;
    }

    public SubscriptionQueryServiceInMemory(
        SubscriptionCrudServiceInMemory subscriptionCrudServiceInMemory,
        PlanCrudService planCrudService
    ) {
        storage = subscriptionCrudServiceInMemory.storage;
        this.planCrudService = planCrudService;
    }

    public void setPlanCrudService(PlanCrudService planCrudService) {
        this.planCrudService = planCrudService;
    }

    @Override
    public List<SubscriptionEntity> findExpiredSubscriptions() {
        return storage
            .stream()
            .filter(
                subscription ->
                    subscription.getStatus().equals(SubscriptionEntity.Status.ACCEPTED) &&
                    subscription.getEndingAt().isBefore(ZonedDateTime.now())
            )
            .toList();
    }

    @Override
    public List<SubscriptionEntity> findSubscriptionsByPlan(String planId) {
        return storage
            .stream()
            .filter(subscription -> subscription.getPlanId().equals(planId))
            .toList();
    }

    @Override
    public List<SubscriptionEntity> findActiveSubscriptionsByPlan(String planId) {
        return storage
            .stream()
            .filter(
                subscription ->
                    List.of(
                        SubscriptionEntity.Status.ACCEPTED,
                        SubscriptionEntity.Status.PENDING,
                        SubscriptionEntity.Status.PAUSED
                    ).contains(subscription.getStatus()) &&
                    subscription.getPlanId().equals(planId)
            )
            .toList();
    }

    @Override
    public List<SubscriptionEntity> findActiveByApplicationIdAndApiId(String applicationId, String apiId) {
        return storage
            .stream()
            .filter(
                subscription ->
                    List.of(
                        SubscriptionEntity.Status.ACCEPTED,
                        SubscriptionEntity.Status.PENDING,
                        SubscriptionEntity.Status.PAUSED
                    ).contains(subscription.getStatus()) &&
                    apiId.equals(subscription.getApiId()) &&
                    applicationId.equals(subscription.getApplicationId())
            )
            .toList();
    }

    @Override
    public List<SubscriptionEntity> findActiveByApplicationIdAndPlanSecurityTypes(
        String applicationId,
        Collection<String> planSecurityTypes
    ) {
        if (planCrudService == null) {
            throw new IllegalStateException("PlanCrudService must be set to use findActiveByApplicationIdAndPlanSecurityTypes");
        }

        Set<String> securityTypesSet = Set.copyOf(planSecurityTypes);

        return storage
            .stream()
            .filter(
                subscription ->
                    List.of(
                        SubscriptionEntity.Status.ACCEPTED,
                        SubscriptionEntity.Status.PENDING,
                        SubscriptionEntity.Status.PAUSED
                    ).contains(subscription.getStatus()) &&
                    applicationId.equals(subscription.getApplicationId())
            )
            .filter(subscription -> {
                try {
                    Plan plan = planCrudService.getById(subscription.getPlanId());
                    var planSecurity = plan.getPlanSecurity();
                    return planSecurity != null && securityTypesSet.contains(planSecurity.getType());
                } catch (Exception e) {
                    return false;
                }
            })
            .toList();
    }

    @Override
    public List<SubscriptionEntity> findActiveByApplicationIdAndReferenceIdAndReferenceType(
        String applicationId,
        String referenceId,
        SubscriptionReferenceType referenceType
    ) {
        return storage
            .stream()
            .filter(
                subscription ->
                    List.of(
                        SubscriptionEntity.Status.ACCEPTED,
                        SubscriptionEntity.Status.PENDING,
                        SubscriptionEntity.Status.PAUSED
                    ).contains(subscription.getStatus()) &&
                    referenceId.equals(subscription.getReferenceId()) &&
                    referenceType.equals(subscription.getReferenceType()) &&
                    applicationId.equals(subscription.getApplicationId())
            )
            .toList();
    }

    @Override
    public List<SubscriptionEntity> findAllByReferenceIdAndReferenceType(String referenceId, SubscriptionReferenceType referenceType) {
        return storage
            .stream()
            .filter(
                subscription -> referenceId.equals(subscription.getReferenceId()) && referenceType.equals(subscription.getReferenceType())
            )
            .toList();
    }

    @Override
    public Optional<SubscriptionEntity> findByIdAndReferenceIdAndReferenceType(
        String subscriptionId,
        String referenceId,
        SubscriptionReferenceType referenceType
    ) {
        return storage
            .stream()
            .filter(
                sub ->
                    sub.getId().equals(subscriptionId) &&
                    referenceId.equals(sub.getReferenceId()) &&
                    referenceType.equals(sub.getReferenceType())
            )
            .findFirst();
    }

    @Override
    public void initWith(List<SubscriptionEntity> items) {
        storage.clear();
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<SubscriptionEntity> storage() {
        return Collections.unmodifiableList(storage);
    }
}
