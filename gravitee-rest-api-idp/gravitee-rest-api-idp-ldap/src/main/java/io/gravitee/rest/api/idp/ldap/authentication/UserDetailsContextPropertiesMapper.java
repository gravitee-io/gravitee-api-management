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
package io.gravitee.rest.api.idp.ldap.authentication;

import io.gravitee.rest.api.idp.ldap.LdapIdentityProvider;
import io.gravitee.rest.api.idp.ldap.utils.LdapUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserDetailsContextPropertiesMapper implements UserDetailsContextMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDetailsContextPropertiesMapper.class);

    private static final String LDAP_ATTRIBUTE_UID = "uid";
    private static final String LDAP_ATTRIBUTE_FIRSTNAME = "givenName";
    private static final String LDAP_ATTRIBUTE_LASTNAME = "sn";
    private static final String LDAP_ATTRIBUTE_MAIL = "mail";

    private Environment environment;

    private String identifierAttribute;

    public void afterPropertiesSet() throws Exception {
        String searchFilter = environment.getProperty("authentication.user.filter");

        if (searchFilter != null) {
            // Search filter can be uid={0} or mail={0}
            identifierAttribute = LdapUtils.extractAttribute(searchFilter);
        }

        if (identifierAttribute == null) {
            identifierAttribute = LDAP_ATTRIBUTE_UID;
        }
    }

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authorities) {
        List<GrantedAuthority> mappedAuthorities = new ArrayList<>();
        try {
            for (GrantedAuthority granted : authorities) {
                String mappedAuthority = environment.getProperty("authentication.group.role.mapper." + granted.getAuthority());
                if (mappedAuthority != null && !mappedAuthority.isEmpty()) {
                    mappedAuthorities.add(new SimpleGrantedAuthority(mappedAuthority));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load mapped authorities", e);
        }

        io.gravitee.rest.api.idp.api.authentication.UserDetails userDetails = new io.gravitee.rest.api.idp.api.authentication.UserDetails(
            ctx.getStringAttribute(identifierAttribute),
            "",
            mappedAuthorities
        );

        String userPhotoAttribute = environment.getProperty("authentication.user.photo-attribute");
        if (userPhotoAttribute == null) {
            userPhotoAttribute = "jpegPhoto";
        }

        userDetails.setFirstname(ctx.getStringAttribute(LDAP_ATTRIBUTE_FIRSTNAME));
        userDetails.setLastname(ctx.getStringAttribute(LDAP_ATTRIBUTE_LASTNAME));
        userDetails.setEmail(ctx.getStringAttribute(LDAP_ATTRIBUTE_MAIL));
        userDetails.setSource(LdapIdentityProvider.PROVIDER_TYPE);
        userDetails.setSourceId(ctx.getNameInNamespace());
        userDetails.setPicture((byte[]) ctx.getObjectAttribute(userPhotoAttribute));

        return userDetails;
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new UnsupportedOperationException(
            "UserDetailsContextPropertiesMapper only supports reading from a context. Please" +
            "use a subclass if mapUserToContext() is required."
        );
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
