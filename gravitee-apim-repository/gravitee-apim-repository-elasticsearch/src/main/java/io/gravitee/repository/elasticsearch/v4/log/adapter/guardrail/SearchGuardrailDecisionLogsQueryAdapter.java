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
package io.gravitee.repository.elasticsearch.v4.log.adapter.guardrail;

import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SORT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.BOOL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.FILTER;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.QUERY;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.SIZE;
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
import io.gravitee.repository.elasticsearch.utils.WildcardValues;
import io.gravitee.repository.log.v4.model.guardrail.GuardrailDecisionLogQuery;
import java.util.Set;

/**
 * @author GraviteeSource Team
 */
public final class SearchGuardrailDecisionLogsQueryAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FROM = "from";
    private static final String ORDER = "order";
    private static final String ASC = "asc";
    private static final String UNMAPPED_TYPE = "unmapped_type";
    private static final String KEYWORD = "keyword";
    private static final String SHOULD = "should";
    private static final String MINIMUM_SHOULD_MATCH = "minimum_should_match";
    private static final String WILDCARD = "wildcard";
    private static final String PREFIX = "prefix";
    private static final String VALUE = "value";
    private static final String CASE_INSENSITIVE = "case_insensitive";
    private static final String NESTED = "nested";
    private static final String PATH = "path";
    private static final String IGNORE_UNMAPPED = "ignore_unmapped";

    private SearchGuardrailDecisionLogsQueryAdapter() {}

    /**
     * @param maxResultWindow the configured {@code max_result_window}: a page reaching past it is refused here rather than
     *     by Elasticsearch, so the caller gets a 400 naming the page instead of a 500.
     * @throws IllegalArgumentException when the page reaches past the window. Refused rather than clamped: a clamped
     *     page would answer with other rows and look like success.
     */
    public static String adapt(QueryContext queryContext, GuardrailDecisionLogQuery query, int maxResultWindow) {
        // long: a page number a client is free to send overflows the int product, and a negative from would slip past.
        long from = (long) (query.getPage() - 1) * query.getSize();
        if (from + query.getSize() > maxResultWindow) {
            throw new IllegalArgumentException(
                "Page %d of %d goes past the %d Guardrail decisions a search serves. Narrow the search instead.".formatted(
                    query.getPage(),
                    query.getSize(),
                    maxResultWindow
                )
            );
        }
        ArrayNode filters = MAPPER.createArrayNode();
        addTerm(filters, GuardrailDecisionLogFields.ORG_ID, queryContext.getOrgId());
        addTerm(filters, GuardrailDecisionLogFields.ENV_ID, queryContext.getEnvId());
        addTerm(filters, GuardrailDecisionLogFields.DECISION_POINT_TYPE, GuardrailDecisionLogFields.GUARDRAIL);
        addTerm(filters, GuardrailDecisionLogFields.PHASE, GuardrailDecisionLogFields.RESOLVED);
        addTerms(filters, GuardrailDecisionLogFields.API_ID, query.getApiIds());
        addTermsIfAny(filters, GuardrailDecisionLogFields.GUARDRAIL_ID, query.getGuardrailIds());
        addTermsIfAny(filters, GuardrailDecisionLogFields.OUTCOME, query.getOutcomes());
        addTextIfAny(filters, query.getText(), query.getTextApiIds());

        if (query.getFrom() != null || query.getTo() != null) {
            ObjectNode bounds = MAPPER.createObjectNode();
            if (query.getFrom() != null) {
                bounds.put(GTE, query.getFrom());
            }
            if (query.getTo() != null) {
                bounds.put(LTE, query.getTo());
            }
            filters.add(MAPPER.createObjectNode().set(RANGE, MAPPER.createObjectNode().set(GuardrailDecisionLogFields.TIMESTAMP, bounds)));
        }

        ObjectNode root = MAPPER.createObjectNode();
        root.set(QUERY, MAPPER.createObjectNode().set(BOOL, MAPPER.createObjectNode().set(FILTER, filters)));
        root.put(FROM, from);
        root.put(SIZE, query.getSize());
        root.put(TRACK_TOTAL_HITS, true);
        // event-id breaks ties: the request and the response run of one call can share a millisecond, and ordering
        // within a tie is not stable across shards, so paging without it repeats and skips rows.
        root.set(
            SORT,
            MAPPER.createArrayNode()
                .add(MAPPER.createObjectNode().set(GuardrailDecisionLogFields.TIMESTAMP, MAPPER.createObjectNode().put(ORDER, DESC)))
                .add(
                    MAPPER.createObjectNode().set(
                        GuardrailDecisionLogFields.EVENT_ID,
                        MAPPER.createObjectNode().put(ORDER, ASC).put(UNMAPPED_TYPE, KEYWORD)
                    )
                )
        );
        return root.toString();
    }

    /** Omitted entirely when nothing is selected: an empty terms clause matches no document. */
    private static void addTermsIfAny(ArrayNode filters, String field, Set<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        addTerms(filters, field, values);
    }

    /**
     * One search box over what a row shows: the request, the guardrail policy and its rules, the api. Any of them matching is
     * enough. The guardrail policy is reached through the reasons, each of which starts with its name. Reasons are sentences and rule names are words of them, so both are matched on a fragment. The request id
     * is matched on its start: it is one value per call, and a fragment anywhere in it would walk every term of the
     * index.
     *
     * <p>Matched rules are nested documents: a plain clause on their name matches nothing, the nested wrapper is what
     * reaches it. {@code ignore_unmapped} keeps an index without that mapping from failing the whole query.
     */
    private static void addTextIfAny(ArrayNode filters, String text, Set<String> textApiIds) {
        if (text == null || text.isBlank()) {
            return;
        }
        var trimmed = text.trim();
        var fragment = WildcardValues.containing(trimmed);
        ArrayNode should = MAPPER.createArrayNode();
        should.add(prefix(GuardrailDecisionLogFields.REQUEST_ID, trimmed));
        should.add(wildcard(GuardrailDecisionLogFields.REASONS, fragment));
        ObjectNode nested = MAPPER.createObjectNode();
        nested.put(PATH, GuardrailDecisionLogFields.MATCHED_RULES);
        nested.put(IGNORE_UNMAPPED, true);
        nested.set(
            QUERY,
            wildcard(GuardrailDecisionLogFields.MATCHED_RULES + "." + GuardrailDecisionLogFields.MATCHED_RULE_NAME, fragment)
        );
        should.add(MAPPER.createObjectNode().set(NESTED, nested));
        if (textApiIds != null && !textApiIds.isEmpty()) {
            ArrayNode apiIds = MAPPER.createArrayNode();
            textApiIds.forEach(apiIds::add);
            should.add(MAPPER.createObjectNode().set(TERMS, MAPPER.createObjectNode().set(GuardrailDecisionLogFields.API_ID, apiIds)));
        }
        ObjectNode bool = MAPPER.createObjectNode();
        bool.set(SHOULD, should);
        bool.put(MINIMUM_SHOULD_MATCH, 1);
        filters.add(MAPPER.createObjectNode().set(BOOL, bool));
    }

    private static ObjectNode wildcard(String field, String fragment) {
        ObjectNode wildcard = MAPPER.createObjectNode();
        wildcard.put(VALUE, fragment);
        wildcard.put(CASE_INSENSITIVE, true);
        return MAPPER.createObjectNode().set(WILDCARD, MAPPER.createObjectNode().set(field, wildcard));
    }

    private static ObjectNode prefix(String field, String start) {
        ObjectNode prefix = MAPPER.createObjectNode();
        prefix.put(VALUE, start);
        prefix.put(CASE_INSENSITIVE, true);
        return MAPPER.createObjectNode().set(PREFIX, MAPPER.createObjectNode().set(field, prefix));
    }

    private static void addTerm(ArrayNode filters, String field, String value) {
        filters.add(MAPPER.createObjectNode().set(TERM, MAPPER.createObjectNode().put(field, value)));
    }

    private static void addTerms(ArrayNode filters, String field, Set<String> values) {
        ArrayNode node = MAPPER.createArrayNode();
        values.forEach(node::add);
        filters.add(MAPPER.createObjectNode().set(TERMS, MAPPER.createObjectNode().set(field, node)));
    }
}
