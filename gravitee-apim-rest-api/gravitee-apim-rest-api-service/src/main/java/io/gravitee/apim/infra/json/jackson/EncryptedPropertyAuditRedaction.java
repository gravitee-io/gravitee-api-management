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
package io.gravitee.apim.infra.json.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class EncryptedPropertyAuditRedaction {

    private EncryptedPropertyAuditRedaction() {}

    public static <T extends JsonNode> T redact(T node) {
        if (isEncryptedProperty(node)) {
            ((ObjectNode) node).remove("value");
        }
        node.forEach(EncryptedPropertyAuditRedaction::redact);
        return node;
    }

    private static boolean isEncryptedProperty(JsonNode node) {
        return node.isObject() && node.has("key") && node.path("encrypted").asBoolean();
    }
}
