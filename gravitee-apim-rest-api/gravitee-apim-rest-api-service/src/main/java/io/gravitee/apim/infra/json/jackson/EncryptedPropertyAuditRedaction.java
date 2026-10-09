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
import org.apache.commons.codec.digest.DigestUtils;

public final class EncryptedPropertyAuditRedaction {

    private static final String VALUE = "value";

    private EncryptedPropertyAuditRedaction() {}

    public static <T extends JsonNode> T redact(T node) {
        if (isEncryptedProperty(node)) {
            redactValue((ObjectNode) node);
        }
        node.forEach(EncryptedPropertyAuditRedaction::redact);
        return node;
    }

    private static void redactValue(ObjectNode property) {
        JsonNode value = property.path(VALUE);
        if (value.isTextual()) {
            property.put(VALUE, fingerprint(value.textValue()));
        } else {
            property.remove(VALUE);
        }
    }

    // Hashes the ciphertext, not the plaintext, so nobody without the encryption key can match the fingerprint against guessed secrets.
    public static String fingerprint(String ciphertext) {
        return "<sha256:" + DigestUtils.sha256Hex(ciphertext) + ">";
    }

    private static boolean isEncryptedProperty(JsonNode node) {
        return node.isObject() && node.has("key") && node.path("encrypted").asBoolean();
    }
}
