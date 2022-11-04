/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.definition.model.kubernetes.v1alpha1;

import static io.gravitee.kubernetes.mapper.GroupVersionKind.*;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.kubernetes.mapper.CustomResource;
import io.gravitee.kubernetes.mapper.ObjectMeta;
import java.util.List;

/**
 * @author GraviteeSource Team
 */
public class ApiDefinitionResource extends CustomResource<ObjectNode> {

    private static final List<String> UNSUPPORTED_API_FIELDS = List.of(
        "definition_context",
        "execution_mode",
        "primaryOwner",
        "groups",
        "members",
        "pages",
        "createdAt",
        "updatedAt",
        "picture",
        "apiMedia"
    );

    private static final List<String> UNSUPPORTED_PLAN_FIELDS = List.of("created_at", "updated_at", "published_at");

    private static final String PLANS_FIELD = "plans";

    private static final String METADATA_FIELD = "metadata";

    private static final List<String> UNSUPPORTED_METADATA_FIELDS = List.of("apiId");

    private static final List<String> API_ID_FIELDS = List.of("crossId", "id");

    private static final List<String> PLAN_ID_FIELDS = List.of("crossId", "id", "api");

    public ApiDefinitionResource(String name, ObjectNode apiDefinition) {
        super(GIO_V1_ALPHA_1_API_DEFINITION, new ObjectMeta(name), apiDefinition);
        removeUnsupportedFields();
    }

    public void removeIds() {
        ObjectNode spec = getSpec();

        spec.remove(API_ID_FIELDS);

        if (spec.hasNonNull(PLANS_FIELD)) {
            ArrayNode plans = (ArrayNode) spec.get(PLANS_FIELD);
            plans.forEach(plan -> ((ObjectNode) plan).remove(PLAN_ID_FIELDS));
        }
    }

    private void removeUnsupportedFields() {
        ObjectNode spec = getSpec();

        spec.remove(UNSUPPORTED_API_FIELDS);

        if (spec.hasNonNull(PLANS_FIELD)) {
            ArrayNode plans = (ArrayNode) spec.get(PLANS_FIELD);
            plans.forEach(plan -> ((ObjectNode) plan).remove(UNSUPPORTED_PLAN_FIELDS));
        }

        if (spec.hasNonNull(METADATA_FIELD)) {
            ArrayNode metadata = (ArrayNode) spec.get(METADATA_FIELD);
            metadata.forEach(data -> ((ObjectNode) data).remove(UNSUPPORTED_METADATA_FIELDS));
        }
    }
}
