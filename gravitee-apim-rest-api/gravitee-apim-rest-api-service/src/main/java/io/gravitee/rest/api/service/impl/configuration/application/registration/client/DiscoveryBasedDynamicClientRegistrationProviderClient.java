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
package io.gravitee.rest.api.service.impl.configuration.application.registration.client;

import io.gravitee.rest.api.service.impl.configuration.application.registration.client.discovery.DiscoveryResponse;
import io.gravitee.rest.api.service.impl.configuration.application.registration.client.token.InitialAccessTokenProvider;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DiscoveryBasedDynamicClientRegistrationProviderClient extends DynamicClientRegistrationProviderClient {

    private final String discoveryEndpoint;
    private final InitialAccessTokenProvider initialAccessTokenProvider;
    private final Map<String, String> attributes = new HashMap<>();
    private final Map<String, Object> metadata = new HashMap<>();

    public DiscoveryBasedDynamicClientRegistrationProviderClient(
        String discoveryEndpoint,
        InitialAccessTokenProvider initialAccessTokenProvider
    ) {
        this.discoveryEndpoint = discoveryEndpoint;
        this.initialAccessTokenProvider = initialAccessTokenProvider;
        initialize();
    }

    private void initialize() {
        httpClient = HttpClients.createDefault();

        try {
            DiscoveryResponse discovery = httpClient.execute(
                new HttpGet(discoveryEndpoint),
                response -> {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();

                        if (entity != null) {
                            return mapper.readValue(EntityUtils.toString(entity), DiscoveryResponse.class);
                        } else {
                            throw new DynamicClientRegistrationException("OIDC Discovery response is not well-formed");
                        }
                    } else {
                        logger.error(
                            "Unexpected response status from OIDC Discovery endpoint: status[{}] message[{}]",
                            status,
                            EntityUtils.toString(response.getEntity())
                        );
                        throw new DynamicClientRegistrationException("Unexpected response status from OIDC Discovery endpoint");
                    }
                }
            );

            registrationEndpoint = discovery.getRegistrationEndpoint();
            attributes.put("token_endpoint", discovery.getTokenEndpoint());
            metadata.put("registration_endpoint", discovery.getRegistrationEndpoint());
            metadata.put("token_endpoint", discovery.getTokenEndpoint());
        } catch (Exception ex) {
            logger.error("Unexpected error while getting OIDC metadata from Discovery endpoint: " + ex.getMessage(), ex);
            throw new DynamicClientRegistrationException(
                "Unexpected error while getting OIDC metadata from Discovery endpoint: " + ex.getMessage(),
                ex
            );
        }
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String getInitialAccessToken() {
        return initialAccessTokenProvider.get(attributes);
    }
}
