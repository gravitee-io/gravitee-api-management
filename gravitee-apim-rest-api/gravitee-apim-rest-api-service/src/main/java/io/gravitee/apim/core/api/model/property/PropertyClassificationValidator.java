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

import io.gravitee.apim.core.api.exception.ApiPropertyEncryptedToPlainException;
import io.gravitee.definition.model.ApiDefinition;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.property.Property;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class PropertyClassificationValidator {

    private PropertyClassificationValidator() {}

    public static void rejectEncryptedToPlain(ApiDefinition storedDefinition, List<EncryptableProperty> incomingProperties) {
        if (storedDefinition instanceof AbstractApi definition) {
            rejectEncryptedToPlain(definition.getProperties(), incomingProperties);
        }
    }

    public static void rejectEncryptedToPlain(List<Property> storedProperties, List<EncryptableProperty> incomingProperties) {
        if (storedProperties == null || incomingProperties == null) {
            return;
        }
        Set<String> encryptedKeys = storedProperties
            .stream()
            .filter(Objects::nonNull)
            .filter(Property::isEncrypted)
            .map(Property::getKey)
            .collect(Collectors.toSet());
        incomingProperties
            .stream()
            .filter(Objects::nonNull)
            .filter(incoming -> encryptedKeys.contains(incoming.getKey()))
            .filter(incoming -> !incoming.isEncrypted() && !incoming.isEncryptable())
            .findFirst()
            .ifPresent(incoming -> {
                throw new ApiPropertyEncryptedToPlainException(incoming.getKey());
            });
    }
}
