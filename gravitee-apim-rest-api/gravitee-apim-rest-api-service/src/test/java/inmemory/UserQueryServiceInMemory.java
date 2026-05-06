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

import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.apim.core.user.model.User;
import io.gravitee.apim.core.user.model.UserSearchQuery;
import io.gravitee.apim.core.user.query_service.UserQueryService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class UserQueryServiceInMemory implements UserQueryService, InMemoryAlternative<BaseUserEntity> {

    final List<BaseUserEntity> storage;

    public UserQueryServiceInMemory() {
        this.storage = new ArrayList<>();
    }

    public UserQueryServiceInMemory(UserCrudServiceInMemory userCrudService) {
        this.storage = userCrudService.storage;
    }

    @Override
    public List<User> search(UserSearchQuery searchQuery) {
        var normalizedQuery = "*".equals(searchQuery.query()) ? "" : searchQuery.query().toLowerCase(Locale.ROOT);
        return storage
            .stream()
            .filter(user -> normalizedQuery.isBlank() || matches(user, normalizedQuery))
            .map(this::toUser)
            .toList();
    }

    private boolean matches(BaseUserEntity user, String query) {
        return contains(user.displayName(), query) || contains(user.getEmail(), query) || contains(user.getSourceId(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private User toUser(BaseUserEntity user) {
        return User.builder()
            .id(user.getId())
            .reference(user.getSourceId())
            .displayName(user.displayName())
            .firstName(user.getFirstname())
            .lastName(user.getLastname())
            .email(user.getEmail())
            .build();
    }

    @Override
    public void initWith(List<BaseUserEntity> items) {
        storage.clear();
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
