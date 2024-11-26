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

import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;

import io.gravitee.definition.model.v4.property.Property;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DynamicApiProperties {

    final Map<String, Property> currentPropertiesByKey;
    private final List<Property> currentUserDefinedProperties;

    public DynamicApiProperties(List<Property> currentProperties) {
        this.currentPropertiesByKey = currentProperties.stream().collect(Collectors.toMap(Property::getKey, identity()));
        this.currentUserDefinedProperties = currentProperties.stream().filter(not(Property::isDynamic)).toList();
    }

    public record DynamicPropertiesResult(List<Property> orderedProperties, boolean needToUpdate) {}

    public DynamicPropertiesResult updateDynamicProperties(List<Property> dynamicProperties) {
        List<Property> updatedDynamicProperties = new ArrayList<>();
        AtomicBoolean needToBeSaved = new AtomicBoolean(false);
        dynamicProperties.forEach(dynamicProperty -> {
            final Property existingMatchingProperty = currentPropertiesByKey.get(dynamicProperty.getKey());
            // If no property for key or current property is dynamic, then update
            if (isNewProperty(existingMatchingProperty) || existingMatchingProperty.isDynamic()) {
                updatedDynamicProperties.add(dynamicProperty);
                // Need to update if property is new or differ from existing one
                needToBeSaved.compareAndSet(
                    false,
                    isNewProperty(existingMatchingProperty) || isUpdatingPreviousProperty(dynamicProperty, existingMatchingProperty)
                );
            }
        });

        final List<Property> orderedProperties = Stream
            .concat(currentUserDefinedProperties.stream(), updatedDynamicProperties.stream())
            .sorted(Comparator.comparing(Property::getKey))
            .toList();

        return new DynamicPropertiesResult(orderedProperties, needToBeSaved.get());
    }

    private static boolean isNewProperty(Property previousProperty) {
        return previousProperty == null;
    }

    private static boolean isUpdatingPreviousProperty(Property newProperty, Property previousProperty) {
        return !newProperty.getValue().equals(previousProperty.getValue());
    }
}
