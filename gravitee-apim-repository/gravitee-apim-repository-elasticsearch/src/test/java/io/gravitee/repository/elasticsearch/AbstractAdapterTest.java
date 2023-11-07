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
package io.gravitee.repository.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    @NotNull
    protected SearchResponse buildSearchHit(String fileName) {
        final SearchResponse searchResponse = new SearchResponse();
        final SearchHits searchHits = new SearchHits();
        final SearchHit searchHit = new SearchHit();

        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(loadFile("/hits/" + fileName));

        searchHit.setSource(jsonNode);
        searchHits.setHits(List.of(searchHit));
        searchResponse.setSearchHits(searchHits);
        return searchResponse;
    }

    protected String loadFile(String resource) throws IOException {
        InputStream stream = this.getClass().getResourceAsStream(resource);
        JsonNode json = objectMapper.readValue(stream, JsonNode.class);
        return objectMapper.writeValueAsString(json);
    }
}
