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
package io.gravitee.gateway.http.vertx;

import io.gravitee.common.util.LinkedCaseInsensitiveMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.gateway.api.http.DefaultHttpHeaders;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.vertx.core.MultiMap;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implements {@link MultiValueMap<String,String>} for backward compatibility due to the changes to Headers in 3.15.
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpHeaders implements HttpHeaders, MultiValueMap<String, String> {

    private final MultiMap headers;

    public VertxHttpHeaders(final MultiMap headers) {
        this.headers = headers;
    }

    @Override
    public String get(CharSequence name) {
        return headers.get(name);
    }

    @Override
    public List<String> getAll(CharSequence name) {
        return headers.getAll(name);
    }

    @Override
    public boolean contains(CharSequence name) {
        return headers.contains(name);
    }

    @Override
    public Set<String> names() {
        return headers.names();
    }

    @Override
    public HttpHeaders add(CharSequence name, CharSequence value) {
        headers.add(name, value);
        return this;
    }

    @Override
    public HttpHeaders add(CharSequence name, Iterable<CharSequence> values) {
        headers.add(name, values);
        return this;
    }

    @Override
    public HttpHeaders set(CharSequence name, CharSequence value) {
        headers.set(name, value);
        return this;
    }

    @Override
    public HttpHeaders set(CharSequence name, Iterable<CharSequence> values) {
        headers.set(name, values);
        return this;
    }

    @Override
    public HttpHeaders remove(CharSequence name) {
        headers.remove(name);
        return this;
    }

    @Override
    public void clear() {
        headers.clear();
    }

    @Override
    public int size() {
        return headers.size();
    }

    @Override
    public boolean isEmpty() {
        return headers.isEmpty();
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return headers.iterator();
    }

    @Override
    public Map<String, String> toSingleValueMap() {
        return HttpHeaders.super.toSingleValueMap();
    }

    @Override
    public boolean containsAllKeys(Collection<String> names) {
        return HttpHeaders.super.containsAllKeys(names);
    }

    @Override
    public boolean containsKey(Object key) {
        return contains(String.valueOf(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return entrySet().stream().anyMatch(entry -> value.equals(entry.getValue()));
    }

    /**
     * @see LinkedCaseInsensitiveMap#get(Object)
     * Contrary to {@link DefaultHttpHeaders#get(CharSequence)}, the list of values is returned, not only the first element
     */
    @Override
    public List<String> get(Object key) {
        String keyAsString = String.valueOf(key);
        return contains(keyAsString) ? getAll(keyAsString) : null;
    }

    /**
     * @see HashMap#putVal(int, Object, Object, boolean, boolean), returns the previous value if present, else null.
     */
    @Override
    public List<String> put(String key, List<String> value) {
        final List<String> previousValues = headers.getAll(key);
        for (int i = 0; i < value.size(); i++) {
            // For the first element, we need to use set to override previous value, then we use add to add new ones.
            if (i == 0) {
                headers.set(key, value.get(i));
            } else {
                headers.add(key, value.get(i));
            }
        }
        return previousValues.isEmpty() ? null : previousValues;
    }

    /**
     * @see HashMap#remove(Object), returns the previous value (can bee {@code null}) associated with {@code key} or null if none.
     */
    @Override
    public List<String> remove(Object key) {
        final List<String> previousValues = headers.getAll(String.valueOf(key));
        headers.remove(String.valueOf(key));
        return previousValues;
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<String>> map) {
        final MultiMap multimap = HeadersMultiMap.headers();

        // Flatten the Map<String, List<String>> to be able to add each entry one by one to a new Multimap object
        map
            .entrySet()
            .stream()
            .flatMap(entry -> entry.getValue().stream().map(v -> Map.entry(entry.getKey(), v)))
            .forEach(entry -> multimap.add(entry.getKey(), entry.getValue()));

        headers.setAll(multimap);
    }

    @Override
    public Set<String> keySet() {
        return headers.entries().stream().map(Entry::getKey).collect(Collectors.toSet());
    }

    @Override
    public Collection<List<String>> values() {
        return headers
            .entries()
            .stream()
            .collect(Collectors.groupingBy(Entry::getKey))
            .values()
            .stream()
            .map(entries -> entries.stream().map(Entry::getValue).collect(Collectors.toList()))
            .collect(Collectors.toList());
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet() {
        return headers
            .entries()
            .stream()
            .collect(Collectors.groupingBy(Entry::getKey))
            .entrySet()
            .stream()
            .map(entry -> Map.entry(entry.getKey(), entry.getValue().stream().map(Entry::getValue).collect(Collectors.toList())))
            .collect(Collectors.toSet());
    }

    @Override
    public String getFirst(String header) {
        return this.headers.get(header);
    }

    @Override
    public void add(String name, String value) {
        headers.add(name, value);
    }

    @Override
    public void set(String name, String value) {
        this.headers.set(name, value);
    }

    @Override
    public void setAll(Map<String, String> values) {
        for (Entry<String, String> entry : values.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
    }
}
