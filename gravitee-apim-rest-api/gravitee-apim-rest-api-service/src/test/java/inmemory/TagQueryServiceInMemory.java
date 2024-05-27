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

import io.gravitee.apim.core.tag.model.Tag;
import io.gravitee.apim.core.tag.query_service.TagQueryService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TagQueryServiceInMemory implements TagQueryService, InMemoryAlternative<Tag> {

    private final List<Tag> storage;

    public TagQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    @Override
    public List<Tag> findByName(String organizationId, String name) {
        return storage
            .stream()
            .filter(group -> organizationId.equals(group.getReferenceId()))
            .filter(group -> group.getName().equals(name))
            .toList();
    }

    @Override
    public void initWith(List<Tag> items) {
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Tag> storage() {
        return Collections.unmodifiableList(storage);
    }
}
