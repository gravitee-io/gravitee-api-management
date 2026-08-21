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
package io.gravitee.definition.jackson.api.deser;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Endpoint;
import org.junit.jupiter.api.Test;

/**
 * Covers APIM-14964: an endpoint's per-endpoint "http" override is preserved verbatim as a raw JSON
 * string (see {@link Endpoint#getConfiguration()}), bypassing the typed HttpClientOptions model and its
 * isClearTextUpgrade() masking entirely. The invalid combo must therefore be normalized directly on the
 * raw JSON in {@link EndpointDeserializer}, both on write (incoming PUT/POST) and on read (stored
 * definition re-parsed by ApiConverter).
 */
class EndpointDeserializerClearTextUpgradeTest {

    private final GraviteeMapper mapper = new GraviteeMapper();

    private static String apiJson(String endpointHttp) {
        return (
            "{\"id\":\"test-id\",\"name\":\"test-api\",\"version\":\"1\"," +
            "\"proxy\":{\"context_path\":\"/test\",\"groups\":[{\"name\":\"default-group\",\"endpoints\":[{" +
            "\"name\":\"ep1\",\"target\":\"http://localhost:5555/echo\",\"weight\":1,\"backup\":false,\"type\":\"http\",\"inherit\":false," +
            "\"http\":" +
            endpointHttp +
            "}]}]}}"
        );
    }

    private JsonNode endpointHttpNode(String rawJson) throws Exception {
        Api api = mapper.readValue(rawJson, Api.class);
        String reserialized = mapper.writeValueAsString(api);
        JsonNode endpoints = mapper.readTree(reserialized).path("proxy").path("groups").get(0).path("endpoints");
        return endpoints.get(0).path("http");
    }

    @Test
    void should_force_clear_text_upgrade_to_false_when_explicitly_true_for_http_1_1() throws Exception {
        JsonNode http = endpointHttpNode(apiJson("{\"version\":\"HTTP_1_1\",\"clearTextUpgrade\":true,\"connectTimeout\":5000}"));

        assertThat(http.path("clearTextUpgrade").asBoolean()).isFalse();
    }

    @Test
    void should_force_clear_text_upgrade_to_false_when_version_is_absent_and_defaults_to_http_1_1() throws Exception {
        // version omitted -> defaults to HTTP_1_1, same invalid-combo risk applies
        JsonNode http = endpointHttpNode(apiJson("{\"clearTextUpgrade\":true,\"connectTimeout\":5000}"));

        assertThat(http.path("clearTextUpgrade").asBoolean()).isFalse();
    }

    @Test
    void should_preserve_clear_text_upgrade_for_http_2() throws Exception {
        JsonNode http = endpointHttpNode(apiJson("{\"version\":\"HTTP_2\",\"clearTextUpgrade\":true,\"connectTimeout\":5000}"));

        assertThat(http.path("clearTextUpgrade").asBoolean()).isTrue();
    }

    @Test
    void should_leave_absent_clear_text_upgrade_untouched() throws Exception {
        JsonNode http = endpointHttpNode(apiJson("{\"version\":\"HTTP_1_1\",\"connectTimeout\":5000}"));

        assertThat(http.has("clearTextUpgrade")).isFalse();
    }
}
