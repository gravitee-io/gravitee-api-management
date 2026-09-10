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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;

/**
 * Field resolver for <b>human approval</b> decisions, stored in the {@code decisions} data stream.
 *
 * <p>Two things separate this family from the others:
 *
 * <ol>
 *   <li><b>The index is shared.</b> Every decision point that reports a {@code DecisionEventMetrics} —
 *       Guardian, external approval, human approval — writes to {@code decisions}. Counting or summing
 *       without {@link #DECISION_POINT_TYPE_FIELD} would silently mix them, so the term is applied to
 *       the root query of every request of this family rather than per metric.
 *   <li><b>Each consultation writes twice</b>, once when the hold starts ({@code phase: REQUESTED}) and
 *       once when it ends ({@code phase: RESOLVED}). Only the second carries the outcome and the cost,
 *       so {@link #PHASE_FIELD} is pinned to {@code RESOLVED} on the root query too — without it every
 *       settled consultation would be counted twice.
 * </ol>
 *
 * <p>The cost itself is not a top-level field: the policy reports it as an additional metric, and the
 * index's {@code additional-metrics.double_*} dynamic template types it as a double. That is what lets
 * a new figure travel from a plugin to a dashboard without a mapping change — the same route the LLM
 * and MCP proxies already take on the request index.
 */
public class HumanApprovalFieldResolver implements FieldResolver {

    public static final String DECISION_POINT_TYPE_FIELD = "decision-point-type";
    public static final String DECISION_POINT_TYPE_HUMAN_APPROVAL = "human-approval";
    public static final String PHASE_FIELD = "phase";
    public static final String PHASE_RESOLVED = "RESOLVED";

    private static final String CASE_ID = "case-id";
    /**
     * A contract with the write side: the human-approval policy publishes the figure under this name
     * ({@code DecisionEvents.COST_METRIC}). Nothing on either side fails if one is renamed — the total
     * just reads zero — so the two move together or not at all.
     */
    private static final String COST = "additional-metrics.double_human-approval_cost";
    private static final String API_ID = "api-id";
    private static final String APP_ID = "app-id";
    private static final String PLAN_ID = "plan-id";
    private static final String GATEWAY_ID = "gw-id";
    private static final String VERDICT = "verdict";
    private static final String RESOURCE_ID = "resource-id";

    /**
     * Both metrics count by the <em>absence</em> of their field rather than by a filter, which is what
     * keeps this family's aggregation tree flat.
     *
     * <p>{@code case-id} is written only once the broker has actually opened an approval, so counting it
     * excludes the calls the policy inspected and let through untouched — the overwhelming majority, and
     * the reason a plain document count would be meaningless here. {@code additional-metrics.double_*}
     * carries the charge, and is written only for approvals a person settled under an environment that
     * had a rate.
     *
     * <p>That is why both metrics exist. Spend alone cannot say why it is zero: no approvals happened,
     * or approvals happened under an environment with no rate set. {@code HUMAN_APPROVALS} answers that,
     * and it is the figure a reader needs before trusting a total of zero.
     */
    @Override
    public String fromMetric(Metric metric) {
        return switch (metric) {
            case HUMAN_APPROVALS -> CASE_ID;
            case HUMAN_APPROVAL_COST -> COST;
            default -> throw new UnsupportedOperationException("HumanApprovalFieldResolver does not support metric " + metric);
        };
    }

    @Override
    public String fromFilter(Filter filter) {
        return fromDimension(filter.name().name(), "filter");
    }

    @Override
    public String fromFacet(Facet facet) {
        return fromDimension(facet.name(), "facet");
    }

    /**
     * Filters and facets name the same six dimensions and resolve to the same fields, so both go
     * through here. They arrive as two unrelated enums, which Java cannot switch over jointly —
     * matching on the name keeps the mapping in one place instead of letting two identical switches
     * drift apart, and costs no exhaustiveness checking since both switches needed a default anyway.
     *
     * @param kind "filter" or "facet", so the failure says which side of the query is at fault
     */
    private String fromDimension(String dimension, String kind) {
        return switch (dimension) {
            case "API" -> API_ID;
            case "APPLICATION" -> APP_ID;
            case "PLAN" -> PLAN_ID;
            case "GATEWAY" -> GATEWAY_ID;
            case "HUMAN_APPROVAL_VERDICT" -> VERDICT;
            case "HUMAN_APPROVAL_TOOL" -> RESOURCE_ID;
            default -> throw new UnsupportedOperationException(
                "HumanApprovalFieldResolver does not support " +
                    kind +
                    " '" +
                    dimension +
                    "' — supported dimensions: API, APPLICATION, PLAN, GATEWAY, HUMAN_APPROVAL_VERDICT, HUMAN_APPROVAL_TOOL"
            );
        };
    }
}
