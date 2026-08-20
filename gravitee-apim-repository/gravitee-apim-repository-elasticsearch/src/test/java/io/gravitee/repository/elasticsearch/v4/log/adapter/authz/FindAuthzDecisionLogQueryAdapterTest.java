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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FindAuthzDecisionLogQueryAdapterTest {

    @Test
    void builds_a_single_hit_query_pinned_to_doc_type_api_and_event_id() {
        var result = FindAuthzDecisionLogQueryAdapter.adapt("api-1", "evt-9");

        assertThatJson(result).isEqualTo(
            """
            {
              "query": {
                "bool": {
                  "filter": [
                    { "term": { "doc-type": "authz" } },
                    { "term": { "api-id": "api-1" } },
                    { "term": { "event-id": "evt-9" } }
                  ]
                }
              },
              "size": 1
            }
            """
        );
    }

    @Test
    void scopes_by_api_so_an_event_id_from_another_api_cannot_be_read() {
        var result = FindAuthzDecisionLogQueryAdapter.adapt("api-1", "evt-9");

        assertThatJson(result).inPath("$.query.bool.filter[1].term.api-id").isEqualTo("api-1");
    }

    @Test
    void keys_on_event_id_because_a_batch_shares_one_request_id() {
        var result = FindAuthzDecisionLogQueryAdapter.adapt("api-1", "evt-9");

        assertThatJson(result).inPath("$.query.bool.filter[2].term.event-id").isEqualTo("evt-9");
        assertThatJson(result).node("query.bool.filter").isArray().hasSize(3);
    }
}
