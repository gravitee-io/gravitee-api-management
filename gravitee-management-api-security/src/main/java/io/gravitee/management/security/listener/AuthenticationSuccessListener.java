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
package io.gravitee.management.security.listener;

import io.gravitee.management.idp.api.authentication.UserDetails;
import io.gravitee.management.model.NewExternalUserEntity;
import io.gravitee.management.model.RoleEntity;
import io.gravitee.management.model.UpdateUserEntity;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.model.permissions.RoleScope;
import io.gravitee.management.model.permissions.SystemRole;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.RoleService;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.exceptions.RoleNotFoundException;
import io.gravitee.management.service.exceptions.UserNotFoundException;
import io.gravitee.repository.management.model.MembershipDefaultReferenceId;
import io.gravitee.repository.management.model.MembershipReferenceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AuthenticationSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {

    @Autowired
    private UserService userService;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private RoleService roleService;

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        final UserDetails details = (UserDetails) event.getAuthentication().getPrincipal();

        try {
            UserEntity registeredUser = userService.findByUsername(details.getUsername(), false);
            updateRegisteredUser(registeredUser, details);
            // Principal username is the technical identifier of the user
            details.setUsername(registeredUser.getId());
        } catch (UserNotFoundException unfe) {
            final NewExternalUserEntity newUser = new NewExternalUserEntity();
            newUser.setUsername(details.getUsername());
            newUser.setSource(details.getSource());
            newUser.setSourceId(details.getSourceId());
            newUser.setFirstname(details.getFirstname());
            newUser.setLastname(details.getLastname());
            newUser.setEmail(details.getEmail());

            boolean addDefaultRole = false;
            if (event.getAuthentication().getAuthorities() == null || event.getAuthentication().getAuthorities().isEmpty()) {
                addDefaultRole = true;
            }
            UserEntity createdUser = userService.create(newUser, addDefaultRole);
            // Principal username is the technical identifier of the user
            details.setUsername(createdUser.getId());

            if (!addDefaultRole) {
                addRole(RoleScope.MANAGEMENT, createdUser.getId(), event.getAuthentication().getAuthorities());
                addRole(RoleScope.PORTAL, createdUser.getId(), event.getAuthentication().getAuthorities());
            }
        }


        userService.connect(details.getUsername());
    }

    private void updateRegisteredUser(UserEntity registeredUser, UserDetails details) {

        if ((registeredUser.getFirstname() != null && !registeredUser.getFirstname().equals(details.getFirstname()))
                || (registeredUser.getLastname() != null && !registeredUser.getLastname().equals(details.getLastname()))
                || (registeredUser.getEmail() != null && !registeredUser.getEmail().equals(details.getEmail()))) {
            UpdateUserEntity updateUserEntity = new UpdateUserEntity(registeredUser);
            updateUserEntity.setFirstname(details.getFirstname());
            updateUserEntity.setLastname(details.getLastname());
            updateUserEntity.setEmail(details.getEmail());
            userService.update(updateUserEntity);
        }
    }

    /**
     * Authorities could be ADMIN, ROLE, SCOPE:ROLE
     * Priority is:
     * 1 - ADMIN
     * 2 - SCOPE:ROLE
     * 3 - ROLE
     * @param roleScope the scope we're looking for
     * @param authorities the authorities to parse
     * @return the role
     */
    private String getRoleFromAuthorities(RoleScope roleScope, Collection<? extends GrantedAuthority> authorities) {
        String globalRole = null;
        String specificRole = null;
        for (GrantedAuthority grantedAuthority : authorities) {
            String authority = grantedAuthority.getAuthority();
            if (SystemRole.ADMIN.name().equals(authority)) {
                return authority;
            }
            if (authority.contains(":")) {
                String[] scopeAndName = authority.split(":");
                if (roleScope.name().equals(scopeAndName[0])) {
                    specificRole = scopeAndName[1];
                }
            } else {
                globalRole = authority;
            }
        }
        return specificRole != null ? specificRole : globalRole;
    }

    /**
     * add a role to a user.
     * If no role found (not provided or no exist), the defaul role is set.
     * if no role set, throw an IllegalArgumentException
     * @param roleScope
     * @param userId
     * @param authorities
     */
    private void addRole(RoleScope roleScope, String userId, Collection<? extends GrantedAuthority> authorities) {
        String roleName;
        String managementRole = getRoleFromAuthorities(roleScope, authorities);
        if (managementRole != null && !SystemRole.ADMIN.name().equals(managementRole)) {
            try {
                roleName = roleService.findById(convertToRepositoryRoleScope(roleScope), managementRole).getName();
            }
            catch (RoleNotFoundException notFoundException) {
                Optional<RoleEntity> first = roleService.findDefaultRoleByScopes(convertToRepositoryRoleScope(roleScope)).stream().findFirst();
                if (first.isPresent()) {
                    roleName = first.get().getName();
                } else {
                    throw new IllegalArgumentException("No default role exist for scope MANAGEMENT");
                }
            }
        } else if (!SystemRole.ADMIN.name().equals(managementRole)) {
            Optional<RoleEntity> first = roleService.findDefaultRoleByScopes(convertToRepositoryRoleScope(roleScope)).stream().findFirst();
            if (first.isPresent()) {
                roleName = first.get().getName();
            } else {
                throw new IllegalArgumentException("No default role exist for scope MANAGEMENT");
            }
        } else {
            roleName = managementRole;
        }

        membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(convertToMembershipReferenceType(roleScope), MembershipDefaultReferenceId.DEFAULT.name()),
                new MembershipService.MembershipUser(userId, null),
                new MembershipService.MembershipRole(convertToRepositoryRoleScope(roleScope), roleName));
    }

    /**
     * convert io.gravitee.management.model.permissions.RoleScope to io.gravitee.repository.management.model.RoleScope
     * @param roleScope
     * @return
     */
    private io.gravitee.repository.management.model.RoleScope convertToRepositoryRoleScope(RoleScope roleScope) {
        if (RoleScope.MANAGEMENT.equals(roleScope)) {
            return io.gravitee.repository.management.model.RoleScope.MANAGEMENT;
        } else {
            return io.gravitee.repository.management.model.RoleScope.PORTAL;
        }
    }

    /**
     * convert io.gravitee.management.model.permissions.RoleScope to io.gravitee.repository.management.model.MembershipReferenceType
     * @param roleScope
     * @return
     */
    private MembershipReferenceType convertToMembershipReferenceType(RoleScope roleScope) {
        if (RoleScope.MANAGEMENT.equals(roleScope)) {
            return MembershipReferenceType.MANAGEMENT;
        } else {
            return MembershipReferenceType.PORTAL;
        }
    }
}
