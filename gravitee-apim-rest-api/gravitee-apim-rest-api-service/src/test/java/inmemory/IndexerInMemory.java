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
package inmemory;

import io.gravitee.apim.core.search.Indexer;
import io.gravitee.rest.api.model.search.Indexable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IndexerInMemory implements Indexer, InMemoryAlternative<Indexable> {

    private final List<Indexable> storage = new ArrayList<>();

    @Override
    public void index(IndexationContext context, Indexable indexable) {
        storage.add(indexable);
    }

    @Override
    public void delete(IndexationContext context, Indexable indexable) {
        storage.removeIf(item -> indexable.getId().equals(item.getId()));
    }

    @Override
    public void commit() {}

    @Override
    public void initWith(List<Indexable> items) {
        reset();
        storage.addAll(items);
    }

    public void reset() {
        storage.clear();
    }

    @Override
    public List<Indexable> storage() {
        return storage;
    }
}
