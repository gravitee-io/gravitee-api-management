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

import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.rest.api.model.v4.api.properties.PropertyEntity;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class ensures that property classification is immutable once encrypted.
 * The only allowed transitions for a key that is already stored as {@code encrypted=true} are:
 * staying encrypted, or a renew ({@code encryptable=true}). A move back to plain
 * ({@code encrypted=false && encryptable=false}) is rejected. The one-way {@code plain -> encrypted}
 * exception is always allowed because the existing property is not yet encrypted.
 */
public final class PropertyClassificationValidator {

    private PropertyClassificationValidator() {}

    public static void rejectEncryptedToPlain(List<Property> existing, List<PropertyEntity> incoming) {
        if (existing == null || incoming == null) {
            return;
        }
        Map<String, Property> existingByKey = existing
            .stream()
            .collect(Collectors.toMap(Property::getKey, Function.identity(), (a, b) -> a));

        for (PropertyEntity in : incoming) {
            Property old = existingByKey.get(in.getKey());
            boolean wasEncrypted = old != null && old.isEncrypted();
            boolean nowPlain = !in.isEncrypted() && !in.isEncryptable();
            if (wasEncrypted && nowPlain) {
                throw new InvalidDataException("Property '" + in.getKey() + "' is encrypted and cannot be reclassified to plain.");
            }
        }
    }
}
