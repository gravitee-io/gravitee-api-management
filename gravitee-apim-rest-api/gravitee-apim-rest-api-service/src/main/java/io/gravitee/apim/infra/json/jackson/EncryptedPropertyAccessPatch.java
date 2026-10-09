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

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.TreeMap;

public final class EncryptedPropertyAccessPatch {

    private static final JsonPointer PROPERTIES = JsonPointer.compile("/properties");

    private EncryptedPropertyAccessPatch() {}

    public static String of(Map<String, String> ciphertextByKey) {
        ArrayNode patch = JsonNodeFactory.instance.arrayNode();
        new TreeMap<>(ciphertextByKey).forEach((key, ciphertext) -> patch.add(accessOperation(key, ciphertext)));
        return patch.toString();
    }

    private static ObjectNode accessOperation(String key, String ciphertext) {
        ObjectNode property = JsonNodeFactory.instance.objectNode();
        if (ciphertext != null) {
            property.put("value", EncryptedPropertyAuditRedaction.fingerprint(ciphertext));
        }
        property.put("encrypted", true);

        ObjectNode operation = JsonNodeFactory.instance.objectNode();
        operation.put("op", "access").put("path", PROPERTIES.appendProperty(key).toString()).set("value", property);
        return operation;
    }
}
