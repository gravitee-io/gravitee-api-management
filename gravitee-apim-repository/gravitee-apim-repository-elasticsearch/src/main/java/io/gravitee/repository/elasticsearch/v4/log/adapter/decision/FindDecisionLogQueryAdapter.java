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

import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.BOOL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.FILTER;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.QUERY;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.SIZE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.TERM;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.repository.common.query.QueryContext;

/**
 * Reads one decision record by its event id. The organization, the environment and the api are part of
 * the predicate rather than a post-filter: they scope the lookup the same way the caller's permission
 * does, so a known event id from another tenant or another api cannot be read through this path. The
 * data stream is shared by every environment — the reporter stamps the tenant on the document, it never
 * splits the index — so the query context has to reach the query, not only the index name.
 *
 * <p>Unlike a search, this pins neither the family nor the phase. An event id names exactly one record,
 * and the record of an open decision has an event id of its own — pinning {@code RESOLVED} here would
 * make a pending approval unreadable while it is still the only thing worth looking at.
 *
 * @author GraviteeSource Team
 */
public final class FindDecisionLogQueryAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FindDecisionLogQueryAdapter() {}

    private static void addTerm(ArrayNode filters, String field, String value) {
        filters.add(MAPPER.createObjectNode().set(TERM, MAPPER.createObjectNode().put(field, value)));
    }

    public static String adapt(QueryContext queryContext, String apiId, String eventId) {
        ArrayNode filters = MAPPER.createArrayNode();
        addTerm(filters, DecisionLogFields.ORG_ID, queryContext.getOrgId());
        addTerm(filters, DecisionLogFields.ENV_ID, queryContext.getEnvId());
        addTerm(filters, DecisionLogFields.API_ID, apiId);
        addTerm(filters, DecisionLogFields.EVENT_ID, eventId);

        ObjectNode root = MAPPER.createObjectNode();
        root.set(QUERY, MAPPER.createObjectNode().set(BOOL, MAPPER.createObjectNode().set(FILTER, filters)));
        root.put(SIZE, 1);

        return root.toString();
    }
}
