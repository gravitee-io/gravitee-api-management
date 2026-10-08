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
package io.gravitee.rest.api.service.v4.impl;

import static io.gravitee.repository.management.model.Audit.AuditProperties.ENCRYPTED;

import io.gravitee.apim.core.api.model.property.EncryptedPropertyAuditMarker;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.repository.management.model.Audit;
import java.util.List;
import java.util.Map;

final class EncryptedPropertyAuditProperties {

    private EncryptedPropertyAuditProperties() {}

    static Map<Audit.AuditProperties, String> of(List<? extends Property> oldProperties, List<? extends Property> newProperties) {
        return EncryptedPropertyAuditMarker.involvesEncryptedProperty(oldProperties, newProperties)
            ? Map.of(ENCRYPTED, Boolean.TRUE.toString())
            : Map.of();
    }
}
