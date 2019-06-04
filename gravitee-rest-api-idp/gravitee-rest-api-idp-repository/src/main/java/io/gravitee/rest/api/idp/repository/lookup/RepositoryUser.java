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
package io.gravitee.rest.api.idp.repository.lookup;

import io.gravitee.rest.api.idp.api.identity.User;
import io.gravitee.rest.api.idp.repository.RepositoryIdentityProvider;
import io.gravitee.rest.api.model.UserEntity;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RepositoryUser implements User {

    private final UserEntity userEntity;

    RepositoryUser(UserEntity userEntity) {
        this.userEntity = userEntity;
    }

    @Override
    public String getEmail() {
        return userEntity.getEmail();
    }

    @Override
    public String getDisplayName() {
        return userEntity.getDisplayName();
    }

    @Override
    public String getFirstname() {
        return userEntity.getFirstname();
    }

    @Override
    public String getId() {
        return userEntity.getId();
    }

    @Override
    public String getLastname() {
        return userEntity.getLastname();
    }

    @Override
    public String getSource() {
        return RepositoryIdentityProvider.PROVIDER_TYPE;
    }
}