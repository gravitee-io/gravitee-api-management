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
package io.gravitee.gamma.rest.core.gravitee_plugin.model;

import com.fasterxml.jackson.core.JsonPointer;
import java.util.List;
import java.util.Objects;

/**
 * A property of a policy configuration that holds the name of a resource, as the policy schema declares it
 * with {@code gioConfig.uiType: resource-type}.
 *
 * @param property where the property sits in the configuration
 * @param acceptedTypes the resource types the policy accepts there, from {@code gioConfig.uiTypeProps.resourceType};
 *     empty when the schema names none, in which case any type is accepted
 */
public record ResourceReference(JsonPointer property, List<String> acceptedTypes) {
    public ResourceReference {
        Objects.requireNonNull(property, "the property of a resource reference is required");
        acceptedTypes = List.copyOf(Objects.requireNonNull(acceptedTypes, "the accepted types of a resource reference are required"));
    }

    /**
     * Whether a resource of this type may be named here. Matched by bare prefix, exactly as the console pickers
     * offer them: {@code cache} accepts a {@code cache-redis} resource.
     *
     * @param type the type of the resource named, never {@code null}: a resource without one is refused as such
     */
    public boolean accepts(String type) {
        return acceptedTypes.isEmpty() || acceptedTypes.stream().anyMatch(type::startsWith);
    }
}
