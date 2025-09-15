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
package io.gravitee.definition.model;

import io.gravitee.definition.model.v4.property.Property;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.jspecify.annotations.Nullable;

public interface ApiDefinition {
    void setId(String id);
    DefinitionVersion getDefinitionVersion();

    Set<String> getTags();
    void setTags(Set<String> tags);

    default boolean updateDynamicProperties(Function<List<Property>, UpdateDynamicPropertiesResult> updateOperator) {
        return false;
    }

    static interface UpdateDynamicPropertiesResult {
        List<Property> orderedProperties();
        boolean needToUpdate();
    }
}
