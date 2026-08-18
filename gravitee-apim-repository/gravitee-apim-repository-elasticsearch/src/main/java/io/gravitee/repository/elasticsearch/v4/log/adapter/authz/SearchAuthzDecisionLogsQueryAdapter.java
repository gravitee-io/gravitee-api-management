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

    public static String adapt(AuthzDecisionLogQuery query) {
        ArrayNode filters = MAPPER.createArrayNode();
        // The event-metrics data stream is shared by every BaseEventMetrics subtype (api, application,
        // topic, operation, authz), so this term is what makes the result set decisions and nothing else.
        filters.add(MAPPER.createObjectNode().set(TERM, MAPPER.createObjectNode().put(AuthzDecisionLogFields.DOC_TYPE, "authz")));

        ArrayNode apiIds = MAPPER.createArrayNode();
        query.getApiIds().forEach(apiIds::add);
        filters.add(MAPPER.createObjectNode().set(TERMS, MAPPER.createObjectNode().set(AuthzDecisionLogFields.API_ID, apiIds)));

        // Every one of these is a keyword field, so an exact terms clause is the whole translation.
        addTermsIfAny(filters, AuthzDecisionLogFields.DECISION, query.getDecisions());
        addTermsIfAny(filters, AuthzDecisionLogFields.SUBJECT_ID, query.getSubjectIds());
        addTermsIfAny(filters, AuthzDecisionLogFields.ACTION, query.getActions());
        addTermsIfAny(filters, AuthzDecisionLogFields.RESOURCE_ID, query.getResourceIds());
        addTermsIfAny(filters, AuthzDecisionLogFields.CALLER, query.getCallers());

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
