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
package io.gravitee.rest.api.security.listener;

import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Optional;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AuthenticationSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationSuccessListener.class);

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
            UserEntity registeredUser = userService.findBySource(details.getSource(), details.getSourceId(), false);
            updateRegisteredUser(registeredUser, details);
            // Principal username is the technical identifier of the user
            // Dirty hack because spring security is requiring a username...
            details.setUsername(registeredUser.getId());
            // Allows to override email of in memory users
            if ("memory".equals(details.getSource()) && registeredUser.getEmail() != null) {
                details.setEmail(registeredUser.getEmail());
                SecurityContextHolder.getContext().setAuthentication(event.getAuthentication());
            }
        } catch (UserNotFoundException unfe) {
            final NewExternalUserEntity newUser = new NewExternalUserEntity();
            newUser.setSource(details.getSource());
            newUser.setSourceId(details.getSourceId());
            newUser.setFirstname(details.getFirstname());
            newUser.setLastname(details.getLastname());
            newUser.setEmail(details.getEmail());

            byte[] pictureData = details.getPicture();
            if (pictureData != null && pictureData.length > 0) {
                String picture = computePicture(pictureData);
                newUser.setPicture(picture);
            }

            boolean addDefaultRole = false;
            if (event.getAuthentication().getAuthorities() == null || event.getAuthentication().getAuthorities().isEmpty()) {
                addDefaultRole = true;
            }
            UserEntity createdUser = userService.create(newUser, addDefaultRole);
            // Principal username is the technical identifier of the user
            details.setUsername(createdUser.getId());

            if (!addDefaultRole) {
                addRole(RoleScope.ENVIRONMENT, createdUser.getId(), event.getAuthentication().getAuthorities());
                addRole(RoleScope.ORGANIZATION, createdUser.getId(), event.getAuthentication().getAuthorities());
            }
        }

        userService.connect(details.getUsername());
    }

    public String computePicture(final byte[] pictureData) {
        String pictureContent = new String(pictureData);
        if (pictureContent.toUpperCase().startsWith("HTTP")) {
            return pictureContent;
        }

        try {
            String contentType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(pictureData));
            if (contentType != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("data:");
                sb.append(contentType);
                sb.append(";base64,");
                sb.append(StringUtils.newStringUtf8(Base64.encodeBase64(pictureData, false)));
                return sb.toString();
            } else {
                //null contentType means that pictureData is a String but doesn't starts with HTTP
                LOGGER.warn("Unable to compute the user picture from URL.");
            }
        } catch (IOException e) {
            LOGGER.warn("Problem while parsing picture", e);
        }

        return null;
    }

    private void updateRegisteredUser(UserEntity registeredUser, UserDetails details) {
        if (
            (details.getFirstname() != null && !details.getFirstname().equals(registeredUser.getFirstname())) ||
            (details.getLastname() != null && !details.getLastname().equals(registeredUser.getLastname())) ||
            (details.getEmail() != null && !details.getEmail().equals(registeredUser.getEmail()))
        ) {
            UpdateUserEntity updateUserEntity = new UpdateUserEntity(registeredUser);
            updateUserEntity.setFirstname(details.getFirstname());
            updateUserEntity.setLastname(details.getLastname());
            updateUserEntity.setEmail(details.getEmail());
            userService.update(registeredUser.getId(), updateUserEntity);
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
     * If no role found (not provided or no exist), the default role is set.
     * if no role set, throw an IllegalArgumentException
     * @param roleScope
     * @param userId
     * @param authorities
     */
    private void addRole(RoleScope roleScope, String userId, Collection<? extends GrantedAuthority> authorities) {
        String roleName;
        String role = getRoleFromAuthorities(roleScope, authorities);
        if (role != null && !SystemRole.ADMIN.name().equals(role)) {
            Optional<RoleEntity> optionalRole = roleService.findByScopeAndName(roleScope, role);
            if (optionalRole.isPresent()) {
                roleName = optionalRole.get().getName();
            } else {
                Optional<RoleEntity> first = roleService.findDefaultRoleByScopes(roleScope).stream().findFirst();
                if (first.isPresent()) {
                    roleName = first.get().getName();
                } else {
                    throw new IllegalArgumentException("No default role exist for scope " + roleScope.name());
                }
            }
        } else if (!SystemRole.ADMIN.name().equals(role)) {
            Optional<RoleEntity> first = roleService.findDefaultRoleByScopes(roleScope).stream().findFirst();
            if (first.isPresent()) {
                roleName = first.get().getName();
            } else {
                throw new IllegalArgumentException("No default role exist for scope " + roleScope.name());
            }
        } else {
            roleName = role;
        }

        MembershipService.MembershipReference membershipRef;
        if (roleScope == RoleScope.ENVIRONMENT) {
            membershipRef =
                new MembershipService.MembershipReference(MembershipReferenceType.ENVIRONMENT, GraviteeContext.getCurrentEnvironment());
        } else {
            membershipRef =
                new MembershipService.MembershipReference(MembershipReferenceType.ORGANIZATION, GraviteeContext.getCurrentOrganization());
        }
        membershipService.addRoleToMemberOnReference(
            membershipRef,
            new MembershipService.MembershipMember(userId, null, MembershipMemberType.USER),
            new MembershipService.MembershipRole(roleScope, roleName)
        );
    }
}
