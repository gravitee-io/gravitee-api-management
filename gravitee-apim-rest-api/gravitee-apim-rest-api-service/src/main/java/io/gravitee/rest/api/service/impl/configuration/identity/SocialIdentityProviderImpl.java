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
package io.gravitee.rest.api.service.impl.configuration.identity;

import io.gravitee.rest.api.model.configuration.identity.*;
import io.gravitee.rest.api.model.configuration.identity.am.AMIdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.github.GitHubIdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.google.GoogleIdentityProviderEntity;
import io.gravitee.rest.api.model.configuration.identity.oidc.OIDCIdentityProviderEntity;
import io.gravitee.rest.api.service.SocialIdentityProviderService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class SocialIdentityProviderImpl extends AbstractService implements SocialIdentityProviderService {

    private final Logger LOGGER = LoggerFactory.getLogger(SocialIdentityProviderImpl.class);

    // Pattern reuse for duplicate slash removal
    private static final Pattern DUPLICATE_SLASH_REMOVER = Pattern.compile("(?<!(http:|https:))[//]+");

    private static final String URI_PATH_SEPARATOR = "/";

    private static final String CLIENT_ID = "clientId";
    private static final String CLIENT_SECRET = "clientSecret";

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private IdentityProviderActivationService identityProviderActivationService;

    @Override
    public Set<SocialIdentityProviderEntity> findAll(
        final ExecutionContext executionContext,
        IdentityProviderActivationService.ActivationTarget target
    ) {
        try {
            Set<String> allIdpByTarget = identityProviderActivationService
                .findAllByTarget(target)
                .stream()
                .map(IdentityProviderActivationEntity::getIdentityProvider)
                .collect(Collectors.toSet());

            Stream<IdentityProviderEntity> identityProviderEntityStream = identityProviderService
                .findAll(executionContext)
                .stream()
                .filter(idp -> allIdpByTarget.contains(idp.getId()));

            if (target.getReferenceType() == IdentityProviderActivationReferenceType.ENVIRONMENT) {
                identityProviderEntityStream = identityProviderEntityStream.filter(IdentityProviderEntity::isEnabled);
            }

            return identityProviderEntityStream
                .sorted((idp1, idp2) -> String.CASE_INSENSITIVE_ORDER.compare(idp1.getName(), idp2.getName()))
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (Exception ex) {
            LOGGER.error("An error occurs while trying to retrieve identity providers", ex);
            throw new TechnicalManagementException("An error occurs while trying to retrieve identity providers", ex);
        }
    }

    @Override
    public SocialIdentityProviderEntity findById(String id, IdentityProviderActivationService.ActivationTarget target) {
        try {
            LOGGER.debug("Find identity provider by ID: {}", id);

            Set<String> allIdpByTarget = identityProviderActivationService
                .findAllByTarget(target)
                .stream()
                .map(IdentityProviderActivationEntity::getIdentityProvider)
                .collect(Collectors.toSet());

            if (!allIdpByTarget.contains(id)) {
                throw new IdentityProviderNotFoundException(id);
            }

            IdentityProviderEntity identityProvider = identityProviderService.findById(id);

            if (target.getReferenceType() == IdentityProviderActivationReferenceType.ENVIRONMENT && !identityProvider.isEnabled()) {
                throw new IdentityProviderNotFoundException(identityProvider.getId());
            }

            return convert(identityProvider);
        } catch (IdentityProviderNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            LOGGER.error("An error occurs while trying to find an identity provider using its ID {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to delete an identity provider using its ID " + id, ex);
        }
    }

    private SocialIdentityProviderEntity convert(IdentityProviderEntity identityProvider) {
        SocialIdentityProviderEntity provider = null;

        if (identityProvider.getType() == IdentityProviderType.GOOGLE) {
            provider = new GoogleIdentityProviderEntity();
        } else if (identityProvider.getType() == IdentityProviderType.GITHUB) {
            provider = new GitHubIdentityProviderEntity();
        } else if (identityProvider.getType() == IdentityProviderType.OIDC) {
            provider = new OIDCIdentityProviderEntity();

            ((OIDCIdentityProviderEntity) provider).setColor((String) identityProvider.getConfiguration().get("color"));
            ((OIDCIdentityProviderEntity) provider).setDiscoveryEndpoint(
                    (String) identityProvider.getConfiguration().get("discoveryEndpoint")
                );
            ((OIDCIdentityProviderEntity) provider).setTokenEndpoint((String) identityProvider.getConfiguration().get("tokenEndpoint"));
            ((OIDCIdentityProviderEntity) provider).setAuthorizationEndpoint(
                    (String) identityProvider.getConfiguration().get("authorizeEndpoint")
                );
            ((OIDCIdentityProviderEntity) provider).setTokenIntrospectionEndpoint(
                    (String) identityProvider.getConfiguration().get("tokenIntrospectionEndpoint")
                );
            ((OIDCIdentityProviderEntity) provider).setUserInfoEndpoint(
                    (String) identityProvider.getConfiguration().get("userInfoEndpoint")
                );
            ((OIDCIdentityProviderEntity) provider).setUserLogoutEndpoint(
                    (String) identityProvider.getConfiguration().get("userLogoutEndpoint")
                );
            ((OIDCIdentityProviderEntity) provider).setScopes((List<String>) identityProvider.getConfiguration().get("scopes"));
            ((OIDCIdentityProviderEntity) provider).setUserProfileMapping(identityProvider.getUserProfileMapping());
        } else if (identityProvider.getType() == IdentityProviderType.GRAVITEEIO_AM) {
            String serverBaseUrl = (String) identityProvider.getConfiguration().get("serverURL");
            String domain = (String) identityProvider.getConfiguration().get("domain");

            // Remove duplicate slash
            String serverUrl = DUPLICATE_SLASH_REMOVER.matcher(serverBaseUrl + '/' + domain).replaceAll(URI_PATH_SEPARATOR);
            if (serverUrl.lastIndexOf(URI_PATH_SEPARATOR) == serverUrl.length() - 1) {
                serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
            }

            provider = new AMIdentityProviderEntity(serverUrl);
            ((AMIdentityProviderEntity) provider).setColor((String) identityProvider.getConfiguration().get("color"));
            ((AMIdentityProviderEntity) provider).setDiscoveryEndpoint(
                    (String) identityProvider.getConfiguration().get("discoveryEndpoint")
                );
            ((AMIdentityProviderEntity) provider).setScopes((List<String>) identityProvider.getConfiguration().get("scopes"));
            ((AMIdentityProviderEntity) provider).setUserProfileMapping(identityProvider.getUserProfileMapping());
        }

        if (provider != null) {
            provider.setId(identityProvider.getId());
            provider.setName(identityProvider.getName());
            provider.setDescription(identityProvider.getDescription());
            provider.setClientId((String) identityProvider.getConfiguration().get(CLIENT_ID));
            provider.setClientSecret((String) identityProvider.getConfiguration().get(CLIENT_SECRET));
            provider.setGroupMappings(identityProvider.getGroupMappings());
            provider.setRoleMappings(identityProvider.getRoleMappings());
            provider.setEmailRequired(identityProvider.isEmailRequired());
            provider.setSyncMappings(identityProvider.isSyncMappings());
            return provider;
        }

        return null;
    }
}
