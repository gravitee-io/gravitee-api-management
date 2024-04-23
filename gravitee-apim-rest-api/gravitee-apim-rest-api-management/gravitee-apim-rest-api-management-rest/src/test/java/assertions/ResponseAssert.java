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
package assertions;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.api.ObjectAssert;

public class ResponseAssert extends AbstractObjectAssert<ResponseAssert, Response> {

    public ResponseAssert(Response apiEntity) {
        super(apiEntity, ResponseAssert.class);
    }

    public ResponseAssert hasStatus(int status) {
        isNotNull();
        if (actual.getStatus() != status) {
            failWithMessage("Expected response status to be <%s> but was <%s>", status, actual.getStatus());
        }

        return this;
    }

    public ResponseAssert hasHeader(Map.Entry<String, String> header) {
        isNotNull();

        assertThat(actual.getHeaders())
            .describedAs("Expected response headers to contains <%s> but was <%s>", header, actual.getHeaders())
            .containsEntry(header.getKey(), List.of(header.getValue()));

        return this;
    }

    public ObjectAssert<JsonObject> asJson() {
        isNotNull();
        String entity = actual.readEntity(String.class);
        return AssertionsForClassTypes.assertThat(new JsonObject(entity));
    }
}
