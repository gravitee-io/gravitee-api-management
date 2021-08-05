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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import io.gravitee.rest.api.portal.rest.model.IdentityProvider;
import io.gravitee.rest.api.portal.rest.model.IdentityProviderType;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class IdentityProviderMapper {

    public IdentityProvider convert(SocialIdentityProviderEntity socialIdentityProviderEntity) {
        IdentityProvider idpItem = new IdentityProvider();
        idpItem.setAuthorizationEndpoint(socialIdentityProviderEntity.getAuthorizationEndpoint());
        idpItem.setClientId(socialIdentityProviderEntity.getClientId());
        idpItem.setColor(socialIdentityProviderEntity.getColor());
        idpItem.setDescription(socialIdentityProviderEntity.getDescription());
        idpItem.setDisplay(socialIdentityProviderEntity.getDisplay());
        idpItem.setEmailRequired(socialIdentityProviderEntity.isEmailRequired());
        idpItem.setId(socialIdentityProviderEntity.getId());
        idpItem.setName(socialIdentityProviderEntity.getName());
        idpItem.setOptionalUrlParams(socialIdentityProviderEntity.getOptionalUrlParams());
        idpItem.setRequiredUrlParams(socialIdentityProviderEntity.getRequiredUrlParams());
        idpItem.setScopes(socialIdentityProviderEntity.getScopes());
        idpItem.setTokenIntrospectionEndpoint(socialIdentityProviderEntity.getTokenIntrospectionEndpoint());
        idpItem.setType(IdentityProviderType.fromValue(socialIdentityProviderEntity.getType().name()));
        idpItem.setUserLogoutEndpoint(socialIdentityProviderEntity.getUserLogoutEndpoint());

        return idpItem;
    }
}
