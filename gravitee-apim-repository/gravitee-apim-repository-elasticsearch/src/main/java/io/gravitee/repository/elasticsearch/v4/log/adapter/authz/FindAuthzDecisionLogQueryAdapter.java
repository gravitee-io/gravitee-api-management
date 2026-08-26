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

import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.BOOL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.FILTER;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.QUERY;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.SIZE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.TERM;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Reads one decision by its event id. The api id is part of the predicate rather than a
 * post-filter: it scopes the lookup the same way the caller's permission does, so a known event id
 * from another api cannot be read through this path.
 *
 * @author GraviteeSource Team
 */
public final class FindAuthzDecisionLogQueryAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FindAuthzDecisionLogQueryAdapter() {}

    public static String adapt(String apiId, String eventId) {
        ArrayNode filters = MAPPER.createArrayNode();
        filters.add(MAPPER.createObjectNode().set(TERM, MAPPER.createObjectNode().put(AuthzDecisionLogFields.API_ID, apiId)));
        filters.add(MAPPER.createObjectNode().set(TERM, MAPPER.createObjectNode().put(AuthzDecisionLogFields.EVENT_ID, eventId)));

        ObjectNode root = MAPPER.createObjectNode();
        root.set(QUERY, MAPPER.createObjectNode().set(BOOL, MAPPER.createObjectNode().set(FILTER, filters)));
        root.put(SIZE, 1);

        return root.toString();
    }
}
