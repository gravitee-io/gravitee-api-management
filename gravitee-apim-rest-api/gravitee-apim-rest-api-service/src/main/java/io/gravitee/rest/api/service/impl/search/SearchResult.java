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
package io.gravitee.rest.api.service.impl.search;

import java.util.Collection;

public class SearchResult {

    private final Collection<String> documents;

    private long hits;

    public SearchResult(final Collection<String> documents) {
        this.documents = documents;
    }

    public SearchResult(final Collection<String> documents, long hits) {
        this.documents = documents;
        this.hits = hits;
    }

    public Collection<String> getDocuments() {
        return documents;
    }

    public long getHits() {
        return hits;
    }

    public void setHits(long hits) {
        this.hits = hits;
    }

    public boolean hasResults() {
        return documents != null && !documents.isEmpty();
    }
}
