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
package io.gravitee.repository.elasticsearch.v4.healthcheck.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.reactivex.rxjava3.core.Maybe;

public interface QueryResponseAdapter<Q, R> {
    ObjectMapper MAPPER = new ObjectMapper();

    default JsonNode adaptQuery(Q query, ElasticsearchInfo esInfo) {
        return adaptQuery(query);
    }

    default JsonNode adaptQuery(Q query) {
        return json();
    }

    Maybe<R> adaptResponse(SearchResponse response);

    default ObjectNode json() {
        return MAPPER.createObjectNode();
    }

    default ArrayNode array() {
        return MAPPER.createArrayNode();
    }
}
