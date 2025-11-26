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

import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents request IDs grouped by join facets for message metrics correlation.
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MessageFacetJoin {

    private final Map<List<String>, Set<String>> requestIDsByFacePath;

    private final JsonObject afterKey;

    public MessageFacetJoin(Map<List<String>, Set<String>> requestIdsByFacetPath, JsonObject afterKey) {
        this.requestIDsByFacePath = new HashMap<>(requestIdsByFacetPath);
        this.afterKey = afterKey;
    }

    public static MessageFacetJoin fromComposite(Set<String> requestIDs, JsonObject afterKey) {
        var requestIDsByFacetPath = new HashMap<List<String>, Set<String>>();
        requestIDsByFacetPath.put(List.of(), requestIDs);
        return new MessageFacetJoin(requestIDsByFacetPath, afterKey);
    }

    public static MessageFacetJoin empty() {
        return new MessageFacetJoin(Map.of(), null);
    }

    public JsonObject getAfterKey() {
        return afterKey;
    }

    public Set<String> getRequestIDs() {
        return requestIDsByFacePath.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
    }

    public boolean isEmpty() {
        return requestIDsByFacePath.isEmpty() || getRequestIDs().isEmpty();
    }
}
