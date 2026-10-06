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
import io.gravitee.definition.model.ApiDefinition;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.property.Property;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EncryptedPropertyAuditMarker {

    private EncryptedPropertyAuditMarker() {}

    public static Map<AuditProperties, String> mark(
        Map<AuditProperties, String> auditProperties,
        ApiDefinition oldDefinition,
        ApiDefinition newDefinition
    ) {
        if (!involvesEncryptedProperty(v4Properties(oldDefinition), v4Properties(newDefinition))) {
            return auditProperties;
        }
        Map<AuditProperties, String> marked = new EnumMap<>(AuditProperties.class);
        marked.putAll(auditProperties);
        marked.put(AuditProperties.ENCRYPTED, Boolean.TRUE.toString());
        return marked;
    }

    public static boolean involvesEncryptedProperty(List<? extends Property> oldProperties, List<? extends Property> newProperties) {
        return holdsEncryptedProperty(oldProperties) || holdsEncryptedProperty(newProperties);
    }

    private static boolean holdsEncryptedProperty(List<? extends Property> properties) {
        return properties != null && properties.stream().filter(Objects::nonNull).anyMatch(Property::isEncrypted);
    }

    private static List<Property> v4Properties(ApiDefinition definition) {
        return definition instanceof AbstractApi v4Definition ? v4Definition.getProperties() : null;
    }
}
