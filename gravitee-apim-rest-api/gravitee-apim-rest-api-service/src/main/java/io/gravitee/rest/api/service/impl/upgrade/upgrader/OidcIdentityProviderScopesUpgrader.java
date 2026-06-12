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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IdentityProviderRepository;
import io.gravitee.repository.management.model.IdentityProvider;
import io.gravitee.repository.management.model.IdentityProviderType;
import java.util.HashMap;
import java.util.List;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@CustomLog
public class OidcIdentityProviderScopesUpgrader implements Upgrader {

    static final List<String> DEFAULT_SCOPES = List.of("openid", "profile", "email");

    @Lazy
    @Autowired
    private IdentityProviderRepository identityProviderRepository;

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::applyUpgrade);
    }

    private boolean applyUpgrade() throws TechnicalException {
        int count = 0;
        for (var idp : identityProviderRepository.findAll()) {
            if (needsDefaultScopes(idp)) {
                var config = idp.getConfiguration() != null ? new HashMap<>(idp.getConfiguration()) : new HashMap<String, Object>();
                config.put("scopes", DEFAULT_SCOPES);
                idp.setConfiguration(config);
                identityProviderRepository.update(idp);
                count++;
            }
        }
        if (count > 0) {
            log.info("{} backfilled default scopes for {} OIDC identity providers", getClass().getSimpleName(), count);
        }
        return true;
    }

    private boolean needsDefaultScopes(IdentityProvider idp) {
        if (idp.getType() != IdentityProviderType.OIDC) {
            return false;
        }
        var config = idp.getConfiguration();
        if (config == null) {
            return true;
        }
        var raw = config.get("scopes");
        return !(raw instanceof List<?> scopes) || scopes.isEmpty();
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.OIDC_IDP_DEFAULT_SCOPES_UPGRADER;
    }
}
