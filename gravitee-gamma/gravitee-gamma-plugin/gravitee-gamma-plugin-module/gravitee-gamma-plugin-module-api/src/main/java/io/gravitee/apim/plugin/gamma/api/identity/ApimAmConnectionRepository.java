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
package io.gravitee.apim.plugin.gamma.api.identity;

import io.gravitee.rest.api.model.AmConnectionEntity;
import io.gravitee.rest.api.service.AmConnectionService;
import java.util.Optional;

// Delegates AM connection storage + token encryption to APIM rest-api core (AmConnectionService),
// which is injected from the plugin's parent Spring context.
public class ApimAmConnectionRepository implements AmConnectionRepository {

    private final AmConnectionService amConnectionService;

    public ApimAmConnectionRepository(AmConnectionService amConnectionService) {
        this.amConnectionService = amConnectionService;
    }

    @Override
    public Optional<AmConnection> findByOrg(String orgId) {
        return amConnectionService.findByOrganizationId(orgId).map(this::toModel);
    }

    @Override
    public boolean hasTokenForOrg(String orgId) {
        return amConnectionService.hasToken(orgId);
    }

    @Override
    public AmConnection save(String orgId, AmConnection connection) {
        return toModel(amConnectionService.save(orgId, toEntity(orgId, connection)));
    }

    private AmConnection toModel(AmConnectionEntity e) {
        return new AmConnection(
            e.getBaseUrl(),
            e.getServiceAccountAccessToken(),
            e.getEnvironmentId(),
            e.getDefaultDomainId(),
            e.getDefaultDomainHrid(),
            e.getGatewayUrl()
        );
    }

    private AmConnectionEntity toEntity(String orgId, AmConnection c) {
        AmConnectionEntity e = new AmConnectionEntity();
        e.setOrganizationId(orgId);
        e.setBaseUrl(c.baseUrl());
        e.setServiceAccountAccessToken(c.serviceAccountAccessToken());
        e.setEnvironmentId(c.environmentId());
        e.setDefaultDomainId(c.defaultDomainId());
        e.setDefaultDomainHrid(c.defaultDomainHrid());
        e.setGatewayUrl(c.gatewayUrl());
        return e;
    }
}
