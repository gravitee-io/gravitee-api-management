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
package io.gravitee.apim.core.api.domain_service.property;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.property.EncryptableProperty;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.v4.property.Property;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

@CustomLog
@AllArgsConstructor
@DomainService
public class PropertyDomainService {

    private final DataEncryptor dataEncryptor;

    /**
     * @param existing the properties currently stored, used only to reject an encrypted key being
     *                 reclassified to plain; {@code null} or empty when there is nothing stored yet.
     */
    public List<Property> encryptProperties(List<Property> existing, List<EncryptableProperty> apiProperties) {
        if (apiProperties == null) {
            return new ArrayList<>();
        }
        Map<String, Property> existingByKey = existing == null
            ? Map.of()
            : existing.stream().filter(Objects::nonNull).collect(Collectors.toMap(Property::getKey, Function.identity(), (a, b) -> a));
        return apiProperties
            .stream()
            .map(property -> encryptProperty(existingByKey, property))
            .filter(Objects::nonNull)
            .toList();
    }

    private Property encryptProperty(Map<String, Property> existingByKey, EncryptableProperty property) {
        if (property == null) {
            return null;
        }
        Property stored = existingByKey.get(property.getKey());
        if (stored != null && stored.isEncrypted() && !property.isEncrypted() && !property.isEncryptable()) {
            throw new ValidationDomainException(
                "Property '" + property.getKey() + "' is encrypted and cannot be reclassified to plain.",
                Map.of("key", property.getKey()),
                "property.encrypted.immutable"
            );
        }
        var asPropertyBuilder = property.toPropertyBuilder();
        if (property.isEncryptable() && !property.isEncrypted()) {
            try {
                asPropertyBuilder.value(dataEncryptor.encrypt(property.getValue())).encrypted(true);
            } catch (GeneralSecurityException e) {
                log.error("Error encrypting property value", e);
            }
        }
        return asPropertyBuilder.build();
    }
}
