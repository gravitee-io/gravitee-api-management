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
package io.gravitee.rest.api.service.impl.configuration.application.registration.client;

import io.gravitee.rest.api.model.configuration.application.registration.KeyStoreEntity;
import io.gravitee.rest.api.model.configuration.application.registration.TrustStoreEntity;
import io.gravitee.rest.api.service.impl.configuration.application.registration.client.common.SecureHttpClientUtils;
import io.gravitee.rest.api.service.impl.configuration.application.registration.client.token.InitialAccessTokenProvider;
import java.util.Collections;

/**
 * HTTP Bridge client for custom IdPs that don't natively support OIDC DCR.
 * Instead of OIDC discovery, this client directly targets a user-provided
 * HTTP endpoint (the "bridge") that translates between the standard DCR
 * request format and the custom IdP's API.
 *
 * <p>The bridge endpoint is expected to accept standard RFC 7591 DCR payloads
 * and return standard DCR responses. The bridge implementation handles the
 * translation to whatever format the custom IdP requires.</p>
 *
 * <p>This pattern is inspired by Kong Konnect's HTTP DCR bridge approach.</p>
 */
public class HttpBridgeDynamicClientRegistrationProviderClient extends DynamicClientRegistrationProviderClient {

    private final String bridgeEndpoint;
    private final InitialAccessTokenProvider initialAccessTokenProvider;
    private final TrustStoreEntity trustStore;
    private final KeyStoreEntity keyStore;

    public HttpBridgeDynamicClientRegistrationProviderClient(
        String bridgeEndpoint,
        InitialAccessTokenProvider initialAccessTokenProvider,
        TrustStoreEntity trustStore,
        KeyStoreEntity keyStore
    ) {
        this.bridgeEndpoint = bridgeEndpoint;
        this.initialAccessTokenProvider = initialAccessTokenProvider;
        this.trustStore = trustStore;
        this.keyStore = keyStore;
        initialize();
    }

    private void initialize() {
        try {
            httpClient = SecureHttpClientUtils.createHttpClient(trustStore, keyStore);
            this.registrationEndpoint = bridgeEndpoint;
        } catch (Exception ex) {
            log.error("An error occurred while initializing HTTP bridge DCR client for endpoint {}", bridgeEndpoint, ex);
            throw new DynamicClientRegistrationException(
                "An error occurred while initializing HTTP bridge DCR client for endpoint " + bridgeEndpoint, ex
            );
        }
    }

    @Override
    public String getInitialAccessToken() {
        return initialAccessTokenProvider.get(Collections.emptyMap(), this.trustStore, this.keyStore);
    }
}
