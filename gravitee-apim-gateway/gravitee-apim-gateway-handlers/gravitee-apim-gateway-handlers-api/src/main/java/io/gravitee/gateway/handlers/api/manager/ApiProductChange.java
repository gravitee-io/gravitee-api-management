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
package io.gravitee.gateway.handlers.api.manager;

import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * What an incoming API Product changes against the version deployed on this gateway, so a deploy redoes only that.
 *
 * <p>Only plans this gateway serves have their subscriptions cached, so only those are loaded, and a plan that stops
 * being served is evicted like a removed one.</p>
 */
public final class ApiProductChange {

    private final ReactableApiProduct deployed;
    private final ReactableApiProduct incoming;
    private final List<Plan> deployedPlans;
    private final List<Plan> incomingPlans;
    private final Map<String, Plan> deployedPlansById;
    private final Map<String, Plan> incomingPlansById;
    private final Set<String> deployedApiIds;
    private final Set<String> incomingApiIds;
    private final Predicate<Plan> servedHere;

    private ApiProductChange(ReactableApiProduct deployed, ReactableApiProduct incoming, Predicate<Plan> servedHere) {
        this.deployed = deployed;
        this.incoming = incoming;
        this.deployedPlans = plansOf(deployed);
        this.incomingPlans = plansOf(incoming);
        this.deployedPlansById = byId(deployedPlans);
        this.incomingPlansById = byId(incomingPlans);
        this.deployedApiIds = apiIdsOf(deployed);
        this.incomingApiIds = apiIdsOf(incoming);
        this.servedHere = servedHere;
    }

    /**
     * @param deployed the version deployed on this gateway, or null on a first deployment
     * @param servedHere whether this gateway exposes a plan, which decides whether the product makes its APIs eligible
     */
    public static ApiProductChange between(ReactableApiProduct deployed, ReactableApiProduct incoming, Predicate<Plan> servedHere) {
        return new ApiProductChange(deployed, incoming, servedHere);
    }

    /**
     * The plans whose subscriptions must be loaded before the product is published.
     *
     * <p>A product subscription is only cached while the product has APIs, so all plans served here are loaded when
     * the first API joins. Otherwise added plans, and kept plans whose definition changed in anything but their flows,
     * which only the member APIs build from.</p>
     */
    public Set<String> planIdsToLoadSubscriptionsFor() {
        if (incomingApiIds.isEmpty()) {
            return Set.of();
        }
        Set<String> planIds = new HashSet<>();
        incomingPlansById.forEach((planId, incomingPlan) -> {
            Plan deployedPlan = deployedPlansById.get(planId);
            boolean toLoad =
                deployedApiIds.isEmpty() || deployedPlan == null || !withoutFlows(deployedPlan).equals(withoutFlows(incomingPlan));
            if (toLoad && servedHere.test(incomingPlan)) {
                planIds.add(planId);
            }
        });
        return Set.copyOf(planIds);
    }

    /** The plans whose subscriptions and API keys must be evicted: removed, or no longer served here. */
    public Set<String> planIdsToEvict() {
        Set<String> planIds = new HashSet<>();
        deployedPlansById.forEach((planId, deployedPlan) -> {
            Plan incomingPlan = incomingPlansById.get(planId);
            boolean removed = incomingPlan == null;
            boolean noLongerServed = !removed && servedHere.test(deployedPlan) && !servedHere.test(incomingPlan);
            if (removed || noLongerServed) {
                planIds.add(planId);
            }
        });
        return Set.copyOf(planIds);
    }

    /**
     * The loaded plans to evict when the load fails part-way: those the deployed version, which is put back, does not
     * cache. A kept plan it caches keeps its entries, which a partial reload only refreshed.
     */
    public Set<String> planIdsToEvictIfLoadFails() {
        Set<String> planIds = new HashSet<>(planIdsToLoadSubscriptionsFor());
        if (!deployedApiIds.isEmpty()) {
            deployedPlansById.forEach((planId, deployedPlan) -> {
                if (servedHere.test(deployedPlan)) {
                    planIds.remove(planId);
                }
            });
        }
        return Set.copyOf(planIds);
    }

    /**
     * The member APIs whose eligibility on this gateway may have changed.
     *
     * <p>An API is eligible through a product only while the product has a plan served here, so gaining the first
     * such plan or losing the last one concerns every API. Otherwise only the APIs that joined or left.</p>
     */
    public Set<String> apiIdsToResync() {
        if (hasPlanServedHere(deployedPlans) != hasPlanServedHere(incomingPlans)) {
            return union(deployedApiIds, incomingApiIds);
        }
        return union(difference(incomingApiIds, deployedApiIds), difference(deployedApiIds, incomingApiIds));
    }

    /**
     * Whether member APIs must rebuild what they build from the product: anything but the fields that are only indexed.
     * Plan order counts, since it breaks ties in the selection of a product plan.
     */
    public boolean affectsMemberApis() {
        return deployed == null || !withoutIndexOnlyFields(deployed).equals(withoutIndexOnlyFields(incoming));
    }

    /** The APIs of both versions, whose reactors build from the product. */
    public Set<String> memberApiIds() {
        return union(deployedApiIds, incomingApiIds);
    }

    private boolean hasPlanServedHere(List<Plan> plans) {
        return plans.stream().filter(Objects::nonNull).anyMatch(servedHere);
    }

    private static ReactableApiProduct withoutIndexOnlyFields(ReactableApiProduct product) {
        return product.toBuilder().deployedAt(null).name(null).description(null).version(null).analytics(null).tags(null).build();
    }

    private static Plan withoutFlows(Plan plan) {
        return plan.toBuilder().flows(null).build();
    }

    private static List<Plan> plansOf(ReactableApiProduct product) {
        return product == null || product.getPlans() == null ? List.of() : product.getPlans();
    }

    private static Set<String> apiIdsOf(ReactableApiProduct product) {
        return product == null || product.getApiIds() == null ? Set.of() : product.getApiIds();
    }

    private static Map<String, Plan> byId(List<Plan> plans) {
        Map<String, Plan> plansById = new LinkedHashMap<>();
        plans
            .stream()
            .filter(Objects::nonNull)
            .forEach(plan -> plansById.put(plan.getId(), plan));
        return plansById;
    }

    private static Set<String> difference(Set<String> from, Set<String> minus) {
        Set<String> result = new HashSet<>(from);
        result.removeAll(minus);
        return Set.copyOf(result);
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        Set<String> result = new HashSet<>(first);
        result.addAll(second);
        return Set.copyOf(result);
    }
}
