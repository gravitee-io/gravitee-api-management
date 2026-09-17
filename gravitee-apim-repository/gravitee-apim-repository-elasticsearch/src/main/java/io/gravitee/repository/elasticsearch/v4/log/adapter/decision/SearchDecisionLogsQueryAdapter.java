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
package io.gravitee.repository.elasticsearch.v4.log.adapter.decision;

import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SORT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.BOOL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.FILTER;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.MUST_NOT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.QUERY;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.SIZE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.TIMESTAMP;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.TRACK_TOTAL_HITS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.GTE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.LTE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.RANGE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.TERM;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Sort.DESC;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Tokens.TERMS;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.model.decision.DecisionLogQuery;
import java.util.Set;

/**
 * Translates a decision search into an Elasticsearch query.
 *
 * <p>Four terms are on every query this builds, and none comes from the caller's filters:
 *
 * <ol>
 *   <li>{@code org-id} and {@code env-id}, from the query context, because the data stream is shared by
 *       every environment — the reporter stamps the tenant on the document, it never splits the index.
 *       The api is an optional filter here, so without these terms a search with no api restriction
 *       reads the whole cluster.
 *   <li>{@code decision-point-type}, because the {@code decisions} data stream is shared by every kind of
 *       point — a guardian verdict, a human approval and an external approval are the same document shape
 *       in the same index. Without it a "guardian activity" table quietly lists other people's decisions.
 *   <li>{@code phase: RESOLVED}, because a point that holds a call writes twice: once when the hold starts
 *       and once when it ends, and only the second carries the outcome. Without it one settled consultation
 *       shows as two rows, one of them still pending.
 * </ol>
 *
 * <p>The caller's exclusions become a {@code must_not} beside that {@code filter} in the same {@code bool},
 * so a value both asked for and ruled out is ruled out — Elasticsearch subtracts the negation from what the
 * filter kept.
 *
 * @author GraviteeSource Team
 */
public final class SearchDecisionLogsQueryAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FROM = "from";
    private static final String ORDER = "order";
    private static final String ASC = "asc";
    private static final String UNMAPPED_TYPE = "unmapped_type";
    private static final String KEYWORD = "keyword";
    private static final String WILDCARD = "wildcard";
    private static final String VALUE = "value";
    private static final String CASE_INSENSITIVE = "case_insensitive";

    private SearchDecisionLogsQueryAdapter() {}

    private static void addTerm(ArrayNode filters, String field, String value) {
        filters.add(MAPPER.createObjectNode().set(TERM, MAPPER.createObjectNode().put(field, value)));
    }

    /** Omitted entirely when nothing is selected: an empty terms clause matches no document. */
    private static void addTermsIfAny(ArrayNode filters, String field, Set<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        ArrayNode node = MAPPER.createArrayNode();
        values.forEach(node::add);
        filters.add(MAPPER.createObjectNode().set(TERMS, MAPPER.createObjectNode().set(field, node)));
    }

    /**
     * Reasons are sentences: the user filters on a fragment, never the whole string.
     *
     * <p>The needle is escaped. {@code *} and {@code ?} are wildcard syntax, so an unescaped one turns
     * the filter into no filter at all while the caller still sees an active filter chip.
     */
    private static void addContainsIfAny(ArrayNode filters, String field, String needle) {
        if (needle == null || needle.isBlank()) {
            return;
        }
        ObjectNode wildcard = MAPPER.createObjectNode();
        wildcard.put(VALUE, "*" + escapeWildcard(needle) + "*");
        wildcard.put(CASE_INSENSITIVE, true);
        filters.add(MAPPER.createObjectNode().set(WILDCARD, MAPPER.createObjectNode().set(field, wildcard)));
    }

    /** Backslash first, or the escapes inserted below would be escaped in turn. */
    private static String escapeWildcard(String needle) {
        return needle.replace("\\", "\\\\").replace("*", "\\*").replace("?", "\\?");
    }

    public static String adapt(QueryContext queryContext, DecisionLogQuery query) {
        ArrayNode filters = MAPPER.createArrayNode();
        addTerm(filters, DecisionLogFields.ORG_ID, queryContext.getOrgId());
        addTerm(filters, DecisionLogFields.ENV_ID, queryContext.getEnvId());
        addTerm(filters, DecisionLogFields.DECISION_POINT_TYPE, query.getDecisionPointType());
        addTerm(filters, DecisionLogFields.PHASE, DecisionLogFields.PHASE_RESOLVED);

        // Every one of these is a keyword field, so an exact terms clause is the whole translation.
        addTermsIfAny(filters, DecisionLogFields.API_ID, query.getApiIds());
        addTermsIfAny(filters, DecisionLogFields.APP_ID, query.getApplicationIds());
        addTermsIfAny(filters, DecisionLogFields.PLAN_ID, query.getPlanIds());
        addTermsIfAny(filters, DecisionLogFields.DECISION_POINT_ID, query.getDecisionPointIds());
        addTermsIfAny(filters, DecisionLogFields.CHECKPOINT, query.getCheckpoints());
        addTermsIfAny(filters, DecisionLogFields.CALLER, query.getCallers());
        addTermsIfAny(filters, DecisionLogFields.OUTCOME, query.getOutcomes());
        addTermsIfAny(filters, DecisionLogFields.ENFORCED, query.getEnforcements());
        addTermsIfAny(filters, DecisionLogFields.VERDICT, query.getVerdicts());
        addTermsIfAny(filters, DecisionLogFields.STATUS, query.getStatuses());
        addTermsIfAny(filters, DecisionLogFields.SUBJECT_ID, query.getSubjectIds());
        addTermsIfAny(filters, DecisionLogFields.ACTOR_ID, query.getActorIds());
        addTermsIfAny(filters, DecisionLogFields.ACTION, query.getActions());
        addTermsIfAny(filters, DecisionLogFields.RESOURCE_ID, query.getResourceIds());
        addTermsIfAny(filters, DecisionLogFields.CASE_ID, query.getCaseIds());
        addTermsIfAny(filters, DecisionLogFields.REQUEST_ID, query.getRequestIds());
        addTermsIfAny(filters, DecisionLogFields.TRACE_ID, query.getTraceIds());
        addContainsIfAny(filters, DecisionLogFields.REASONS, query.getReasonContains());

        // The same terms clauses, on the other side of the bool. An empty exclusion is dropped by
        // addTermsIfAny, which is exactly the wanted reading: ruling out nothing, not ruling out
        // everything — the opposite of what an empty inclusion means.
        ArrayNode exclusions = MAPPER.createArrayNode();
        addTermsIfAny(exclusions, DecisionLogFields.API_ID, query.getExcludedApiIds());
        addTermsIfAny(exclusions, DecisionLogFields.APP_ID, query.getExcludedApplicationIds());
        addTermsIfAny(exclusions, DecisionLogFields.DECISION_POINT_ID, query.getExcludedDecisionPointIds());
        addTermsIfAny(exclusions, DecisionLogFields.OUTCOME, query.getExcludedOutcomes());

        if (query.getFrom() != null || query.getTo() != null) {
            ObjectNode bounds = MAPPER.createObjectNode();
            if (query.getFrom() != null) {
                bounds.put(GTE, query.getFrom());
            }
            if (query.getTo() != null) {
                bounds.put(LTE, query.getTo());
            }
            filters.add(MAPPER.createObjectNode().set(RANGE, MAPPER.createObjectNode().set(TIMESTAMP, bounds)));
        }

        ObjectNode bool = MAPPER.createObjectNode();
        bool.set(FILTER, filters);
        if (!exclusions.isEmpty()) {
            // Left out when nothing is excluded: an empty must_not is noise in the query the cluster logs.
            bool.set(MUST_NOT, exclusions);
        }

        ObjectNode root = MAPPER.createObjectNode();
        root.set(QUERY, MAPPER.createObjectNode().set(BOOL, bool));
        root.put(FROM, (query.getPage() - 1) * query.getSize());
        root.put(SIZE, query.getSize());
        root.put(TRACK_TOTAL_HITS, true);
        // event-id breaks ties: a batch stamps every decision with the same millisecond, and ordering
        // within a tie is not stable across shards, so paging without it repeats and skips rows.
        root.set(
            SORT,
            MAPPER.createArrayNode()
                .add(MAPPER.createObjectNode().set(TIMESTAMP, MAPPER.createObjectNode().put(ORDER, DESC)))
                .add(
                    MAPPER.createObjectNode().set(
                        DecisionLogFields.EVENT_ID,
                        MAPPER.createObjectNode().put(ORDER, ASC).put(UNMAPPED_TYPE, KEYWORD)
                    )
                )
        );

        return root.toString();
    }
}
