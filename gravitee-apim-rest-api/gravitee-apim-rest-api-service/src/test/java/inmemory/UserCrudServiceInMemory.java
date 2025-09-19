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

import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UserCrudServiceInMemory implements UserCrudService, InMemoryAlternative<BaseUserEntity> {

    private final List<BaseUserEntity> storage = new ArrayList<>();

    @Override
    public Optional<BaseUserEntity> findBaseUserById(String userId) {
        return storage
            .stream()
            .filter(user -> userId.equals(user.getId()))
            .findFirst();
    }

    @Override
    public Set<BaseUserEntity> findBaseUsersByIds(List<String> userIds) {
        return storage
            .stream()
            .filter(user -> userIds.contains(user.getId()))
            .collect(Collectors.toSet());
    }

    @Override
    public BaseUserEntity getBaseUser(String userId) {
        return storage
            .stream()
            .filter(user -> userId.equals(user.getId()))
            .findFirst()
            .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public void initWith(List<BaseUserEntity> items) {
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<BaseUserEntity> storage() {
        return Collections.unmodifiableList(storage);
    }
}
