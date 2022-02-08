/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.debug.reactor.handler.context;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AttributeHelper {

    public static Map<String, Serializable> filterAndSerializeAttributes(Map<String, Object> attributes) {
        if (attributes == null) {
            return null;
        }

        // FIXME: context-path is removed for now as it is updated with the event id in the debug mode context
        // we need to rework that before making it accessible to the user
        // https://github.com/gravitee-io/issues/issues/7072
        List<String> attributesToExclude = List.of("gravitee.attribute.context-path");

        Map<String, Serializable> filteredAttributes = new HashMap<>();
        attributes
            .keySet()
            .stream()
            .filter(key -> !attributesToExclude.contains(key))
            .filter(key -> attributes.get(key) != null)
            .filter(key -> attributes.get(key) instanceof Serializable)
            .forEach(
                key -> {
                    filteredAttributes.put(key, (Serializable) attributes.get(key));
                }
            );

        return filteredAttributes;
    }
}
