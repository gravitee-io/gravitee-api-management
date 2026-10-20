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
package io.gravitee.apim.core.api.model.property;

import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.ApiDefinition;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.property.Property;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public final class EncryptedPropertyAuditMarker {

    private EncryptedPropertyAuditMarker() {}

    public static Map<AuditProperties, String> mark(
        Map<AuditProperties, String> auditProperties,
        ApiDefinition oldDefinition,
        ApiDefinition newDefinition
    ) {
        if (!writesEncryptedValue(oldDefinition, newDefinition)) {
            return auditProperties;
        }
        Map<AuditProperties, String> marked = new EnumMap<>(AuditProperties.class);
        marked.putAll(auditProperties);
        marked.put(AuditProperties.ENCRYPTED, Boolean.TRUE.toString());
        return marked;
    }

    public static boolean writesEncryptedValue(List<? extends Property> oldProperties, List<? extends Property> newProperties) {
        return holdsNewCiphertext(ciphertextByKey(oldProperties), ciphertextByKey(newProperties));
    }

    private static boolean writesEncryptedValue(ApiDefinition oldDefinition, ApiDefinition newDefinition) {
        return holdsNewCiphertext(ciphertextByKey(oldDefinition), ciphertextByKey(newDefinition));
    }

    public static boolean holdsNewCiphertext(Map<String, String> oldCiphertextByKey, Map<String, String> newCiphertextByKey) {
        return newCiphertextByKey
            .entrySet()
            .stream()
            .anyMatch(property -> isNewCiphertext(property.getValue(), oldCiphertextByKey.get(property.getKey())));
    }

    private static boolean isNewCiphertext(String ciphertext, String storedCiphertext) {
        return ciphertext != null && !ciphertext.equals(storedCiphertext);
    }

    public static boolean holdsEncryptedProperty(List<? extends Property> properties) {
        return properties != null && properties.stream().filter(Objects::nonNull).anyMatch(Property::isEncrypted);
    }

    private static Map<String, String> ciphertextByKey(ApiDefinition definition) {
        return switch (definition) {
            case AbstractApi v4Definition -> ciphertextByKey(v4Definition.getProperties());
            case Api v2Definition -> ciphertextByKey(v2Definition.getProperties());
            case null, default -> Map.of();
        };
    }

    public static Map<String, String> ciphertextByKey(List<? extends Property> v4Properties) {
        if (v4Properties == null) {
            return Map.of();
        }
        return valueByKey(
            v4Properties.stream().filter(Objects::nonNull).filter(Property::isEncrypted),
            Property::getKey,
            Property::getValue
        );
    }

    private static Map<String, String> ciphertextByKey(Properties v2Properties) {
        if (v2Properties == null) {
            return Map.of();
        }
        return valueByKey(
            v2Properties
                .getProperties()
                .stream()
                .filter(Objects::nonNull)
                .filter(property -> property.isEncrypted()),
            property -> property.getKey(),
            property -> property.getValue()
        );
    }

    private static <T> Map<String, String> valueByKey(Stream<T> properties, Function<T, String> key, Function<T, String> value) {
        return properties.collect(
            HashMap::new,
            (valueByKey, property) -> valueByKey.put(key.apply(property), value.apply(property)),
            HashMap::putAll
        );
    }
}
