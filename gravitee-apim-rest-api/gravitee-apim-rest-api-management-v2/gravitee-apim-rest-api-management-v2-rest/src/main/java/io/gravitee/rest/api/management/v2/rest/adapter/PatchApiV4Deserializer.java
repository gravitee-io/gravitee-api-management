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
package io.gravitee.rest.api.management.v2.rest.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.use_case.PatchApiUseCase.ApiV4Deserializer;
import io.gravitee.apim.core.api.use_case.PatchApiUseCase.ApiV4Fields;
import io.gravitee.apim.core.api.use_case.PatchApiUseCase.PatchableView;
import io.gravitee.rest.api.management.v2.rest.mapper.ApiMapper;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.GenericApi;
import java.io.IOException;
import java.util.List;

/**
 * @author GraviteeSource Team
 */
public class PatchApiV4Deserializer implements ApiV4Deserializer {

    private static final List<String> POLYMORPHIC_LIST_FIELDS = List.of("listeners", "endpointGroups", "flows", "resources");

    private final ObjectMapper objectMapper;

    public PatchApiV4Deserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode toCurrentStateNode(Api api) {
        var node = (ObjectNode) objectMapper.valueToTree(PatchableView.from(api));
        var apiV4 = ApiMapper.INSTANCE.mapToV4(api, null, null);
        setIfNonNull(node, "listeners", apiV4.getListeners());
        setIfNonNull(node, "endpointGroups", apiV4.getEndpointGroups());
        setIfNonNull(node, "flows", apiV4.getFlows());
        setIfNonNull(node, "resources", apiV4.getResources());
        return node;
    }

    @Override
    public ApiV4Fields fromPatchedNode(JsonNode patchedNode) throws IOException {
        var slim = objectMapper.createObjectNode();
        slim.put("definitionVersion", "V4");
        for (var field : POLYMORPHIC_LIST_FIELDS) {
            var n = patchedNode.get(field);
            if (n != null) {
                slim.set(field, n);
            }
        }
        var apiV4 = (ApiV4) objectMapper.treeToValue(slim, GenericApi.class);
        return ApiMapper.INSTANCE.mapToCoreApi(apiV4);
    }

    private void setIfNonNull(ObjectNode node, String field, Object value) {
        if (value != null) {
            node.set(field, objectMapper.valueToTree(value));
        }
    }
}
