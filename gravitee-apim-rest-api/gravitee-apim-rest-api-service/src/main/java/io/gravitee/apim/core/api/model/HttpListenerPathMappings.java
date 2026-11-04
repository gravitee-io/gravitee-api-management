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
package io.gravitee.apim.core.api.model;

import io.gravitee.definition.model.PathMapping;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class HttpListenerPathMappings {

    private HttpListenerPathMappings() {}

    public static Optional<Set<String>> firstHttpListenerMappings(List<? extends Listener> listeners) {
        if (listeners == null) {
            return Optional.empty();
        }
        return listeners
            .stream()
            .filter(l -> l instanceof HttpListener)
            .map(l -> ((HttpListener) l).getPathMappings())
            .filter(Objects::nonNull)
            .findFirst();
    }

    public static List<Listener> withFirstHttpListenerMappings(List<? extends Listener> listeners, Set<String> mappings) {
        if (listeners == null) {
            return null;
        }
        var result = new ArrayList<Listener>();
        var replaced = false;
        for (var listener : listeners) {
            if (!replaced && listener instanceof HttpListener httpListener) {
                replaced = true;
                var linkedMappings = new LinkedHashSet<>(mappings);
                Map<String, Pattern> patternMap = linkedMappings
                    .stream()
                    .collect(Collectors.toMap(Function.identity(), PathMapping::buildPattern, (a, b) -> a, LinkedHashMap::new));
                result.add(httpListener.toBuilder().pathMappings(linkedMappings).pathMappingsPattern(patternMap).build());
            } else {
                result.add(listener);
            }
        }
        return result;
    }
}
