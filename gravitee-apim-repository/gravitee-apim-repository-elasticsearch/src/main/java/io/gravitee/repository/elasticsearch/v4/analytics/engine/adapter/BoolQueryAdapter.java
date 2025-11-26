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

import io.gravitee.repository.analytics.engine.api.query.Query;
import io.vertx.core.json.JsonObject;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class BoolQueryAdapter {

    private final FilterAdapter filterAdapter;

    public BoolQueryAdapter(FilterAdapter filterAdapter) {
        this.filterAdapter = filterAdapter;
    }

    JsonObject adaptForMessage(Query query) {
        return JsonObject.of("bool", messageConnexionFilter(query));
    }

    JsonObject adaptForHTTP(Query query) {
        return JsonObject.of("bool", httpFilter(query));
    }

    JsonObject messageFilter(Query query) {
        return JsonObject.of("filter", filterAdapter.adaptForMessage(query));
    }

    JsonObject httpFilter(Query query) {
        return JsonObject.of("filter", filterAdapter.adaptForHTTP(query));
    }

    JsonObject messageConnexionFilter(Query query) {
        return JsonObject.of("filter", filterAdapter.adaptForMessageConnexion(query));
    }
}
