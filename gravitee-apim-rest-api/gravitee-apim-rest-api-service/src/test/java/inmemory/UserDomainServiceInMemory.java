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

import io.gravitee.apim.core.user.domain_service.UserDomainService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDomainServiceInMemory implements UserDomainService, InMemoryAlternative<BaseUserEntity> {

    private final List<BaseUserEntity> storage = new ArrayList<>();

    @Override
    public Optional<BaseUserEntity> findBySource(String organizationId, String source, String sourceId) {
        return storage
            .stream()
            .filter(userEntity ->
                userEntity.getOrganizationId().equals(organizationId) &&
                userEntity.getSource().equals(source) &&
                userEntity.getSourceId().equals(sourceId)
            )
            .findFirst();
    }

    public void initWith(List<BaseUserEntity> users) {
        storage.addAll(users);
    }

    @Override
    public void reset() {
        this.storage.clear();
    }

    @Override
    public List<BaseUserEntity> storage() {
        return this.storage;
    }
}
