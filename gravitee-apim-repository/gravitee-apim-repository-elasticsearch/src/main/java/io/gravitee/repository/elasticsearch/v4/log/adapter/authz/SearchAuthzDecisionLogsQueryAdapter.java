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
package io.gravitee.repository.elasticsearch.v4.log.adapter.authz;

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
import io.gravitee.repository.log.v4.model.authz.AuthzDecisionLogQuery;
import java.util.Set;

/**
 * @author GraviteeSource Team
 */
public final class SearchAuthzDecisionLogsQueryAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FROM = "from";
    private static final String ORDER = "order";
    private static final String ASC = "asc";
    private static final String UNMAPPED_TYPE = "unmapped_type";
    private static final String KEYWORD = "keyword";
    private static final String NESTED = "nested";
    private static final String IGNORE_UNMAPPED = "ignore_unmapped";
    private static final String PATH = "path";
    private static final String WILDCARD = "wildcard";
    private static final String VALUE = "value";
    private static final String CASE_INSENSITIVE = "case_insensitive";

    private SearchAuthzDecisionLogsQueryAdapter() {}

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
     * Nested objects are indexed as separate Lucene documents, so a plain terms clause on the
     * sub-field matches nothing at all — the wrapper is what makes the field reachable.
     *
     * <p>{@code ignore_unmapped} matters because the search spans every backing index of the stream,
     * including ones written before decisions carried policy names. Without it those shards fail the
     * whole query, and the response model carries no {@code _shards}, so the failure would be silent.
     */
    private static void addNestedTermsIfAny(ArrayNode filters, String path, String field, Set<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        ArrayNode node = MAPPER.createArrayNode();
        values.forEach(node::add);
        ObjectNode inner = MAPPER.createObjectNode().set(TERMS, MAPPER.createObjectNode().set(path + "." + field, node));
        ObjectNode nested = MAPPER.createObjectNode();
        nested.put(PATH, path);
        nested.put(IGNORE_UNMAPPED, true);
        nested.set(QUERY, inner);
        filters.add(MAPPER.createObjectNode().set(NESTED, nested));
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

    public static String adapt(AuthzDecisionLogQuery query) {
        ArrayNode filters = MAPPER.createArrayNode();
        ArrayNode apiIds = MAPPER.createArrayNode();
        query.getApiIds().forEach(apiIds::add);
        filters.add(MAPPER.createObjectNode().set(TERMS, MAPPER.createObjectNode().set(AuthzDecisionLogFields.API_ID, apiIds)));

        // Every one of these is a keyword field, so an exact terms clause is the whole translation.
        addTermsIfAny(filters, AuthzDecisionLogFields.DECISION, query.getDecisions());
        addTermsIfAny(filters, AuthzDecisionLogFields.SUBJECT_ID, query.getSubjectIds());
        addTermsIfAny(filters, AuthzDecisionLogFields.ACTION, query.getActions());
        addTermsIfAny(filters, AuthzDecisionLogFields.RESOURCE_ID, query.getResourceIds());
        addTermsIfAny(filters, AuthzDecisionLogFields.CALLER, query.getCallers());
        addTermsIfAny(filters, AuthzDecisionLogFields.STATUS, query.getStatuses());
        addTermsIfAny(filters, AuthzDecisionLogFields.OPERATION, query.getOperations());
        addTermsIfAny(filters, AuthzDecisionLogFields.TARGET_PDP_ID, query.getTargetPdpIds());
        addTermsIfAny(filters, AuthzDecisionLogFields.POLICY_GENERATION, query.getPolicyGenerations());
        addTermsIfAny(filters, AuthzDecisionLogFields.REQUEST_ID, query.getRequestIds());
        addNestedTermsIfAny(
            filters,
            AuthzDecisionLogFields.MATCHED_POLICIES,
            AuthzDecisionLogFields.MATCHED_POLICY_NAME,
            query.getMatchedPolicyNames()
        );
        addContainsIfAny(filters, AuthzDecisionLogFields.REASONS, query.getReasonContains());

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

        ObjectNode root = MAPPER.createObjectNode();
        root.set(QUERY, MAPPER.createObjectNode().set(BOOL, MAPPER.createObjectNode().set(FILTER, filters)));
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
                        AuthzDecisionLogFields.EVENT_ID,
                        MAPPER.createObjectNode().put(ORDER, ASC).put(UNMAPPED_TYPE, KEYWORD)
                    )
                )
        );

        return root.toString();
    }
}
