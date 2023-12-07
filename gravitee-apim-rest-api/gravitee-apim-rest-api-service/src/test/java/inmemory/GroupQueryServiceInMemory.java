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

import static java.util.stream.Collectors.toSet;

import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.group.query_service.GroupQueryService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class GroupQueryServiceInMemory implements GroupQueryService, InMemoryAlternative<Group> {

    private final List<Group> storage;

    public GroupQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    @Override
    public Optional<Group> findById(String id) {
        return storage.stream().filter(group -> id.equals(group.getId())).findFirst();
    }

    @Override
    public Set<Group> findByIds(Set<String> ids) {
        return storage.stream().filter(group -> ids.contains(group.getId())).collect(toSet());
    }

    @Override
    public Set<Group> findByEvent(String environmentId, Group.GroupEvent event) {
        return storage
            .stream()
            .filter(group -> environmentId.equals(group.getEnvironmentId()))
            .filter(group -> group.getEventRules().stream().anyMatch(rule -> rule.event() == event))
            .collect(toSet());
    }

    @Override
    public void initWith(List<Group> items) {
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<Group> storage() {
        return Collections.unmodifiableList(storage);
    }
}
