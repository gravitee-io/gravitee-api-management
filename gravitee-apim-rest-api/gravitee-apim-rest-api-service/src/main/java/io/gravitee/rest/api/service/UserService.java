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
package io.gravitee.rest.api.service;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.search.UserCriteria;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.configuration.identity.RoleMappingEntity;
import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.JWTHelper.ACTION;
import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize Elamrani (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface UserService {
    UserEntity connect(ExecutionContext executionContext, String userId);

    UserEntity findById(ExecutionContext executionContext, String id, boolean defaultValue);

    default UserEntity findById(ExecutionContext executionContext, String id) {
        return findById(executionContext, id, false);
    }

    Optional<UserEntity> findByEmail(ExecutionContext executionContext, String email);

    UserEntity findByIdWithRoles(ExecutionContext executionContext, String id);

    UserEntity findBySource(ExecutionContext executionContext, String source, String sourceId, boolean loadRoles);

    Set<UserEntity> findByIds(ExecutionContext executionContext, Collection<String> ids);

    Set<UserEntity> findByIds(ExecutionContext executionContext, Collection<String> ids, boolean withUserMetadata);

    UserEntity create(ExecutionContext executionContext, NewExternalUserEntity newExternalUserEntity, boolean addDefaultRole);

    UserEntity update(ExecutionContext executionContext, String userId, UpdateUserEntity updateUserEntity);

    UserEntity update(ExecutionContext executionContext, String userId, UpdateUserEntity updateUserEntity, String newsletterEmail);

    Page<UserEntity> search(ExecutionContext executionContext, String query, Pageable pageable);

    Page<UserEntity> search(ExecutionContext executionContext, UserCriteria criteria, Pageable pageable);

    UserEntity register(ExecutionContext executionContext, NewExternalUserEntity newExternalUserEntity);

    UserEntity register(ExecutionContext executionContext, NewExternalUserEntity newExternalUserEntity, String confirmationPageUrl);

    UserEntity finalizeRegistration(ExecutionContext executionContext, RegisterUserEntity registerUserEntity);

    UserEntity finalizeResetPassword(ExecutionContext executionContext, ResetPasswordUserEntity registerUserEntity);

    UserEntity processRegistration(ExecutionContext executionContext, String userId, boolean accepted);

    PictureEntity getPicture(ExecutionContext executionContext, String id);

    void delete(ExecutionContext executionContext, String id);

    void resetPassword(ExecutionContext executionContext, String id);

    UserEntity resetPasswordFromSourceId(ExecutionContext executionContext, String sourceId, String resetPageUrl);

    Map<String, Object> getTokenRegistrationParams(
        ExecutionContext executionContext,
        UserEntity userEntity,
        String portalUri,
        ACTION action
    );

    Map<String, Object> getTokenRegistrationParams(
        ExecutionContext executionContext,
        UserEntity userEntity,
        String portalUri,
        ACTION action,
        String confirmationPageUrl
    );

    UserEntity create(ExecutionContext executionContext, NewPreRegisterUserEntity newPreRegisterUserEntity);

    UserEntity createOrUpdateUserFromSocialIdentityProvider(
        ExecutionContext executionContext,
        SocialIdentityProviderEntity socialProvider,
        String userInfo
    );

    void updateUserRoles(
        ExecutionContext executionContext,
        String userId,
        MembershipReferenceType referenceType,
        String referenceId,
        List<String> roleIds
    );

    void computeRolesToAddUser(
        ExecutionContext executionContext,
        String username,
        List<RoleMappingEntity> mappings,
        String userInfo,
        Set<RoleEntity> rolesToAddToOrganization,
        Map<String, Set<RoleEntity>> rolesToAddToEnvironments
    );
}
