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
package io.gravitee.gateway.registry.mongodb.converters;

import java.util.*;

/**
 * Defines an abstraction to convert from a source to a target type and vice versa.
 *
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
public abstract class AbstractConverter<S, T> {

    public abstract T convertTo(S object);

    public abstract S convertFrom(T object);

    public List<T> convertToAll(final Collection<? extends S> objects) {
        if (objects == null) {
            return null;
        }
        final List<T> results = new ArrayList<>(objects.size());
        for (final S object : objects) {
            results.add(convertTo(object));
        }
        return results;
    }

    public List<S> convertFromAll(final Collection<? extends T> objects) {
        if (objects == null) {
            return null;
        }
        final List<S> results = new ArrayList<>(objects.size());
        for (final T object : objects) {
            results.add(convertFrom(object));
        }
        return results;
    }
}

