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
package io.gravitee.management.service;

import io.gravitee.common.data.domain.Page;
import io.gravitee.management.model.*;
import io.gravitee.management.model.common.Pageable;
import io.gravitee.repository.management.api.search.UserCriteria;

import java.util.List;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface UserService {

    UserEntity connect(String userId);
    UserEntity findById(String id);
    UserEntity findByIdWithRoles(String id);
    UserEntity findByUsername(String username, boolean loadRoles);
    Set<UserEntity> findByIds(List<String> ids);
    UserEntity create(RegisterUserEntity registerUserEntity);
    UserEntity create(NewExternalUserEntity newExternalUserEntity, boolean addDefaultRole);
    UserEntity update(UpdateUserEntity updateUserEntity);
    Page<UserEntity> search(Pageable pageable);
    Page<UserEntity> search(UserCriteria criteria, Pageable pageable);
    UserEntity register(NewExternalUserEntity newExternalUserEntity);
    PictureEntity getPicture(String id);
    void delete(String id);
    void resetPassword(String id);
}
