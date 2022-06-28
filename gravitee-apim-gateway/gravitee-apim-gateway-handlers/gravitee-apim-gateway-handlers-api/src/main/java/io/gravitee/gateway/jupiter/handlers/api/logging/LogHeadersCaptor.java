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
package io.gravitee.gateway.jupiter.handlers.api.logging;

import io.gravitee.gateway.api.http.HttpHeaders;
import java.util.*;
import java.util.function.Consumer;

/**
 * Aims to capture headers that could be removed or added on an existing {@link HttpHeaders} instance.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LogHeadersCaptor implements HttpHeaders {

    private final HttpHeaders delegate;
    private final HttpHeaders captured;

    public LogHeadersCaptor(HttpHeaders delegate) {
        this.delegate = delegate;
        this.captured = HttpHeaders.create();
    }

    public HttpHeaders getDelegate() {
        return delegate;
    }

    public HttpHeaders getCaptured() {
        return this.captured;
    }

    @Override
    public String getFirst(CharSequence name) {
        return delegate.getFirst(name);
    }

    @Override
    public String get(CharSequence name) {
        return delegate.get(name);
    }

    @Override
    public List<String> getAll(CharSequence name) {
        return delegate.getAll(name);
    }

    @Override
    public boolean containsKey(CharSequence name) {
        return delegate.containsKey(name);
    }

    @Override
    public boolean contains(CharSequence name) {
        return delegate.contains(name);
    }

    @Override
    public Set<String> names() {
        return delegate.names();
    }

    @Override
    public HttpHeaders add(CharSequence name, CharSequence value) {
        captured.add(name, value);
        return delegate.add(name, value);
    }

    @Override
    public HttpHeaders add(CharSequence name, Iterable<CharSequence> values) {
        captured.add(name, values);
        return delegate.add(name, values);
    }

    @Override
    public HttpHeaders set(CharSequence name, CharSequence value) {
        captured.add(name, value);
        return delegate.set(name, value);
    }

    @Override
    public HttpHeaders set(CharSequence name, Iterable<CharSequence> values) {
        captured.add(name, values);
        return delegate.set(name, values);
    }

    @Override
    public HttpHeaders remove(CharSequence name) {
        captured.remove(name);
        return delegate.remove(name);
    }

    @Override
    public void clear() {
        captured.clear();
        delegate.clear();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public List<String> getOrDefault(CharSequence key, List<String> defaultValue) {
        return delegate.getOrDefault(key, defaultValue);
    }

    @Override
    public Map<String, String> toSingleValueMap() {
        return delegate.toSingleValueMap();
    }

    @Override
    public Map<String, List<String>> toListValuesMap() {
        return delegate.toListValuesMap();
    }

    @Override
    public boolean containsAllKeys(Collection<String> names) {
        return delegate.containsAllKeys(names);
    }

    @Override
    public boolean deeplyEquals(HttpHeaders other) {
        return delegate.deeplyEquals(other);
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return delegate.iterator();
    }

    @Override
    public void forEach(Consumer<? super Map.Entry<String, String>> action) {
        delegate.forEach(action);
    }

    @Override
    public Spliterator<Map.Entry<String, String>> spliterator() {
        return delegate.spliterator();
    }
}
