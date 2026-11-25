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
import java.util.Collection;

/**
 * The clauses every search of the decisions data stream shares, whatever the decision point.
 *
 * @author GraviteeSource Team
 */
public final class DecisionQueryClauses {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FROM = "from";
    private static final String ORDER = "order";
    private static final String ASC = "asc";
    private static final String UNMAPPED_TYPE = "unmapped_type";
    private static final String KEYWORD = "keyword";
    private static final String WILDCARD = "wildcard";
    private static final String VALUE = "value";
    private static final String CASE_INSENSITIVE = "case_insensitive";

    private DecisionQueryClauses() {}

    /**
     * The organization, the environment and the decision point type are part of the predicate rather than a
     * post-filter: they scope the search the same way the caller's permission does, so a decision of another tenant
     * or of another kind of decision point cannot be read through it.
     */
    public static ArrayNode scopedFilters(QueryContext queryContext, String decisionPointType) {
        ArrayNode filters = MAPPER.createArrayNode();
        addTerm(filters, DecisionReportFields.ORG_ID, queryContext.getOrgId());
        addTerm(filters, DecisionReportFields.ENV_ID, queryContext.getEnvId());
        addTerm(filters, DecisionReportFields.DECISION_POINT_TYPE, decisionPointType);
        return filters;
    }

    public static void addTerm(ArrayNode filters, String field, String value) {
        filters.add(MAPPER.createObjectNode().set(TERM, MAPPER.createObjectNode().put(field, value)));
    }

    /** Added even when empty: an empty terms clause matches no document, which is what an empty selection means. */
    public static void addTerms(ArrayNode filters, String field, Collection<String> values) {
        ArrayNode node = MAPPER.createArrayNode();
        values.forEach(node::add);
        filters.add(MAPPER.createObjectNode().set(TERMS, MAPPER.createObjectNode().set(field, node)));
    }

    /** Omitted entirely when nothing is selected: an empty terms clause matches no document. */
    public static void addTermsIfAny(ArrayNode filters, String field, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        addTerms(filters, field, values);
    }

    /**
     * Matches a fragment of the field, ignoring case.
     *
     * <p>The needle is escaped. {@code *} and {@code ?} are wildcard syntax, so an unescaped one turns
     * the filter into no filter at all while the caller still sees an active filter chip.
     */
    public static void addContainsIfAny(ArrayNode filters, String field, String needle) {
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

    public static void addTimeRange(ArrayNode filters, Long from, Long to) {
        if (from == null && to == null) {
            return;
        }
        ObjectNode bounds = MAPPER.createObjectNode();
        if (from != null) {
            bounds.put(GTE, from);
        }
        if (to != null) {
            bounds.put(LTE, to);
        }
        filters.add(MAPPER.createObjectNode().set(RANGE, MAPPER.createObjectNode().set(TIMESTAMP, bounds)));
    }

    /** One page of the matching decisions, newest first, with the total count of matches. */
    public static String newestFirstPage(ArrayNode filters, int page, int size) {
        ObjectNode root = filtered(filters);
        root.put(FROM, (page - 1) * size);
        root.put(SIZE, size);
        root.put(TRACK_TOTAL_HITS, true);
        // event-id breaks ties: a batch stamps every decision with the same millisecond, and ordering
        // within a tie is not stable across shards, so paging without it repeats and skips rows.
        root.set(
            SORT,
            MAPPER.createArrayNode()
                .add(MAPPER.createObjectNode().set(TIMESTAMP, MAPPER.createObjectNode().put(ORDER, DESC)))
                .add(
                    MAPPER.createObjectNode().set(
                        DecisionReportFields.EVENT_ID,
                        MAPPER.createObjectNode().put(ORDER, ASC).put(UNMAPPED_TYPE, KEYWORD)
                    )
                )
        );
        return root.toString();
    }

    public static String singleHit(ArrayNode filters) {
        ObjectNode root = filtered(filters);
        root.put(SIZE, 1);
        return root.toString();
    }

    private static ObjectNode filtered(ArrayNode filters) {
        ObjectNode root = MAPPER.createObjectNode();
        root.set(QUERY, MAPPER.createObjectNode().set(BOOL, MAPPER.createObjectNode().set(FILTER, filters)));
        return root;
    }
}
