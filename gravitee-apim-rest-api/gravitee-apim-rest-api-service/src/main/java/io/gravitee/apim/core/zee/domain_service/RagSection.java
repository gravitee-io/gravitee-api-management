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
package io.gravitee.apim.core.zee.domain_service;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * Declarative descriptor for one section of RAG context.
 *
 * <p>
 * Each section has a markdown title, a fetcher that retrieves items
 * (scoped by request context), a formatter that turns each item into
 * a markdown line, and a maximum item cap.
 *
 * @param <T> the item type returned by the fetcher
 * @author Derek Burger
 */
public record RagSection<T>(String title, ScopedFetcher<T> fetcher, Function<T, String> formatter, int maxItems) {
    /**
     * Fetches a collection of items using the request's contextData.
     *
     * @param <T> the item type
     */
    @FunctionalInterface
    public interface ScopedFetcher<T> {
        Collection<T> fetch(Map<String, Object> contextData);
    }
}
