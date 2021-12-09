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

import io.gravitee.gateway.api.http.HttpHeaders;
import io.vertx.core.MultiMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpHeaders implements HttpHeaders {

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
}
