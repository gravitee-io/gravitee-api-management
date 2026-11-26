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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import java.io.IOException;
import java.util.List;

/**
 * The swagger-import response is the created API, plus the paths that were not imported.
 * {@code skippedPaths} is not stored on the API.
 * Serialized as a flat object: {@code @JsonUnwrapped} cannot be used because {@code ApiV4} carries
 * {@code @JsonTypeInfo} and Jackson refuses to unwrap typed values.
 */
@JsonSerialize(using = ApiV4SwaggerImportResponse.Serializer.class)
public class ApiV4SwaggerImportResponse {

    private final ApiV4 api;
    private final List<String> skippedPaths;

    public ApiV4SwaggerImportResponse(ApiV4 api, List<String> skippedPaths) {
        this.api = api;
        this.skippedPaths = skippedPaths == null ? List.of() : skippedPaths;
    }

    public static final class Serializer extends JsonSerializer<ApiV4SwaggerImportResponse> {

        @Override
        public void serialize(ApiV4SwaggerImportResponse value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectMapper mapper = (ObjectMapper) gen.getCodec();
            ObjectNode node = mapper.valueToTree(value.api);
            if (!value.skippedPaths.isEmpty()) {
                ArrayNode paths = node.putArray("skippedPaths");
                value.skippedPaths.forEach(paths::add);
            }
            mapper.writeTree(gen, node);
        }
    }
}
